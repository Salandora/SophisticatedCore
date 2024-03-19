package net.p3pp3rf1y.porting_lib.transfer.items;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public class SCSlotItemHandler extends Slot {
	private static final Container emptyInventory = new SimpleContainer(0);
	private final SlottedStackStorage itemHandler;
	private final int index;

	public SCSlotItemHandler(SlottedStackStorage itemHandler, int index, int xPosition, int yPosition) {
		super(emptyInventory, index, xPosition, yPosition);
		this.itemHandler = itemHandler;
		this.index = index;
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		if (stack.isEmpty())
			return false;

		return this.getItemHandler().isItemValid(this.index, ItemVariant.of(stack));
	}

	@Override
	@NotNull
	public ItemStack getItem() {
		return this.getItemHandler().getStackInSlot(this.index);
	}

	// Override if your IItemHandler does not implement IItemHandlerModifiable
	@Override
	public void set(ItemStack stack) {
		this.getItemHandler().setStackInSlot(this.index, stack);
		this.setChanged();
	}

	@Override
	public void onQuickCraft(@NotNull ItemStack oldStackIn, @NotNull ItemStack newStackIn) {
	}

	@Override
	public int getMaxStackSize() {
		return this.getItemHandler().getSlotLimit(this.index);
	}

	@Override
	public int getMaxStackSize(@NotNull ItemStack stack) {
		return getItemHandler().getSlotLimit(this.index);
	}

	@Override
	public boolean mayPickup(@NotNull Player playerIn) {
		return !this.getItemHandler().getStackInSlot(this.index).isEmpty();
	}

	@Override
	@NotNull
	public ItemStack remove(int amount) {
		ItemStack held = this.getItemHandler().getStackInSlot(this.index).copy();
		ItemStack removed = held.split(amount);
		this.getItemHandler().setStackInSlot(this.index, held);
		return removed;
	}

	public SlottedStackStorage getItemHandler() {
		return itemHandler;
	}
}