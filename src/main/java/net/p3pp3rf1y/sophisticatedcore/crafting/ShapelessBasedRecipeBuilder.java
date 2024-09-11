package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.google.common.base.Preconditions;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

public class ShapelessBasedRecipeBuilder extends ShapelessRecipeBuilder {
	private final Function<ShapelessRecipe, ? extends ShapelessRecipe> factory;


	public ShapelessBasedRecipeBuilder(ItemStack result, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		super(RecipeCategory.MISC, result.getItem(), result.getCount());
		this.factory = factory;
	}

	public ShapelessBasedRecipeBuilder(ItemLike result, int count, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		this(new ItemStack(result, count), factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemStack result) {
		// Fabric does not have a NBT storing recipe so we make our own
		return new ShapelessBasedRecipeBuilder(result, r -> new SCShapelessRecipe(r.getGroup(), r.category(), result, r.getIngredients()));
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result) {
		return shapeless(result, 1);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result, int count) {
		return shapeless(new ItemStack(result, count));
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		return shapeless(result, 1, factory);
	}

	public static ShapelessBasedRecipeBuilder shapeless(ItemLike result, int count, Function<ShapelessRecipe, ? extends ShapelessRecipe> factory) {
		return new ShapelessBasedRecipeBuilder(result, count, factory);
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceLocation id) {
		HoldingRecipeOutput holdingRecipeOutput = new HoldingRecipeOutput(recipeOutput.advancement());
		super.save(holdingRecipeOutput, id);

		if (!(holdingRecipeOutput.getRecipe() instanceof ShapelessRecipe compose)) {
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