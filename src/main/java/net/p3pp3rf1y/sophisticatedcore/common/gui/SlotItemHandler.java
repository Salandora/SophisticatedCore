package net.p3pp3rf1y.sophisticatedcore.common.gui;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotItemHandler<T extends SlottedStorage<ItemVariant>> extends Slot {
	private static final Container emptyInventory = new SimpleContainer(0);
	private final T slottedStackStorage;
	private final int index;

	public SlotItemHandler(T slottedStorage, int slot, int x, int y) {
		super(emptyInventory, slot, x, y);
		this.slottedStackStorage = slottedStorage;
		this.index = slot;
	}

	public T getItemHandler() {
		return slottedStackStorage;
	}

	@Override
	public ItemStack getItem() {
		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			return storage.getStackInSlot(this.index);
		}

		var slot = this.slottedStackStorage.getSlot(this.index);
		return slot.getResource().toStack((int) slot.getAmount());
	}

	@Override
	public void set(ItemStack stack) {
		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			storage.setStackInSlot(this.index, stack);
			this.setChanged();
			return;
		}

		// Extract content only if the slot is not blank
		var slot = this.slottedStackStorage.getSlot(this.index);
		if (!slot.isResourceBlank()) {
			try (Transaction extract = Transaction.openOuter()) {
				this.slottedStackStorage.extract(slot.getResource(), slot.getAmount(), extract);
				extract.commit();
			}
		}

		// Only insert if new content is not blank
		var resource = ItemVariant.of(stack);
		if (!resource.isBlank()) {
			try (Transaction insert = Transaction.openOuter()) {
				this.slottedStackStorage.insert(ItemVariant.of(stack), stack.getCount(), insert);
				insert.commit();
			}
		}
		this.setChanged();
	}

	@Override
	public ItemStack remove(int amount) {
		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			ItemStack stack = storage.getStackInSlot(this.index).copy();
			ItemStack removed = stack.split(amount);
			storage.setStackInSlot(this.index, stack);
			return removed;
		} else {
			var slot = this.slottedStackStorage.getSlot(this.index);
			long extracted;
			try (Transaction extract = Transaction.openOuter()) {
				extracted = this.slottedStackStorage.extract(slot.getResource(), amount, extract);
				extract.commit();
			}
			return slot.getResource().toStack((int) extracted);
		}
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			return storage.isItemValid(this.index, ItemVariant.of(stack), stack.getCount());
		}

		return true;
	}

	@Override
	public boolean mayPickup(Player player) {
		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			return !storage.getStackInSlot(this.index).isEmpty();
		}

		return !slottedStackStorage.getSlot(this.index).isResourceBlank();
	}

	@Override
	public int getMaxStackSize() {
		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			return storage.getSlotLimit(this.index);
		}

		return (int)this.slottedStackStorage.getSlot(this.index).getCapacity();
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		int maxSize;
		if (slottedStackStorage instanceof SlottedStackStorage storage) {
			maxSize = storage.getSlotLimit(this.index);
		} else {
			maxSize = (int)this.slottedStackStorage.getSlot(this.index).getCapacity();
		}

		return Math.min(stack.getMaxStackSize(), maxSize);
	}

	@Override
	public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
	}
}
