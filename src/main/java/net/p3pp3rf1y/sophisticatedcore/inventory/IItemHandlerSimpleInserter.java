package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;

public interface IItemHandlerSimpleInserter extends SlottedStackStorage {
	// This is already in Storage interface, so we only use this interface for compatibility
	//@Override
	//long insert(ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx);
}
