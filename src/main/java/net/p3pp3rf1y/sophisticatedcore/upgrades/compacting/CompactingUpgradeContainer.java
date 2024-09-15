package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class CompactingUpgradeContainer extends UpgradeContainerBase<CompactingUpgradeWrapper, CompactingUpgradeContainer> {
	private static final String DATA_SHOULD_WORKD_IN_GUI = "shouldWorkdInGUI";
	private final FilterLogicContainer<FilterLogic> filterLogicContainer;
	private static final String DATA_SHOULD_COMPACT_NON_UNCRAFTABLE = "shouldCompactNonUncraftable";

	public CompactingUpgradeContainer(Player player, int containerId, CompactingUpgradeWrapper wrapper, UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(upgradeWrapper::getFilterLogic, this, slots::add);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setCompactNonUncraftable(boolean shouldCompactNonUncraftable) {
		upgradeWrapper.setCompactNonUncraftable(shouldCompactNonUncraftable);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHOULD_COMPACT_NON_UNCRAFTABLE, shouldCompactNonUncraftable));
	}

	public boolean shouldCompactNonUncraftable() {
		return upgradeWrapper.shouldCompactNonUncraftable();
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_SHOULD_COMPACT_NON_UNCRAFTABLE)) {
			setCompactNonUncraftable(data.getBoolean(DATA_SHOULD_COMPACT_NON_UNCRAFTABLE));
		} else if (data.contains(DATA_SHOULD_WORKD_IN_GUI)) {
			setShouldWorkdInGUI(data.getBoolean(DATA_SHOULD_WORKD_IN_GUI));
		} else {
			filterLogicContainer.handlePacket(data);
		}
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		upgradeWrapper.setShouldWorkdInGUI(shouldWorkdInGUI);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHOULD_WORKD_IN_GUI, shouldWorkdInGUI));
	}

	public boolean shouldWorkInGUI() {
		return upgradeWrapper.shouldWorkInGUI();
	}
}
