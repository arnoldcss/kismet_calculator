package com.kismetcalc.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// pricing 
public final class PriceBook {

    public enum Basis {
        INSTANT,
        OFFER
    }

    public record Entry(long insta, long offer, String source) {

        public long by(Basis basis) {
            return basis == Basis.OFFER ? offer : insta;
        }
    }

    private static final Map<String, Long> BAKED = bakedPrices();

    private static final Pattern BOOK_LEVEL = Pattern.compile("^(ENCHANTMENT_.+)_(\\d{1,2})$");

    private static final int COMBINE_REACH = 2;

    private static final Entry NO_ENTRY = new Entry(0L, 0L, "none");

    private final Map<String, Entry> entries;
    private final Basis basis;
    private final long fetchedAt;
    private final boolean live;

    private final Set<String> unknown = ConcurrentHashMap.newKeySet();

    private final Map<String, Entry> combined = new ConcurrentHashMap<>();

    private PriceBook(Map<String, Entry> entries, Basis basis, long fetchedAt, boolean live) {
        this.entries = entries;
        this.basis = basis;
        this.fetchedAt = fetchedAt;
        this.live = live;
    }

    public static PriceBook of(Map<String, Entry> entries, Basis basis, long fetchedAt) {
        Map<String, Entry> copy = new LinkedHashMap<>(entries);
        for (Map.Entry<String, Long> fallback : BAKED.entrySet()) {
            copy.putIfAbsent(fallback.getKey(),
                new Entry(fallback.getValue(), fallback.getValue(), "baked"));
        }
        return new PriceBook(Collections.unmodifiableMap(copy), basis, fetchedAt, true);
    }

    public static PriceBook baked(Basis basis) {
        Map<String, Entry> copy = new LinkedHashMap<>();
        BAKED.forEach((id, price) -> copy.put(id, new Entry(price, price, "baked")));
        return new PriceBook(Collections.unmodifiableMap(copy), basis, 0L, false);
    }

    public PriceBook with(Basis other) {
        return other == basis ? this : new PriceBook(entries, other, fetchedAt, live);
    }

    public Basis basis() {
        return basis;
    }

    public long fetchedAt() {
        return fetchedAt;
    }

    public boolean live() {
        return live;
    }

    public long value(String itemId) {
        Entry entry = entry(itemId);
        if (entry == null) {
            unknown.add(itemId);
            return 0L;
        }
        return entry.by(basis);
    }

    public Entry entry(String itemId) {
        Entry direct = entries.get(itemId);
        if (direct != null) return direct;

        Entry derived = combined.computeIfAbsent(itemId, this::byCombining);
        return derived == NO_ENTRY ? null : derived;
    }

    private Entry byCombining(String itemId) {
        Matcher book = BOOK_LEVEL.matcher(itemId);
        if (!book.matches()) return NO_ENTRY;

        int level;
        try {
            level = Integer.parseInt(book.group(2));
        } catch (NumberFormatException e) {
            return NO_ENTRY;
        }
        String family = book.group(1);

        for (int reach = 1; reach <= COMBINE_REACH; reach++) {
            for (int other : new int[] {level + reach, level - reach}) {
                if (other < 1 || other > 10) continue;
                Entry found = entries.get(family + "_" + other);
                if (found == null) continue;
                return new Entry(halve(found.insta(), level - other),
                    halve(found.offer(), level - other), "combine");
            }
        }
        return NO_ENTRY;
    }

    private static long halve(long price, int steps) {
        return steps >= 0 ? price << steps : price >> -steps;
    }

    public Set<String> unknownIds() {
        return Collections.unmodifiableSet(unknown);
    }

    public int size() {
        return entries.size();
    }

    public long kismetCost() {
        return value(M7LootTable.KISMET_FEATHER);
    }

    // Read at the book's basis like every other price, so it is what a key sells for rather than
    // what one costs to buy. The two sides are ~15% apart and this feeds a comparison, not a sum.
    public long keyCost() {
        return value(M7LootTable.DUNGEON_CHEST_KEY);
    }

    private static Map<String, Long> bakedPrices() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("IMPLOSION_SCROLL", 171_766_604L);
        map.put("SHADOW_WARP_SCROLL", 166_498_726L);
        map.put("WITHER_SHIELD_SCROLL", 164_478_684L);
        map.put("NECRON_HANDLE", 422_629_442L);
        map.put("DARK_CLAYMORE", 229_435_549L);
        map.put("AUTO_RECOMBOBULATOR", 9_089_146L);
        map.put("FIFTH_MASTER_STAR", 105_121_460L);
        map.put("WITHER_CHESTPLATE", 9_701_999L);
        map.put("ENCHANTMENT_ULTIMATE_ONE_FOR_ALL_1", 1_181_948L);
        map.put("RECOMBOBULATOR_3000", 9_739_195L);
        map.put("WITHER_LEGGINGS", 5_095_999L);
        map.put("MASTER_SKULL_TIER_5", 30_407_664L);
        map.put("WITHER_CLOAK", 1_971_287L);
        map.put("WITHER_BLOOD", 1_228_314L);
        map.put("WITHER_HELMET", 1_744_400L);
        map.put("ENCHANTMENT_THUNDERLORD_7", 2_273_007L);
        map.put("ENCHANTMENT_ULTIMATE_SOUL_EATER_1", 727_106L);
        map.put("FUMING_POTATO_BOOK", 1_180_391L);
        map.put("WITHER_BOOTS", 1_068_200L);
        map.put("HOT_POTATO_BOOK", 78_953L);
        map.put("WITHER_CATALYST", 1_002_452L);
        map.put("PRECURSOR_GEAR", 355_132L);
        map.put("ENCHANTMENT_ULTIMATE_COMBO_2", 0L);
        map.put("ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_2", 0L);
        map.put("ENCHANTMENT_ULTIMATE_LAST_STAND_2", 105_601L);
        map.put("ENCHANTMENT_REJUVENATE_3", 33_027L);
        map.put("ENCHANTMENT_ULTIMATE_WISE_2", 31_976L);
        map.put("ENCHANTMENT_ULTIMATE_JERRY_3", 0L);
        map.put("ENCHANTMENT_ULTIMATE_BANK_3", 0L);
        map.put("ENCHANTMENT_ULTIMATE_WISDOM_2", 126_778L);
        map.put("ENCHANTMENT_INFINITE_QUIVER_7", 13L);
        map.put("ENCHANTMENT_FEATHER_FALLING_7", 1L);
        map.put("GOLDOR_THE_FISH", 159_874L);
        map.put("MAXOR_THE_FISH", 100_000L);
        map.put("STORM_THE_FISH", 137_752L);
        map.put(M7LootTable.ESSENCE_WITHER, 2_258L);
        map.put(M7LootTable.ESSENCE_UNDEAD, 655L);
        map.put(M7LootTable.DRAGON_SHARD, 354_601L);
        map.put(M7LootTable.APEX_DRAGON_SHARD, 235_290L);
        map.put(M7LootTable.WITHER_SHARD, 450_378L);
        map.put(M7LootTable.KISMET_FEATHER, 1_423_634L);
        map.put(M7LootTable.DUNGEON_CHEST_KEY, 203_460L);
        return Collections.unmodifiableMap(map);
    }
}
