package net.p3pp3rf1y.sophisticatedcore.api;

import com.github.salandora.sophisticatedlibrary.transfer.api.v1.IItemHandler;

public interface ISlotChangeResponseUpgrade {
	void onSlotChange(IItemHandler inventoryHandler, int slot);
}
