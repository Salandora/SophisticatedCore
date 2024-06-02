package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.item.crafting.RecipeManager;
import net.p3pp3rf1y.sophisticatedcore.event.client.ClientRecipesUpdated;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Shadow
	@Final
	private RecipeManager recipeManager;

	@Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
	 private void sophisticatedCore$handleUpdateRecipes(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
		 ClientRecipesUpdated.EVENT.invoker().onRecipesUpdated(this.recipeManager);
	 }
}
