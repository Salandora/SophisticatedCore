package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.FilteredItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class TransferItemsMessage extends SimplePacketBase {
	private final boolean transferToInventory;
	private final boolean filterByContents;

	public TransferItemsMessage(boolean transferToInventory, boolean filterByContents) {
		this.transferToInventory = transferToInventory;
		this.filterByContents = filterByContents;
	}

	public TransferItemsMessage(FriendlyByteBuf packetBuffer) {
		this(packetBuffer.readBoolean(), packetBuffer.readBoolean());
	}

	public void write(FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBoolean(this.transferToInventory);
		packetBuffer.writeBoolean(this.filterByContents);
	}

	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu)) {
				return;
			}
			IStorageWrapper storageWrapper = storageMenu.getStorageWrapper();
			if (this.transferToInventory) {
				if (this.filterByContents) {
					mergeToPlayersInventoryFiltered(player, storageWrapper);
				} else {
					mergeToPlayersInventory(storageWrapper, player);
				}
			} else {
				InventoryHelper.transfer(new PlayerMainInvWithoutHotbarWrapper(player.getInventory()), new FilteredStorageItemHandler(storageWrapper, this.filterByContents), s -> {}, null);
			}
		});
		return true;
	}

	private static void mergeToPlayersInventory(IStorageWrapper storageWrapper, Player player) {
		InventoryHelper.iterate(storageWrapper.getInventoryHandler(), (slot, stack) -> {
			if (stack.isEmpty()) {
				return;
			}

			ItemStack result = InventoryHelper.mergeIntoPlayerInventory(player, stack, 9);
			if (result.getCount() != stack.getCount()) {
				storageWrapper.getInventoryHandler().setStackInSlot(slot, result);
			}
		});
	}

	private static void mergeToPlayersInventoryFiltered(Player player, IStorageWrapper storageWrapper) {
		Set<ItemStackKey> uniqueStacks = InventoryHelper.getUniqueStacks(new PlayerMainInvWrapper(player.getInventory()));
		InventoryHelper.iterate(storageWrapper.getInventoryHandler(), (slot, stack) -> {
			if (stack.isEmpty() || !uniqueStacks.contains(ItemStackKey.of(stack))) {
				return;
			}
			ItemStack result = InventoryHelper.mergeIntoPlayerInventory(player, stack, 0);
			if (result.getCount() != stack.getCount()) {
				storageWrapper.getInventoryHandler().setStackInSlot(slot, result);
			}
		});
	}

	public boolean transferToInventory() {
		return transferToInventory;
	}

	public boolean filterByContents() {
		return filterByContents;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (TransferItemsMessage) obj;
		return this.transferToInventory == that.transferToInventory &&
				this.filterByContents == that.filterByContents;
	}

	@Override
	public int hashCode() {
		return Objects.hash(transferToInventory, filterByContents);
	}

	@Override
	public String toString() {
		return "TransferItemsMessage[" +
				"transferToInventory=" + transferToInventory + ", " +
				"filterByContents=" + filterByContents + ']';
	}


	private static class PlayerMainInvWithoutHotbarWrapper extends RangedWrapper {
		private final Inventory inventoryPlayer;

		public PlayerMainInvWithoutHotbarWrapper(Inventory inv) {
			super(inv, 9, inv.items.size());
			this.inventoryPlayer = inv;
		}

		@Override
		public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
			long inserted = super.insertSlot(slot, resource, maxAmount, transaction);
			if (inserted != maxAmount) {
				ItemStack inSlot = this.getStackInSlot(slot);
				if (!inSlot.isEmpty()) {
					if (this.getInventoryPlayer().player.level().isClientSide) {
						inSlot.setPopTime(5);
					} else if (this.getInventoryPlayer().player instanceof ServerPlayer) {
						this.getInventoryPlayer().player.containerMenu.broadcastChanges();
					}
				}
			}

			return 0;
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			long inserted = super.insert(resource, maxAmount, transaction);
			if (inserted != maxAmount) {
				if (this.getInventoryPlayer().player.level().isClientSide) {
					// resource.toStack((int) (maxAmount - inserted)).setPopTime(5);
				} else if (this.getInventoryPlayer().player instanceof ServerPlayer) {
					this.getInventoryPlayer().player.containerMenu.broadcastChanges();
				}
			}

			return inserted;
		}

		public Inventory getInventoryPlayer() {
			return this.inventoryPlayer;
		}
	}

	private static class FilteredStorageItemHandler extends TransferItemsMessage.FilteredItemHandler<ITrackedContentsItemHandler> implements IItemHandlerSimpleInserter {
		private final IStorageWrapper storageWrapper;

		public FilteredStorageItemHandler(IStorageWrapper storageWrapper, boolean smart) {
			super(storageWrapper.getInventoryHandler(), smart);
			this.storageWrapper = storageWrapper;
		}

		@Override
		protected Set<ItemStackKey> getUniqueStacks(ITrackedContentsItemHandler itemHandler) {
			return itemHandler.getTrackedStacks();
		}

		@Override
		protected boolean matchesFilter(ItemStack stack) {
			return super.matchesFilter(stack) || storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(stack);
		}

		@Override
		public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (!matchContents || matchesFilter(resource.toStack((int) maxAmount))) {
				return itemHandler.insert(resource, maxAmount, transaction);
			} else {
				return 0;
			}
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			itemHandler.setStackInSlot(slot, stack);
		}
	}

	private static class FilteredItemHandler<T extends IItemHandlerSimpleInserter> implements IItemHandlerSimpleInserter {
		protected final T itemHandler;
		protected final boolean matchContents;
		private final Set<ItemStackKey> uniqueStacks;

		public FilteredItemHandler(T itemHandler, boolean matchContents) {
			this.itemHandler = itemHandler;
			this.matchContents = matchContents;
			uniqueStacks = getUniqueStacks(itemHandler);
		}

		protected Set<ItemStackKey> getUniqueStacks(T itemHandler) {
			return InventoryHelper.getUniqueStacks(itemHandler);
		}

		@Override
		public int getSlotCount() {
			return itemHandler.getSlotCount();
		}

		@Override
		public SingleSlotStorage<ItemVariant> getSlot(int slot) {
			return itemHandler.getSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return itemHandler.getStackInSlot(slot);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			// noop
		}

		@Override
		public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (!matchContents || matchesFilter(resource.toStack((int) maxAmount))) {
				return itemHandler.insertSlot(slot, resource, maxAmount, transaction);
			} else {
				return 0;
			}
		}

		protected boolean matchesFilter(ItemStack stack) {
			return uniqueStacks.contains(ItemStackKey.of(stack));
		}

		@Override
		public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
			return itemHandler.extractSlot(slot, resource, maxAmount, transaction);
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemVariant resource, int count) {
			return itemHandler.isItemValid(slot, resource, count);
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			if (!matchContents || matchesFilter(resource.toStack((int) maxAmount))) {
				return itemHandler.insert(resource, maxAmount, transaction);
			} else {
				return 0;
			}
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			return itemHandler.extract(resource, maxAmount, transaction);
		}
	}

	private static class PlayerMainInvWrapper extends RangedWrapper {
		private final Inventory inventoryPlayer;

		public PlayerMainInvWrapper(Inventory inv) {
			super(inv, 0, inv.items.size());
			this.inventoryPlayer = inv;
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			long inserted = super.insert(resource, maxAmount, transaction);
			if (inserted != maxAmount) {
				if (this.getInventoryPlayer().player.level().isClientSide) {
					// resource.toStack((int) (maxAmount - inserted)).setPopTime(5);
				} else if (this.getInventoryPlayer().player instanceof ServerPlayer) {
					this.getInventoryPlayer().player.containerMenu.broadcastChanges();
				}
			}

			return inserted;
		}

		public Inventory getInventoryPlayer() {
			return this.inventoryPlayer;
		}
	}

	private static class RangedWrapper implements IItemHandlerSimpleInserter {
		private final InventoryStorageImpl inventoryStorage;

		public RangedWrapper(Inventory inv, int start, int end) {
			this.inventoryStorage = (InventoryStorageImpl) InventoryStorage.of(inv, null);
			this.inventoryStorage.parts = Collections.unmodifiableList(inventoryStorage.parts.subList(start, end));
		}

		@Override
		public int getSlotCount() {
			return inventoryStorage.getSlotCount();
		}

		@Override
		public SingleSlotStorage<ItemVariant> getSlot(int slot) {
			return inventoryStorage.getSlot(slot);
		}

		@Override
		public List<SingleSlotStorage<ItemVariant>> getSlots() {
			return inventoryStorage.parts;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			var s = getSlot(slot);
			return s.getResource().toStack((int) s.getAmount());
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			// noop
		}

		@Override
		public int getSlotLimit(int slot) {
			// noop
			return 0;
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			return inventoryStorage.insert(resource, maxAmount, transaction);
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			return inventoryStorage.extract(resource, maxAmount, transaction);
		}
	}
}
