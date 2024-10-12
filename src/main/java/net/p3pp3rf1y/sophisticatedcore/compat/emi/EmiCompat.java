package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SetGhostSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SetMemorySlotPayload;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;

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
		ModPayloads.registerC2S(EmiFillRecipePacket.TYPE, EmiFillRecipePacket.STREAM_CODEC, EmiFillRecipePacket::handlePayload);
		ModPayloads.registerC2S(SetGhostSlotPayload.TYPE, SetGhostSlotPayload.STREAM_CODEC, SetGhostSlotPayload::handlePayload);
		ModPayloads.registerC2S(SetMemorySlotPayload.TYPE, SetMemorySlotPayload.STREAM_CODEC, SetMemorySlotPayload::handlePayload);
    }
}
