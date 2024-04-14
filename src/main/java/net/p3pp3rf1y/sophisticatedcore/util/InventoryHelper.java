package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.collect.Lists;

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
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

	public static Set<Integer> getItemSlots(SlottedStorage<ItemVariant> inventory, Predicate<ItemStack> matches) {
		Set<Integer> slots = new HashSet<>();
		for (int slotIndex = 0; slotIndex < inventory.getSlotCount(); slotIndex++) {
			var slot = inventory.getSlot(slotIndex);
			if (!slot.isResourceBlank() && matches.test(slot.getResource().toStack((int) slot.getAmount()))) {
				slots.add(slotIndex);
			}
		}
		return slots;
	}

	public static void copyTo(SlottedStorage<ItemVariant> handlerA, SlottedStorage<ItemVariant> handlerB) {
		int slotsA = handlerA.getSlotCount();
		int slotsB = handlerB.getSlotCount();
		try (Transaction ctx = Transaction.openOuter()) {
			for (int slot = 0; slot < slotsA && slot < slotsB; slot++) {
				SingleSlotStorage<ItemVariant> slotStorage = handlerA.getSlot(slot);
				if (!slotStorage.isResourceBlank()) {
					handlerB.getSlots().get(slot).insert(slotStorage.getResource(), slotStorage.getAmount(), ctx);
				}
			}
			ctx.commit();
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
			return insertIntoInventory(inventory, resource, maxAmount,  simulate);
		}
	}

	public static ItemStack insertIntoInventory(SlottedStackStorage inventory, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		long remaining = maxAmount;
		int slots = inventory.getSlotCount();
		for (int slot = 0; slot < slots && remaining > 0; slot++) {
			remaining -= inventory.insertSlot(slot, resource, remaining, ctx);
		}
		return resource.toStack((int) remaining);
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level world, UpgradeHandler upgradeHandler, ItemStack remainingStack, TransactionContext ctx) {
		return runPickupOnPickupResponseUpgrades(world, null, upgradeHandler, remainingStack, ctx);
	}

	public static ItemStack runPickupOnPickupResponseUpgrades(Level world, @Nullable Player player, UpgradeHandler upgradeHandler, ItemStack remainingStack, TransactionContext ctx) {
		List<IPickupResponseUpgrade> pickupUpgrades = upgradeHandler.getWrappersThatImplement(IPickupResponseUpgrade.class);

		for (IPickupResponseUpgrade pickupUpgrade : pickupUpgrades) {
			int countBeforePickup = remainingStack.getCount();
			try (Transaction pickupTransaction = Transaction.openNested(ctx)) {
				remainingStack = pickupUpgrade.pickup(world, remainingStack, pickupTransaction);

				ItemStack finalRemainingStack = remainingStack;
				TransactionCallback.onSuccess(pickupTransaction, () -> {
					if (player != null && finalRemainingStack.getCount() != countBeforePickup) {
						playPickupSound(world, player);
					}
				});

				pickupTransaction.commit();
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

	public static int getCountMissingInHandler(Storage<ItemVariant> itemHandler, ItemStack filter, int expectedCount) {
		int missingCount = expectedCount;
		for (var view : itemHandler.nonEmptyViews()) {
			ItemStack stack = view.getResource().toStack((int) view.getAmount());
			if (ItemStack.isSameItemSameTags(stack, filter)) {
				missingCount -= Math.min(stack.getCount(), missingCount);
				if (missingCount == 0) {
					break;
				}
			}
		}
		return missingCount;
	}

	public static void transfer(Storage<ItemVariant> handlerA, Storage<ItemVariant> handlerB, Consumer<Supplier<ItemStack>> onInserted, @Nullable TransactionContext ctx) {
		if (handlerA == null || handlerB == null) {
			return;
		}

		try (Transaction outer = Transaction.openNested(ctx)) {
			for (StorageView<ItemVariant> view : handlerA.nonEmptyViews()) {
				ItemVariant resource = view.getResource();
				long maxExtracted;

				// check how much can be extracted
				try (Transaction extractionTestTransaction = outer.openNested()) {
					maxExtracted = view.extract(resource, view.getAmount(), extractionTestTransaction);
				}

				try (Transaction transferTransaction = outer.openNested()) {
					// check how much can be inserted
					long accepted = handlerB.insert(resource, maxExtracted, transferTransaction);

					// extract it, or rollback if the amounts don't match
					if (accepted > 0 && view.extract(resource, accepted, transferTransaction) == accepted) {
						TransactionCallback.onSuccess(transferTransaction, () -> onInserted.accept(() -> resource.toStack((int) accepted)));
						transferTransaction.commit();
					}
				}
			}

			outer.commit();
		} catch (Exception e) {
			CrashReport report = CrashReport.forThrowable(e, "Moving resources between storages");
			report.addCategory("Move details")
					.setDetail("Input storage", handlerA::toString)
					.setDetail("Output storage", handlerB::toString);
			throw new ReportedException(report);
		}
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

	static Map<ItemStackKey, Integer> getCompactedStacks(SlottedStorage<ItemVariant> handler) {
		return getCompactedStacks(handler, new HashSet<>());
	}

	static Map<ItemStackKey, Integer> getCompactedStacks(SlottedStorage<ItemVariant> handler, Set<Integer> ignoreSlots) {
		Map<ItemStackKey, Integer> ret = new HashMap<>();
		for (int slotIndex = 0; slotIndex < handler.getSlotCount(); slotIndex++) {
			var slot = handler.getSlot(slotIndex);
			if (slot.isResourceBlank() || ignoreSlots.contains(slotIndex)) {
				continue;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(slot.getResource().toStack());
			ret.put(itemStackKey, ret.computeIfAbsent(itemStackKey, fs -> 0) + (int)slot.getAmount());
		}
		return ret;
	}

	public static List<ItemStack> getCompactedStacksSortedByCount(SlottedStorage<ItemVariant> handler) {
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

	public static Set<ItemStackKey> getUniqueStacks(SlottedStorage<ItemVariant> handler) {
		Set<ItemStackKey> uniqueStacks = new HashSet<>();
		for (StorageView<ItemVariant> view : handler.nonEmptyViews()) {
			ItemStack stack = view.getResource().toStack((int) view.getAmount());
			if (stack.isEmpty()) {
				continue;
			}
			ItemStackKey itemStackKey = ItemStackKey.of(stack);
			uniqueStacks.add(itemStackKey);
		}
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
		for (StorageView<ItemVariant> view : inventoryHandler.nonEmptyViews()) {
			long extracted;
			ItemVariant resource = view.getResource();
			try (Transaction ctx = Transaction.openOuter()) {
				extracted = view.extract(resource, view.getAmount(), ctx);
				ctx.commit();
			}
			ItemStack extractedStack = resource.toStack((int) extracted);
			while (!extractedStack.isEmpty()) {
				Containers.dropItemStack(level, x, y, z, extractedStack.split(Math.min(extractedStack.getCount(), extractedStack.getMaxStackSize())));
			}
		}
	}

	public static int getAnalogOutputSignal(ITrackedContentsItemHandler handler) {
		double totalFilled = 0;
		boolean isEmpty = true;
		for (int slot = 0; slot < handler.getSlotCount(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (!stack.isEmpty()) {
				int slotLimit = handler.getInternalSlotLimit(slot);
				totalFilled += stack.getCount() / (slotLimit / ((float) 64 / stack.getMaxStackSize()));
				isEmpty = false;
			}
		}
		double percentFilled = totalFilled / handler.getSlotCount();
		return Mth.floor(percentFilled * 14.0F) + (isEmpty ? 0 : 1);
	}
}
