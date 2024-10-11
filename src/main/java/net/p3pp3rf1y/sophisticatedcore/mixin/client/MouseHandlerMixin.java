package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.InteractionResult;
import net.p3pp3rf1y.sophisticatedcore.event.client.ClientRawInputEvent;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onScroll", at= @At(value="FIELD", target="Lnet/minecraft/client/MouseHandler;accumulatedScroll:D", ordinal = 5, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void  sophisticatedCore$onScroll(long handle, double xOffset, double yOffset, CallbackInfo ci, double d) {
        if (handle == this.minecraft.getWindow().getWindow()) {
            var result = ClientRawInputEvent.MOUSE_SCROLLED.invoker().mouseScrolled(minecraft, d);
            if (result != InteractionResult.PASS)
                ci.cancel();
        }
    }
}
