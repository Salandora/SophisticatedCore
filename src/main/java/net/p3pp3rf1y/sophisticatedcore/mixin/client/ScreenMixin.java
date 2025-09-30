package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
	@Inject(
			method = "render",
			at = @At("HEAD"),
			order = 100000, // Make sure this injection runs last
			cancellable = true
	)
	private void sophisticatedCore$renderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		//noinspection ConstantValue
		if ((Object) this instanceof StorageScreenBase<?> || (Object) this instanceof SettingsScreen) {
			ci.cancel();
		}
	}
}
