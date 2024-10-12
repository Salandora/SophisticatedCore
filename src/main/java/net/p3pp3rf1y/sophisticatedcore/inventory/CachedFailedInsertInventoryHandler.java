package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class CachedFailedInsertInventoryHandler implements IItemHandlerSimpleInserter {
	private final Supplier<IItemHandlerSimpleInserter> wrappedHandlerGetter;
	private final LongSupplier timeSupplier;
	private long currentCacheTime = 0;
	private final Set<ItemStack> failedInsertStacks = new HashSet<>();

	public CachedFailedInsertInventoryHandler(Supplier<IItemHandlerSimpleInserter> wrappedHandlerGetter, LongSupplier timeSupplier) {
		this.wrappedHandlerGetter = wrappedHandlerGetter;
		this.timeSupplier = timeSupplier;
	}

	@Override
	public void setStackInSlot(int slot, @NotNull ItemStack stack) {
		wrappedHandlerGetter.get().setStackInSlot(slot, stack);
	}

	@Override
	public int getSlotCount() {
		return wrappedHandlerGetter.get().getSlotCount();
	}

	@Override
	public SingleSlotStorage<ItemVariant> getSlot(int slot) {
		return wrappedHandlerGetter.get().getSlot(slot);
	}

	@NotNull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return wrappedHandlerGetter.get().getStackInSlot(slot);
	}

	@NotNull
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStacks.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		if (failedInsertStacks.contains(stack)) {
			return stack;
		}

		ItemStack result = wrappedHandlerGetter.get().insertItem(slot, stack, simulate);

		if (result == stack) {
			failedInsertStacks.add(stack); //only working with stack references because this logic is meant to handle the case where something tries to insert the same stack number of slots times
		}

		return result;
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStacks.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		if (failedInsertStacks.contains(resource.toStack())) {
			return 0;
		}

		long inserted = wrappedHandlerGetter.get().insert(resource, maxAmount, ctx);
		if (inserted == 0) {
			failedInsertStacks.add(resource.toStack()); //only working with stack references because this logic is meant to handle the case where something tries to insert the same stack number of slots times
		}

		return inserted;
	}

	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStacks.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		if (failedInsertStacks.contains(resource.toStack())) {
			return 0;
		}

		long inserted = wrappedHandlerGetter.get().insertSlot(slot, resource, maxAmount, ctx);
		if (inserted == 0) {
			failedInsertStacks.add(resource.toStack()); //only working with stack references because this logic is meant to handle the case where something tries to insert the same stack number of slots times
		}

		return inserted;
	}

	@NotNull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return wrappedHandlerGetter.get().extractItem(slot, amount, simulate);
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return wrappedHandlerGetter.get().extract(resource, maxAmount, ctx);
	}

	@Override
	public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return wrappedHandlerGetter.get().extractSlot(slot, resource, maxAmount, ctx);
	}

	@Override
	public int getSlotLimit(int slot) {
		return wrappedHandlerGetter.get().getSlotLimit(slot);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemStack stack) {
		return wrappedHandlerGetter.get().isItemValid(slot, stack);
	}

	@Override
	public boolean isItemValid(int slot, @NotNull ItemVariant resource, int count) {
		return wrappedHandlerGetter.get().isItemValid(slot, resource, count);
	}

	@Override
	public Iterator<StorageView<ItemVariant>> iterator() {
		return wrappedHandlerGetter.get().iterator();
	}
}
