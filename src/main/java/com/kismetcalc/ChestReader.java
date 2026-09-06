package com.kismetcalc;

import com.kismetcalc.core.ChestLore;
import com.kismetcalc.core.ChestValuation;
import com.kismetcalc.core.NameToItem;
import com.kismetcalc.core.PriceBook;
import com.kismetcalc.core.RewardIds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ChestReader {

    public static final Pattern CHEST_TITLE =
        Pattern.compile("^(?:Wood|Gold|Diamond|Emerald|Obsidian|Bedrock)(?: Chest)?$");

    // M7 only: the loot table and every verdict below it are Master Floor VII's.
    public static final Pattern RUN_TITLE =
        Pattern.compile("^Master (?:Mode )?Catacombs\\b.*\\bVII$");

    private static final String[] TIERS = {"Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock"};

    private static final String BOOK = "Enchanted Book";

    private static final int INVENTORY_SLOTS = 36;

    public record Chest(String label, ChestValuation valuation, int unpriced, boolean bedrock) {

        public long net() {
            return valuation.net();
        }

        public boolean profitable() {
            return valuation.profitable();
        }
    }

    private ChestReader() {
    }

    public static List<Chest> read(AbstractContainerScreen<?> screen, PriceBook prices) {
        String title = screen.getTitle().getString();
        if (CHEST_TITLE.matcher(title).matches()) {
            Chest chest = readOpenChest(screen, title, prices);
            return chest == null ? List.of() : List.of(chest);
        }
        if (RUN_TITLE.matcher(title).matches()) {
            return readRunPage(screen, prices);
        }
        return List.of();
    }

    private static Chest readOpenChest(AbstractContainerScreen<?> screen, String title, PriceBook prices) {
        int containerSlots = screen.getMenu().slots.size() - INVENTORY_SLOTS;
        if (containerSlots <= 0) return null;

        List<RewardIds.Stack> stacks = new ArrayList<>();
        long cost = -1L;
        int unpriced = 0;

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (stack.isEmpty()) continue;

            String name = plain(stack.getHoverName().getString());
            if (name == null || name.isBlank()) continue;

            if (cost < 0 && name.toLowerCase(Locale.ROOT).startsWith("open ")) {
                cost = ChestLore.coinsIn(plainLore(stack));
            }
            if (isControl(name)) continue;

            RewardIds.Stack reward = null;
            if (name.equalsIgnoreCase(BOOK)) {
                reward = NameToItem.fromLore(plainLore(stack));
            }
            if (reward == null) {
                reward = NameToItem.resolve(name);
            }
            if (reward == null) {
                unpriced++;
                continue;
            }
            int count = Math.max(reward.count(), stack.getCount());
            stacks.add(new RewardIds.Stack(reward.itemId(), count));
        }

        String label = title.replace(" Chest", "");
        boolean bedrock = title.toLowerCase(Locale.ROOT).startsWith("bedrock");
        return new Chest(label, ChestValuation.ofStacks(label, stacks, prices, cost), unpriced, bedrock);
    }

    private static List<Chest> readRunPage(AbstractContainerScreen<?> screen, PriceBook prices) {
        List<Chest> chests = new ArrayList<>();
        int containerSlots = screen.getMenu().slots.size() - INVENTORY_SLOTS;

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (stack.isEmpty()) continue;

            String name = plain(stack.getHoverName().getString());
            if (name == null) continue;
            String tier = tierOf(name);
            if (tier == null) continue;

            ChestLore.Read read = ChestLore.fromLore(tier, plainLore(stack), prices);
            chests.add(new Chest(tier, read.valuation(), read.unpriced(), tier.equals("Bedrock")));
        }
        return chests;
    }

    private static boolean isControl(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("reroll")
            || lower.contains("kismet")
            || lower.startsWith("open ")
            || lower.startsWith("go back")
            || lower.startsWith("close");
    }

    private static String tierOf(String name) {
        for (String tier : TIERS) {
            if (name.startsWith(tier)) return tier;
        }
        return null;
    }

    private static List<String> plainLore(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        ItemLore itemLore = stack.get(DataComponents.LORE);
        if (itemLore == null) return lines;

        for (Component line : itemLore.lines()) {
            String text = plain(line.getString());
            if (text != null) lines.add(text);
        }
        return lines;
    }

    public static String plain(String text) {
        if (text == null) return null;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
