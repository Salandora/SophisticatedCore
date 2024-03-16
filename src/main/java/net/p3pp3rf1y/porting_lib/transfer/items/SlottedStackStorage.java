package net.p3pp3rf1y.porting_lib.transfer.items;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;

import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

// Code from https://github.com/Fabricators-of-Create/Porting-Lib/blob/1.20.1/modules/transfer/src/main/java/io/github/fabricators_of_create/porting_lib/transfer/item/SlottedStackStorage.java
public interface SlottedStackStorage extends SlottedStorage<ItemVariant> {
	ItemStack getStackInSlot(int slot);

	void setStackInSlot(int slot, @NotNull ItemStack stack);

	int getSlotLimit(int slot);

	default boolean isItemValid(int slot, ItemVariant resource) {
		return true;
	}

	default long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return getSlot(slot).insert(resource, maxAmount, ctx);
	}

	default long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return getSlot(slot).extract(resource, maxAmount, ctx);
	}

	@Override
	default Iterator<StorageView<ItemVariant>> iterator() {
		//noinspection unchecked,rawtypes
		return (Iterator) getSlots().iterator();
	}
}
