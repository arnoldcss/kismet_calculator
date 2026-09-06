package com.kismetcalc;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class RngMeterTracker {

    private static final RngMeterTracker INSTANCE = new RngMeterTracker();

    private static final Path FILE = Path.of("config", "kismetcalc_meter.properties");

    private static final Pattern TEAM_SCORE =
        Pattern.compile("Team Score:\\s*([\\d,]+)\\s*\\(([A-Za-z+]+)\\)");

    private static final Pattern METER_FLOOR = Pattern.compile("\\(([EFM]\\d?)\\)");

    private static final Pattern SIDEBAR_FLOOR =
        Pattern.compile("The Catacombs \\(([EFM]\\d?)\\)");

    public static final String M7 = "M7";

    private static final long READ_INTERVAL_MS = 250;

    
    private static double efficiency(String grade) {
        return switch (grade.toUpperCase(Locale.ROOT)) {
            case "S+" -> 1.0;
            case "S" -> 0.7;
            default -> 0.0;
        };
    }

    public record Progress(double fraction, long stored, long needed, int runs, long seededAt) {

        public boolean exact() {
            return runs == 0;
        }
    }

    private final Map<String, Long> seeded = new HashMap<>();
    private final Map<String, Long> needed = new HashMap<>();
    private final Map<String, Long> seededAt = new HashMap<>();
    private final Map<String, Long> gained = new HashMap<>();
    private final Map<String, Integer> runs = new HashMap<>();

    private String lastFloor;

    private Object lastScreen;
    private long lastReadAt;
    private boolean lastScreenRead;

    private RngMeterTracker() {
    }

    public static RngMeterTracker getInstance() {
        return INSTANCE;
    }

    public void register() {
        load();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) onChat(message.getString());
        });
    }

    public void tick() {
        for (String line : Sidebar.lines()) {
            Matcher matcher = SIDEBAR_FLOOR.matcher(line);
            if (matcher.find()) {
                lastFloor = matcher.group(1);
                return;
            }
        }
    }

    public void readScreen(AbstractContainerScreen<?> screen) {
        if (screen != lastScreen) {
            lastScreen = screen;
            lastScreenRead = false;
            lastReadAt = 0;
        }
        if (lastScreenRead) return;

        String title = Sidebar.strip(screen.getTitle().getString());
        if (!title.endsWith("RNG Meter")) return;

        long now = System.currentTimeMillis();
        if (now - lastReadAt < READ_INTERVAL_MS) return;
        lastReadAt = now;

        Matcher floorMatch = METER_FLOOR.matcher(title);
        if (!floorMatch.find()) return;
        String floor = floorMatch.group(1);

        int containerSlots = screen.getMenu().slots.size() - 36;
        long stored = -1;
        long handleNeeded = -1;
        long selectedNeeded = -1;

        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (stack.isEmpty()) continue;

            String name = Sidebar.strip(stack.getHoverName().getString());
            boolean selected = false;
            long itemNeeds = -1;

            for (Component line : lore(stack)) {
                String raw = line.getString();
                if (raw.contains("SELECTED")) selected = true;

                MeterLore.Pair pair = MeterLore.pair(Sidebar.strip(raw));
                if (pair == null) continue;

                if (stored < 0) stored = pair.stored();
                itemNeeds = pair.needed();
            }

            if (itemNeeds < 0) continue;
            if (name.contains("Necron's Handle")) handleNeeded = itemNeeds;
            if (selected) selectedNeeded = itemNeeds;
        }

        if (stored < 0) return;

        long need = handleNeeded > 0 ? handleNeeded : selectedNeeded;
        if (need <= 0) return;

        seeded.put(floor, stored);
        needed.put(floor, need);
        seededAt.put(floor, System.currentTimeMillis());
        gained.put(floor, 0L);
        runs.put(floor, 0);
        save();
        lastScreenRead = true;
    }

    private void onChat(String message) {
        String plain = Sidebar.strip(message);
        Matcher matcher = TEAM_SCORE.matcher(plain);
        if (!matcher.find()) return;

        String floor = lastFloor;
        if (floor == null || !seededAt.containsKey(floor)) return;

        long gain = Math.round(MeterLore.number(matcher.group(1)) * efficiency(matcher.group(2)));
        if (gain <= 0) return;

        gained.merge(floor, gain, Long::sum);
        runs.merge(floor, 1, Integer::sum);
        save();
    }

    /** Last dungeon floor seen on the sidebar, or null before one has been read. */
    public String floor() {
        return lastFloor;
    }

    public Progress progress(String floor) {
        Long seed = seeded.get(floor);
        Long need = needed.get(floor);
        Long when = seededAt.get(floor);
        if (seed == null || need == null || need <= 0 || when == null) return null;

        long stored = seed + gained.getOrDefault(floor, 0L);
        double fraction = Math.max(0.0, Math.min(1.0, (double) stored / need));
        return new Progress(fraction, stored, need, runs.getOrDefault(floor, 0), when);
    }

    private static List<Component> lore(ItemStack stack) {
        ItemLore itemLore = stack.get(DataComponents.LORE);
        return itemLore == null ? List.of() : itemLore.lines();
    }

    private void save() {
        StringBuilder body = new StringBuilder("# Kismet Calculator - RNG meter readings\n");
        for (String floor : seededAt.keySet()) {
            body.append(floor).append(".stored=").append(seeded.getOrDefault(floor, 0L)).append('\n');
            body.append(floor).append(".needed=").append(needed.getOrDefault(floor, 0L)).append('\n');
            body.append(floor).append(".seededAt=").append(seededAt.getOrDefault(floor, 0L)).append('\n');
            body.append(floor).append(".gained=").append(gained.getOrDefault(floor, 0L)).append('\n');
            body.append(floor).append(".runs=").append(runs.getOrDefault(floor, 0)).append('\n');
        }
        try {
            Path parent = FILE.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(FILE, body.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Losing a reading costs one menu opening.
        }
    }

    private void load() {
        if (!Files.isRegularFile(FILE)) return;
        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String text = line.trim();
                if (text.isEmpty() || text.startsWith("#")) continue;
                int equals = text.indexOf('=');
                int dot = text.indexOf('.');
                if (equals <= 0 || dot <= 0 || dot > equals) continue;

                String floor = text.substring(0, dot);
                String field = text.substring(dot + 1, equals);
                String value = text.substring(equals + 1).trim();
                switch (field) {
                    case "stored" -> seeded.put(floor, Long.parseLong(value));
                    case "needed" -> needed.put(floor, Long.parseLong(value));
                    case "seededAt" -> seededAt.put(floor, Long.parseLong(value));
                    case "gained" -> gained.put(floor, Long.parseLong(value));
                    case "runs" -> runs.put(floor, Integer.parseInt(value));
                    default -> {
                        // A field from a newer version.
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            seeded.clear();
            needed.clear();
            seededAt.clear();
            gained.clear();
            runs.clear();
        }
    }

    public boolean hasReading(String floor) {
        return seededAt.containsKey(floor);
    }
}
