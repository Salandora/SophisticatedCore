package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.*;

public class ModRecipes {
	private ModRecipes() {
	}

	// Fabric does not have a NBT storing recipe so we make our own
	public static final RecipeSerializer<SCShapedRecipe> SHAPED_RECIPE_SERIALIZER = register("shaped_recipe", new SCShapedRecipe.Serializer());
	public static final RecipeSerializer<SCShapelessRecipe> SHAPLESS_RECIPE_SERIALIZER = register("shapless_recipe", new SCShapelessRecipe.Serializer());

	public static final RecipeSerializer<?> UPGRADE_NEXT_TIER_SERIALIZER = register("upgrade_next_tier", new UpgradeNextTierRecipe.Serializer());
	public static final SimpleCraftingRecipeSerializer<?> UPGRADE_CLEAR_SERIALIZER = register("upgrade_clear", new SimpleCraftingRecipeSerializer<>(UpgradeClearRecipe::new));

	public static <T extends RecipeSerializer<?>> T register(String id, T value) {
		return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SophisticatedCore.getRL(id), value);
	}

	public static void registerHandlers() {
		ResourceConditions.register(ItemEnabledCondition.NAME, ItemEnabledCondition::test);
	}
}
