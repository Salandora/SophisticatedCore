package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.github.salandora.sophisticatedlibrary.transfer.IItemHandlerModifiable;
import net.minecraft.world.item.ItemStack;

public interface IItemHandlerSimpleInserter extends IItemHandlerModifiable {
	ItemStack insertItem(ItemStack stack, boolean simulate);
}
