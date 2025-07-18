package net.p3pp3rf1y.sophisticatedcore.inventory;

import java.util.List;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

// TODO: Revisit
public class SlottedStackStorageContainerItemContext implements ContainerItemContext {
	public static SlottedStackStorageContainerItemContext of(SlottedStackStorage storage, int slot) {
		return new SlottedStackStorageContainerItemContext(storage, slot);
	}

	private final SlottedStackStorage wrappedStorage;
	private final int slot;

	public SlottedStackStorageContainerItemContext(SlottedStackStorage storage, int slot) {
		this.wrappedStorage = storage;
		this.slot = slot;
	}

	@Override
	public <A> @Nullable A find(ItemApiLookup<A, ContainerItemContext> lookup) {
		return this.getItemVariant().isBlank() ? null : lookup.find(this.wrappedStorage.getStackInSlot(this.slot), this);
	}

	@Override
	public SingleSlotStorage<ItemVariant> getMainSlot() {
		return this.wrappedStorage.getSlot(slot);
	}

	@Override
	public long insertOverflow(ItemVariant itemVariant, long maxAmount, TransactionContext transactionContext) {
		return this.wrappedStorage.insert(itemVariant, maxAmount, transactionContext);
	}

	@Override
	public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
		return this.wrappedStorage.getSlots();
	}
}
