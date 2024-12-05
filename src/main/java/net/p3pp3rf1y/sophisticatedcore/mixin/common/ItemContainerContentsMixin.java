package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.p3pp3rf1y.sophisticatedcore.extensions.item.component.SophisticatedItemContainerContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemContainerContents.class)
public class ItemContainerContentsMixin implements SophisticatedItemContainerContents {
	@Shadow
	@Final
	private NonNullList<ItemStack> items;

	@Override
	public int sophisticatedCore_getSlots() {
		return this.items.size();
	}

	@Override
	public ItemStack sophisticatedCore_getStackInSlot(int slot) {
		this.sophisticatedCore_validateSlotIndex(slot);
		return this.items.get(slot).copy();
	}

	@Unique
	private void sophisticatedCore_validateSlotIndex(int slot) {
		if (slot < 0 || slot >= this.sophisticatedCore_getSlots()) {
			throw new UnsupportedOperationException("Slot " + slot + " not in valid range - [0," + this.sophisticatedCore_getSlots() + ")");
		}
	}
}
