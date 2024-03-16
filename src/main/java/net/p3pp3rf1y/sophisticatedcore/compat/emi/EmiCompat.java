package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class EmiCompat implements EmiPlugin, ICompat {
    @Override
    public void register(EmiRegistry registry) {
        ClientRecipeHelper.getAndTransformAvailableRecipes(UpgradeNextTierRecipe.REGISTERED_RECIPES, ShapedRecipe.class, ClientRecipeHelper::copyShapedRecipe).forEach(r ->
            registry.addRecipe(new EmiCraftingRecipe(
                r.getIngredients().stream().map(EmiIngredient::of).toList(),
                EmiStack.of(r.getResultItem()),
                r.getId()
        )));
    }

    @Override
    public void setup() {
        PacketHandler.registerC2SMessage(EmiFillRecipeC2SPacket.class, EmiFillRecipeC2SPacket::new);
        PacketHandler.registerC2SMessage(SetGhostSlotMessage.class, SetGhostSlotMessage::new);
        PacketHandler.registerC2SMessage(SetMemorySlotMessage.class, SetMemorySlotMessage::new);
    }
}
