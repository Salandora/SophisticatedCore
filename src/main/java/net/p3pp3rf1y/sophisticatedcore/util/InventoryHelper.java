package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.fabricators_of_create.porting_lib.transfer.MutableContainerItemContext;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.inventory.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class InventoryHelper {
	private InventoryHelper() {}

	private static final List<Function<Player, SlottedStorage<ItemVariant>>> PLAYER_INVENTORY_PROVIDERS = new ArrayList<>();

	static {
		//registerPlayerInventoryProvider(player -> player.getCapability(Capabilities.ItemHandler.ENTITY));
		registerPlayerInventoryProvider(player -> PlayerInventoryStorage.of(player));
	}

	public static void registerPlayerInventoryProvider(Function<Player, SlottedStorage<ItemVariant>> provider) {
		PLAYER_INVENTORY_PROVIDERS.add(provider);
	}

	public static Optional<ItemStack> getItemFromEitherHand(Player player, Item item) {
		ItemStack mainHandItem = player.getMainHandItem();
		if (mainHandItem.getItem() == item) {
			return Optional.of(mainHandItem);
		}
		ItemStack offhandItem = player.getOffhandItem();
		if (offhandItem.getItem() == item) {
			return Optional.of(offhandItem);
		}
		return Optional.empty();
	}

	public static boolean hasItem(SlottedStorage<ItemVariant> inventory, Predicate<ItemStack> matches) {
		AtomicBoolean result = new AtomicBoolean(false);
		iterate(inventory, (slot, stack) -> {
			if (!stack.isEmpty() && matches.test(stack)) {
				result.set(true);
			}
		}, result::get);
		return result.get();
	}

	public static Set<Integer> getItemSlots(SlottedStackStorage inventory, Predicate<ItemStack> matches) {
		Set<Integer> slots = new HashSet<>();
		iterate(inventory, (slot, stack) -> {
			if (!stack.isEmpty() && matches.test(stack)) {
				slots.add(slot);
			}
		});
		return slots;
	}

	public static void copyTo(SlottedStackStorage handlerA, SlottedStackStorage handlerB) {
		int slotsA = handlerA.getSlotCount();
		int slotsB = handlerB.getSlotCount();
		for (int slot = 0; slot < slotsA && slot < slotsB; slot++) {
			ItemStack slotStack = handlerA.getStackInSlot(slot);
			if (!slotStack.isEmpty()) {
				handlerB.setStackInSlot(slot, slotStack);
			}
		}
	}

	/// Do not call from an open transaction
	public static List<ItemStack> insertIntoInventory(List<ItemStack> stacks, Storage<ItemVariant> inventory, boolean simulate) {
		if (stacks.isEmpty()) {
			return stacks;
		}

		List<ItemStack> remaining = new ArrayList<>();
		try (Transaction ctx = Transaction.openOuter()) {
			for (ItemStack stack : stacks) {
				ItemVariant resource = ItemVariant.of(stack);

				long remainingCount = stack.getCount() - inventory.insert(resource, stack.getCount(), ctx);
				if (remainingCount > 0) {
					remaining.add(resource.toStack((int) remainingCount));
				}
			}

			if (!simulate) {
				ctx.commit();
			}
		}
		return remaining;
	}

	public static ItemStackHandler cloneInventory(SlottedStackStorage inventory) {
		ItemStackHandler cloned = new ItemStackHandler(inventory.getSlotCount());
		for (int slot = 0; slot < inventory.getSlotCount(); slot++) {
			cloned.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
		}
		return cloned;
	}

	/// Do not call from an open transaction
	public static ItemStack insertIntoInventory(ItemStack stack, SlottedStorage<ItemVariant> inventory, boolean simulate) {
		if (inventory instanceof IItemHandlerSimpleInserter itemHandlerSimpleInserter) {
			return itemHandlerSimpleInserter.insertItem(stack, simulate);
		}

		ItemStack remainingStack;
		try (Transaction ctx = Transaction.openOuter()) {
			remainingStack = stack.copyWithCount((int)(stack.getCount() - inventory.insert(ItemVariant.of(stack), stack.getCount(), ctx)));
			if (!simulate) {
				ctx.commit();
			}
		}
		return remainingStack;
	}

	/// Do not call from an open transaction
	public static ItemStack extractFromInventory(Item item, int count, IItemHandlerSimpleInserter inventory, boolean simulate) {
		return extractFromInventory(stack -> stack.getItem() == item, count, inventory, simulate);
	}

	public static ItemStack extractFromInventory(Predicate<ItemStack> stackMatcher, int count, IItemHandlerSimpleInserter inventory, boolean simulate) {
		ItemStack ret = ItemStack.EMPTY;
		int slots = inventory.getSlotCount();
		for (int slot = 0; slot < slots && ret.getCount() < count; slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (stackMatcher.test(slotStack) && (ret.isEmpty() || ItemStack.isSameItemSameComponents(ret, slotStack))) {
				int toExtract = Math.min(slotStack.getCount(), count - ret.getCount());
				ItemStack extractedStack = inventory.extractItem(slot, toExtract, simulate);
				if (ret.isEmpty()) {
					ret = extractedStack;
				} else {
					ret.setCount(ret.getCount() + extractedStack.getCount());
				}
			}
		}
		return ret;
	}

	/// Do not call from an open transaction
	public static ItemStack extractFromInventory(ItemStack stack, IItemHandlerSimpleInserter inventory, boolean simulate) {
		int extractedCount = 0;
		int slots = inventory.getSlotCount();
		for (int slot = 0; slot < slots && extractedCount < stack.getCount(); slot++) {
			ItemStack slotStack = inventory.getStackInSlot(slot);
			if (ItemStack.isSameItemSameComponents(stack, slotStack)) {
				int toExtract = Math.min(slotStack.getCount(), stack.getCount() - extractedCount);
				extractedCount += inventory.extractItem(slot, toExtract, simulate).getCount();
			}
		}

		if (extractedCount == 0) {
			return ItemStack.EMPTY;
		}

		ItemStack result = stack.copy();
		result.setCount(extractedCount);

		return result;
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level level, UpgradeHandler upgradeHandler, ItemStack remainingStack, boolean simulate) {
		return runPickupOnPickupResponseUpgrades(level, null, upgradeHandler, remainingStack, simulate);
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level level,
			@Nullable Player player, UpgradeHandler upgradeHandler, ItemStack remainingStack, boolean simulate) {
		List<IPickupResponseUpgrade> pickupUpgrades = upgradeHandler.getWrappersThatImplement(IPickupResponseUpgrade.class);

		for (IPickupResponseUpgrade pickupUpgrade : pickupUpgrades) {
			int countBeforePickup = remainingStack.getCount();
			Item item = remainingStack.getItem();
			remainingStack = pickupUpgrade.pickup(level, remainingStack, simulate);
			if (!simulate && player != null && remainingStack.getCount() != countBeforePickup) {
				playPickupSound(level, player);
				player.awardStat(Stats.ITEM_PICKED_UP.get(item), countBeforePickup - remainingStack.getCount());
			}

			if (remainingStack.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}

		return remainingStack;
	}

	private static void playPickupSound(Level level, @Nonnull Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, RandHelper.getRandomMinusOneToOne(level.random) * 1.4F + 2.0F);
	}

	public static void iterate(Storage<ItemVariant> handler, Consumer<ItemStack> actOn) {
		iterate(handler, actOn, () -> false);
	}

	public static void iterate(Storage<ItemVariant> handler, Consumer<ItemStack> actOn, BooleanSupplier shouldExit) {
		for (StorageView<ItemVariant> view : handler.nonEmptyViews()) {
			actOn.accept(view.isResourceBlank() ? ItemStack.EMPTY : view.getResource().toStack((int) view.getAmount()));
			if (shouldExit.getAsBoolean()) {
				break;
			}
		}
	}

	public static void iterate(SlottedStorage<ItemVariant> handler, BiConsumer<Integer, ItemStack> actOn) {
		iterate(handler, actOn, () -> false);
	}

	public static void iterate(SlottedStorage<ItemVariant> handler, BiConsumer<Integer, ItemStack> actOn, BooleanSupplier shouldExit) {
		Function<Integer, ItemStack> getStackHandler;
		if (handler instanceof SlottedStackStorage slottedHandler) {
			getStackHandler = slottedHandler::getStackInSlot;
		} else {
			getStackHandler = slot -> {
				var slotStorage = handler.getSlot(slot);
				return slotStorage.isResourceBlank() ? ItemStack.EMPTY : slotStorage.getResource().toStack((int) slotStorage.getAmount());
			};
		}

		int slots = handler.getSlotCount();
		for (int slot = 0; slot < slots; slot++) {
			actOn.accept(slot, getStackHandler.apply(slot));
			if (shouldExit.getAsBoolean()) {
				break;
			}
		}
	}

	public static int getCountMissingInHandler(IInventoryHandlerHelper itemHandler, ItemStack filter, int expectedCount) {
		MutableInt missingCount = new MutableInt(expectedCount);
		iterate(itemHandler, (slot, stack) -> {
			if (ItemStack.isSameItemSameComponents(stack, filter)) {
				missingCount.subtract(Math.min(stack.getCount(), missingCount.getValue()));
			}
		}, () -> missingCount.getValue() == 0);
		return missingCount.getValue();
	}

	public static <T> T iterate(SlottedStackStorage handler, BiFunction<Integer, ItemStack, T> getFromSlotStack, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
		T ret = supplyDefault.get();
		int slots = handler.getSlotCount();
		for (int slot = 0; slot < slots; slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			ret = getFromSlotStack.apply(slot, stack);
			if (shouldExit.test(ret)) {
				break;
			}
		}
		return ret;
	}

	/*public static <T> T iterate(SlottedStorage<ItemVariant> handler, BiFunction<Integer, ItemStack, T> getFromSlotStack, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
		T ret = supplyDefault.get();
		int slots = handler.getSlotCount();
		for (int slot = 0; slot < slots; slot++) {
			SingleSlotStorage<ItemVariant> storage = handler.getSlot(slot);
			ItemStack stack = storage.getResource().toStack((int) storage.getAmount());
			ret = getFromSlotStack.apply(slot, stack);
			if (shouldExit.test(ret)) {
				break;
			}
		}
		return ret;
	}*/

	/// Do not call from an open transaction
	public static void transfer(IItemHandlerSimpleInserter handlerA, IItemHandlerSimpleInserter handlerB, Consumer<Supplier<ItemStack>> onInserted) {
		int slotsA = handlerA.getSlotCount();
		for (int slot = 0; slot < slotsA; slot++) {
			ItemStack slotStack = handlerA.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}

			int countToTransfer = slotStack.getCount();
			while (countToTransfer > 0) {
				ItemStack toInsert = slotStack.copy();
				toInsert.setCount(Math.min(slotStack.getMaxStackSize(), countToTransfer));
				ItemStack remainingAfterInsert = insertIntoInventory(toInsert, handlerB, true);
				if (remainingAfterInsert.getCount() == toInsert.getCount()) {
					break;
				}
				int toExtract = toInsert.getCount() - remainingAfterInsert.getCount();

				ItemStack extractedStack = handlerA.extractItem(slot, toExtract, true);
				if (extractedStack.isEmpty()) {
					break;
				}

				insertIntoInventory(handlerA.extractItem(slot, extractedStack.getCount(), false), handlerB, false);

				onInserted.accept(() -> {
					ItemStack copiedStack = slotStack.copy();
					copiedStack.setCount(extractedStack.getCount());
					return copiedStack;
				});
				countToTransfer -= extractedStack.getCount();
			}
		}
	}

	public static void transfer(Storage<ItemVariant> handlerA, Storage<ItemVariant> handlerB, Consumer<Supplier<ItemStack>> onInserted, @Nullable TransactionContext ctx) {
		for(StorageView<ItemVariant> view : handlerA.nonEmptyViews()) {
			ItemVariant resource = view.getResource();

			long countToTransfer = view.getAmount();
			while (countToTransfer > 0) {
				long inserted = StorageUtil.simulateInsert(handlerB, resource, Math.min(resource.toStack().getMaxStackSize(), countToTransfer), ctx);
				if (inserted == 0) {
					break;
				}

				long extracted = StorageUtil.simulateExtract(handlerA, resource, inserted, ctx);
				if (extracted == 0) {
					break;
				}

				try (Transaction transferTransaction = Transaction.openNested(ctx)) {
					extracted = view.extract(resource, extracted, transferTransaction);
					long accepted = handlerB.insert(resource, inserted, transferTransaction);
					TransactionCallback.onSuccess(transferTransaction, () -> onInserted.accept(() -> resource.toStack((int) accepted).copy()));
					transferTransaction.commit();
				}
				countToTransfer -= extracted;
			}
		}
	}

	public static boolean isEmpty(SlottedStackStorage itemHandler) {
		int slots = itemHandler.getSlotCount();
		for (int slot = 0; slot < slots; slot++) {
			if (!itemHandler.getStackInSlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static ItemStack getAndRemove(SlottedStorage<ItemVariant> itemHandler, int slotIndex) {
		if (slotIndex >= itemHandler.getSlotCount()) {
			return ItemStack.EMPTY;
		}

		SingleSlotStorage<ItemVariant> slot = itemHandler.getSlot(slotIndex);
		ItemVariant resource = slot.getResource();
		return resource.toStack((int) slot.extract(resource, Long.MAX_VALUE, null));
	}

	public static void insertOrDropItem(Player player, ItemStack stack, Storage<ItemVariant>... inventories) {
		ItemVariant resource = ItemVariant.of(stack);
		long toInsert = stack.getCount();
		for (Storage<ItemVariant> inventory : inventories) {
			try (Transaction ctx = Transaction.openOuter()) {
				toInsert -= inventory.insert(resource, toInsert, ctx);
				ctx.commit();
			}
			if (toInsert == 0) {
				return;
			}
		}
		if (toInsert > 0) {
			player.drop(resource.toStack((int) toInsert), true);
		}
	}
	/*public static void insertOrDropItem(Player player, ItemStack stack, IItemHandler... inventories) {
		ItemStack ret = stack;
		for (IItemHandler inventory : inventories) {
			ret = insertIntoInventory(ret, inventory, false);
			if (ret.isEmpty()) {
				return;
			}
		}
		if (!ret.isEmpty()) {
			player.drop(ret, true);
		}
	}*/

	public static ItemStack mergeIntoPlayerInventory(Player player, ItemStack stack, int startSlot) {
		ItemStack result = stack.copy();
		List<Integer> emptySlots = new ArrayList<>();
		for (int slot = startSlot; slot < player.getInventory().items.size(); slot++) {
			ItemStack slotStack = player.getInventory().getItem(slot);
			if (slotStack.isEmpty()) {
				emptySlots.add(slot);
			}
			if (ItemStack.isSameItemSameComponents(slotStack, result)) {
				int count = Math.min(slotStack.getMaxStackSize() - slotStack.getCount(), result.getCount());
				slotStack.grow(count);
				result.shrink(count);
				if (result.isEmpty()) {
					return ItemStack.EMPTY;
				}
			}
		}

		for (int slot : emptySlots) {
			ItemStack slotStack = result.copy();
			slotStack.setCount(Math.min(slotStack.getMaxStackSize(), result.getCount()));
			player.getInventory().setItem(slot, slotStack);
			result.shrink(slotStack.getCount());
			if (result.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}

		return result;
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(SlottedStackStorage handler) {
		return getCompactedStacks(handler, new HashSet<>());
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(SlottedStackStorage handler, Set<Integer> ignoreSlots) {
		Map<ItemStackKey, Integer> ret = new HashMap<>();
		iterate(handler, (slot, stack) -> {
			if (stack.isEmpty() || ignoreSlots.contains(slot)) {
				return;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(stack);
			ret.put(itemStackKey, ret.computeIfAbsent(itemStackKey, fs -> 0) + stack.getCount());
		});
		return ret;
	}

	public static List<ItemStack> getCompactedStacksSortedByCount(SlottedStackStorage handler) {
		Map<ItemStackKey, Integer> compactedStacks = getCompactedStacks(handler);
		List<Map.Entry<ItemStackKey, Integer>> sortedList = new ArrayList<>(compactedStacks.entrySet());
		sortedList.sort(InventorySorter.BY_COUNT);

		List<ItemStack> ret = new ArrayList<>();
		sortedList.forEach(e -> {
			ItemStack stackCopy = e.getKey().getStack().copy();
			stackCopy.setCount(e.getValue());
			ret.add(stackCopy);
		});
		return ret;
	}

	public static Set<ItemStackKey> getUniqueStacks(Storage<ItemVariant> handler) {
		Set<ItemStackKey> uniqueStacks = new HashSet<>();
		iterate(handler, stack -> {
			if (stack.isEmpty()) {
				return;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(stack);
			uniqueStacks.add(itemStackKey);
		});
		return uniqueStacks;
	}

	public static List<Integer> getEmptySlotsRandomized(SlottedStorage<ItemVariant> inventory) {
		List<Integer> list = Lists.newArrayList();

		for (int i = 0; i < inventory.getSlotCount(); ++i) {
			if (inventory.getSlot(i).isResourceBlank()) {
				list.add(i);
			}
		}

		Collections.shuffle(list, new Random());
		return list;
	}

	public static void shuffleItems(List<ItemStack> stacks, int emptySlotsCount, RandomSource rand) {
		List<ItemStack> list = Lists.newArrayList();
		Iterator<ItemStack> iterator = stacks.iterator();

		while (iterator.hasNext()) {
			ItemStack itemstack = iterator.next();
			if (itemstack.isEmpty()) {
				iterator.remove();
			} else if (itemstack.getCount() > 1) {
				list.add(itemstack);
				iterator.remove();
			}
		}

		while (emptySlotsCount - stacks.size() - list.size() > 0 && !list.isEmpty()) {
			ItemStack itemstack2 = list.remove(Mth.nextInt(rand, 0, list.size() - 1));
			int i = Mth.nextInt(rand, 1, itemstack2.getCount() / 2);
			ItemStack itemstack1 = itemstack2.split(i);
			if (itemstack2.getCount() > 1 && rand.nextBoolean()) {
				list.add(itemstack2);
			} else {
				stacks.add(itemstack2);
			}

			if (itemstack1.getCount() > 1 && rand.nextBoolean()) {
				list.add(itemstack1);
			} else {
				stacks.add(itemstack1);
			}
		}

		stacks.addAll(list);
		Collections.shuffle(stacks, new Random());
	}

	public static void dropItems(SlottedStackStorage inventoryHandler, Level level, BlockPos pos) {
		dropItems(inventoryHandler, level, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void dropItems(SlottedStackStorage inventoryHandler, Level level, double x, double y, double z) {
		iterate(inventoryHandler, (slot, stack) -> dropItem(inventoryHandler, level, x, y, z, slot, stack));
	}

	public static void dropItem(SlottedStackStorage inventoryHandler, Level level, double x, double y, double z, Integer slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		ItemVariant resource = ItemVariant.of(stack);
		long extracted;
		try (Transaction ctx = Transaction.openOuter()) {
			extracted = inventoryHandler.extractSlot(slot, resource, stack.getMaxStackSize(), ctx);
			ctx.commit();
		}
		while (extracted > 0) {
			Containers.dropItemStack(level, x, y, z, resource.toStack((int) extracted));
			try (Transaction ctx = Transaction.openOuter()) {
				extracted = inventoryHandler.extractSlot(slot, resource, stack.getMaxStackSize(), ctx);
				ctx.commit();
			}
		}
		inventoryHandler.setStackInSlot(slot, ItemStack.EMPTY);
	}
	/*public static void dropItem(ItemStackHandler inventoryHandler, Level level, double x, double y, double z, Integer slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}
		ItemStack extractedStack = inventoryHandler.extractItem(slot, stack.getMaxStackSize(), false);
		while (!extractedStack.isEmpty()) {
			Containers.dropItemStack(level, x, y, z, extractedStack);
			extractedStack = inventoryHandler.extractItem(slot, stack.getMaxStackSize(), false);
		}
		inventoryHandler.setStackInSlot(slot, ItemStack.EMPTY);
	}*/

	public static int getAnalogOutputSignal(ITrackedContentsItemHandler handler) {
		AtomicDouble totalFilled = new AtomicDouble(0);
		AtomicBoolean isEmpty = new AtomicBoolean(true);
		iterate(handler, (slot, stack) -> {
			if (!stack.isEmpty()) {
				int slotLimit = handler.getInternalSlotLimit(slot);
				totalFilled.addAndGet(stack.getCount() / (slotLimit / ((float) 64 / stack.getMaxStackSize())));
				isEmpty.set(false);
			}
		});
		double percentFilled = totalFilled.get() / handler.getSlotCount();
		return Mth.floor(percentFilled * 14.0F) + (isEmpty.get() ? 0 : 1);
	}

	public static List<Storage<ItemVariant>> getItemHandlersFromPlayerIncludingContainers(Player player) {
		List<Storage<ItemVariant>> itemHandlers = new ArrayList<>();
		PLAYER_INVENTORY_PROVIDERS.forEach(provider -> {
			SlottedStorage<ItemVariant> itemHandler = provider.apply(player);
			itemHandlers.add(itemHandler);
			for (StorageView<ItemVariant> view : itemHandler.nonEmptyViews()) {
				if (view.isResourceBlank()) {
					continue;
				}

				Storage<ItemVariant> containerHandler = new MutableContainerItemContext(view.getResource().toStack((int) view.getAmount())).find(ItemStorage.ITEM);
				if (containerHandler != null) {
					itemHandlers.add(containerHandler);
				}
			}
		});
		return itemHandlers;
	}
}
