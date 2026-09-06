package com.kismetcalc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChestLore {

    private static final Pattern CONTENTS_HEADING = Pattern.compile("^(?:Contents|Rewards):?$");

    private static final Pattern COINS = Pattern.compile("([\\d,]+)\\s*coins?", Pattern.CASE_INSENSITIVE);

    private static final Pattern FREE = Pattern.compile("^free!?$", Pattern.CASE_INSENSITIVE);

    public record Read(ChestValuation valuation, int unpriced, boolean costKnown) {
    }

    private ChestLore() {
    }

    public static Read fromLore(String label, List<String> lore, PriceBook prices) {
        List<RewardIds.Stack> stacks = new ArrayList<>();
        long cost = -1L;
        int unpriced = 0;
        boolean inContents = false;

        for (String raw : lore) {
            if (raw == null) continue;
            String text = raw.trim();

            if (text.isEmpty()) {
                inContents = false;
                continue;
            }
            if (CONTENTS_HEADING.matcher(text).matches()) {
                inContents = true;
                continue;
            }
            if (!inContents) {
                if (cost < 0) {
                    long found = coinsIn(text);
                    if (found >= 0) cost = found;
                }
                continue;
            }

            RewardIds.Stack reward = NameToItem.resolve(text);
            if (reward == null) {
                unpriced++;
                continue;
            }
            stacks.add(reward);
        }
        return new Read(ChestValuation.ofStacks(label, stacks, prices, cost), unpriced, cost >= 0);
    }

    public static long coinsIn(List<String> lore) {
        for (String line : lore) {
            long found = coinsIn(line);
            if (found >= 0) return found;
        }
        return -1L;
    }

    public static long coinsIn(String text) {
        if (text == null) return -1L;
        if (FREE.matcher(text.trim()).matches()) return 0L;

        Matcher matcher = COINS.matcher(text);
        if (!matcher.find()) return -1L;

        String digits = matcher.group(1).replace(",", "");
        if (digits.isEmpty()) return -1L;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
