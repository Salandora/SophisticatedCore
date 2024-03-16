package net.p3pp3rf1y.sophisticatedcore.inventory;

import org.apache.commons.lang3.NotImplementedException;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public class FilteredItemHandler<T extends SlottedStorage<ItemVariant>> extends FilteringStorage<ItemVariant> implements SlottedStorage<ItemVariant> {
	protected final List<FilterLogic> inputFilters;
	private final List<FilterLogic> outputFilters;

	public FilteredItemHandler(T inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		super(inventoryHandler);

		this.inputFilters = inputFilters;
		this.outputFilters = outputFilters;
	}

	@Override
	protected boolean canInsert(ItemVariant resource) {
		if (inputFilters.isEmpty()) {
			return true;
		}

		ItemStack stack = resource.toStack();
		for (FilterLogic filter : inputFilters) {
			if (filter.matchesFilter(stack)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean canExtract(ItemVariant resource) {
		if (outputFilters.isEmpty()) {
			return true;
		}

		ItemStack stack = resource.toStack();
		for (FilterLogic filter : outputFilters) {
			if (filter.matchesFilter(stack)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int getSlotCount() {
		return ((SlottedStorage<ItemVariant>) backingStorage.get()).getSlotCount();
	}

	@Override
	public SingleSlotStorage<ItemVariant> getSlot(int slot) {
		return new FilteredSingleSlotStorage(((SlottedStorage<ItemVariant>) backingStorage.get()).getSlot(slot));
	}

	public static class Modifiable extends FilteredItemHandler<ITrackedContentsItemHandler> implements ITrackedContentsItemHandler {
		public Modifiable(ITrackedContentsItemHandler inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
			super(inventoryHandler, inputFilters, outputFilters);
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			throw new NotImplementedException();
		}

		@Override
		public void setStackInSlot(int slot, @NotNull ItemStack stack) {
			throw new NotImplementedException();
		}


		@Override
		public Set<ItemStackKey> getTrackedStacks() {
			Set<ItemStackKey> ret = new HashSet<>();

			((ITrackedContentsItemHandler) backingStorage).getTrackedStacks().forEach(ts -> {
				if (inputFiltersMatchStack(ts.stack())) {
					ret.add(ts);
				}
			});

			return ret;
		}

		private boolean inputFiltersMatchStack(ItemStack stack) {
			for (FilterLogic filter : inputFilters) {
				if (filter.matchesFilter(stack)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
			((ITrackedContentsItemHandler) backingStorage.get()).registerTrackingListeners(
					isk -> {
						if (inputFiltersMatchStack(isk.stack())) {
							onAddStackKey.accept(isk);
						}
					},
					isk -> {
						if (inputFiltersMatchStack(isk.stack())) {
							onRemoveStackKey.accept(isk);
						}
					},
					onAddFirstEmptySlot,
					onRemoveLastEmptySlot
			);
		}

		@Override
		public void unregisterStackKeyListeners() {
			((ITrackedContentsItemHandler) backingStorage.get()).unregisterStackKeyListeners();
		}

		@Override
		public boolean hasEmptySlots() {
			return ((ITrackedContentsItemHandler) backingStorage.get()).hasEmptySlots();
		}

		@Override
		public int getInternalSlotLimit(int slot) {
			return ((ITrackedContentsItemHandler) backingStorage.get()).getInternalSlotLimit(slot);
		}

		@Override
		public int getSlotLimit(int slot) {
			return ((ITrackedContentsItemHandler) backingStorage.get()).getSlotLimit(slot);
		}
	}

	public class FilteredSingleSlotStorage implements SingleSlotStorage<ItemVariant> {
		private final SingleSlotStorage<ItemVariant> backingSlot;
		public FilteredSingleSlotStorage(SingleSlotStorage<ItemVariant> backingSlot) {
			this.backingSlot = backingSlot;
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (canInsert(resource)) {
				return backingSlot.insert(resource, maxAmount, transaction);
			} else {
				return 0;
			}
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (canExtract(resource)) {
				return backingSlot.extract(resource, maxAmount, transaction);
			} else {
				return 0;
			}
		}

		@Override
		public boolean isResourceBlank() {
			return backingSlot.isResourceBlank();
		}

		@Override
		public ItemVariant getResource() {
			return backingSlot.getResource();
		}

		@Override
		public long getAmount() {
			return backingSlot.getAmount();
		}

		@Override
		public long getCapacity() {
			return 1; // backingSlot.getSlotCount()
		}
	}
}
