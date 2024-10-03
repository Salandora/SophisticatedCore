package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

@SuppressWarnings("unused")
@JeiPlugin
public class JeiCompat implements IModPlugin, ICompat {
	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(SophisticatedCore.MOD_ID, "default");
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(RecipeTypes.CRAFTING, ClientRecipeHelper.getAndTransformAvailableRecipes(UpgradeNextTierRecipe.REGISTERED_RECIPES, ShapedRecipe.class, ClientRecipeHelper::copyShapedRecipe));
	}

	@Override
	public void setup() {
		PacketHandler.registerC2SMessage(TransferRecipeMessage.class, TransferRecipeMessage::new);
		PacketHandler.registerC2SMessage(SetGhostSlotMessage.class, SetGhostSlotMessage::new);
		PacketHandler.registerC2SMessage(SetMemorySlotMessage.class, SetMemorySlotMessage::new);
	}
}
