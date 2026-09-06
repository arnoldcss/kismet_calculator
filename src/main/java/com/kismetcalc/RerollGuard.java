package com.kismetcalc;

import com.kismetcalc.core.KismetCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.Locale;

public final class RerollGuard {

    private static final int INVENTORY_SLOTS = 36;

    private static final long WARN_INTERVAL = 5_000L;

    private static AbstractContainerScreen<?> screen;

    private static KismetCalculator.Verdict verdict;

    private static long blockedAt;

    private static long warnedAt;

    private RerollGuard() {
    }

    public static void update(AbstractContainerScreen<?> forScreen, KismetCalculator.Verdict decision) {
        screen = forScreen;
        verdict = decision;
    }

    public static void clear() {
        screen = null;
        verdict = null;
    }

    public static boolean blocks(AbstractContainerScreen<?> clicked, Slot slot) {
        if (!KismetConfig.gamblingMode) return false;
        if (slot == null || clicked == null || clicked != screen) return false;
        if (verdict == null || verdict.reroll()) return false;

        if (!ChestReader.CHEST_TITLE.matcher(clicked.getTitle().getString()).matches()) return false;
        
        int containerSlots = clicked.getMenu().slots.size() - INVENTORY_SLOTS;
        int position = clicked.getMenu().slots.indexOf(slot);
        if (position < 0 || position >= containerSlots) return false;

        return isRerollButton(ChestReader.plain(slot.getItem().getHoverName().getString()));
    }

    private static boolean isRerollButton(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("reroll") || lower.contains("kismet");
    }

    public static void onBlocked() {
        long now = System.currentTimeMillis();
        blockedAt = now;
        if (now - warnedAt < WARN_INTERVAL) return;
        warnedAt = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;
        mc.gui.getChat().addClientSystemMessage(Component.literal(
            "§cReroll blocked§7 - this chest is worth more than a reroll."
                + " §f/kismet gamblingmode false§7 to click it anyway."));
    }

    public static boolean recentlyBlocked() {
        return blockedAt != 0 && System.currentTimeMillis() - blockedAt < 2_000L;
    }
}
