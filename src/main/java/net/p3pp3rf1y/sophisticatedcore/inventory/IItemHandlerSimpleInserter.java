package net.p3pp3rf1y.sophisticatedcore.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import javax.annotation.Nullable;

public interface IItemHandlerSimpleInserter extends SlotExposedStorage {
	@Override
	long insert(ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx);
	//ItemStack insertItem(ItemStack stack, boolean simulate);
}
