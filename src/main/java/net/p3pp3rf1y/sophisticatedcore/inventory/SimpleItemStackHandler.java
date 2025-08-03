package net.p3pp3rf1y.sophisticatedcore.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SimpleItemStackHandler extends ItemStackHandler implements IItemHandlerSimpleInserter {
	public SimpleItemStackHandler(int size) {
		super(size);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public boolean isItemValid(int slot, ItemVariant resource, int count) {
		return true;
	}

	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;

		if (!isItemValid(slot, stack))
			return stack;

		if (slot < 0 || slot >= getSlotCount())
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlotCount() + ")");

		ItemStack existing = this.getStackInSlot(slot);

		int limit = getStackLimit(slot, ItemVariant.of(stack));

		if (!existing.isEmpty()) {
			if (!ItemStack.isSameItemSameComponents(stack, existing))
				return stack;

			limit -= existing.getCount();
		}

		if (limit <= 0)
			return stack;

		boolean reachedLimit = stack.getCount() > limit;

		if (!simulate) {
			ItemStack result;
			if (existing.isEmpty()) {
				result = reachedLimit ? stack.copyWithCount(limit) : stack;
			} else {
				existing.grow(reachedLimit ? limit : stack.getCount());
				result = existing;
			}
			setStackInSlot(slot, result);
		}

		return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
	}

	@Override
	public @NotNull ItemStack insertItem(@NotNull ItemStack stack, boolean simulate) {
		ItemStack remaining = stack;
		for (int slot = 0; slot < getSlotCount(); slot++) {
			remaining = insertItem(slot, remaining, simulate);
			if (remaining.isEmpty()) {
				return remaining;
			}
		}

		return remaining;
	}

	@Override
	public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		ItemStack existing = getStackInSlot(slot);

		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int toExtract = Math.min(amount, existing.getMaxStackSize());

		if (existing.getCount() <= toExtract) {
			if (!simulate) {
				setStackInSlot(slot, ItemStack.EMPTY);
				return existing;
			} else {
				return existing.copy();
			}
		} else {
			if (!simulate) {
				setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
			}

			return existing.copyWithCount(toExtract);
		}
	}
}
