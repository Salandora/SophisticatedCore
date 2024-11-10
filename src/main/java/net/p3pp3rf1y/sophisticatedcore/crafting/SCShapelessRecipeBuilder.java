package net.p3pp3rf1y.sophisticatedcore.crafting;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SCShapelessRecipeBuilder implements RecipeBuilder {
	private final RecipeCategory category;
	private final Item result;
	private final ItemStack resultStack;
	private final NonNullList<Ingredient> ingredients = NonNullList.create();
	private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
	@Nullable
	private String group;

	public SCShapelessRecipeBuilder(RecipeCategory category, ItemLike result, int count) {
		this(category, new ItemStack(result, count));
	}

	public SCShapelessRecipeBuilder(RecipeCategory category, ItemStack result) {
		this.category = category;
		this.result = result.getItem();
		this.resultStack = result;
	}

	/**
	 * Creates a new builder for a shapeless recipe.
	 */
	public static SCShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result) {
		return new SCShapelessRecipeBuilder(category, result, 1);
	}

	/**
	 * Creates a new builder for a shapeless recipe.
	 */
	public static SCShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count) {
		return new SCShapelessRecipeBuilder(category, result, count);
	}

	/**
	 * Adds an ingredient that can be any item in the given tag.
	 */
	public SCShapelessRecipeBuilder requires(TagKey<Item> tag) {
		return this.requires(Ingredient.of(tag));
	}

	/**
	 * Adds an ingredient of the given item.
	 */
	public SCShapelessRecipeBuilder requires(ItemLike item) {
		return this.requires(item, 1);
	}

	/**
	 * Adds the given ingredient multiple times.
	 */
	public SCShapelessRecipeBuilder requires(ItemLike item, int quantity) {
		for (int i = 0; i < quantity; i++) {
			this.requires(Ingredient.of(item));
		}

		return this;
	}

	/**
	 * Adds an ingredient.
	 */
	public SCShapelessRecipeBuilder requires(Ingredient ingredient) {
		return this.requires(ingredient, 1);
	}

	/**
	 * Adds an ingredient multiple times.
	 */
	public SCShapelessRecipeBuilder requires(Ingredient ingredient, int quantity) {
		for (int i = 0; i < quantity; i++) {
			this.ingredients.add(ingredient);
		}

		return this;
	}

	public SCShapelessRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
		this.criteria.put(name, criterion);
		return this;
	}

	public SCShapelessRecipeBuilder group(@Nullable String groupName) {
		this.group = groupName;
		return this;
	}

	@Override
	public Item getResult() {
		return this.result;
	}

	@Override
	public void save(RecipeOutput recipeOutput, ResourceLocation id) {
		this.ensureValid(id);
		Advancement.Builder builder = recipeOutput.advancement()
				.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
				.rewards(AdvancementRewards.Builder.recipe(id))
				.requirements(AdvancementRequirements.Strategy.OR);
		this.criteria.forEach(builder::addCriterion);
		ShapelessRecipe shapelessRecipe = new ShapelessRecipe(
				Objects.requireNonNullElse(this.group, ""),
				RecipeBuilder.determineBookCategory(this.category),
				this.resultStack,
				this.ingredients
		);
		recipeOutput.accept(id, shapelessRecipe, builder.build(id.withPrefix("recipes/" + this.category.getFolderName() + "/")));
	}

	/**
	 * Makes sure that this recipe is valid and obtainable.
	 */
	private void ensureValid(ResourceLocation id) {
		if (this.criteria.isEmpty()) {
			throw new IllegalStateException("No way of obtaining recipe " + id);
		}
	}
}
