package com.kismetcalc.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RewardIds {

    public record Stack(String itemId, int count) {
    }

    private static final String ENCH = "ENCHANTMENT_";

    private static final Set<String> ULTIMATES = Set.of(
        "wise", "jerry", "master_jerry", "bank", "combo", "last_stand",
        "no_pain_no_gain", "one_for_all", "soul_eater", "wisdom", "swarm", "chimera",
        "the_one", "ultimate_wise", "ultimate_jerry", "legion", "inferno", "fatal_tempo",
        "duplex", "bobbin_time", "flash", "flowstate"
    );

    private static final Map<String, String> ULTIMATE_STEMS = Map.of(
        "master_jerry", "ultimate_jerry",
        "jerry", "ultimate_jerry"
    );

    private static final Map<String, String> ITEMS = items();

    private static final Set<String> UNRESOLVED = Collections.synchronizedSet(new LinkedHashSet<>());

    private RewardIds() {
    }

    public static Stack resolve(String reward) {
        if (reward == null || reward.isBlank()) return null;
        String id = reward.trim();

        int firstColon = id.indexOf(':');
        if (firstColon > 0) {
            String[] parts = id.split(":");
            if (parts.length >= 3) {
                int count;
                try {
                    count = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    count = 1;
                }
                String kind = parts[1].trim().toUpperCase(Locale.ROOT);
                String prefix = parts[0].trim().toUpperCase(Locale.ROOT);
                return new Stack(prefix + "_" + kind, count);
            }
            return null;
        }

        String lower = id.toLowerCase(Locale.ROOT);
        String mapped = ITEMS.get(lower);
        if (mapped != null) return new Stack(mapped, 1);

        int lastUnderscore = lower.lastIndexOf('_');
        if (lastUnderscore > 0 && isNumber(lower.substring(lastUnderscore + 1))) {
            String stem = lower.substring(0, lastUnderscore);
            String level = lower.substring(lastUnderscore + 1);
            String resolved = ULTIMATE_STEMS.getOrDefault(stem, stem);
            if (ULTIMATES.contains(stem) && !resolved.startsWith("ultimate_")) {
                resolved = "ultimate_" + resolved;
            }
            return new Stack(ENCH + (resolved + "_" + level).toUpperCase(Locale.ROOT), 1);
        }

        UNRESOLVED.add(lower);
        return new Stack(lower.toUpperCase(Locale.ROOT), 1);
    }

    public static Set<String> unresolved() {
        synchronized (UNRESOLVED) {
            return Set.copyOf(UNRESOLVED);
        }
    }

    private static boolean isNumber(String text) {
        if (text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) return false;
        }
        return true;
    }

    private static Map<String, String> items() {
        Map<String, String> map = new HashMap<>();
        map.put("implosion", "IMPLOSION_SCROLL");
        map.put("shadow_warp", "SHADOW_WARP_SCROLL");
        map.put("wither_shield", "WITHER_SHIELD_SCROLL");
        map.put("necrons_handle", "NECRON_HANDLE");
        map.put("necron_handle", "NECRON_HANDLE");
        map.put("handle", "NECRON_HANDLE");
        map.put("master_skull_tier_5", "MASTER_SKULL_TIER_5");
        map.put("fifth_master_star", "FIFTH_MASTER_STAR");
        map.put("goldor_the_fish", "GOLDOR_THE_FISH");
        map.put("maxor_the_fish", "MAXOR_THE_FISH");
        map.put("storm_the_fish", "STORM_THE_FISH");
        map.put("wither_shard", "SHARD_WITHER");
        return Collections.unmodifiableMap(map);
    }
}
