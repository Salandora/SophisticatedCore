package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Iterator;
import java.util.List;

public class PlayerInventoryStorageWrapper implements IInventoryHandlerHelper {
	public static PlayerInventoryStorageWrapper of(Player player) {
		return new PlayerInventoryStorageWrapper(PlayerInventoryStorage.of(player.getInventory()));
	}

	public static PlayerInventoryStorageWrapper of(Inventory playerInventory) {
		return new PlayerInventoryStorageWrapper(PlayerInventoryStorage.of(playerInventory));
	}

	private PlayerInventoryStorage wrapped;

	private PlayerInventoryStorageWrapper(PlayerInventoryStorage wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public @UnmodifiableView List<SingleSlotStorage<ItemVariant>> getSlots() {
		return List.of();
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

	public long offer(ItemVariant variant, long maxAmount, TransactionContext transaction) {
		return wrapped.offer(variant, maxAmount, transaction);
	}

	public void drop(ItemVariant variant, long amount, boolean throwRandomly, boolean retainOwnership, TransactionContext transaction) {
		wrapped.drop(variant, amount, throwRandomly, retainOwnership, transaction);
	}

	public SingleSlotStorage<ItemVariant> getHandSlot(InteractionHand hand) {
		return wrapped.getHandSlot(hand);
	}

	@Override
	public Iterator<StorageView<ItemVariant>> iterator() {
		return wrapped.iterator();
	}
}
