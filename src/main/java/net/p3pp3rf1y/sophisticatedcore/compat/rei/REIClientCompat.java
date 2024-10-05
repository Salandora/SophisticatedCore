package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.compat.common.ClientRecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.crafting.UpgradeNextTierRecipe;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;

import java.util.Collection;
import java.util.Objects;

public class REIClientCompat implements REIClientPlugin {
	@Override
	public void registerDisplays(DisplayRegistry registry) {
		ClientRecipeHelper.transformAllRecipesOfType(RecipeType.CRAFTING, UpgradeNextTierRecipe.class, ClientRecipeHelper::copyShapedRecipe).forEach(r -> {
			Collection<Display> displays = registry.tryFillDisplay(r);
			for (Display display : displays) {
				if (Objects.equals(display.getCategoryIdentifier(), BuiltinPlugin.CRAFTING)) {
					registry.add(display, r);
				}
			}
		});
	}

	@Override
	public void registerScreens(ScreenRegistry registry) {
		registry.registerDraggableStackVisitor(new SettingsGhostIngredientHandler<>());
	}
}
