package net.p3pp3rf1y.sophisticatedcore.inventory;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Iterator;
import java.util.List;

public class PlayerInventoryStorageWrapper implements SlottedStackStorage, IInventoryHandlerHelper {
	public static PlayerInventoryStorageWrapper of(Player player) {
		return new PlayerInventoryStorageWrapper(player.getInventory());
	}

	private final PlayerInventoryStorage wrapped;
	private final Inventory wrappedInventory;

	private PlayerInventoryStorageWrapper(Inventory playerInventory) {
		this.wrapped = PlayerInventoryStorage.of(playerInventory);
		this.wrappedInventory = playerInventory;
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

	public void offerOrDrop(ItemVariant variant, long amount, TransactionContext transaction) {
		long offered = offer(variant, amount, transaction);
		drop(variant, amount - offered, transaction);
	}

	public long offer(ItemVariant variant, long maxAmount, TransactionContext transaction) {
		return wrapped.offer(variant, maxAmount, transaction);
	}

	public void drop(ItemVariant variant, long amount, boolean throwRandomly, boolean retainOwnership, TransactionContext transaction) {
		wrapped.drop(variant, amount, throwRandomly, retainOwnership, transaction);
	}

	public void drop(ItemVariant variant, long amount, boolean retainOwnership, TransactionContext transaction) {
		drop(variant, amount, false, retainOwnership, transaction);
	}

	public void drop(ItemVariant variant, long amount, TransactionContext transaction) {
		drop(variant, amount, false, transaction);
	}

	public SingleSlotStorage<ItemVariant> getHandSlot(InteractionHand hand) {
		return wrapped.getHandSlot(hand);
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
