package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;

import java.util.function.Consumer;

public class SCRecipeProvider extends FabricRecipeProvider {
	public SCRecipeProvider(FabricDataGenerator output) {
		super(output);
	}

	@Override
	protected void generateRecipes(Consumer<FinishedRecipe> exporter) {
		SpecialRecipeBuilder.special(ModRecipes.UPGRADE_CLEAR_SERIALIZER).save(exporter, SophisticatedCore.getRL("upgrade_clear").toString());
	}
}
