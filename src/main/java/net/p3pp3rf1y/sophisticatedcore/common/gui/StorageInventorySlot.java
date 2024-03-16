package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.porting_lib.transfer.items.SlotItemHandler;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public class StorageInventorySlot extends SlotItemHandler {
	private final boolean isClientSide;
	private final IStorageWrapper storageWrapper;
	private final InventoryHandler inventoryHandler;
	private final int slotIndex;

	public StorageInventorySlot(boolean isClientSide, IStorageWrapper storageWrapper, InventoryHandler inventoryHandler, int slotIndex) {
		super(inventoryHandler, slotIndex, 0, 0);
		this.isClientSide = isClientSide;
		this.storageWrapper = storageWrapper;
		this.inventoryHandler = inventoryHandler;
		this.slotIndex = slotIndex;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		// saving here as well because there are many cases where vanilla modifies stack directly without and inventory handler isn't aware of it
		// however it does notify the slot of change
		storageWrapper.getInventoryHandler().onContentsChanged(slotIndex);
		processSlotChangeResponse(slotIndex, storageWrapper.getInventoryHandler(), storageWrapper);
	}

	private void processSlotChangeResponse(int slot, SlottedStackStorage handler, IStorageWrapper storageWrapper) {
		if (!isClientSide) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(ISlotChangeResponseUpgrade.class).forEach(u -> u.onSlotChange(handler, slot));
		}
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return inventoryHandler.getStackLimit(slotIndex, ItemVariant.of(stack));
	}

	@Override
	public ItemStack safeInsert(ItemStack stack, int maxCount) {
		if (!stack.isEmpty() && mayPlace(stack)) {
			ItemStack itemstack = getItem();
			int i = Math.min(Math.min(maxCount, stack.getCount()), getMaxStackSize(stack) - itemstack.getCount());
			if (itemstack.isEmpty()) {
				set(stack.split(i));
			} else if (ItemStack.isSameItemSameTags(itemstack, stack)) {
				stack.shrink(i);
				ItemStack copy = itemstack.copy();
				copy.grow(i);
				set(copy);
			}

			return stack;
		} else {
			return stack;
		}
	}
}
