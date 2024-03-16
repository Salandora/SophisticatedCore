package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.porting_lib.transfer.items.SlotItemHandler;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;

import java.util.function.Supplier;

public class SlotSuppliedHandler extends SlotItemHandler {
	private final Supplier<SlottedStackStorage> itemHandlerSupplier;
	private final int slot;

	public SlotSuppliedHandler(Supplier<SlottedStackStorage> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(itemHandlerSupplier.get(), slot, xPosition, yPosition);

		this.itemHandlerSupplier = itemHandlerSupplier;
		this.slot = slot;
	}

	@Override
	public SlottedStackStorage getItemHandler() {
		return itemHandlerSupplier.get();
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return itemHandlerSupplier.get().isItemValid(slot, ItemVariant.of(stack));
	}

	@Override
	public int getMaxStackSize() {
		return itemHandlerSupplier.get().getSlotLimit(slot);
	}
}
