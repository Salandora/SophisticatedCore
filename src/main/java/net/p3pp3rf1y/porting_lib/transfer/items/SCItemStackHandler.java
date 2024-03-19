package net.p3pp3rf1y.porting_lib.transfer.items;

import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;

import io.github.fabricators_of_create.porting_lib.core.util.INBTSerializable;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import io.github.fabricators_of_create.porting_lib.util.DualSortedSetIterator;
import io.github.fabricators_of_create.porting_lib.util.EmptySortedSet;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * An implementation of a item storage designed for ease of use and speed.
 */
public class SCItemStackHandler implements SlottedStackStorage, INBTSerializable<CompoundTag> {
	private final List<SCItemStackHandlerSlot> slots;
	private final SortedSet<SCItemStackHandlerSlot> nonEmptySlots;
	private final Map<Item, SortedSet<SCItemStackHandlerSlot>> lookup;

	public SCItemStackHandler() {
		this(1);
	}

	public SCItemStackHandler(int stacks) {
		this(createEmptyStackArray(stacks));
	}

	public SCItemStackHandler(ItemStack[] stacks) {
		this.slots = new ArrayList<>(stacks.length);
		this.nonEmptySlots = createSlotSet();
		this.lookup = new HashMap<>();
		for (int i = 0; i < stacks.length; i++) {
			ItemStack stack = stacks[i];
			// slot handles filling lookup
			this.slots.add(makeSlot(i, stack));
		}
	}

	// core functionality

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(resource, maxAmount);
		long inserted = 0;
		Iterator<SCItemStackHandlerSlot> iter = getInsertableSlotsFor(resource);
		while (iter.hasNext()) {
			SCItemStackHandlerSlot slot = iter.next();
			inserted += slot.insert(resource, maxAmount - inserted, transaction);
			if (inserted >= maxAmount) {
				break;
			}
		}
		return inserted;
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		StoragePreconditions.notBlankNotNegative(resource, maxAmount);
		Item item = resource.getItem();
		SortedSet<SCItemStackHandlerSlot> slots = getSlotsContaining(item);
		if (slots.isEmpty()) {
			return 0; // no slots hold this item
		}
		long extracted = 0;
		for (SCItemStackHandlerSlot slot : slots) {
			extracted += slot.extract(resource, maxAmount - extracted, transaction);
			if (extracted >= maxAmount) {
				break;
			}
		}
		return extracted;
	}

	@Override
	@Nullable
	public StorageView<ItemVariant> exactView(ItemVariant resource) {
		StoragePreconditions.notBlank(resource);
		SortedSet<SCItemStackHandlerSlot> slots = getSlotsContaining(resource.getItem());
		return slots.isEmpty() ? null : slots.first();
	}

	// iteration

	@Override
	public Iterable<StorageView<ItemVariant>> nonEmptyViews() {
		//noinspection unchecked,rawtypes
		return (Iterable) this.nonEmptySlots;
	}

	@Override
	public Iterator<StorageView<ItemVariant>> nonEmptyIterator() {
		//noinspection unchecked,rawtypes
		return (Iterator) this.nonEmptySlots.iterator();
	}

	// slot support

	@Override
	public int getSlotCount() {
		return slots.size();
	}

	@Override
	public SCItemStackHandlerSlot getSlot(int slot) {
		return this.slots.get(slot);
	}

	@Override
	public List<SingleSlotStorage<ItemVariant>> getSlots() {
		//noinspection unchecked,rawtypes
		return (List) slots;
	}

	// API, mostly from forge, with extras

	@Override
	public ItemStack getStackInSlot(int slot) {
		return getSlot(slot).getStack();
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		getSlot(slot).setNewStack(stack);
	}

	public ItemVariant getVariantInSlot(int slot) {
		return getSlot(slot).getResource();
	}

	public int getSlotLimit(int slot) {
		return getStackInSlot(slot).getMaxStackSize();
	}

	/**
	 * Determines the maximum amount of an item that can be stored in the given slot.
	 */
	protected int getStackLimit(int slot, ItemVariant resource) {
		return Math.min(getSlotLimit(slot), resource.getItem().getMaxStackSize());
	}

	/**
	 * Once a transaction is committed, this is invoked once for each modified slot.
	 */
	protected void onContentsChanged(int slot) {
	}

	/**
	 * Get a set of all slots containing the given item, sorted by ascending indices.
	 * The returned set may be empty, and should not be modified in any way.
	 */
	public SortedSet<SCItemStackHandlerSlot> getSlotsContaining(Item item) {
		return lookup.containsKey(item) ? lookup.get(item) : EmptySortedSet.cast();
	}

	/**
	 * Called after NBT is loaded and this handler has been updated.
	 */
	protected void onLoad() {
	}

	/**
	 * True if this handler only contains empty stacks.
	 */
	public boolean empty() {
		return nonEmptySlots.isEmpty();
	}

	/**
	 * Resize this handler, clearing all existing content.
	 */
	public void setSize(int size) {
		this.slots.clear();
		nonEmptySlots.clear();
		lookup.clear();
		for (int i = 0; i < size; i++) {
			this.slots.add(makeSlot(i, ItemStack.EMPTY));
		}
	}

	protected SCItemStackHandlerSlot makeSlot(int index, ItemStack stack) {
		return new SCItemStackHandlerSlot(index, this, stack);
	}

	// serialization

	@Override
	public CompoundTag serializeNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("Size", this.slots.size());

		ListTag slots = new ListTag();
		for (SCItemStackHandlerSlot slot : this.slots) {
			CompoundTag slotTag = slot.save();
			if (slotTag != null) {
				slotTag.putInt("Slot", slot.getIndex());
				slots.add(slotTag);
			}
		}

		nbt.put("Items", slots);
		return nbt;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : slots.size()); // also clears
		ListTag slots = nbt.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < slots.size(); i++) {
			CompoundTag slotTag = slots.getCompound(i);
			int index = slotTag.getInt("Slot");

			if (index >= 0 && index < this.slots.size()) {
				this.slots.get(index).load(slotTag);
			}
		}
		onLoad();
	}

	// misc

	void onStackChange(SCItemStackHandlerSlot slot, ItemStack oldStack, ItemStack newStack) {
		if (ItemStack.matches(oldStack, newStack)) {
			return;
		}

		SortedSet<SCItemStackHandlerSlot> oldItemSlots = this.getSlotsContaining(oldStack.getItem());
		if (!oldItemSlots.isEmpty()) {
			oldItemSlots.remove(slot);
		}
		lookup.computeIfAbsent(newStack.getItem(), $ -> createSlotSet()).add(slot);
		if (oldStack.isEmpty()) { // no longer empty
			nonEmptySlots.add(slot);
		} else if (newStack.isEmpty()) { // became empty
			nonEmptySlots.remove(slot);
		}
	}

	void initSlot(SCItemStackHandlerSlot slot) {
		ItemStack stack = slot.getStack();
		lookup.computeIfAbsent(stack.getItem(), $ -> createSlotSet()).add(slot);
		if (!stack.isEmpty()) {
			nonEmptySlots.add(slot);
		}
	}

	@Override
	public String toString() {
		return  getClass().getSimpleName() + '[' + this.slots + ']';
	}

	private Iterator<SCItemStackHandlerSlot> getInsertableSlotsFor(ItemVariant variant) {
		SortedSet<SCItemStackHandlerSlot> slots = this.getSlotsContaining(variant.getItem());
		SortedSet<SCItemStackHandlerSlot> emptySlots = this.getSlotsContaining(Items.AIR);
		if (slots.isEmpty()) {
			return emptySlots.isEmpty() ? Collections.emptyIterator() : emptySlots.iterator();
		} else {
			return emptySlots.isEmpty() ? slots.iterator() : new DualSortedSetIterator<>(slots, emptySlots);
		}
	}

	private static SortedSet<SCItemStackHandlerSlot> createSlotSet() {
		return new ObjectAVLTreeSet<>(Comparator.comparingInt(SCItemStackHandlerSlot::getIndex));
	}

	public static ItemStack[] createEmptyStackArray(int size) {
		ItemStack[] stacks = new ItemStack[size];
		Arrays.fill(stacks, ItemStack.EMPTY);
		return stacks;
	}
}