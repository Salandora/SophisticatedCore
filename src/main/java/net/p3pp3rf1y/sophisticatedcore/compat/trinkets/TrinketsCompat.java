package net.p3pp3rf1y.sophisticatedcore.compat.trinkets;

import dev.emi.trinkets.api.TrinketsApi;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.*;

public class TrinketsCompat implements ICompat {
	@Override
	public void setup() {
		addInventoryItemHandler();
	}

	private void addInventoryItemHandler() {
		InventoryHelper.registerPlayerInventoryProvider(player ->
				TrinketsApi.getTrinketComponent(player)
						.map(comp ->
							comp.getInventory().values().stream()
									.flatMap(group -> group.values().stream())
									.map(Wrapper::new)
									.toList()
						)
						.map(list -> (SlottedStackStorage) new CombinedWrapper<>(list))
						.orElse(null));
	}

	private static class CombinedWrapper<S extends SlottedStackStorage> extends CombinedSlottedStorage<ItemVariant, S> implements SlottedStackStorage {
		protected final int[] baseIndex;
		protected final int slotCount;

		public CombinedWrapper(List<S> inventories) {
			super(inventories);

			this.baseIndex = new int[inventories.size()];
			int index = 0;
			for (int i = 0; i < inventories.size(); i++) {
				index += inventories.get(i).getSlotCount();
				baseIndex[i] = index;
			}
			this.slotCount = index;
		}

		// returns the handler index for the slot
		protected int getIndexForSlot(int slot) {
			if (slot < 0)
				return -1;

			for (int i = 0; i < baseIndex.length; i++)
			{
				if (slot - baseIndex[i] < 0)
				{
					return i;
				}
			}
			return -1;
		}

		protected Optional<S> getHandlerFromIndex(int index) {
			if (index < 0 || index >= parts.size())
			{
				return Optional.empty();
			}
			return Optional.of(parts.get(index));
		}

		protected int getSlotFromIndex(int slot, int index) {
			if (index <= 0 || index >= baseIndex.length)
			{
				return slot;
			}
			return slot - baseIndex[index - 1];
		}

		@Override
		public int getSlotCount() {
			return slotCount;
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			int index = getIndexForSlot(slot);
			Optional<S> handler = getHandlerFromIndex(index);
			if (handler.isEmpty()) {
				return;
			}
			slot = getSlotFromIndex(slot, index);
			handler.get().setStackInSlot(slot, stack);
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			int index = getIndexForSlot(slot);
			Optional<S> handler = getHandlerFromIndex(index);
			if (handler.isEmpty()) {
				return ItemStack.EMPTY;
			}
			slot = getSlotFromIndex(slot, index);
			return handler.get().getStackInSlot(slot);
		}

		@Override
		public int getSlotLimit(int slot) {
			int index = getIndexForSlot(slot);
			Optional<S> handler = getHandlerFromIndex(index);
			if (handler.isEmpty()) {
				return 0;
			}
			int localSlot = getSlotFromIndex(slot, index);
			return handler.get().getSlotLimit(localSlot);
		}

		@Override
		public boolean isItemValid(int slot, ItemVariant resource, int count) {
			int index = getIndexForSlot(slot);
			Optional<S> handler = getHandlerFromIndex(index);
			if (handler.isEmpty()) {
				return false;
			}
			int localSlot = getSlotFromIndex(slot, index);
			return handler.get().isItemValid(localSlot, resource, count);
		}

		@Override
		public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
			int index = getIndexForSlot(slot);
			Optional<S> handler = getHandlerFromIndex(index);
			if (handler.isEmpty()) {
				return 0;
			}
			slot = getSlotFromIndex(slot, index);
			return handler.get().insertSlot(slot, resource, maxAmount, ctx);
		}

		@Override
		public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
			int index = getIndexForSlot(slot);
			Optional<S> handler = getHandlerFromIndex(index);
			if (handler.isEmpty()) {
				return 0;
			}
			slot = getSlotFromIndex(slot, index);
			return handler.get().extractSlot(slot, resource, maxAmount, ctx);
		}
	}

	private static class Wrapper extends CombinedSlottedStorage<ItemVariant, SingleSlotStorage<ItemVariant>> implements SlottedStackStorage {
		private final Container inventory;

		private final List<SingleStackStorage> backingList;

		final MarkDirtyParticipant markDirtyParticipant = new MarkDirtyParticipant();

		public Wrapper(Container inventory) {
			super(Collections.emptyList());
			this.inventory = inventory;
			this.backingList = new ArrayList<>();
			resizeSlotList();
		}

		private void resizeSlotList() {
			int inventorySize = inventory.getContainerSize();

			// If the public-facing list must change...
			if (inventorySize != parts.size()) {
				// Ensure we have enough wrappers in the backing list.
				while (backingList.size() < inventorySize) {
					backingList.add(new InventorySlotWrapper(backingList.size()));
				}

				// Update the public-facing list.
				parts = Collections.unmodifiableList(backingList.subList(0, inventorySize));
			}
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inventory.getItem(slot);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			inventory.setItem(slot, stack);
		}

		@Override
		public int getSlotLimit(int slot) {
			return (int) getSlot(slot).getCapacity();
		}

		class MarkDirtyParticipant extends SnapshotParticipant<Boolean> {
			@Override
			protected Boolean createSnapshot() {
				return Boolean.TRUE;
			}

			@Override
			protected void readSnapshot(Boolean snapshot) {
			}

			@Override
			protected void onFinalCommit() {
				inventory.setChanged();
			}
		}

		class InventorySlotWrapper extends SingleStackStorage {
			final int slot;
			private ItemStack lastReleasedSnapshot = null;

			InventorySlotWrapper(int slot) {
				this.slot = slot;
			}

			@Override
			protected ItemStack getStack() {
				return inventory.getItem(slot);
			}

			@Override
			protected void setStack(ItemStack stack) {
				inventory.setItem(slot, stack);
			}

			@Override
			public int getCapacity(ItemVariant variant) {
				return Math.min(inventory.getMaxStackSize(), variant.getItem().getMaxStackSize());
			}

			@Override
			public void updateSnapshots(TransactionContext transaction) {
				Wrapper.this.markDirtyParticipant.updateSnapshots(transaction);
				super.updateSnapshots(transaction);
			}

			@Override
			protected void releaseSnapshot(ItemStack snapshot) {
				lastReleasedSnapshot = snapshot;
			}

			@Override
			protected void onFinalCommit() {
				// Try to apply the change to the original stack
				ItemStack original = lastReleasedSnapshot;
				ItemStack currentStack = getStack();

				if (!original.isEmpty() && original.getItem() == currentStack.getItem()) {
					// None is empty and the items match: just update the amount and NBT, and reuse the original stack.
					original.setCount(currentStack.getCount());
					original.setTag(currentStack.hasTag() ? currentStack.getTag().copy() : null);
					setStack(original);
				} else {
					// Otherwise assume everything was taken from original so empty it.
					original.setCount(0);
				}
			}
		}
	}
}
