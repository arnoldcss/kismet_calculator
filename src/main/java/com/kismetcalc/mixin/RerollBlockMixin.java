package com.kismetcalc.mixin;

import com.kismetcalc.RerollGuard;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Every way of clicking a slot - the mouse, a hotbar key, a double click - ends up in slotClicked,
// so this is the one place a reroll can be stopped before it becomes a packet.
@Mixin(AbstractContainerScreen.class)
public class RerollBlockMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kismetcalc$guard(Slot slot, int slotId, int button, ContainerInput input,
                                  CallbackInfo ci) {
        if (RerollGuard.blocks((AbstractContainerScreen<?>) (Object) this, slot)) {
            RerollGuard.onBlocked();
            ci.cancel();
        }
    }
}
