package net.p3pp3rf1y.sophisticatedcore.api;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(SlottedStackStorage inventoryHandler, int slot);
}
