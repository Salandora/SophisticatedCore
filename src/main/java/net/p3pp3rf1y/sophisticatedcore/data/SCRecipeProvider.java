package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;

public class SCRecipeProvider extends FabricRecipeProvider {
	public SCRecipeProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void buildRecipes(RecipeOutput recipeOutput) {
		SpecialRecipeBuilder.special(UpgradeClearRecipe::new).save(recipeOutput, SophisticatedCore.getRegistryName("upgrade_clear"));
	}
}
