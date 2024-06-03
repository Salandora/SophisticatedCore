package net.p3pp3rf1y.sophisticatedcore.util;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public class FilterItemStackHandler extends ItemStackHandler {
	private boolean onlyEmptyFilters = true;

	public FilterItemStackHandler(int size) {super(size);}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return 0;
	}

	@Override
	protected void onContentsChanged(int slot) {
		super.onContentsChanged(slot);

		updateEmptyFilters();
	}

	@Override
	protected void onLoad() {
		super.onLoad();

		updateEmptyFilters();
	}

	private void updateEmptyFilters() {
		onlyEmptyFilters = InventoryHelper.iterate(this, (s, filter) -> filter.isEmpty(), () -> true, result -> !result);
	}

	public boolean hasOnlyEmptyFilters() {
		return onlyEmptyFilters;
	}
}
