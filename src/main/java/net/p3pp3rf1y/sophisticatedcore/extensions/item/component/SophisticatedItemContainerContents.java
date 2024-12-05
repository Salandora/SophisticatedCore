package net.p3pp3rf1y.sophisticatedcore.extensions.item.component;

import net.minecraft.world.item.ItemStack;

public interface SophisticatedItemContainerContents {
	default int sophisticatedCore_getSlots() {
		throw new RuntimeException("Should have been overriden by mixin.");
	}
	default ItemStack sophisticatedCore_getStackInSlot(int slot) {
		throw new RuntimeException("Should have been overriden by mixin.");
	}
}
