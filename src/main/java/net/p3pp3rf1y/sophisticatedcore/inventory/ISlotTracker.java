package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public interface ISlotTracker {

	void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty);

	Set<ItemStackKey> getFullStacks();

	Set<ItemStackKey> getPartialStacks();

	Set<Item> getItems();

	void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack);

	void clear();

	void refreshSlotIndexesFrom(InventoryHandler itemHandler);

	long insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx);
	//ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, ItemStack stack, boolean simulate);

	long insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx);
	//ItemStack insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, int slot, ItemStack stack, boolean simulate);

	void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot);

	void unregisterStackKeyListeners();

	boolean hasEmptySlots();

	interface IItemHandlerInserter {
		long insertItem(int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext transaction);
		//ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
	}

	class Noop implements ISlotTracker {
		@Override
		public void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty) {
			//noop
		}

		@Override
		public Set<ItemStackKey> getFullStacks() {
			return Collections.emptySet();
		}

		@Override
		public Set<ItemStackKey> getPartialStacks() {
			return Collections.emptySet();
		}

		@Override
		public Set<Item> getItems() {
			return Collections.emptySet();
		}

		@Override
		public void removeAndSetSlotIndexes(InventoryHandler inventoryHandler, int slot, ItemStack stack) {
			//noop
		}

		@Override
		public void clear() {
			//noop
		}

		@Override
		public void refreshSlotIndexesFrom(InventoryHandler itemHandler) {
			//noop
		}

		@Override
		public long insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
			return maxAmount;
		}

		@Override
		public long insertItemIntoHandler(InventoryHandler itemHandler, IItemHandlerInserter inserter, UnaryOperator<ItemStack> overflowHandler, int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
			return inserter.insertItem(slot, resource, maxAmount, ctx);
		}

		@Override
		public void registerListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
			//noop
		}

		@Override
		public void unregisterStackKeyListeners() {
			//noop
		}

		@Override
		public boolean hasEmptySlots() {
			return false;
		}
	}
}
