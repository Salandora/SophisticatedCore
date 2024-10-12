package net.p3pp3rf1y.sophisticatedcore.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;

public interface IItemHandlerSimpleInserter extends SlottedStackStorage, IInventoryHandlerHelper {
	default boolean isItemValid(int slot, ItemStack stack) {
		return isItemValid(slot, ItemVariant.of(stack), stack.getCount());
	}

	@Override
	default boolean isItemValid(int slot, ItemVariant resource, int count) {
		return isItemValid(slot, resource.toStack(count));
	}
}
