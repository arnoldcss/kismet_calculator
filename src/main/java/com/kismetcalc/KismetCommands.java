package com.kismetcalc;

import com.kismetcalc.core.CombinationTable;
import com.kismetcalc.core.KismetCalculator;
import com.kismetcalc.core.KismetItem;
import com.kismetcalc.core.M7LootTable;
import com.kismetcalc.core.MeterMath;
import com.kismetcalc.core.NameToItem;
import com.kismetcalc.core.PriceBook;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Set;

//command tree
public final class KismetCommands {

    private KismetCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
            dispatcher.register(ClientCommands.literal("kismet")
                .executes(context -> report(context.getSource()))
                .then(ClientCommands.literal("meter")
                    .executes(context -> meter(context.getSource()))
                    .then(ClientCommands.argument("percent", IntegerArgumentType.integer(0, 100))
                        .executes(context -> {
                            KismetConfig.meterProgress = IntegerArgumentType.getInteger(context, "percent");
                            KismetConfig.save();
                            return report(context.getSource());
                        })))
                .then(ClientCommands.literal("key")
                    .executes(context -> key(context.getSource(), 0L))
                    .then(ClientCommands.argument("coins", LongArgumentType.longArg(0))
                        .executes(context -> key(context.getSource(),
                            LongArgumentType.getLong(context, "coins")))))
                .then(ClientCommands.literal("offers")
                    .then(ClientCommands.argument("on", BoolArgumentType.bool())
                        .executes(context -> {
                            KismetConfig.priceOffers = BoolArgumentType.getBool(context, "on");
                            KismetConfig.save();
                            say(context.getSource(), "§7Pricing at §f"
                                + (KismetConfig.priceOffers ? "sell offers" : "instant sell"));
                            return 1;
                        })))
                .then(ClientCommands.literal("overlay")
                    .then(ClientCommands.argument("on", BoolArgumentType.bool())
                        .executes(context -> {
                            KismetConfig.overlay = BoolArgumentType.getBool(context, "on");
                            KismetConfig.save();
                            say(context.getSource(), "§7Overlay §f"
                                + (KismetConfig.overlay ? "on" : "off"));
                            return 1;
                        })))
                .then(ClientCommands.literal("breakdown")
                    .then(ClientCommands.argument("on", BoolArgumentType.bool())
                        .executes(context -> {
                            KismetConfig.breakdown = BoolArgumentType.getBool(context, "on");
                            KismetConfig.save();
                            say(context.getSource(), "§7Chest breakdown §f"
                                + (KismetConfig.breakdown ? "on" : "off"));
                            return 1;
                        })))
                .then(ClientCommands.literal("gamblingmode")
                    .then(ClientCommands.argument("on", BoolArgumentType.bool())
                        .executes(context -> gamblingMode(context.getSource(),
                            BoolArgumentType.getBool(context, "on")))))
                .then(ClientCommands.literal("prices")
                    .executes(context -> prices(context.getSource())))
                .then(ClientCommands.literal("unknown")
                    .executes(context -> unknown(context.getSource())))
                .then(ClientCommands.literal("debug")
                    .executes(context -> debug(context.getSource())))
                .then(ClientCommands.literal("dev")
                    .executes(context -> dev(context.getSource(), !KismetConfig.dev))
                    .then(ClientCommands.argument("on", BoolArgumentType.bool())
                        .executes(context ->
                            dev(context.getSource(), BoolArgumentType.getBool(context, "on")))))
                .then(ClientCommands.literal("dump")
                    .executes(context -> dump(context.getSource())))));
    }

    private static int report(FabricClientCommandSource source) {
        PriceClient client = PriceClient.getInstance();
        client.ensureFresh();

        KismetCalculator.Analysis analysis = client.analysis();
        if (analysis == null) {
            String failure = CombinationTable.failure();
            say(source, failure == null
                ? "§7Still loading the combination table - try again in a moment."
                : "§cThe combination table did not load: §7" + failure);
            return 1;
        }

        AdvisorOverlay.Reading reading = AdvisorOverlay.meter();
        double meter = reading.fraction();
        double handleChance = MeterMath.handleChance(meter);
        double base = KismetCalculator.baseThreshold(analysis, meter);
        PriceBook book = client.book();

        say(source, "§6Kismet §8(M7 bedrock)");
        say(source, "§7Reroll EV §f" + AdvisorOverlay.coins(Math.round(analysis.evProfitable()))
            + " §8over " + analysis.profitableCount() + " profitable rolls, "
            + String.format(Locale.ROOT, "%.2f%%", analysis.profitableChance() * 100));
        say(source, "§7  without the handle §f"
            + AdvisorOverlay.coins(Math.round(analysis.evProfitableNoHandle())));
        say(source, "§7  less a feather §f-" + AdvisorOverlay.coins(analysis.kismetCost()));
        say(source, "§7  plus the handle at " + Math.round(meter * 100) + "% meter §f+"
            + AdvisorOverlay.coins(Math.round(handleChance * analysis.handleProfit()))
            + " §8(" + String.format(Locale.ROOT, "%.4f%%", handleChance * 100)
            + " x " + AdvisorOverlay.coins(analysis.handleProfit()) + ")");
        say(source, "§7Reroll anything under §a" + AdvisorOverlay.coins(Math.round(base))
            + " §8before the run's other chests");
        say(source, "§7Dungeon key: §f"
            + AdvisorOverlay.coins(KismetConfig.keyCostOverride > 0
                ? KismetConfig.keyCostOverride : book.keyCost())
            + (KismetConfig.keyCostOverride > 0 ? " §8(set by hand)" : " §8(bazaar)"));
        say(source, "§7Meter: §f" + reading.label()
            + (reading.tracked() ? " §8(read from the menu)" : " §8(set by hand - open /rng to track it)"));
        say(source, "§8Prices: "
            + (book.basis() == PriceBook.Basis.OFFER ? "sell offers" : "instant sell")
            + ", " + (book.live() ? "fetched" : "baked into the jar")
            + (client.lastError() == null ? "" : " - last fetch failed: " + client.lastError()));
        return 1;
    }

    private static int meter(FabricClientCommandSource source) {
        RngMeterTracker.Progress progress = RngMeterTracker.getInstance().progress(RngMeterTracker.M7);
        if (progress == null) {
            say(source, "§7No reading yet. Open §f/rng catacombs m7§7 once and it starts tracking.");
            say(source, "§8Until then the set value is used: " + KismetConfig.meterProgress + "%");
            return 1;
        }

        long ago = (System.currentTimeMillis() - progress.seededAt()) / 60_000L;
        say(source, "§6RNG Meter §8(M7 - Necron's Handle)");
        say(source, "§7" + AdvisorOverlay.coins(progress.stored()) + " §8/ §7"
            + AdvisorOverlay.coins(progress.needed()) + " §f"
            + String.format(Locale.ROOT, "%.1f%%", progress.fraction() * 100));
        say(source, "§7Read from the menu §f" + (ago < 90 ? ago + "m" : (ago / 60) + "h") + " ago");
        say(source, progress.exact()
            ? "§7Nothing added since - this is exact."
            : "§7Since then: §f" + progress.runs() + " runs counted"
                + " §8(runs this mod did not see are not in there - open the menu again)");
        return 1;
    }

    private static int prices(FabricClientCommandSource source) {
        PriceClient client = PriceClient.getInstance();
        client.ensureFresh();
        PriceBook book = client.book();

        say(source, "§6Kismet prices §8(" + book.size() + " items, "
            + (book.live() ? "fetched" : "baked") + ")");
        for (KismetItem item : M7LootTable.ITEMS) {
            PriceBook.Entry entry = book.entry(item.itemId());
            if (entry == null) {
                say(source, "§8" + item.name() + " §c- no price");
                continue;
            }
            say(source, "§7" + item.name() + " §f" + AdvisorOverlay.coins(entry.insta())
                + " §8/ " + AdvisorOverlay.coins(entry.offer()) + " " + entry.source());
        }
        return 1;
    }

    private static int key(FabricClientCommandSource source, long coins) {
        KismetConfig.keyCostOverride = coins;
        KismetConfig.save();
        if (coins == 0) {
            say(source, "§7Dungeon key cost: §fbazaar §8("
                + AdvisorOverlay.coins(PriceClient.getInstance().book().keyCost()) + ")");
            return 1;
        }
        say(source, "§7Dungeon key cost: §f" + AdvisorOverlay.coins(coins));
        say(source, "§8Set by hand, so the bazaar price is ignored. §f/kismet key 0§8 to go back"
            + " to it.");
        return 1;
    }

    private static int gamblingMode(FabricClientCommandSource source, boolean on) {
        KismetConfig.gamblingMode = on;
        KismetConfig.save();
        say(source, "§7Gambling mode §f" + (on ? "on" : "off"));
        if (on) {
            say(source, "§8Clicks on the reroll button do not reach the server while the chest is"
                + " worth more than a reroll. Everything else in the window still works.");
        }
        return 1;
    }

    private static int dev(FabricClientCommandSource source, boolean on) {
        KismetConfig.dev = on;
        KismetConfig.save();
        say(source, "§7Dev mode §f" + (on ? "on" : "off"));
        if (on) {
            say(source, "§8The panel now stays up on windows it cannot read, and every window you"
                + " open is captured for §f/kismet dump§8.");
        }
        return 1;
    }

    private static int dump(FabricClientCommandSource source) {
        if (!KismetConfig.dev) {
            say(source, "§7Dev mode is off, so no window has been captured. §f/kismet dev§7 first.");
            return 1;
        }
        WindowDump.Result result = WindowDump.write();
        if (result == null) {
            say(source, WindowDump.captured()
                ? "§cCould not write the dump file."
                : "§7No window captured yet - open one with dev mode on, then close it.");
            return 1;
        }
        say(source, "§6Dumped §f" + result.title() + "§7, " + result.slots() + " filled slots");
        say(source, "§8" + result.file());
        return 1;
    }

    private static int unknown(FabricClientCommandSource source) {
        Set<String> lines = NameToItem.unresolved();
        Set<String> ids = PriceClient.getInstance().book().unknownIds();

        if (lines.isEmpty() && ids.isEmpty()) {
            say(source, "§7Everything seen so far resolved and had a price.");
            return 1;
        }
        if (!lines.isEmpty()) {
            say(source, "§6Reward lines with no item: §7" + String.join(", ", lines));
        }
        if (!ids.isEmpty()) {
            say(source, "§6Items with no price: §7" + String.join(", ", ids));
        }
        return 1;
    }

    private static int debug(FabricClientCommandSource source) {
        PriceClient client = PriceClient.getInstance();
        say(source, "§6Kismet prices §8(request debug)");
        say(source, "§7" + client.debug());

        long last = client.lastRequestAt();
        say(source, "§7Last request: §f"
            + (last == 0 ? "none this session"
                : ((System.currentTimeMillis() - last) / 1000L) + "s ago"));
        say(source, "§8Table: " + (CombinationTable.loaded()
            ? "loaded"
            : (CombinationTable.failure() == null ? "loading" : "failed")));
        return 1;
    }

    private static void say(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(message));
    }
}
