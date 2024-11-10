package net.p3pp3rf1y.sophisticatedcore.extensions.item.component;

import net.minecraft.world.item.ItemStack;

public interface SophisticatedItemContainerContents {
	default int getSlots() {
		throw new RuntimeException("Should have been overriden by mixin.");
	}
	default ItemStack getStackInSlot(int slot) {
		throw new RuntimeException("Should have been overriden by mixin.");
	}
}
