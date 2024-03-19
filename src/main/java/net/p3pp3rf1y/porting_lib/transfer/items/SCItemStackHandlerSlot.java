package net.p3pp3rf1y.porting_lib.transfer.items;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class SCItemStackHandlerSlot extends SingleStackStorage {
	private final int index;
	private final SCItemStackHandler handler;
	private ItemStack stack;
	private ItemStack lastStack; // last stack pre-transaction
	private ItemVariant variant;

	public SCItemStackHandlerSlot(int index, SCItemStackHandler handler, ItemStack initial) {
		this.index = index;
		this.handler = handler;
		this.lastStack = initial;
		this.setStack(initial);
		handler.initSlot(this);
	}

	@Override
	protected boolean canInsert(ItemVariant itemVariant) {
		return handler.isItemValid(this.index, itemVariant);
	}

	@Override
	public int getCapacity(ItemVariant itemVariant) {
		return this.handler.getStackLimit(this.index, itemVariant);
	}

	@Override
	public ItemStack getStack() {
		return this.stack;
	}

	/**
	 * Should only be used in transactions.
	 */
	@Override
	protected void setStack(ItemStack stack) {
		this.stack = stack;
		this.variant = ItemVariant.of(stack);
	}

	public void setNewStack(ItemStack stack) {
		this.setStack(stack);
		this.onFinalCommit();
	}

	@Override
	public ItemVariant getResource() {
		return this.variant;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	protected void onFinalCommit() {
		onStackChange();
		notifyHandlerOfChange();
	}

	protected void onStackChange() {
		handler.onStackChange(this, lastStack, stack);
		this.lastStack = stack;
	}

	protected void notifyHandlerOfChange() {
		handler.onContentsChanged(index);
	}

	/**
	 * Save this slot to a new NBT tag.
	 * Note that "Slot" is a reserved key.
	 * @return null to skip saving this slot
	 */
	@Nullable
	public CompoundTag save() {
		return stack.isEmpty() ? null : stack.save(new CompoundTag());
	}

	public void load(CompoundTag tag) {
		load(ItemStack.of(tag));
	}

	public void load(ItemStack stack) {
		setStack(stack);
		onStackChange();
		// intentionally do not notify handler, matches forge
	}
}