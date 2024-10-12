package net.p3pp3rf1y.sophisticatedcore.util;

import com.google.common.base.Preconditions;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.p3pp3rf1y.sophisticatedcore.extensions.component.SophisticatedMutableDataComponentHolder;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;

public abstract class ComponentItemHandler implements IItemHandlerSimpleInserter {
	protected final SophisticatedMutableDataComponentHolder parent;
	protected final DataComponentType<ItemContainerContents> component;
	protected final int size;

	public ComponentItemHandler(SophisticatedMutableDataComponentHolder parent, DataComponentType<ItemContainerContents> component, int size) {
		this.parent = parent;
		this.component = component;
		this.size = size;
		Preconditions.checkArgument(size <= 256, "The max size of ItemContainerContents is 256 slots.");
	}

	// TODO: implement?
	@Override
	public SingleSlotStorage<ItemVariant> getSlot(int slot) {
		return null;
	}

	@Override
	public int getSlotCount() {
		return size;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		ItemContainerContents contents = getContents();
		return this.getStackFromContents(contents, slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		this.validateSlotIndex(slot);
		if (!this.isItemValid(slot, stack)) {
			throw new RuntimeException("Invalid stack " + stack + " for slot " + slot + ")");
		}
		ItemContainerContents contents = this.getContents();
		ItemStack existing = this.getStackFromContents(contents, slot);
		if (!ItemStack.matches(stack, existing)) {
			this.updateContents(contents, stack, slot);
		}
	}

	// TODO: implement?
	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return insertItem(slot, resource.toStack((int) maxAmount), ctx).getCount();
	}

	public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
		ItemStack result;
		try (Transaction ctx = Transaction.openOuter()) {
			result = insertItem(slot, toInsert, ctx);
			if (!simulate) {
				ctx.commit();
			}
		}
		return result;
	}

	public ItemStack insertItem(int slot, ItemStack toInsert, TransactionContext ctx) {
		this.validateSlotIndex(slot);

		if (toInsert.isEmpty()) {
			return ItemStack.EMPTY;
		}

		if (!this.isItemValid(slot, toInsert)) {
			return toInsert;
		}

		ItemContainerContents contents = this.getContents();
		ItemStack existing = this.getStackFromContents(contents, slot);
		// Max amount of the stack that could be inserted
		int insertLimit = Math.min(this.getSlotLimit(slot), toInsert.getMaxStackSize());

		if (!existing.isEmpty()) {
			if (!ItemStack.isSameItemSameComponents(toInsert, existing)) {
				return toInsert;
			}

			insertLimit -= existing.getCount();
		}

		if (insertLimit <= 0) {
			return toInsert;
		}

		int inserted = Math.min(insertLimit, toInsert.getCount());
		TransactionCallback.onSuccess(ctx, () -> this.updateContents(contents, toInsert.copyWithCount(existing.getCount() + inserted), slot));

		return toInsert.copyWithCount(toInsert.getCount() - inserted);
	}

	// TODO: implement?
	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return extractItem(slot, (int) maxAmount, ctx).getCount();
	}

	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack result;
		try (Transaction ctx = Transaction.openOuter()) {
			result = extractItem(slot, amount, ctx);
			if (!simulate) {
				ctx.commit();
			}
		}
		return result;
	}

	public ItemStack extractItem(int slot, int amount, TransactionContext ctx) {
		this.validateSlotIndex(slot);

		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		ItemContainerContents contents = this.getContents();
		ItemStack existing = this.getStackFromContents(contents, slot);

		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int toExtract = Math.min(amount, existing.getMaxStackSize());
		TransactionCallback.onSuccess(ctx, () -> this.updateContents(contents, existing.copyWithCount(existing.getCount() - toExtract), slot));

		return existing.copyWithCount(toExtract);
	}

	@Override
	public int getSlotLimit(int slot) {
		return Item.ABSOLUTE_MAX_STACK_SIZE;
	}

	@Override
	public boolean isItemValid(int slot, ItemVariant resource, int count) {
		return isItemValid(slot, resource.toStack(count));
	}

	public boolean isItemValid(int slot, ItemStack stack) {
		return stack.getItem().canFitInsideContainerItems();
	}

	/**
	 * Called from {@link #updateContents} after the stack stored in a slot has been updated.
	 * <p>
	 * Modifications to the stacks used as parameters here will not write-back to the stored data.
	 *
	 * @param slot     The slot that changed
	 * @param oldStack The old stack that was present in the slot
	 * @param newStack The new stack that is now present in the slot
	 */
	protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {}

	/**
	 * Retrieves the {@link ItemContainerContents} from the parent object's data component map.
	 */
	protected ItemContainerContents getContents() {
		return this.parent.getOrDefault(this.component, ItemContainerContents.EMPTY);
	}

	/**
	 * Retrieves a copy of a single stack from the underlying data component, returning {@link ItemStack#EMPTY} if the component does not have a slot present.
	 * <p>
	 * Throws an exception if the slot is out-of-bounds for this capability.
	 *
	 * @param contents The existing contents from {@link #getContents()}
	 * @param slot     The target slot
	 * @return A copy of the stack in the target slot
	 */
	protected ItemStack getStackFromContents(ItemContainerContents contents, int slot) {
		this.validateSlotIndex(slot);
		return contents.getSlots() <= slot ? ItemStack.EMPTY : contents.getStackInSlot(slot);
	}

	/**
	 * Performs a copy and write operation on the underlying data component, changing the stack in the target slot.
	 * <p>
	 * If the existing component is larger than {@link #getSlots()}, additional slots will <b>not</b> be truncated.
	 *
	 * @param contents The existing contents from {@link #getContents()}
	 * @param stack    The new stack to set to the slot
	 * @param slot     The target slot
	 */
	protected void updateContents(ItemContainerContents contents, ItemStack stack, int slot) {
		this.validateSlotIndex(slot);
		// Use the max of the contents slots and the capability slots to avoid truncating
		NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), this.getSlotCount()), ItemStack.EMPTY);
		contents.copyInto(list);
		ItemStack oldStack = list.get(slot);
		list.set(slot, stack);
		this.parent.set(this.component, ItemContainerContents.fromItems(list));
		this.onContentsChanged(slot, oldStack, stack);
	}

	/**
	 * Throws {@link UnsupportedOperationException} if the provided slot index is invalid.
	 */
	protected final void validateSlotIndex(int slot) {
		if (slot < 0 || slot >= getSlotCount()) {
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlotCount() + ")");
		}
	}
}
