package net.p3pp3rf1y.sophisticatedcore.api;

import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(IItemHandlerSimpleInserter inventoryHandler, int slot);
}
