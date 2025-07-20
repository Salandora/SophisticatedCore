package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.github.salandora.sophisticatedlibrary.transfer.FabricStorageWrapper;
import com.github.salandora.sophisticatedlibrary.transfer.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;

import java.util.List;

public class FabricFilteredItemHandler<T extends IItemHandler> extends FilteringStorage<ItemVariant> {
	protected final List<FilterLogic> inputFilters;
	protected final List<FilterLogic> outputFilters;

	public FabricFilteredItemHandler(Storage<ItemVariant> inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		super(inventoryHandler);

		this.inputFilters = inputFilters;
		this.outputFilters = outputFilters;

	}

	public FabricFilteredItemHandler(T inventoryHandler, List<FilterLogic> inputFilters, List<FilterLogic> outputFilters) {
		this(FabricStorageWrapper.of(inventoryHandler), inputFilters, outputFilters);
	}

	public ItemStack getStackInSlot(int slot) {
		return ((ITrackedContentsItemHandler) backingStorage.get()).getStackInSlot(slot);
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
}
