package net.p3pp3rf1y.sophisticatedcore.init;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.crafting.ItemEnabledCondition;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeClearRecipe;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleIdentifiablePrepareableReloadListener;

public class ModRecipes {
	public static final RecipeSerializer<?> UPGRADE_NEXT_TIER_SERIALIZER = register("upgrade_next_tier", new UpgradeNextTierRecipe.Serializer());
	public static final SimpleCraftingRecipeSerializer<?> UPGRADE_CLEAR_SERIALIZER = register("upgrade_clear", new SimpleCraftingRecipeSerializer<>(UpgradeClearRecipe::new));

	public static <T extends RecipeSerializer<?>> T register(String id, T value) {
		return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SophisticatedCore.getRL(id), value);
	}

	public static void registerHandlers() {
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new SimpleIdentifiablePrepareableReloadListener<>(SophisticatedCore.getRL("modrecipes")) {
			@Override
			protected void apply(Object object, ResourceManager resourceManager, ProfilerFiller profiler) {
				UpgradeNextTierRecipe.REGISTERED_RECIPES.clear();
			}
		});

		ResourceConditions.register(ItemEnabledCondition.NAME, ItemEnabledCondition::test);
	}
}
