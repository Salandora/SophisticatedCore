package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorageModifiable;
import net.minecraft.world.item.ItemStack;

public interface IItemHandlerSimpleInserter extends SlottedStackStorageModifiable {
	ItemStack insertItem(ItemStack stack, boolean simulate);
}
