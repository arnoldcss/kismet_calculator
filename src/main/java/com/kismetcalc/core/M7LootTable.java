package com.kismetcalc.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public final class M7LootTable {

    public static final long BASE_CHEST_COST = 2_000_000L;

    public static final int QUALITY_CEILING = 516;

    public static final int GUARANTEED_WITHER = 100;

    public static final int GUARANTEED_UNDEAD = 125;

    public static final int GUARANTEED_SHARDS = 1;

    public static final int HANDLE_PRIME = 131;

    public static final String ESSENCE_WITHER = "ESSENCE_WITHER";
    public static final String ESSENCE_UNDEAD = "ESSENCE_UNDEAD";
    public static final String DRAGON_SHARD = "SHARD_POWER_DRAGON";
    public static final String APEX_DRAGON_SHARD = "SHARD_APEX_DRAGON";

    //just for price
    public static final String WITHER_SHARD = "SHARD_WITHER";
    public static final String KISMET_FEATHER = "KISMET_FEATHER";

    // Not a drop. Priced because a competing chest is measured against what a key costs.
    public static final String DUNGEON_CHEST_KEY = "DUNGEON_CHEST_KEY";


    public static final double APEX_SHARD_CHANCE = 0.5;

    public static long shardValue(long powerPrice, long apexPrice) {
        return Math.round(powerPrice * (1 - APEX_SHARD_CHANCE) + apexPrice * APEX_SHARD_CHANCE);
    }

    public static final List<KismetItem> ITEMS = List.of(
        new KismetItem("Implosion", "IMPLOSION_SCROLL", 380, 25, 149, 48_000_000L),
        new KismetItem("Shadow Warp", "SHADOW_WARP_SCROLL", 380, 25, 139, 48_000_000L),
        new KismetItem("Wither Shield", "WITHER_SHIELD_SCROLL", 380, 25, 137, 48_000_000L),
        new KismetItem("Necron's Handle", "NECRON_HANDLE", 380, 18, 131, 98_000_000L),
        new KismetItem("Dark Claymore", "DARK_CLAYMORE", 360, 10, 127, 148_000_000L),
        new KismetItem("Auto Recombobulator", "AUTO_RECOMBOBULATOR", 330, 80, 113, 8_000_000L),
        new KismetItem("Fifth Master Star", "FIFTH_MASTER_STAR", 320, 30, 109, 7_000_000L),
        new KismetItem("Wither Chestplate", "WITHER_CHESTPLATE", 310, 80, 107, 8_000_000L),
        new KismetItem("Enchanted Book (One For All)", "ENCHANTMENT_ULTIMATE_ONE_FOR_ALL_1", 290, 80, 103, 0L),
        new KismetItem("Recombobulator 3000", "RECOMBOBULATOR_3000", 250, 500, 101, 4_000_000L),
        new KismetItem("Wither Leggings", "WITHER_LEGGINGS", 250, 320, 97, 4_000_000L),
        new KismetItem("Master Skull - Tier 5", "MASTER_SKULL_TIER_5", 250, 24, 89, 30_000_000L),
        new KismetItem("Wither Cloak Sword", "WITHER_CLOAK", 230, 480, 83, 2_500_000L),
        new KismetItem("Wither Blood", "WITHER_BLOOD", 210, 480, 79, 1_000_000L),
        new KismetItem("Wither Helmet", "WITHER_HELMET", 210, 480, 73, 2_000_000L),
        new KismetItem("Enchanted Book (Thunderlord VII)", "ENCHANTMENT_THUNDERLORD_7", 200, 20, 71, 0L),
        new KismetItem("Enchanted Book (Soul Eater I)", "ENCHANTMENT_ULTIMATE_SOUL_EATER_1", 180, 800, 67, 0L),
        new KismetItem("Fuming Potato Book", "FUMING_POTATO_BOOK", 175, 400, 61, 0L),
        new KismetItem("Wither Boots", "WITHER_BOOTS", 170, 480, 59, 500_000L),
        new KismetItem("Hot Potato Book", "HOT_POTATO_BOOK", 160, 800, 53, 0L),
        new KismetItem("Wither Catalyst", "WITHER_CATALYST", 160, 400, 47, 0L),
        new KismetItem("Precursor Gear", "PRECURSOR_GEAR", 140, 1200, 43, 0L),
        new KismetItem("Enchanted Book (Combo II)", "ENCHANTMENT_ULTIMATE_COMBO_2", 120, 1000, 41, 0L),
        new KismetItem("Enchanted Book (No Pain No Gain II)", "ENCHANTMENT_ULTIMATE_NO_PAIN_NO_GAIN_2", 120, 400, 37, 0L),
        new KismetItem("Enchanted Book (Last Stand II)", "ENCHANTMENT_ULTIMATE_LAST_STAND_2", 100, 1000, 31, 0L),
        new KismetItem("Enchanted Book (Rejuvenate III)", "ENCHANTMENT_REJUVENATE_3", 100, 1000, 29, 0L),
        new KismetItem("Enchanted Book (Ultimate Wise II)", "ENCHANTMENT_ULTIMATE_WISE_2", 100, 800, 23, 0L),
        new KismetItem("Enchanted Book (Ultimate Jerry III)", "ENCHANTMENT_ULTIMATE_JERRY_3", 100, 600, 19, 0L),
        new KismetItem("Enchanted Book (Bank III)", "ENCHANTMENT_ULTIMATE_BANK_3", 100, 500, 17, 0L),
        new KismetItem("Enchanted Book (Wisdom II)", "ENCHANTMENT_ULTIMATE_WISDOM_2", 100, 500, 13, 0L),
        new KismetItem("Enchanted Book (Infinite Quiver VII)", "ENCHANTMENT_INFINITE_QUIVER_7", 80, 1000, 11, 0L),
        new KismetItem("Enchanted Book (Feather Falling VII)", "ENCHANTMENT_FEATHER_FALLING_7", 80, 320, 7, 0L),
        new KismetItem("Goldor the Fish", "GOLDOR_THE_FISH", 61, 5, 5, 0L),
        new KismetItem("Maxor the Fish", "MAXOR_THE_FISH", 61, 5, 3, 0L),
        new KismetItem("Storm the Fish", "STORM_THE_FISH", 61, 5, 2, 0L),
        new KismetItem("Wither Essence (1)", ESSENCE_WITHER, 10, 1, 1, 0L)
    );

    private static final Map<Integer, KismetItem> BY_PRIME = ITEMS.stream()
        .collect(java.util.stream.Collectors.toUnmodifiableMap(KismetItem::prime, i -> i));

    public static final KismetItem HANDLE = BY_PRIME.get(HANDLE_PRIME);

    private M7LootTable() {
    }

    public static KismetItem byPrime(int prime) {
        return BY_PRIME.get(prime);
    }

    // Trial division against the table's own primes, so a leftover means the key is not ours.
    public static Map<KismetItem, Integer> contents(long key) {
        java.util.LinkedHashMap<KismetItem, Integer> found = new java.util.LinkedHashMap<>();
        long remaining = key;
        for (KismetItem item : ITEMS) {
            if (item.prime() == 1) continue;
            int count = 0;
            while (remaining % item.prime() == 0) {
                count++;
                remaining /= item.prime();
            }
            if (count > 0) found.put(item, count);
        }
        return remaining == 1 ? found : null;
    }

    public static boolean hasHandle(long key) {
        return key % HANDLE_PRIME == 0;
    }

    public static long essenceRefund(int quality, long witherPrice, long undeadPrice) {
        int remaining = Math.max(0, QUALITY_CEILING - quality);
        return (remaining / 10L) * witherPrice + (remaining % 10L) * undeadPrice;
    }

    public static List<String> pricedIds() {
        List<String> ids = new ArrayList<>();
        for (KismetItem item : ITEMS) ids.add(item.itemId());
        Collections.addAll(ids,
            ESSENCE_UNDEAD, DRAGON_SHARD, APEX_DRAGON_SHARD, WITHER_SHARD, KISMET_FEATHER,
            DUNGEON_CHEST_KEY);
        return List.copyOf(ids);
    }
}
