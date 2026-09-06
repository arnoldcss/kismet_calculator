package com.kismetcalc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class KismetConfig {

    private static final Path FILE = Path.of("config", "kismetcalc.properties");

    public static boolean overlay = true;

    public static boolean breakdown = true;

    public static int meterProgress = 0;

    // 0 means take the key's price from the price book. Anything else overrides it, in coins.
    public static long keyCostOverride = 0L;

    public static boolean priceOffers = false;

    public static boolean gamblingMode = false;

    public static boolean dev = false;

    private KismetConfig() {
    }

    public static void load() {
        if (!Files.isRegularFile(FILE)) return;
        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String text = line.trim();
                if (text.isEmpty() || text.startsWith("#")) continue;
                int equals = text.indexOf('=');
                if (equals <= 0) continue;

                String key = text.substring(0, equals).trim().toLowerCase(Locale.ROOT);
                String value = text.substring(equals + 1).trim();
                switch (key) {
                    case "overlay" -> overlay = Boolean.parseBoolean(value);
                    case "breakdown" -> breakdown = Boolean.parseBoolean(value);
                    case "meter" -> meterProgress = clamp(parseInt(value, meterProgress), 0, 100);
                    case "keycostoverride" -> keyCostOverride = Math.max(0L, parseLong(value, keyCostOverride));
                    case "priceoffers" -> priceOffers = Boolean.parseBoolean(value);
                    case "gamblingmode" -> gamblingMode = Boolean.parseBoolean(value);
                    case "dev" -> dev = Boolean.parseBoolean(value);
                    default -> {
                        // deafult 
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            //defaults
        }
    }

    public static void save() {
        String body = """
            # Kismet Calculator
            # meter is the one to keep current: it is your RNG meter progress towards
            # Necron's Handle, as a percentage, and it moves the reroll threshold a long way.
            overlay=%s
            breakdown=%s
            meter=%d
            keyCostOverride=%d
            priceOffers=%s
            gamblingMode=%s
            dev=%s
            """.formatted(overlay, breakdown, meterProgress, keyCostOverride, priceOffers,
                    gamblingMode, dev);
        try {
            Path parent = FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(FILE, body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            //not needed to handle
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }
}
