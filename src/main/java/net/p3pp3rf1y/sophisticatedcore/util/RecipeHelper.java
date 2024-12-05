package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape.*;

public class RecipeHelper {
	private static final int MAX_FOLLOW_UP_COMPACTING_RECIPES = 30;
	private static RecipeCache clientCache;
	private static RecipeCache serverCache;

	private RecipeHelper() {
	}

	public static void setLevel(Level l) {
		if (l instanceof ServerLevel) {
			serverCache = new RecipeCache(l);
		} else {
			clientCache = new RecipeCache(l);
		}
	}

	private static void runOnCache(Consumer<RecipeCache> consumer) {
		if (SophisticatedCore.isLogicalServerThread()) {
			if (serverCache != null) {
				consumer.accept(serverCache);
			}
		} else {
			if (clientCache != null) {
				consumer.accept(clientCache);
			}
		}
	}

	private static <T> T getFromCache(Function<RecipeCache, T> getter, T defaultValue) {
		if (SophisticatedCore.isLogicalServerThread()) {
			return serverCache == null ? defaultValue : getter.apply(serverCache);
		} else {
			return clientCache == null ? defaultValue : getter.apply(clientCache);
		}
	}

	public static void addRecipeChangeListener(Runnable runnable) {
		runOnCache(cache -> cache.addRecipeChangeListener(runnable));
	}

	@SuppressWarnings("unused") //event parameter used to identify which event this listener is for
	public static void onRecipesUpdated(RecipeManager manager) {
		runOnCache(cache -> {
			cache.clearCache();
			cache.recipeChangeListeners.notifyAllListeners();
		});
	}

	@SuppressWarnings("unused") //event parameter used to identify which event this listener is for
	public static void onDataPackSync(ServerPlayer serverPlayer, boolean b) {
		runOnCache(cache -> {
			cache.clearCache();
			cache.recipeChangeListeners.notifyAllListeners();
		});
	}

	private static Optional<Level> getLevel() {
		return getFromCache(cache -> Optional.ofNullable(cache.level.get()), Optional.empty());
	}

	private static Set<CompactingShape> getCompactingShapes(Item item) {
		return getLevel().map(w -> {
			Set<CompactingShape> compactingShapes = new HashSet<>();
			getCompactingShape(item, w, 2, 2, TWO_BY_TWO_UNCRAFTABLE, TWO_BY_TWO).ifPresent(compactingShapes::add);
			getCompactingShape(item, w, 3, 3, THREE_BY_THREE_UNCRAFTABLE, THREE_BY_THREE).ifPresent(compactingShapes::add);
			if (compactingShapes.isEmpty()) {
				compactingShapes.add(NONE);
			}
			return compactingShapes;
		}).orElse(Collections.emptySet());
	}

	private static Optional<CompactingShape> getCompactingShape(Item item, Level w, int width, int height, CompactingShape uncraftableShape, CompactingShape shape) {
		CompactingResult compactingResult = getCompactingResult(item, w, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			if (item == compactingResult.getResult().getItem()) {
				return Optional.empty();
			}

			if (isPartOfCompactingLoop(item, compactingResult.getResult().getItem(), w)) {
				return Optional.empty();
			}

			if (uncompactMatchesItem(compactingResult.getResult(), w, item, width * height)) {
				return Optional.of(uncraftableShape);
			} else {
				return Optional.of(shape);
			}
		}
		return Optional.empty();
	}

	private static boolean isPartOfCompactingLoop(Item firstCompacted, Item firstCompactResult, Level w) {
		ItemStack compactingResultStack;
		int iterations = 0;
		Set<Item> compactedItems = new HashSet<>();
		Queue<Item> itemsToCompact = new LinkedList<>();
		itemsToCompact.add(firstCompactResult);
		while (!itemsToCompact.isEmpty()) {
			Item itemToCompact = itemsToCompact.poll();
			compactingResultStack = getCompactingResult(itemToCompact, w, 2, 2).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (compactingResultStack.getItem() == firstCompacted) {
					return true;
				} else if (compactedItems.contains(compactingResultStack.getItem())) {
					return false; //loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will happen
				}
				itemsToCompact.add(compactingResultStack.getItem());
			}

			compactingResultStack = getCompactingResult(itemToCompact, w, 3, 3).getResult();
			if (!compactingResultStack.isEmpty()) {
				if (compactingResultStack.getItem() == firstCompacted) {
					return true;
				} else if (compactedItems.contains(compactingResultStack.getItem())) {
					return false; //loop exists but the first compacted item isn't part of it so we will let it be compacted, but no follow up compacting will happen
				}
				itemsToCompact.add(compactingResultStack.getItem());
			}
			compactedItems.add(itemToCompact);
			iterations++;
			if (iterations > MAX_FOLLOW_UP_COMPACTING_RECIPES) {
				return true; //we were unable to figure out if the loop exists because of way too many follow up compacting recipe thus not allowing to compact anyway
			}
		}
		return false;
	}

	private static boolean uncompactMatchesItem(ItemStack result, Level w, Item item, int count) {
		Item itemToUncompact = result.getItem();
		for (ItemStack uncompactResult : getUncompactResultItems(w, itemToUncompact)) {
			if (uncompactResult.getItem() == item && uncompactResult.getCount() == count) {
				return true;
			}
		}
		return false;
	}

	public static UncompactingResult getUncompactingResult(Item resultItem) {
		return getFromCache(cache -> cache.getUncompactingResults().computeIfAbsent(resultItem, k -> getLevel().map(w -> {
			for (ItemStack uncompactResultItem : getUncompactResultItems(w, resultItem)) {
				if (uncompactResultItem.getCount() == 9) {
					if (getCompactingResult(uncompactResultItem.getItem(), 3, 3).getResult().getItem() == resultItem) {
						return new UncompactingResult(uncompactResultItem.getItem(), THREE_BY_THREE_UNCRAFTABLE);
					}
				} else if (uncompactResultItem.getCount() == 4 && getCompactingResult(uncompactResultItem.getItem(), 2, 2).getResult().getItem() == resultItem) {
					return new UncompactingResult(uncompactResultItem.getItem(), TWO_BY_TWO_UNCRAFTABLE);
				}
			}
			return UncompactingResult.EMPTY;
		}).orElse(UncompactingResult.EMPTY)), UncompactingResult.EMPTY);
	}

	private static List<ItemStack> getUncompactResultItems(Level w, Item itemToUncompact) {
		CraftingContainer craftingInventory = getFilledCraftingInventory(itemToUncompact, 1, 1);
		return safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory.asCraftInput(), w).stream().map(r -> r.value().assemble(craftingInventory.asCraftInput(), w.registryAccess())).toList();
	}

	public static CompactingResult getCompactingResult(Item item, CompactingShape shape) {
		if (shape == TWO_BY_TWO_UNCRAFTABLE || shape == TWO_BY_TWO) {
			return RecipeHelper.getCompactingResult(item, 2, 2);
		} else if (shape == THREE_BY_THREE_UNCRAFTABLE || shape == THREE_BY_THREE) {
			return RecipeHelper.getCompactingResult(item, 3, 3);
		}
		return CompactingResult.EMPTY;
	}

	public static CompactingResult getCompactingResult(Item item, int width, int height) {
		return getLevel().map(w -> getCompactingResult(item, w, width, height)).orElse(CompactingResult.EMPTY);
	}

	private static CompactingResult getCompactingResult(Item item, Level level, int width, int height) {
		return getFromCache(cache -> getCompactingResult(item, level, width, height, cache.getCompactingResults()), CompactingResult.EMPTY);
	}

	private static CompactingResult getCompactingResult(Item item, Level level, int width, int height, Map<CompactedItem, CompactingResult> cachedCompactingResults) {
		CompactedItem compactedItem = new CompactedItem(item, width, height);
		if (cachedCompactingResults.containsKey(compactedItem)) {
			return cachedCompactingResults.get(compactedItem);
		}

		CraftingContainer craftingInventory = getFilledCraftingInventory(item, width, height);
		List<RecipeHolder<CraftingRecipe>> compactingRecipes = safeGetRecipesFor(RecipeType.CRAFTING, craftingInventory.asCraftInput(), level);

		if (compactingRecipes.isEmpty()) {
			cachedCompactingResults.put(compactedItem, CompactingResult.EMPTY);
			return CompactingResult.EMPTY;
		}

		if (compactingRecipes.size() == 1) {
			return cacheAndGetCompactingResult(compactedItem, compactingRecipes.getFirst().value(), craftingInventory);
		}

		for (RecipeHolder<CraftingRecipe> recipeHolder : compactingRecipes) {
			CraftingRecipe recipe = recipeHolder.value();
			ItemStack result = recipe.assemble(craftingInventory.asCraftInput(), level.registryAccess());
			if (uncompactMatchesItem(result, level, item, width * height)) {
				return cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, result);
			}
		}

		return cacheAndGetCompactingResult(compactedItem, compactingRecipes.getFirst().value(), craftingInventory);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory) {
		return getLevel().map(level ->
				cacheAndGetCompactingResult(compactedItem, recipe, craftingInventory, recipe.assemble(craftingInventory.asCraftInput(), level.registryAccess()))
		).orElse(CompactingResult.EMPTY);
	}

	private static CompactingResult cacheAndGetCompactingResult(CompactedItem compactedItem, CraftingRecipe recipe, CraftingContainer craftingInventory, ItemStack result) {
		List<ItemStack> remainingItems = new ArrayList<>();
		recipe.getRemainingItems(craftingInventory.asCraftInput()).forEach(stack -> {
			if (!stack.isEmpty()) {
				remainingItems.add(stack);
			}
		});

		CompactingResult compactingResult = new CompactingResult(result, remainingItems);
		return getFromCache(cache -> {
			if (!result.isEmpty()) {
				cache.getCompactingResults().put(compactedItem, compactingResult);
			}
			return compactingResult;
		}, compactingResult);
	}

	private static CraftingContainer getFilledCraftingInventory(Item item, int width, int height) {
		CraftingContainer craftinginventory = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, width, height);

		for (int i = 0; i < craftinginventory.getContainerSize(); i++) {
			craftinginventory.setItem(i, new ItemStack(item));
		}
		return craftinginventory;
	}

	public static <T extends AbstractCookingRecipe> Optional<RecipeHolder<T>> getCookingRecipe(ItemStack stack, RecipeType<T> recipeType) {
		return getLevel().flatMap(w -> safeGetRecipeFor(recipeType, new SingleRecipeInput(stack), w));
	}

	public static Set<CompactingShape> getItemCompactingShapes(Item item) {
		return getFromCache(cache -> cache.getItemCompactingShapes().getUnchecked(item), Collections.emptySet());
	}

	public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipesOfType(RecipeType<T> recipeType, I inventory) {
		return getLevel().map(w -> w.getRecipeManager().getRecipesFor(recipeType, inventory, w)).orElse(Collections.emptyList());
	}

	public static <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> safeGetRecipeFor(RecipeType<T> recipeType, I inventory, Level level) {
		try {
			return level.getRecipeManager().getRecipeFor(recipeType, inventory, level);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Optional.empty();
		}
	}

	public static <I extends CraftingInput, T extends Recipe<I>> List<RecipeHolder<T>> safeGetRecipesFor(RecipeType<T> recipeType, I inventory, Level level) {
		try {
			return level.getRecipeManager().getRecipesFor(recipeType, inventory, level);
		} catch (Exception e) {
			SophisticatedCore.LOGGER.error("Error while getting recipe ", e);
			return Collections.emptyList();
		}
	}

	public enum CompactingShape {
		NONE(false, 0),
		THREE_BY_THREE(false, 9),
		TWO_BY_TWO(false, 4),
		THREE_BY_THREE_UNCRAFTABLE(true, 9),
		TWO_BY_TWO_UNCRAFTABLE(true, 4);

		private final int numberOfIngredients;

		private final boolean uncraftable;

		CompactingShape(boolean uncraftable, int numberOfIngredients) {
			this.uncraftable = uncraftable;
			this.numberOfIngredients = numberOfIngredients;
		}

		public boolean isUncraftable() {
			return uncraftable;
		}

		public int getNumberOfIngredients() {
			return numberOfIngredients;
		}
	}

	public static class CompactingResult {
		public static final CompactingResult EMPTY = new CompactingResult(ItemStack.EMPTY, Collections.emptyList());

		private final ItemStack result;
		private final List<ItemStack> remainingItems;

		public CompactingResult(ItemStack result, List<ItemStack> remainingItems) {
			this.result = result;
			this.remainingItems = remainingItems;
		}

		public ItemStack getResult() {
			return result;
		}

		public List<ItemStack> getRemainingItems() {
			return remainingItems;
		}
	}

	public static class UncompactingResult {
		public static final UncompactingResult EMPTY = new UncompactingResult(Items.AIR, NONE);

		private final Item result;

		private final CompactingShape compactUsingShape;

		public UncompactingResult(Item result, CompactingShape compactUsingShape) {
			this.result = result;
			this.compactUsingShape = compactUsingShape;
		}

		public Item getResult() {
			return result;
		}

		public CompactingShape getCompactUsingShape() {
			return compactUsingShape;
		}
	}

	private static class CompactedItem {
		private final Item item;
		private final int width;
		private final int height;

		private CompactedItem(Item item, int width, int height) {
			this.item = item;
			this.width = width;
			this.height = height;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CompactedItem that = (CompactedItem) o;
			return width == that.width &&
					height == that.height &&
					item.equals(that.item);
		}

		@Override
		public int hashCode() {
			return Objects.hash(item, width, height);
		}
	}

	private static class RecipeChangeListenerList {
		private final List<WeakReference<Runnable>> list = Collections.synchronizedList(new ArrayList<>());

		public void add(Runnable runnable) {
			list.add(new WeakReference<>(runnable));
		}

		public void notifyAllListeners() {
			list.removeIf(ref -> {
				Runnable runnable = ref.get();
				if (runnable != null) {
					runnable.run();
					return false;
				}
				return true;
			});
		}
	}

	private static class RecipeCache {
		private final LoadingCache<Item, Set<CompactingShape>> itemCompactingShapes = CacheBuilder.newBuilder().expireAfterAccess(10L, TimeUnit.MINUTES).build(new CacheLoader<>() {
			@Override
			public Set<CompactingShape> load(Item item) {
				SophisticatedCore.LOGGER.debug("Compacting shapes not found in cache for \"{}\" - querying recipes to get these", BuiltInRegistries.ITEM.getKey(item));
				return getCompactingShapes(item);
			}
		});
		private final Map<CompactedItem, CompactingResult> compactingResults = new HashMap<>();
		private final Map<Item, UncompactingResult> uncompactingResults = new HashMap<>();
		private final RecipeChangeListenerList recipeChangeListeners = new RecipeChangeListenerList();
		private final WeakReference<Level> level;

		public RecipeCache(Level level) {
			this.level = new WeakReference<>(level);
		}

		public void addRecipeChangeListener(Runnable runnable) {
			recipeChangeListeners.add(runnable);
		}

		public Map<Item, UncompactingResult> getUncompactingResults() {
			return uncompactingResults;
		}

		public Map<CompactedItem, CompactingResult> getCompactingResults() {
			return compactingResults;
		}

		public LoadingCache<Item, Set<CompactingShape>> getItemCompactingShapes() {
			return itemCompactingShapes;
		}

		private void clearCache() {
			compactingResults.clear();
			uncompactingResults.clear();
			itemCompactingShapes.invalidateAll();
		}
	}
}
