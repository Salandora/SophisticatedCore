package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class InventoryHelper {
	private InventoryHelper() {}

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

	public static <T> Iterator<StorageView<T>> filterViews(Iterator<StorageView<T>> iterator, Predicate<ResourceAmount<T>> filter) {
		return new Iterator<>() {
			StorageView<T> next;

			{
				findNext();
			}

			private void findNext() {
				while (iterator.hasNext()) {
					next = iterator.next();

					if (filter.test(new ResourceAmount<>(next.getResource(), next.getAmount()))) {
						return;
					}
				}

				next = null;
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public StorageView<T> next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}

				StorageView<T> ret = next;
				findNext();
				return ret;
			}
		};
	}
	public static boolean hasItem(SlottedStorage<ItemVariant> inventory, Predicate<ItemStack> matches) {
		return filterViews(inventory.nonEmptyIterator(), resource -> matches.test(resource.resource().toStack((int) resource.amount()))).hasNext();
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

	public static List<ItemStack> insertIntoInventory(List<ItemStack> stacks, Storage<ItemVariant> inventory, TransactionContext ctx) {
		if (stacks.isEmpty()) {
			return stacks;
		}

		List<ItemStack> remainingStacks = new ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemVariant resource = ItemVariant.of(stack);

			long remaining = stack.getCount() - inventory.insert(resource, stack.getCount(), ctx);
			if (remaining > 0) {
				remainingStacks.add(resource.toStack((int) remaining));
			}
		}
		return remainingStacks;
	}

	public static ItemStack simulateInsertIntoInventory(SlottedStackStorage inventory, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
		try (Transaction simulate = Transaction.openNested(ctx)) {
			return insertIntoInventory(inventory, resource, maxAmount, simulate);
		}
	}

	public static ItemStack insertIntoInventory(SlottedStackStorage inventory, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
		try (Transaction inner = Transaction.openNested(ctx)) {
			long inserted = inventory.insert(resource, maxAmount, inner);
			inner.commit();
			return resource.toStack((int)(maxAmount - inserted));
		}
	}

	public static ItemStack extractFromInventory(ItemVariant resource, long maxAmount, SlottedStackStorage inventory, @Nullable TransactionContext ctx) {
		long extractedCount;
		try (Transaction inner = Transaction.openNested(ctx)) {
			extractedCount = inventory.extract(resource, maxAmount, inner);
			inner.commit();
		}

		if (extractedCount == 0) {
			return ItemStack.EMPTY;
		}

		return resource.toStack((int) extractedCount);
	}

	public static ItemStack extractFromInventory(ItemStack stack, SlottedStackStorage inventory, @Nullable TransactionContext ctx) {
		return extractFromInventory(ItemVariant.of(stack), stack.getCount(), inventory, ctx);
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level world, UpgradeHandler upgradeHandler, ItemStack remainingStack, TransactionContext ctx) {
		return runPickupOnPickupResponseUpgrades(world, null, upgradeHandler, remainingStack, ctx);
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level world, @Nullable Player player, UpgradeHandler upgradeHandler, ItemStack remainingStack, TransactionContext ctx) {
		List<IPickupResponseUpgrade> pickupUpgrades = upgradeHandler.getWrappersThatImplement(IPickupResponseUpgrade.class);

		for (IPickupResponseUpgrade pickupUpgrade : pickupUpgrades) {
			int countBeforePickup = remainingStack.getCount();
			try (Transaction inner = Transaction.openNested(ctx)) {
				remainingStack = pickupUpgrade.pickup(world, remainingStack, inner);

				ItemStack finalRemainingStack = remainingStack;
				TransactionCallback.onSuccess(inner, () -> {
					if (player != null && finalRemainingStack.getCount() != countBeforePickup) {
						playPickupSound(world, player);
					}
				});

				inner.commit();
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

	public static int getCountMissingInHandler(Storage<ItemVariant> itemHandler, ItemStack filter, int expectedCount) {
		int missingCount = expectedCount;
		for (var view : itemHandler.nonEmptyViews()) {
			if (view.getResource().matches(filter)) {
				missingCount -= Math.min((int) view.getAmount(), missingCount);
				if (missingCount == 0) {
					break;
				}
			}
		}
		return missingCount;
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

	public static <T> T iterate(SlottedStorage<ItemVariant> handler, BiFunction<Integer, ItemStack, T> getFromSlotStack, Supplier<T> supplyDefault, Predicate<T> shouldExit) {
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
	}

	public static void transfer(Storage<ItemVariant> handlerA, Storage<ItemVariant> handlerB, Consumer<Supplier<ItemStack>> onInserted, @Nullable TransactionContext ctx) {
		if (handlerA == null || handlerB == null) {
			return;
		}

		for (StorageView<ItemVariant> view : handlerA.nonEmptyViews()) {
			ItemVariant resource = view.getResource();
			long maxExtracted;

			// check how much can be extracted
			try (Transaction extractionTestTransaction = Transaction.openNested(ctx)) {
				maxExtracted = view.extract(resource, view.getAmount(), extractionTestTransaction);
			}

			try (Transaction transferTransaction = Transaction.openNested(ctx)) {
				// check how much can be inserted
				long accepted = handlerB.insert(resource, maxExtracted, transferTransaction);

				// extract it, or rollback if the amounts don't match
				if (accepted > 0 && view.extract(resource, accepted, transferTransaction) == accepted) {
					TransactionCallback.onSuccess(transferTransaction, () -> onInserted.accept(() -> resource.toStack((int) accepted)));
					transferTransaction.commit();
				}
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
}
