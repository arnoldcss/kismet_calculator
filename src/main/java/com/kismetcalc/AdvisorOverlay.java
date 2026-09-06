package com.kismetcalc;

import com.kismetcalc.core.KismetCalculator;
import com.kismetcalc.core.MeterMath;
import com.kismetcalc.core.PriceBook;
import com.kismetcalc.mixin.ContainerScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdvisorOverlay {

    private static final int PANEL = 0xEE101014;
    private static final int BORDER = 0xFF2A2A34;
    private static final int ACCENT = 0xFF2AF5A0;
    private static final int PENDING = 0xFFFFD24A;
    private static final int TEXT = 0xFFE8E8EE;
    private static final int TEXT_MUTED = 0xFF8A8A96;
    private static final int TEXT_DIM = 0xFF5A5A66;

    private static final int SHOUT_STOP = 0xFFFF3B30;
    private static final int SHOUT_GO = 0xFF2AF5A0;
    private static final int SHOUT_BACK = 0xCC08080C;
    private static final int SHOUT_BACK_BLOCKED = 0xEE4A0006;

    private static final String STOP = "DO NOT REROLL";
    private static final String GO = "KISMET KISMET WEEEEE SO HAPPY KISMET";

    private AdvisorOverlay() {
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
        if (!KismetConfig.overlay) return;

        if (KismetConfig.dev) WindowDump.remember(screen);

        String title = screen.getTitle().getString();
        if (!ChestReader.CHEST_TITLE.matcher(title).matches()
            && !ChestReader.RUN_TITLE.matcher(title).matches()) {
            // A verdict must not outlive the window it was read from.
            RerollGuard.clear();
            return;
        }

        PriceClient prices = PriceClient.getInstance();
        prices.ensureFresh();

        PriceBook book = prices.book();
        List<ChestReader.Chest> chests = ChestReader.read(screen, book);
        if (chests.isEmpty() && !KismetConfig.dev) {
            RerollGuard.clear();
            return;
        }

        draw(screen, graphics, chests, prices, book);
    }

    private static void draw(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                             List<ChestReader.Chest> chests, PriceClient prices, PriceBook book) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ContainerScreenAccessor container = (ContainerScreenAccessor) screen;

        ChestReader.Chest bedrock = null;
        for (ChestReader.Chest chest : chests) {
            if (chest.bedrock()) bedrock = chest;
        }

        KismetCalculator.Analysis analysis = prices.analysis();
        Reading reading = meter();
        double meter = reading.fraction();
        long keyCost = KismetConfig.keyCostOverride > 0
            ? KismetConfig.keyCostOverride
            : book.keyCost();

        KismetCalculator.Verdict verdict = null;
        if (analysis != null && bedrock != null) {
            List<Long> alts = new ArrayList<>();
            for (ChestReader.Chest chest : chests) {
                if (!chest.bedrock()) alts.add(chest.net());
            }
            verdict = KismetCalculator.decide(analysis, meter, bedrock.net(), alts, keyCost);
        }
        RerollGuard.update(screen, verdict);

        String header = "Kismet";
        String status;
        int statusColor;
        if (chests.isEmpty()) {
            status = "nothing read";
            statusColor = PENDING;
        } else if (analysis == null) {
            status = com.kismetcalc.core.CombinationTable.failure() == null ? "loading" : "no table";
            statusColor = TEXT_DIM;
        } else if (bedrock == null) {
            status = "not M7";
            statusColor = TEXT_DIM;
        } else if (verdict.reroll()) {
            status = "REROLL";
            statusColor = ACCENT;
        } else {
            status = "OPEN";
            statusColor = PENDING;
        }

        List<String[]> lines = new ArrayList<>();
        int unpriced = 0;
        if (KismetConfig.breakdown) {
            for (ChestReader.Chest chest : chests) {
                lines.add(new String[]{chest.label(), coins(chest.net())});
                unpriced += chest.unpriced();
            }
        } else if (bedrock != null) {
            lines.add(new String[]{bedrock.label(), coins(bedrock.net())});
            unpriced = bedrock.unpriced();
        }
        if (KismetConfig.dev) {
            for (ChestReader.Chest chest : chests) {
                lines.add(new String[]{chest.label() + " val", coins(chest.valuation().value())});
            }
            if (chests.isEmpty()) {
                lines.add(new String[]{"Slots",
                    Integer.toString(Math.max(0, screen.getMenu().slots.size() - 36))});
            }
        }
        if (verdict != null) {
            lines.add(new String[]{"Threshold", coins(Math.round(verdict.threshold()))});
        }
        if (analysis != null) {
            lines.add(new String[]{"Reroll EV", coins(Math.round(analysis.evProfitable()))});
        }

        String footerLeft = reading.label() + (unpriced > 0 ? " · " + unpriced + " unpriced" : "");
        if (KismetConfig.dev) footerLeft += " · dev";
        String footerRight = priceAge(book, prices);

        final int pad = 5;
        final int rowH = font.lineHeight + 2;
        final int headerH = font.lineHeight + 4;
        final int footerH = font.lineHeight + 5;
        final int gap = 10;

        int labelW = 0;
        int valueW = 0;
        for (String[] line : lines) {
            labelW = Math.max(labelW, font.width(line[0]));
            valueW = Math.max(valueW, font.width(line[1]));
        }
        int colW = Math.max(labelW + gap + valueW, font.width(header) + gap + font.width(status));
        int panelW = pad * 2 + colW;

        int footerW = pad * 2 + font.width(footerLeft) + gap + font.width(footerRight);
        if (panelW < footerW) {
            colW += footerW - panelW;
            panelW = pad * 2 + colW;
        }
        int panelH = pad * 2 + headerH + lines.size() * rowH + footerH;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int x = container.getLeftPos() + container.getImageWidth() + 4;
        if (x + panelW > screenW) x = container.getLeftPos() - panelW - 4;
        x = Math.max(2, Math.min(x, screenW - panelW - 2));
        int y = Math.max(2, Math.min(container.getTopPos(), screenH - panelH - 2));

        graphics.fill(x, y, x + panelW, y + panelH, PANEL);
        outline(graphics, x, y, panelW, panelH);

        text(graphics, font, header, x + pad, y + pad, headerH, ACCENT);
        textRight(graphics, font, status, x + panelW - pad, y + pad, headerH, statusColor);
        graphics.fill(x + pad, y + pad + headerH - 1, x + panelW - pad, y + pad + headerH, BORDER);

        int rowsTop = y + pad + headerH;
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i);
            int rowY = rowsTop + i * rowH;
            boolean isChest = KismetConfig.breakdown && i < chests.size();

            int labelColor = isChest && chests.get(i).bedrock() ? TEXT : TEXT_MUTED;
            // Colour carries the sign, which is one character at this size.
            int valueColor = isChest ? (chests.get(i).profitable() ? ACCENT : TEXT_DIM) : TEXT;

            text(graphics, font, line[0], x + pad, rowY, rowH, labelColor);
            textRight(graphics, font, line[1], x + panelW - pad, rowY, rowH, valueColor);
        }

        int footerY = rowsTop + lines.size() * rowH;
        graphics.fill(x + pad, footerY + 1, x + panelW - pad, footerY + 2, BORDER);
        text(graphics, font, footerLeft, x + pad, footerY + 2, footerH, unpriced > 0 ? PENDING : TEXT_DIM);
        textRight(graphics, font, footerRight, x + panelW - pad, footerY + 2, footerH,
            book.live() ? TEXT_DIM : PENDING);

        shout(graphics, font, verdict, container, screenW, screenH);
    }

    // The panel is a table you have to read. This is the one line you cannot miss on the way to
    // clicking, so it says only which of the two things to do.
    private static void shout(GuiGraphicsExtractor graphics, Font font, KismetCalculator.Verdict verdict,
                              ContainerScreenAccessor container, int screenW, int screenH) {
        if (verdict == null) return;

        String message = verdict.reroll() ? GO : STOP;
        int color = verdict.reroll() ? SHOUT_GO : SHOUT_STOP;
        boolean blocked = !verdict.reroll() && RerollGuard.recentlyBlocked();

        int textW = font.width(message);
        // The happy line is long; shrink rather than run off the edge of a small window.
        float scale = Math.min(3.0f, (screenW - 16) / (float) Math.max(1, textW));
        if (scale < 1.0f) scale = 1.0f;

        int drawnW = Math.round(textW * scale);
        int drawnH = Math.round(font.lineHeight * scale);

        int x = container.getLeftPos() + container.getImageWidth() / 2 - drawnW / 2;
        x = Math.max(4, Math.min(x, screenW - drawnW - 4));
        int y = container.getTopPos() - drawnH - 8;
        if (y < 4) y = 4;
        if (y + drawnH > screenH - 4) y = Math.max(4, screenH - drawnH - 4);

        graphics.fill(x - 5, y - 4, x + drawnW + 5, y + drawnH + 4,
            blocked ? SHOUT_BACK_BLOCKED : SHOUT_BACK);

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, Component.literal(message).withStyle(ChatFormatting.BOLD), 0, 0, color, true);
        graphics.pose().popMatrix();

        if (blocked) {
            String note = "click blocked · /kismet gamblingmode false to override";
            int noteX = x + drawnW / 2 - font.width(note) / 2;
            graphics.text(font, note, Math.max(2, noteX), y + drawnH + 6, SHOUT_STOP, true);
        }
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
    }

    private static void text(GuiGraphicsExtractor g, Font font, String s, int x, int rowY, int rowH, int color) {
        g.text(font, s, x, rowY + (rowH - font.lineHeight) / 2 + 1, color, false);
    }

    private static void textRight(GuiGraphicsExtractor g, Font font, String s, int rightX, int rowY,
                                  int rowH, int color) {
        g.text(font, s, rightX - font.width(s), rowY + (rowH - font.lineHeight) / 2 + 1, color, false);
    }

    public record Reading(double fraction, String label, boolean tracked) {
    }

    public static Reading meter() {
        RngMeterTracker.Progress progress = RngMeterTracker.getInstance().progress(RngMeterTracker.M7);
        if (progress != null) {
            int percent = (int) Math.round(progress.fraction() * 100);
            return new Reading(progress.fraction(),
                progress.exact() ? "meter " + percent + "%"
                    : "meter ~" + percent + "% (" + progress.runs() + " runs)",
                true);
        }
        double manual = Math.max(0, Math.min(100, KismetConfig.meterProgress)) / 100.0;
        return new Reading(manual, "meter " + KismetConfig.meterProgress + "% set", false);
    }

    static String coins(long amount) {
        long magnitude = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";
        if (magnitude >= 1_000_000_000L) return sign + String.format(Locale.ROOT, "%.2fB", magnitude / 1e9);
        if (magnitude >= 1_000_000L) return sign + String.format(Locale.ROOT, "%.2fM", magnitude / 1e6);
        if (magnitude >= 1_000L) return sign + String.format(Locale.ROOT, "%.0fk", magnitude / 1e3);
        return Long.toString(amount);
    }

    private static String priceAge(PriceBook book, PriceClient prices) {
        String basis = book.basis() == PriceBook.Basis.OFFER ? "offer" : "insta";
        if (!book.live()) {
            return prices.lastError() == null ? basis + " · baked" : basis + " · offline";
        }
        long age = System.currentTimeMillis() - book.fetchedAt();
        if (age < 60_000L) return basis + " · now";
        long minutes = age / 60_000L;
        return minutes < 90 ? basis + " · " + minutes + "m" : basis + " · " + (minutes / 60) + "h";
    }

    static double handleChance() {
        return MeterMath.handleChance(meter().fraction());
    }
}
