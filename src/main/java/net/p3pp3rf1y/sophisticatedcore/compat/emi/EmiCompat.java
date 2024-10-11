package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

public class EmiCompat implements EmiPlugin, ICompat {
    @Override
    public void register(EmiRegistry registry) {
		Minecraft mc = Minecraft.getInstance();
        ClientRecipeHelper.transformAllRecipesOfType(RecipeType.CRAFTING, UpgradeNextTierRecipe.class, ClientRecipeHelper::copyShapedRecipe).forEach(r ->
            registry.addRecipe(new EmiCraftingRecipe(
                r.value().getIngredients().stream().map(EmiIngredient::of).toList(),
                EmiStack.of(r.value().getResultItem(mc.level.registryAccess())),
                r.id()
        )));
    }

    @Override
    public void setup() {
		ServerPlayNetworking.registerGlobalReceiver(EmiFillRecipePacket.TYPE, EmiFillRecipePacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(SetGhostSlotMessage.TYPE, SetGhostSlotMessage::handle);
		ServerPlayNetworking.registerGlobalReceiver(SetMemorySlotMessage.TYPE, SetMemorySlotMessage::handle);
    }
}
