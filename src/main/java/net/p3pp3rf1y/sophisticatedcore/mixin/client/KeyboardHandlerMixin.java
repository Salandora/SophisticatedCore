package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.p3pp3rf1y.sophisticatedcore.event.client.ClientRawInputEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("RETURN"), cancellable = true)
    public void sophisticatedcore$keyPress(long handle, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (handle == this.minecraft.getWindow().getWindow()) {
            var result = ClientRawInputEvent.KEY_PRESSED.invoker().keyPressed(minecraft, key, scanCode, action, modifiers);
            if (result != InteractionResult.PASS)
                ci.cancel();
        }
    }
}
