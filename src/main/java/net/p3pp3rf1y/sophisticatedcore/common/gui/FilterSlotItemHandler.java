package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;

import java.util.function.Supplier;

public class FilterSlotItemHandler extends SlotSuppliedHandler implements IFilterSlot {
	public FilterSlotItemHandler(Supplier<SlottedStackStorage> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(itemHandlerSupplier, slot, xPosition, yPosition);
	}

	@Override
	public boolean mayPickup(Player playerIn) {
		return false;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return 1;
	}

}
