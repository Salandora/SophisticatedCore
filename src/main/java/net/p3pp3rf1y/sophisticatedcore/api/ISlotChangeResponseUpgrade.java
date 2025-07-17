package net.p3pp3rf1y.sophisticatedcore.api;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(SlottedStackStorage inventoryHandler, int slot);
}
