package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.p3pp3rf1y.sophisticatedcore.compat.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class REIClientCompat implements REIClientPlugin {
	@Override
	public void registerDisplays(DisplayRegistry registry) {
		List<CraftingRecipe> recipes = ClientRecipeHelper.getAndTransformAvailableRecipes(UpgradeNextTierRecipe.REGISTERED_RECIPES, ShapedRecipe.class, ClientRecipeHelper::copyShapedRecipe);
		for (CraftingRecipe recipe : recipes) {
			Collection<Display> displays = registry.tryFillDisplay(recipe);
			for (Display display : displays) {
				if (Objects.equals(display.getCategoryIdentifier(), BuiltinPlugin.CRAFTING)) {
					registry.add(display, recipe);
				}
			}
		}
	}

	@Override
	public void registerScreens(ScreenRegistry registry) {
		registry.registerDraggableStackVisitor(new SettingsGhostIngredientHandler<>());
	}
}
