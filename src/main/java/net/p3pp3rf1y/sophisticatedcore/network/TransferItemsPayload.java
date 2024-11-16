package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.impl.transfer.item.InventoryStorageImpl;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public record TransferItemsPayload(boolean transferToInventory,
								   boolean filterByContents) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TransferItemsPayload> TYPE = new CustomPacketPayload.Type<>(SophisticatedCore.getRL("transfer_items"));
	public static final StreamCodec<ByteBuf, TransferItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			TransferItemsPayload::transferToInventory,
			ByteBufCodecs.BOOL,
			TransferItemsPayload::filterByContents,
			TransferItemsPayload::new);

	public static void handlePayload(TransferItemsPayload payload, ServerPlayNetworking.Context context) {
		Player player = context.player();

		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = storageMenu.getStorageWrapper();
		if (payload.transferToInventory) {
			if (payload.filterByContents) {
				mergeToPlayersInventoryFiltered(player, storageWrapper);
			} else {
				mergeToPlayersInventory(storageWrapper, player);
			}
		} else {
			InventoryHelper.transfer(new PlayerMainInvWithoutHotbarWrapper(player.getInventory()), new FilteredStorageItemHandler(storageWrapper, payload.filterByContents), s -> {
			});
		}
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

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private static class PlayerMainInvWithoutHotbarWrapper extends RangedWrapper {
		private final Inventory inventoryPlayer;

		public PlayerMainInvWithoutHotbarWrapper(Inventory inv) {
			super(inv, 9, inv.items.size());
			this.inventoryPlayer = inv;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			ItemStack rest = super.insertItem(slot, stack, simulate);
			if (rest.getCount() != stack.getCount()) {
				ItemStack inSlot = this.getStackInSlot(slot);
				if (!inSlot.isEmpty()) {
					if (this.getInventoryPlayer().player.level().isClientSide) {
						inSlot.setPopTime(5);
					} else if (this.getInventoryPlayer().player instanceof ServerPlayer) {
						this.getInventoryPlayer().player.containerMenu.broadcastChanges();
					}
				}
			}

			return rest;
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

	private static class FilteredStorageItemHandler extends TransferItemsPayload.FilteredItemHandler<ITrackedContentsItemHandler> implements IItemHandlerSimpleInserter {
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

		@Nonnull
		@Override
		public ItemStack insertItem(ItemStack stack, boolean simulate) {
			if (!matchContents || matchesFilter(stack)) {
				return itemHandler.insertItem(stack, simulate);
			} else {
				return stack;
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

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!matchContents || matchesFilter(stack)) {
				return itemHandler.insertItem(slot, stack, simulate);
			} else {
				return stack;
			}
		}

		protected boolean matchesFilter(ItemStack stack) {
			return uniqueStacks.contains(ItemStackKey.of(stack));
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return itemHandler.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandler.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return itemHandler.isItemValid(slot, stack);
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			// noop
			return 0;
		}

		@Override
		public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			// noop
			return 0;
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
