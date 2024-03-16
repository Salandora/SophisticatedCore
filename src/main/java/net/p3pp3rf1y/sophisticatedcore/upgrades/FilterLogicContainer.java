package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.porting_lib.transfer.items.ItemStackHandler;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.common.gui.FilterSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FilterLogicContainer<T extends FilterLogic> extends FilterLogicContainerBase<T, FilterLogicContainer.FilterLogicSlot> {
	public FilterLogicContainer(Supplier<T> filterLogic, IServerUpdater serverUpdater, Consumer<Slot> addSlot) {
		super(serverUpdater, filterLogic, addSlot);
		ItemStackHandler filterHandler = filterLogic.get().getFilterHandler();
		for (int slot = 0; slot < filterHandler.getSlotCount(); slot++) {
			FilterLogicSlot filterSlot = new FilterLogicSlot(() -> filterLogic.get().getFilterHandler(), slot);
			addSlot.accept(filterSlot);
			filterSlots.add(filterSlot);
		}
	}

	public static class FilterLogicSlot extends FilterSlotItemHandler {
		private boolean enabled = true;

		public FilterLogicSlot(Supplier<SlottedStackStorage> filterHandler, Integer slot) {
			super(filterHandler, slot, -100, -100);
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@Override
		public boolean isActive() {
			return enabled;
		}
	}
}
