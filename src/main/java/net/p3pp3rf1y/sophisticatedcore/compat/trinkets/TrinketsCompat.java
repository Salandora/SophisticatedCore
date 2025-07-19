package net.p3pp3rf1y.sophisticatedcore.compat.trinkets;

import com.github.salandora.sophisticatedlibrary.items.EmptyItemHandler;
import com.github.salandora.sophisticatedlibrary.transfer.FabricStorageWrapper;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
						.map(list -> (SlottedStorage<ItemVariant>) new CombinedWrapper<>(list))
						.orElse(FabricStorageWrapper.of(EmptyItemHandler.INSTANCE)));
	}

	private static class CombinedWrapper<S extends SlottedStorage<ItemVariant>> extends CombinedSlottedStorage<ItemVariant, S> {
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

		@Override
		public int getSlotCount() {
			return slotCount;
		}
	}

	private static class Wrapper extends CombinedSlottedStorage<ItemVariant, SingleSlotStorage<ItemVariant>> {
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
				return Math.min(inventory.getMaxStackSize(), variant.getItem().getDefaultMaxStackSize());
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
					// Components have changed, we need to copy the stack.
					if (!Objects.equals(original.getComponentsPatch(), currentStack.getComponentsPatch())) {
						// Remove all the existing components and copy the new ones on top.
						for (DataComponentType<?> type : original.getComponents().keySet()) {
							original.set(type, null);
						}

						original.applyComponents(currentStack.getComponents());
					}

					// None is empty and the items and components match: update the amount, and reuse the original stack.
					original.setCount(currentStack.getCount());
					setStack(original);
				} else {
					// Otherwise, assume everything was taken from the original so empty it.
					original.setCount(0);
				}
			}
		}
	}
}
