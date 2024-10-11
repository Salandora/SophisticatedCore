package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
	@Inject(method = "drop(Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
	private void sophisticatedcore$drop(boolean dropStack, CallbackInfoReturnable<Boolean> cir, @Local Inventory inventory) {
		ItemStack selected = inventory.getSelected();
		if (selected.isEmpty() || !selected.onDroppedByPlayer((ServerPlayer) (Object) this)) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}
}
