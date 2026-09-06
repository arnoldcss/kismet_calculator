package com.kismetcalc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class MeterLore {

    private static final String AMOUNT = "([\\d,]+(?:\\.\\d+)?[kKmMbB]?)";

    private static final Pattern LABELLED =
        Pattern.compile("Dungeon Score:\\s*" + AMOUNT + "\\s*/\\s*" + AMOUNT);

    private static final Pattern BARE = Pattern.compile(AMOUNT + "\\s*/\\s*" + AMOUNT);

    private static final long MIN_NEEDED = 1_000L;

    public record Pair(long stored, long needed, boolean labelled) {
    }

    private MeterLore() {
    }

    public static Pair pair(String line) {
        if (line == null) return null;

        Matcher labelled = LABELLED.matcher(line);
        if (labelled.find()) {
            Pair pair = of(labelled, true);
            if (pair != null) return pair;
        }
        Matcher bare = BARE.matcher(line);
        while (bare.find()) {
            Pair pair = of(bare, false);
            if (pair != null && pair.needed() >= MIN_NEEDED) return pair;
        }
        return null;
    }

    private static Pair of(Matcher matcher, boolean labelled) {
        long stored = number(matcher.group(1));
        long needed = number(matcher.group(2));
        if (stored < 0 || needed <= 0) return null;
        return new Pair(stored, needed, labelled);
    }

    public static long number(String text) {
        if (text == null) return -1L;
        String value = text.replace(",", "").trim();
        if (value.isEmpty()) return -1L;

        long multiplier = switch (Character.toLowerCase(value.charAt(value.length() - 1))) {
            case 'k' -> 1_000L;
            case 'm' -> 1_000_000L;
            case 'b' -> 1_000_000_000L;
            default -> 1L;
        };
        if (multiplier > 1) value = value.substring(0, value.length() - 1);

        try {
            return Math.round(Double.parseDouble(value) * multiplier);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
