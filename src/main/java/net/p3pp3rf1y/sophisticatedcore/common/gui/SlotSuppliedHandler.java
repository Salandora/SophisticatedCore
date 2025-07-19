package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.github.salandora.sophisticatedlibrary.gui.SlotItemHandler;
import com.github.salandora.sophisticatedlibrary.transfer.IItemHandler;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class SlotSuppliedHandler extends SlotItemHandler {
	private final Supplier<? extends IItemHandler> itemHandlerSupplier;
	private final int slot;

	public SlotSuppliedHandler(Supplier<? extends IItemHandler> itemHandlerSupplier, int slot, int xPosition, int yPosition) {
		super(itemHandlerSupplier.get(), slot, xPosition, yPosition);

		this.itemHandlerSupplier = itemHandlerSupplier;
		this.slot = slot;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return itemHandlerSupplier.get().isItemValid(slot, stack);
	}

	@Override
	public int getMaxStackSize() {
		return itemHandlerSupplier.get().getSlotLimit(slot);
	}
}
