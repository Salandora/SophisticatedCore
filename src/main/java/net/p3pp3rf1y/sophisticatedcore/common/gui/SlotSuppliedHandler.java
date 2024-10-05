package net.p3pp3rf1y.sophisticatedcore.common.gui;

import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.p3pp3rf1y.porting_lib.transfer.items.SCSlotItemHandler;

import java.util.function.Supplier;

public class SlotSuppliedHandler extends SCSlotItemHandler {
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
		return itemHandlerSupplier.get().isItemValid(slot, ItemVariant.of(stack), stack.getCount());
	}

	@Override
	public int getMaxStackSize() {
		return itemHandlerSupplier.get().getSlotLimit(slot);
	}
}
