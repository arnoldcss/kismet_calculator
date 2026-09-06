package com.kismetcalc.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record ChestValuation(
    String label,
    long value,
    long cost,
    List<Line> lines
) {

    public record Line(String itemId, int count, long unit) {

        public long total() {
            return unit * count;
        }
    }

    public long net() {
        return value - cost;
    }

    public boolean profitable() {
        return net() > 0;
    }

    public static ChestValuation ofRewards(String label, Collection<String> rewards,
                                           PriceBook prices, long knownCost) {
        List<RewardIds.Stack> stacks = new ArrayList<>();
        for (String reward : rewards) {
            RewardIds.Stack stack = RewardIds.resolve(reward);
            if (stack != null) stacks.add(stack);
        }
        return ofStacks(label, stacks, prices, knownCost);
    }

    public static ChestValuation ofStacks(String label, Collection<RewardIds.Stack> stacks,
                                          PriceBook prices, long knownCost) {
        List<Line> lines = new ArrayList<>();
        long value = 0L;
        long derivedCost = M7LootTable.BASE_CHEST_COST;

        for (RewardIds.Stack stack : stacks) {
            if (stack == null) continue;
            long unit = prices.value(stack.itemId());
            lines.add(new Line(stack.itemId(), stack.count(), unit));
            value += unit * stack.count();

            KismetItem item = itemById(stack.itemId());
            if (item != null) derivedCost += item.addedCost() * stack.count();
        }
        return new ChestValuation(label, value, knownCost >= 0 ? knownCost : derivedCost, lines);
    }

    public static ChestValuation ofCombination(long key, int quality, PriceBook prices) {
        var contents = M7LootTable.contents(key);
        if (contents == null) return null;  // not a key from this table

        long witherPrice = prices.value(M7LootTable.ESSENCE_WITHER);
        long undeadPrice = prices.value(M7LootTable.ESSENCE_UNDEAD);

        List<Line> lines = new ArrayList<>();
        long value = witherPrice * M7LootTable.GUARANTEED_WITHER
            + undeadPrice * M7LootTable.GUARANTEED_UNDEAD
            + M7LootTable.shardValue(prices.value(M7LootTable.DRAGON_SHARD),
                prices.value(M7LootTable.APEX_DRAGON_SHARD)) * M7LootTable.GUARANTEED_SHARDS;
        long cost = M7LootTable.BASE_CHEST_COST;

        for (var entry : contents.entrySet()) {
            KismetItem item = entry.getKey();
            int count = entry.getValue();
            long unit = prices.value(item.itemId());
            lines.add(new Line(item.itemId(), count, unit));
            value += unit * count;
            cost += item.addedCost() * count;
        }

        value += M7LootTable.essenceRefund(quality, witherPrice, undeadPrice);
        return new ChestValuation("roll " + key, value, cost, lines);
    }

    private static KismetItem itemById(String itemId) {
        for (KismetItem item : M7LootTable.ITEMS) {
            if (item.itemId().equals(itemId)) return item;
        }
        return null;
    }
}
