package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

public interface IInventoryHandlerHelper extends SlottedStorage<ItemVariant> {
	default ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		long inserted;
		try (Transaction ctx = Transaction.openOuter()) {
			inserted = getSlot(slot).insert(ItemVariant.of(stack), stack.getCount(), ctx);
			if (!simulate) {
				ctx.commit();
			}
		}
		return inserted < stack.getCount() ? stack.copyWithCount(stack.getCount() - (int) inserted) : ItemStack.EMPTY;
	}

	default ItemStack insertItem(ItemStack stack, boolean simulate) {
		long inserted;
		try (Transaction ctx = Transaction.openOuter()) {
			inserted = insert(ItemVariant.of(stack), stack.getCount(), ctx);
			if (!simulate) {
				ctx.commit();
			}
		}
		return inserted < stack.getCount() ? stack.copyWithCount(stack.getCount() - (int) inserted) : ItemStack.EMPTY;
	}

	default ItemStack extractItem(int slot, int amount, boolean simulate) {
		var slotStorage = getSlot(slot);
		ItemVariant resource = slotStorage.getResource();
		long extracted;
		try (Transaction outer = Transaction.openOuter()) {
			extracted = slotStorage.extract(resource, amount, outer);
			if (!simulate) {
				outer.commit();
			}
		}
		return resource.toStack((int) extracted);
	}
}
