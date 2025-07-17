package net.p3pp3rf1y.sophisticatedcore.upgrades;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;
import net.minecraft.world.item.ItemStack;

public interface IInsertResponseUpgrade {
	ItemStack onBeforeInsert(SlottedStackStorage inventoryHandler, int slot, ItemStack stack, boolean simulate);

	void onAfterInsert(SlottedStackStorage inventoryHandler, int slot);
}
