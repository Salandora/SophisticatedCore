package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Iterator;
import java.util.List;

public class InventoryStorageWrapper implements SlottedStackStorage {
	public static InventoryStorageWrapper of(Player player) {
		return new InventoryStorageWrapper(player.getInventory());
	}
	public static InventoryStorageWrapper of(Container container) {
		return new InventoryStorageWrapper(container);
	}

	private final InventoryStorage wrapped;
	private final Container wrappedInventory;

	private InventoryStorageWrapper(Container inventory) {
		this.wrapped = InventoryStorage.of(inventory, null);
		this.wrappedInventory = inventory;
	}

	@Override
	public @UnmodifiableView List<SingleSlotStorage<ItemVariant>> getSlots() {
		return wrapped.getSlots();
	}

	@Override
	public int getSlotCount() {
		return wrapped.getSlotCount();
	}

	@Override
	public SingleSlotStorage<ItemVariant> getSlot(int slot) {
		return wrapped.getSlot(slot);
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		return wrapped.insert(resource, maxAmount, transaction);
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		return wrapped.extract(resource, maxAmount, transaction);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return wrappedInventory.getItem(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		this.wrappedInventory.setItem(slot, stack);
	}

	@Override
	public int getSlotLimit(int slot) {
		return (int) wrapped.getSlot(slot).getCapacity();
	}

	@Override
	public Iterator<StorageView<ItemVariant>> iterator() {
		return wrapped.iterator();
	}
}
