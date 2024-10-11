package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.SlotAccessor;

import java.util.List;

public class TankUpgradeTab extends UpgradeSettingsTab<TankUpgradeContainer> {
	public TankUpgradeTab(TankUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
		super(upgradeContainer, position, screen, TranslationHelper.INSTANCE.translUpgrade("tank"), TranslationHelper.INSTANCE.translUpgradeTooltip("tank"));
		openTabDimension = new Dimension(48, 48);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, minecraft, mouseX, mouseY);
		if (getContainer().isOpen()) {
			GuiHelper.renderSlotsBackground(guiGraphics, x + 3, y + 24, 1, 1);
			GuiHelper.renderSlotsBackground(guiGraphics, x + 24, y + 24, 1, 1);
		}
	}

	@Override
	protected void moveSlotsToTab() {
		List<Slot> slots = getContainer().getSlots();
		positionSlot(slots.get(TankUpgradeWrapper.INPUT_SLOT), ((AbstractContainerScreenAccessor) screen).getGuiLeft(), ((AbstractContainerScreenAccessor) screen).getGuiTop(), 4);
		positionSlot(slots.get(TankUpgradeWrapper.OUTPUT_SLOT), ((AbstractContainerScreenAccessor) screen).getGuiLeft(), ((AbstractContainerScreenAccessor) screen).getGuiTop(), 25);
	}

	private void positionSlot(Slot slot, int screenGuiLeft, int screenGuiTop, int xOffset) {
		((SlotAccessor) slot).setX(x - screenGuiLeft + xOffset);
		((SlotAccessor) slot).setY(y - screenGuiTop + 25);
	}
}
