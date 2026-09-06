package com.kismetcalc.core;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// name -> itemId
public final class NameToItem {

    private static final String ENCH = "ENCHANTMENT_";

    private static final String[] ROMAN = {
        "", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x"
    };

    private static final Pattern COUNTED = Pattern.compile("^(\\d+)\\s*x\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern COUNTED_AFTER =
        Pattern.compile("^(.+?)\\s*x\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern LEVELLED = Pattern.compile("^(.*?)\\s+([IVX]+)$");

    private static final Pattern WRAPPED =
        Pattern.compile("^enchanted book\\s*\\((.+)\\)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern LORE_ENCHANT = Pattern.compile("^([A-Za-z][A-Za-z' ]*?)\\s+([IVX]+)$");

    private static final Map<String, String> BY_NAME = byName();

    private static final Map<String, String> BOOK_FAMILIES = bookFamilies();

    private static final Set<String> UNRESOLVED = java.util.Collections.synchronizedSet(new LinkedHashSet<>());

    private NameToItem() {
    }

    public static RewardIds.Stack resolve(String line) {
        if (line == null) return null;
        String text = line.trim();
        if (text.isEmpty()) return null;

        int count = 1;
        Matcher counted = COUNTED.matcher(text);
        if (counted.matches()) {
            count = number(counted.group(1));
            text = counted.group(2).trim();
        } else {
            Matcher after = COUNTED_AFTER.matcher(text);
            if (after.matches()) {
                count = number(after.group(2));
                text = after.group(1).trim();
            }
        }

        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.endsWith("wither essence") || lower.startsWith("wither essence")) {
            return new RewardIds.Stack(M7LootTable.ESSENCE_WITHER, count);
        }
        if (lower.endsWith("undead essence") || lower.startsWith("undead essence")) {
            return new RewardIds.Stack(M7LootTable.ESSENCE_UNDEAD, count);
        }
        if (lower.contains("wither shard")) {
            return new RewardIds.Stack(M7LootTable.WITHER_SHARD, count);
        }
        if (lower.contains("apex dragon")) {
            return new RewardIds.Stack(M7LootTable.APEX_DRAGON_SHARD, count);
        }
        if (lower.contains("dragon shard") || lower.contains("dragon fragment")) {
            return new RewardIds.Stack(M7LootTable.DRAGON_SHARD, count);
        }

        String direct = BY_NAME.get(lower);
        if (direct != null) return new RewardIds.Stack(direct, count);

        Matcher wrapped = WRAPPED.matcher(text);
        String inner = wrapped.matches() ? wrapped.group(1).trim() : text;
        if (wrapped.matches()) {
            String named = BY_NAME.get(inner.toLowerCase(Locale.ROOT));
            if (named != null) return new RewardIds.Stack(named, count);
        }

        Matcher levelled = LEVELLED.matcher(inner);
        if (levelled.matches()) {
            int level = fromRoman(levelled.group(2));
            if (level > 0) {
                String stem = stemKey(levelled.group(1));
                if (!stem.isEmpty()) {
                    String family = BOOK_FAMILIES.getOrDefault(stem, stem.toUpperCase(Locale.ROOT));
                    return new RewardIds.Stack(ENCH + family + "_" + level, count);
                }
            }
        }

        UNRESOLVED.add(text);
        return null;
    }

    public static RewardIds.Stack fromLore(List<String> lore) {
        for (String line : lore) {
            if (line == null) continue;
            String text = line.trim();

            Matcher enchant = LORE_ENCHANT.matcher(text);
            if (!enchant.matches()) continue;
            if (fromRoman(enchant.group(2)) == 0) continue;

            RewardIds.Stack stack = resolve(text);
            if (stack != null) return stack;
        }
        return null;
    }

    public static Set<String> unresolved() {
        synchronized (UNRESOLVED) {
            return Set.copyOf(UNRESOLVED);
        }
    }

    private static int number(String digits) {
        try {
            int value = Integer.parseInt(digits);
            return value > 0 ? value : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String stemKey(String name) {
        return name.toLowerCase(Locale.ROOT)
            .replace("enchanted book", "")
            .replace("(", "")
            .replace(")", "")
            .trim()
            .replace("'", "")
            .replace(' ', '_');
    }

    private static int fromRoman(String numeral) {
        String lower = numeral.toLowerCase(Locale.ROOT);
        for (int i = 1; i < ROMAN.length; i++) {
            if (ROMAN[i].equals(lower)) return i;
        }
        return 0;
    }

    private static Map<String, String> bookFamilies() {
        Map<String, String> map = new HashMap<>();
        for (KismetItem item : M7LootTable.ITEMS) {
            String id = item.itemId();
            if (!id.startsWith(ENCH)) continue;

            String name = item.name();
            int open = name.indexOf('(');
            int close = name.lastIndexOf(')');
            if (open < 0 || close <= open) continue;

            Matcher levelled = LEVELLED.matcher(name.substring(open + 1, close).trim());
            String stem = stemKey(levelled.matches() ? levelled.group(1) : name.substring(open + 1, close));
            if (stem.isEmpty()) continue;

            String family = id.substring(ENCH.length());
            int cut = family.lastIndexOf('_');
            if (cut > 0 && family.substring(cut + 1).chars().allMatch(Character::isDigit)) {
                family = family.substring(0, cut);
            }
            map.put(stem, family);
        }
        return map;
    }

    private static Map<String, String> byName() {
        Map<String, String> map = new HashMap<>();
        for (KismetItem item : M7LootTable.ITEMS) {
            String name = item.name().toLowerCase(Locale.ROOT);
            map.put(name, item.itemId());

            int open = name.indexOf('(');
            int close = name.lastIndexOf(')');
            if (open > 0 && close > open) {
                map.put(name.substring(open + 1, close).trim(), item.itemId());
            }
        }
        map.put("necron's handle", "NECRON_HANDLE");
        map.put("implosion scroll", "IMPLOSION_SCROLL");
        map.put("shadow warp scroll", "SHADOW_WARP_SCROLL");
        map.put("wither shield scroll", "WITHER_SHIELD_SCROLL");
        map.put("master skull - tier 5", "MASTER_SKULL_TIER_5");
        map.put("master skull tier 5", "MASTER_SKULL_TIER_5");
        map.put("kismet feather", M7LootTable.KISMET_FEATHER);
        return map;
    }
}
