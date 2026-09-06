package com.kismetcalc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kismetcalc.core.CombinationTable;
import com.kismetcalc.core.KismetCalculator;
import com.kismetcalc.core.PriceBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

//call every 30m (it is cached server-side but this will restrcit traffic)
public final class PriceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("kismetcalc/prices");

    private static final PriceClient INSTANCE = new PriceClient();

    private static final String ENDPOINT = "https://api.meowpixel.cloud/v1/prices";

    //very weak but whatever atleast nons cant use it to spam :(
    private static final String TOKEN = "FiXqqmVDMXe6NTG4c8v4A6JpS5jmuWCs";

    private static final long REFRESH_MS = 30 * 60 * 1000L;

    private static final long RETRY_MS = 2 * 60 * 1000L;

    private static final int TIMEOUT_MS = 15_000;

    private static final Path CACHE = Path.of("config", "kismetcalc_prices.json");

    private volatile PriceBook book = PriceBook.baked(PriceBook.Basis.INSTANT);
    private volatile KismetCalculator.Analysis analysis;
    private volatile Object analysisFor;

    private volatile long nextFetch;
    private volatile boolean fetching;
    private volatile String lastError;
    private volatile String etag;

    private boolean loadedFromDisk;
    private boolean tableRequested;

    private static volatile String endpointOverride;

    private static final AtomicLong calls = new AtomicLong();
    private static final AtomicLong requests = new AtomicLong();
    private static final AtomicLong notModified = new AtomicLong();
    private static final AtomicLong throttled = new AtomicLong();
    private static final AtomicLong failures = new AtomicLong();
    private static final AtomicLong payloads = new AtomicLong();

    private volatile long lastRequestAt;

    private PriceClient() {
    }

    public static PriceClient getInstance() {
        return INSTANCE;
    }

    public PriceBook book() {
        PriceBook current = book;
        PriceBook.Basis wanted = KismetConfig.priceOffers ? PriceBook.Basis.OFFER : PriceBook.Basis.INSTANT;
        if (current.basis() == wanted) return current;

        PriceBook rebased = current.with(wanted);
        book = rebased;
        return rebased;
    }

    public String lastError() {
        return lastError;
    }

    public KismetCalculator.Analysis analysis() {
        PriceBook current = book();
        KismetCalculator.Analysis cached = analysis;
        if (cached != null && analysisFor == current) return cached;

        if (!CombinationTable.loaded()) {
            requestTable();
            return null;
        }
        KismetCalculator.Analysis fresh = KismetCalculator.analyse(current);
        analysis = fresh;
        analysisFor = current;
        return fresh;
    }

    public void ensureFresh() {
        calls.incrementAndGet();
        long now = System.currentTimeMillis();
        if (fetching || now < nextFetch) return;

        if (!loadedFromDisk) {
            loadedFromDisk = true;
            loadCache();
            if (book.live() && now - book.fetchedAt() < REFRESH_MS) {
                nextFetch = book.fetchedAt() + REFRESH_MS;
                LOGGER.info("prices: disk cache is {}m old, no request; next due in {}m",
                    (now - book.fetchedAt()) / 60_000L, (nextFetch - now) / 60_000L);
                requestTable();
                return;
            }
        }

        fetching = true;
        nextFetch = now + REFRESH_MS;
        lastRequestAt = now;
        requests.incrementAndGet();
        requestTable();

        Thread worker = new Thread(this::fetch, "KismetCalc-Prices");
        worker.setDaemon(true);
        worker.start();
    }

    private void requestTable() {
        if (tableRequested || CombinationTable.loaded()) return;
        tableRequested = true;

        Thread loader = new Thread(CombinationTable::load, "KismetCalc-Table");
        loader.setDaemon(true);
        loader.start();
    }

    private void fetch() {
        try {
            String endpoint = endpoint();
            HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "KismetCalc");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + TOKEN);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            String previous = etag;
            if (previous != null) connection.setRequestProperty("If-None-Match", previous);

            long started = System.currentTimeMillis();
            int status = connection.getResponseCode();
            LOGGER.info("prices: GET {} -> {} in {}ms (request #{}, after {} ensureFresh calls)",
                endpoint, status, System.currentTimeMillis() - started, requests.get(), calls.get());

            // 429 you got throttled
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED || status == 429) {
                if (status == 429) throttled.incrementAndGet(); else notModified.incrementAndGet();
                lastError = null;
                connection.disconnect();
                return;
            }

            JsonObject root = read(connection, status);
            if (root == null || root.has("error")) {
                failures.incrementAndGet();
                lastError = root == null ? "HTTP " + status : root.get("error").getAsString();
                nextFetch = System.currentTimeMillis() + RETRY_MS;
                LOGGER.warn("prices: {} - retrying in {}m", lastError, RETRY_MS / 60_000L);
                return;
            }

            PriceBook parsed = parse(root);
            if (parsed == null) {
                failures.incrementAndGet();
                lastError = "prices came back empty";
                nextFetch = System.currentTimeMillis() + RETRY_MS;
                LOGGER.warn("prices: snapshot carried no items - retrying in {}m", RETRY_MS / 60_000L);
                return;
            }

            String tag = connection.getHeaderField("ETag");
            if (tag != null) etag = tag;

            payloads.incrementAndGet();
            book = parsed;
            lastError = null;
            saveCache(root);
            LOGGER.info("prices: {} items, snapshot {}s old, next request in {}m",
                parsed.size(), (System.currentTimeMillis() - parsed.fetchedAt()) / 1000L,
                REFRESH_MS / 60_000L);
        } catch (IOException | RuntimeException e) {
            failures.incrementAndGet();
            lastError = String.valueOf(e.getMessage());
            nextFetch = System.currentTimeMillis() + RETRY_MS;
            LOGGER.warn("prices: fetch failed ({}) - retrying in {}m", lastError, RETRY_MS / 60_000L);
        } finally {
            fetching = false;
        }
    }

    private String endpoint() {
        String override = endpointOverride;
        return override != null ? override : ENDPOINT;
    }

    static void endpointForTest(String url) {
        endpointOverride = url;
    }

    public String debug() {
        long now = System.currentTimeMillis();
        long due = nextFetch - now;
        PriceBook current = book;
        return String.format(Locale.ROOT,
            "calls=%d requests=%d (304=%d 429=%d fail=%d payload=%d) next=%s age=%s src=%s%s",
            calls.get(), requests.get(), notModified.get(), throttled.get(), failures.get(),
            payloads.get(),
            due <= 0 ? "due" : (due / 1000L) + "s",
            current.live() ? ((now - current.fetchedAt()) / 1000L) + "s" : "-",
            current.live() ? "fetched" : "baked",
            lastError == null ? "" : " lastError=" + lastError);
    }

    public static long requestCount() {
        return requests.get();
    }

    public static long callCount() {
        return calls.get();
    }

    public long lastRequestAt() {
        return lastRequestAt;
    }

    private PriceBook parse(JsonObject root) {
        JsonElement itemsElement = root.get("items");
        if (itemsElement == null || !itemsElement.isJsonObject()) return null;

        Map<String, PriceBook.Entry> entries = new HashMap<>();
        for (Map.Entry<String, JsonElement> item : itemsElement.getAsJsonObject().entrySet()) {
            if (!item.getValue().isJsonObject()) continue;
            JsonObject entry = item.getValue().getAsJsonObject();
            entries.put(item.getKey(), new PriceBook.Entry(
                entry.has("insta") ? entry.get("insta").getAsLong() : 0L,
                entry.has("offer") ? entry.get("offer").getAsLong() : 0L,
                entry.has("src") ? entry.get("src").getAsString() : ""));
        }
        if (entries.isEmpty()) return null;

        long fetched = root.has("fetched") ? root.get("fetched").getAsLong() : System.currentTimeMillis();
        return PriceBook.of(entries, book.basis(), fetched);
    }

    private void saveCache(JsonObject snapshot) {
        try {
            Path parent = CACHE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(CACHE, snapshot.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            //zzz we dont handle
        }
    }

    private void loadCache() {
        if (!Files.isRegularFile(CACHE)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(CACHE, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) return;
            PriceBook cached = parse(root.getAsJsonObject());
            if (cached != null) book = cached;
        } catch (IOException | RuntimeException e) {
           //zzz
        }
    }

    private static JsonObject read(HttpURLConnection connection, int status) {
        try (InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
            if (stream == null) return null;
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
            }
        } catch (IOException | RuntimeException e) {
            return null;
        } finally {
            connection.disconnect();
        }
    }
}
