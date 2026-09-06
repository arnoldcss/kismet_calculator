package com.kismetcalc;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

//for debug
public final class WindowDump {

    private static final Path FILE = Path.of("config", "kismetcalc", "window-dump.txt");

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int INVENTORY_SLOTS = 36;

    private static WeakReference<AbstractContainerScreen<?>> lastScreen = new WeakReference<>(null);

    private static volatile List<String> snapshot;
    private static volatile String snapshotTitle;
    private static volatile int snapshotSlots;

    public record Result(Path file, String title, int slots) {
    }

    private WindowDump() {
    }

    public static void remember(AbstractContainerScreen<?> screen) {
        if (screen == null || lastScreen.get() == screen) return;
        lastScreen = new WeakReference<>(screen);

        List<String> lines = new ArrayList<>();
        String title = screen.getTitle().getString();
        int total = screen.getMenu().slots.size();
        int container = total - INVENTORY_SLOTS;

        lines.add("=== " + STAMP.format(LocalDateTime.now()));
        lines.add("title: " + title);
        lines.add("slots: " + total + " total, " + container + " in the container");

        int filled = 0;
        for (int i = 0; i < container; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            filled++;

            lines.add("");
            lines.add("[" + i + "] " + ChestReader.plain(stack.getHoverName().getString())
                + " x" + stack.getCount());
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;
            for (Component line : lore.lines()) {
                lines.add("    | " + ChestReader.plain(line.getString()));
            }
        }
        lines.add("");

        snapshot = lines;
        snapshotTitle = title;
        snapshotSlots = filled;
    }

    public static Result write() {
        List<String> lines = snapshot;
        if (lines == null) return null;

        try {
            Path parent = FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(FILE, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            return null;
        }
        return new Result(FILE.toAbsolutePath(), snapshotTitle, snapshotSlots);
    }

    public static boolean captured() {
        return snapshot != null;
    }
}
