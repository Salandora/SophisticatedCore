package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.google.common.base.Preconditions;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.ShapedRecipeAccessor;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class ShapeBasedRecipeBuilder extends ShapedRecipeBuilder {
	private final Function<ShapedRecipe, ? extends ShapedRecipe> factory;

	private ShapeBasedRecipeBuilder(ItemStack result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		super(RecipeCategory.MISC, result.getItem(), result.getCount());
		this.factory = factory;
	}

	public static ShapeBasedRecipeBuilder shaped(ItemStack result) {
		// Fabric does not have a NBT storing recipe so we make our own
		return new ShapeBasedRecipeBuilder(result, r -> new SCShapedRecipe(r.getGroup(), r.category(), ((ShapedRecipeAccessor) r).getPattern(), result, r.showNotification()));
	}

	public static ShapeBasedRecipeBuilder shaped(ItemLike result) {
		return shaped(new ItemStack(result));
	}

	public static ShapeBasedRecipeBuilder shaped(ItemLike result, Function<ShapedRecipe, ? extends ShapedRecipe> factory) {
		return new ShapeBasedRecipeBuilder(new ItemStack(result, 1), factory);
	}

	@Override
	public void save(RecipeOutput recipeOutput) {
		save(recipeOutput, BuiltInRegistries.ITEM.getKey(getResult().asItem()));
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceLocation id) {
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		super.save(holdingRecipeOutput, id);

		if (!(holdingRecipeOutput.getRecipe() instanceof ShapedRecipe compose)) {
			return;
		}

		withConditions(recipeOutput, new ItemEnabledCondition(getResult())).accept(id, factory.apply(compose), holdingRecipeOutput.getAdvancementHolder());
	}

	protected RecipeOutput withConditions(final RecipeOutput exporter, final ConditionJsonProvider... conditions) {
		Preconditions.checkArgument(conditions.length > 0, "Must add at least one condition.");
		return new RecipeOutput() {
			public void accept(ResourceLocation identifier, Recipe<?> recipe, @Nullable AdvancementHolder advancementEntry) {
				FabricDataGenHelper.addConditions(recipe, conditions);
				exporter.accept(identifier, recipe, advancementEntry);
			}

			public Advancement.Builder advancement() {
				return exporter.advancement();
			}
		};
	}
}
