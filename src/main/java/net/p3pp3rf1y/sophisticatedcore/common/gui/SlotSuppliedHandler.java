package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import java.util.function.Supplier;

public class SlotSuppliedHandler extends SlotItemHandler<SlottedStackStorage> {
	private final Supplier<? extends SlottedStackStorage> itemHandlerSupplier;
	private final int slot;

	public SlotSuppliedHandler(Supplier<? extends SlottedStackStorage> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(itemHandlerSupplier.get(), slot, xPosition, yPosition);

		this.itemHandlerSupplier = itemHandlerSupplier;
		this.slot = slot;
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
