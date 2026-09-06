package com.kismetcalc.client;

import com.kismetcalc.KismetCommands;
import com.kismetcalc.KismetConfig;
import com.kismetcalc.RngMeterTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

// Prices and the 80KB combination table are not started here - they wait until a chest window
// is first open. The meter tracker is the exception: a run's score line arrives once and is gone.
public class KismetCalcClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KismetConfig.load();
        KismetCommands.register();

        RngMeterTracker.getInstance().register();
        // The sidebar's floor line, so a score in chat knows which meter it belongs to.
        ClientTickEvents.END_CLIENT_TICK.register(client -> RngMeterTracker.getInstance().tick());
    }
}
