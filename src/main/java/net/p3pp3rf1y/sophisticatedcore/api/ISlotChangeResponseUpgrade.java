package net.p3pp3rf1y.sophisticatedcore.api;


import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(SlottedStackStorage inventoryHandler, int slot);
}
