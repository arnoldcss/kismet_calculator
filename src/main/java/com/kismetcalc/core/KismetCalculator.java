package com.kismetcalc.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// base = EV(profitable rolls, handle excluded) - kismet cost + h_m(meter) * handle profit
// A competing chest is added, not subtracted: it is what the reroll has to beat, so a good
// alternative raises the bar rather than lowering it.
//math math thank you splooder
public final class KismetCalculator {

    public static final double ALT_DISCOUNT = 0.85;

    public record Analysis(
        double evProfitable,
        double evProfitableNoHandle,
        double profitableChance,
        int profitableCount,
        long handleProfit,
        long kismetCost
    ) {
    }

    public record Verdict(
        boolean reroll,
        double threshold,
        long chestNet,
        double baseThreshold,
        String reason
    ) {
    }

    private KismetCalculator() {
    }

    public static Analysis analyse(PriceBook prices) {
        CombinationTable table = CombinationTable.get();
        if (table == null) return null;

        List<KismetItem> items = M7LootTable.ITEMS;
        int itemCount = items.size();
        int[] primes = new int[itemCount];
        long[] unit = new long[itemCount];
        long[] added = new long[itemCount];
        for (int i = 0; i < itemCount; i++) {
            KismetItem item = items.get(i);
            primes[i] = item.prime();
            unit[i] = prices.value(item.itemId());
            added[i] = item.addedCost();
        }

        long witherPrice = prices.value(M7LootTable.ESSENCE_WITHER);
        long undeadPrice = prices.value(M7LootTable.ESSENCE_UNDEAD);
        long guaranteed = witherPrice * M7LootTable.GUARANTEED_WITHER
            + undeadPrice * M7LootTable.GUARANTEED_UNDEAD
            + M7LootTable.shardValue(prices.value(M7LootTable.DRAGON_SHARD),
                prices.value(M7LootTable.APEX_DRAGON_SHARD)) * M7LootTable.GUARANTEED_SHARDS;

        double ev = 0.0;
        double evNoHandle = 0.0;
        double chance = 0.0;
        int count = 0;

        for (int row = 0; row < table.size(); row++) {
            long key = table.key(row);
            long remaining = key;
            long value = guaranteed;
            long cost = M7LootTable.BASE_CHEST_COST;

            for (int i = 0; i < itemCount && remaining > 1; i++) {
                int prime = primes[i];
                if (prime == 1) continue;
                while (remaining % prime == 0) {
                    remaining /= prime;
                    value += unit[i];
                    cost += added[i];
                }
            }
            if (remaining != 1) continue; 

            value += M7LootTable.essenceRefund(table.quality(row), witherPrice, undeadPrice);

            long net = value - cost;
            if (net <= 0) continue;

            double probability = table.probability(row);
            ev += net * probability;
            chance += probability;
            count++;
            if (!M7LootTable.hasHandle(key)) evNoHandle += net * probability;
        }

        long handleProfit = Math.max(0L,
            prices.value(M7LootTable.HANDLE.itemId()) - M7LootTable.HANDLE.addedCost());

        return new Analysis(ev, evNoHandle, chance, count, handleProfit, prices.kismetCost());
    }

    public static double baseThreshold(Analysis analysis, double meterProgress) {
        double handleTerm = MeterMath.handleChance(meterProgress) * analysis.handleProfit();
        return analysis.evProfitableNoHandle() - analysis.kismetCost() + handleTerm;
    }

    public static Verdict decide(Analysis analysis, double meterProgress,
                                 long chestNet, List<Long> altNets, long keyCost) {
        double base = baseThreshold(analysis, meterProgress);

        List<Long> profitable = new ArrayList<>();
        for (Long net : altNets) {
            if (net != null && net > 0) profitable.add(net);
        }
        profitable.sort(Comparator.reverseOrder());

        int others = profitable.size();
        boolean bestOverKey = others > 0 && profitable.get(0) > keyCost;

        double threshold = base;
        String reason;
        if (others == 0) {
            reason = "no other chest in the run is worth opening";
        } else if (others == 1 && bestOverKey) {
            reason = "one other chest, and it clears the key on its own";
        } else if (others == 1) {
            threshold = base + ALT_DISCOUNT * profitable.get(0);
            reason = "one other chest under the key, competing for the reroll";
        } else if (bestOverKey) {
            threshold = base + ALT_DISCOUNT * profitable.get(1);
            reason = "best chest clears the key; second best is the competition";
        } else {
            threshold = base + ALT_DISCOUNT * profitable.get(0);
            reason = others + " other chests, none over the key";
        }

        return new Verdict(chestNet < threshold, threshold, chestNet, base, reason);
    }
}
