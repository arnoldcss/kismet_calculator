package com.kismetcalc.mixin;

import com.kismetcalc.AdvisorOverlay;
import com.kismetcalc.RngMeterTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
//for container screens
public class ContainerScreenMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void kismetcalc$advise(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   float partialTick, CallbackInfo ci) {
        AdvisorOverlay.render((AbstractContainerScreen<?>) (Object) this, guiGraphics);
        RngMeterTracker.getInstance().readScreen((AbstractContainerScreen<?>) (Object) this);
    }
}
