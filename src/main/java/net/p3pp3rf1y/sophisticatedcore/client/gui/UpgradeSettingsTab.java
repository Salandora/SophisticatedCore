package net.p3pp3rf1y.sophisticatedcore.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ItemButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.SlotAccessor;

public abstract class UpgradeSettingsTab<C extends UpgradeContainerBase<?, ?>> extends SettingsTabBase<StorageScreenBase<?>> {

	private final C upgradeContainer;

	protected UpgradeSettingsTab(C upgradeContainer, Position position, StorageScreenBase<?> screen, Component tabLabel, Component closedTooltip) {
		super(position, screen, tabLabel, closedTooltip,
				onTabIconClicked -> new ItemButton(new Position(position.x() + 1, position.y() + 4), onTabIconClicked, upgradeContainer.getUpgradeStack(), Component.translatable("gui.sophisticatedcore.narrate.tab_button")));
		this.upgradeContainer = upgradeContainer;
		moveSlotsOutOfView();
	}

	protected C getContainer() {
		return upgradeContainer;
	}

	protected abstract void moveSlotsToTab();

	protected void moveSlotsOutOfView() {
		getContainer().getSlots().forEach(slot -> ((SlotAccessor) slot).setX(StorageScreenBase.DISABLED_SLOT_X_POS));
	}

	@Override
	protected void onTabOpen() {
		super.onTabOpen();
		moveSlotsToTab();
	}

	@Override
	protected void onTabClose() {
		super.onTabClose();
		moveSlotsOutOfView();
	}

	@Override
	protected void setOpen(boolean isOpen) {
		upgradeContainer.setIsOpen(isOpen);
		super.setOpen(isOpen);
	}

	public void onAfterInit() {
		if (upgradeContainer.isOpen()) {
			setOpen(true);
		}
	}

	@SuppressWarnings("unused") //parameters used in overrides
	public boolean slotIsNotCoveredAt(Slot slot, double mouseX, double mouseY) {
		return true;
	}
}
