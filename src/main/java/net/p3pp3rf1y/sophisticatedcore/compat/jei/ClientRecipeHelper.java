package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ClientRecipeHelper {
	private ClientRecipeHelper() {}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>> List<RecipeHolder<T>> transformAllRecipesOfType(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, T> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return level.getRecipeManager()
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> new RecipeHolder<>(r.id(), transformRecipe.apply(filterRecipeClass.cast(r.value()))))
				.toList();
	}

	public static <I extends RecipeInput, T extends Recipe<I>, U extends Recipe<?>> List<RecipeHolder<T>> transformAllRecipesOfTypeIntoMultiple(RecipeType<T> recipeType, Class<U> filterRecipeClass, Function<U, List<RecipeHolder<T>>> transformRecipe) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return Collections.emptyList();
		}

		return level.getRecipeManager()
				.getAllRecipesFor(recipeType)
				.stream()
				.filter(r -> filterRecipeClass.isInstance(r.value()))
				.map(r -> transformRecipe.apply(filterRecipeClass.cast(r.value())))
				.collect(ArrayList::new, List::addAll, List::addAll);
	}

	public static CraftingRecipe copyShapedRecipe(ShapedRecipe recipe) {
		return new ShapedRecipe("", recipe.category(), recipe.pattern, getResultItem(recipe));
	}

	public static CraftingRecipe copyShapelessRecipe(ShapelessRecipe recipe) {
		return new ShapelessRecipe("", recipe.category(), getResultItem(recipe), recipe.getIngredients());
	}

	private static ItemStack registryAccessAware(Function<RegistryAccess, ItemStack> func) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			throw new NullPointerException("level must not be null.");
		}
		RegistryAccess registryAccess = level.registryAccess();
		return func.apply(registryAccess);
	}

	public static ItemStack getResultItem(Recipe<?> recipe) {
		return registryAccessAware(recipe::getResultItem);
	}

	public static <I extends RecipeInput> ItemStack assemble(Recipe<I> recipe, I container) {
		return registryAccessAware(registry -> recipe.assemble(container, registry));
	}
}
