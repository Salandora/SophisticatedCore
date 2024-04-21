package net.salandora.sophisticatedcore.unittests;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecipeHelperTest {
	private static Level regularOrderRecipesLevel;
	private static Level reverseOrderRecipesLevel;

	private static List<CraftingRecipe> getCraftingRecipes() {
		List<CraftingRecipe> craftingRecipes = new ArrayList<>();
		//stones
		craftingRecipes.add(new ShapedRecipe(new ResourceLocation("granite_to_diorite"), "", CraftingBookCategory.MISC, 3, 3, ingredients(Items.GRANITE), new ItemStack(Items.DIORITE)));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("granite_from_diorite"), "", CraftingBookCategory.MISC, new ItemStack(Items.GRANITE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DIORITE))));
		craftingRecipes.add(new ShapedRecipe(new ResourceLocation("stone_to_granite"), "", CraftingBookCategory.MISC, 3, 3, ingredients(Items.STONE), new ItemStack(Items.GRANITE)));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("stone_from_granite"), "", CraftingBookCategory.MISC, new ItemStack(Items.STONE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GRANITE))));

		//gold
		craftingRecipes.add(new ShapedRecipe(new ResourceLocation("gold_ingot_to_gold_block"), "", CraftingBookCategory.MISC, 3, 3, ingredients(Items.GOLD_INGOT), new ItemStack(Items.GOLD_BLOCK)));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("gold_ingot_from_gold_block"), "", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_INGOT, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_BLOCK))));
		craftingRecipes.add(new ShapedRecipe(new ResourceLocation("gold_nugget_to_gold_ingot"), "", CraftingBookCategory.MISC, 3, 3, ingredients(Items.GOLD_NUGGET), new ItemStack(Items.GOLD_INGOT)));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("gold_nugget_from_gold_ingot"), "", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_NUGGET, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_INGOT))));

		//confusion recipes
		craftingRecipes.add(new ShapedRecipe(new ResourceLocation("gold_nugget_to_diorite"), "", CraftingBookCategory.MISC, 3, 3, ingredients(Items.GOLD_NUGGET), new ItemStack(Items.DIORITE)));
		craftingRecipes.add(new ShapedRecipe(new ResourceLocation("granite_to_gold_block"), "", CraftingBookCategory.MISC, 3, 3, ingredients(Items.GRANITE), new ItemStack(Items.GOLD_BLOCK)));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("gold_nugget_from_granite"), "", CraftingBookCategory.MISC, new ItemStack(Items.GOLD_NUGGET, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GRANITE))));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("granite_from_diamond"), "", CraftingBookCategory.MISC, new ItemStack(Items.GRANITE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.DIAMOND))));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("iron_nugget_from_granite"), "", CraftingBookCategory.MISC, new ItemStack(Items.IRON_NUGGET, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GRANITE))));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("stone_from_gold_ingot"), "", CraftingBookCategory.MISC, new ItemStack(Items.STONE, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_INGOT))));
		craftingRecipes.add(new ShapelessRecipe(new ResourceLocation("torches_from_gold_block"), "", CraftingBookCategory.MISC, new ItemStack(Items.TORCH, 9), NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.GOLD_BLOCK))));

		return craftingRecipes;
	}

	private static Stream<Level> classParams() {
		return java.util.stream.Stream.of(regularOrderRecipesLevel, reverseOrderRecipesLevel);
	}

	public static void runTests() {
		regularOrderRecipesLevel = getLevelWithRecipeManagerFor(getCraftingRecipes());

		List<CraftingRecipe> reverseOrderRecipes = getCraftingRecipes();
		Collections.reverse(reverseOrderRecipes);
		reverseOrderRecipesLevel = getLevelWithRecipeManagerFor(reverseOrderRecipes);

		classParams().flatMap(RecipeHelperTest::testGetCompactingResult).forEach(RecipeHelperTest::testGetCompactingResult);
		classParams().flatMap(RecipeHelperTest::testGetUncompactingResult).forEach(RecipeHelperTest::testGetUncompactingResult);
		classParams().flatMap(RecipeHelperTest::testGetItemCompactingShapes).forEach(RecipeHelperTest::testGetItemCompactingShapes);

		LoggerFactory.getLogger("sophisticatedcore testmod").info("RecipeHelperTests successful.");
	}

	private static Level getLevelWithRecipeManagerFor(List<CraftingRecipe> craftingRecipes) {
		RecipeManager mockRecipeManager = mock(RecipeManager.class);
		when(mockRecipeManager.getRecipesFor(eq(RecipeType.CRAFTING), any(CraftingContainer.class), any())).thenAnswer(i -> {
			List<CraftingRecipe> matchingRecipes = new ArrayList<>();
			CraftingContainer craftingContainer = i.getArgument(1);
			Level level = i.getArgument(2);
			for (CraftingRecipe craftingRecipe : craftingRecipes) {
				if (craftingRecipe.matches(craftingContainer, level)) {
					matchingRecipes.add(craftingRecipe);
				}
			}
			return matchingRecipes;
		});

		Level level = mock(Level.class);
		when(level.getRecipeManager()).thenReturn(mockRecipeManager);
		return level;
	}

	private static NonNullList<Ingredient> ingredients(Item item) {
		return NonNullList.of(Ingredient.EMPTY,
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item),
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item),
				Ingredient.of(item), Ingredient.of(item), Ingredient.of(item)
		);
	}


	private record TestGetCompactingResult(Level level, Item item, RecipeHelper.CompactingResult expectedResult) {}
	private static void testGetCompactingResult(TestGetCompactingResult params) {
		RecipeHelper.setWorld(params.level);

		RecipeHelper.CompactingResult actualResult = RecipeHelper.getCompactingResult(params.item, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE);

		assertCompactingResultEquals(params.expectedResult, actualResult, "getCompactingResult returned wrong result");
		RecipeHelper.clearCache();
	}

	private static Stream<TestGetCompactingResult> testGetCompactingResult(Level level) {
		return java.util.stream.Stream.of(
				new TestGetCompactingResult(level, Items.GOLD_INGOT, new RecipeHelper.CompactingResult(new ItemStack(Items.GOLD_BLOCK), Collections.emptyList())),
				new TestGetCompactingResult(level, Items.GOLD_NUGGET, new RecipeHelper.CompactingResult(new ItemStack(Items.GOLD_INGOT), Collections.emptyList())),
				new TestGetCompactingResult(level, Items.GRANITE, new RecipeHelper.CompactingResult(new ItemStack(Items.DIORITE), Collections.emptyList())),
				new TestGetCompactingResult(level, Items.STONE, new RecipeHelper.CompactingResult(new ItemStack(Items.GRANITE), Collections.emptyList()))
		);
	}


	private record TestGetUncompactingResult(Level level, Item item, RecipeHelper.UncompactingResult expectedResult) {}
	private static void testGetUncompactingResult(TestGetUncompactingResult params) {
		RecipeHelper.setWorld(params.level);

		RecipeHelper.UncompactingResult actualResult = RecipeHelper.getUncompactingResult(params.item);

		assertUncompactingResultEquals(params.expectedResult, actualResult, "getUncompactingResult returned wrong result");
		RecipeHelper.clearCache();
	}

	private static Stream<TestGetUncompactingResult> testGetUncompactingResult(Level level) {
		return java.util.stream.Stream.of(
				new TestGetUncompactingResult(level, Items.GOLD_BLOCK, new RecipeHelper.UncompactingResult(Items.GOLD_INGOT, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
				new TestGetUncompactingResult(level, Items.GOLD_INGOT, new RecipeHelper.UncompactingResult(Items.GOLD_NUGGET, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
				new TestGetUncompactingResult(level, Items.DIORITE, new RecipeHelper.UncompactingResult(Items.GRANITE, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
				new TestGetUncompactingResult(level, Items.GRANITE, new RecipeHelper.UncompactingResult(Items.STONE, RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))
		);
	}


	private record TestGetItemCompactingShapes(Level level, Item item, Set<RecipeHelper.CompactingShape> shapes) {}
	private static void testGetItemCompactingShapes(TestGetItemCompactingShapes params) {
		RecipeHelper.setWorld(params.level);

		Set<RecipeHelper.CompactingShape> actualShapes = RecipeHelper.getItemCompactingShapes(params.item);

		if (!Objects.equals(params.shapes, actualShapes)) {
			throw new AssertionError(String.format("getItemCompactingShapes returned wrong result%n expected: %s%n but was: %s", params.shapes, actualShapes));
		}
		RecipeHelper.clearCache();
	}

	private static Stream<TestGetItemCompactingShapes> testGetItemCompactingShapes(Level level) {
		return java.util.stream.Stream.of(
				new TestGetItemCompactingShapes(level, Items.GOLD_INGOT, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
				new TestGetItemCompactingShapes(level, Items.GOLD_NUGGET, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
				new TestGetItemCompactingShapes(level, Items.GRANITE, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)),
				new TestGetItemCompactingShapes(level, Items.STONE, Set.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE))
		);
	}


	private static void assertCompactingResultEquals(RecipeHelper.CompactingResult expected, RecipeHelper.CompactingResult actual, Object message) {
		if (ItemStack.matches(expected.getResult(), actual.getResult())
				&& areItemListsEqual(expected.getRemainingItems(), actual.getRemainingItems())) {
			return;
		}

		throw new AssertionError(String.format("%s%n expected: %s%n but was: %s", message, expected.getResult() + ":" + expected.getRemainingItems(), actual.getResult() + ":" + actual.getRemainingItems()));
	}
	private static boolean areItemListsEqual(List<ItemStack> expected, List<ItemStack> actual) {
		if (expected.size() != actual.size()) {
			return false;
		}
		for (int i = 0; i < expected.size(); i++) {
			if (!ItemStack.matches(expected.get(i), actual.get(i))) {
				return false;
			}
		}
		return true;
	}
	private static void assertUncompactingResultEquals(RecipeHelper.UncompactingResult expected, RecipeHelper.UncompactingResult actual, Object message) {
		if (expected.getResult() == actual.getResult() && expected.getCompactUsingShape() == actual.getCompactUsingShape()) {
			return;
		}

		throw new AssertionError(String.format("%s%n expected: %s%n but was: %s", message, expected.getResult() + ":" + expected.getCompactUsingShape(), actual.getResult() + ":" + actual.getCompactUsingShape()));
	}
}
