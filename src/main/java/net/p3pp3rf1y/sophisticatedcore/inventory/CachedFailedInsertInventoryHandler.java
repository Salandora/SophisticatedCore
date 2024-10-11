package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public class CachedFailedInsertInventoryHandler implements SlottedStackStorage {
	private final Supplier<SlottedStackStorage> wrappedHandlerGetter;
	private final LongSupplier timeSupplier;
	private long currentCacheTime = 0;
	private final Set<Integer> failedInsertStackHashes = new HashSet<>();

	public CachedFailedInsertInventoryHandler(Supplier<SlottedStackStorage> wrappedHandlerGetter, LongSupplier timeSupplier) {
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

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStackHashes.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		boolean hashCalculated = false;
		int stackHash = 0;
		if (!failedInsertStackHashes.isEmpty()) {
			stackHash = ItemStackKey.getHashCode(resource);
			hashCalculated = true;
			if (failedInsertStackHashes.contains(stackHash)) {
				return 0;
			}
		}

		long inserted = wrappedHandlerGetter.get().insert(resource, maxAmount, ctx);
		if (inserted == 0) {
			if (!hashCalculated) {
				stackHash = ItemStackKey.getHashCode(resource);
			}
			failedInsertStackHashes.add(stackHash);
		}

		return inserted;
	}
	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStackHashes.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}

		boolean hashCalculated = false;
		int stackHash = 0;
		if (!failedInsertStackHashes.isEmpty()) {
			stackHash = ItemStackKey.getHashCode(resource);
			hashCalculated = true;
			if (failedInsertStackHashes.contains(stackHash)) {
				return 0;
			}
		}

		long inserted = wrappedHandlerGetter.get().insertSlot(slot, resource, maxAmount, ctx);
		if (inserted == 0) {
			if (!hashCalculated) {
				stackHash = ItemStackKey.getHashCode(resource);
			}
			failedInsertStackHashes.add(stackHash);
		}

		return inserted;
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
	public boolean isItemValid(int slot, @NotNull ItemVariant resource) {
		return wrappedHandlerGetter.get().isItemValid(slot, resource);
	}

	@Override
	public Iterator<StorageView<ItemVariant>> iterator() {
		return wrappedHandlerGetter.get().iterator();
	}
}
