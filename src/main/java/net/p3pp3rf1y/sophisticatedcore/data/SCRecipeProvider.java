package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;

import java.util.concurrent.CompletableFuture;

public class SCRecipeProvider extends FabricRecipeProvider {
	public SCRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	public void buildRecipes(RecipeOutput recipeOutput) {
		SpecialRecipeBuilder.special(UpgradeClearRecipe::new).save(recipeOutput, SophisticatedCore.getRegistryName("upgrade_clear"));
	}
}
