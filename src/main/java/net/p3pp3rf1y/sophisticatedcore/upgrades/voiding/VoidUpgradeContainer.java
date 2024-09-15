package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class VoidUpgradeContainer extends UpgradeContainerBase<VoidUpgradeWrapper, VoidUpgradeContainer> {
	private static final String DATA_SHOULD_WORKD_IN_GUI = "shouldWorkdInGUI";
	private static final String DATA_SHOULD_VOID_OVERFLOW = "shouldVoidOverflow";
	private final FilterLogicContainer<FilterLogic> filterLogicContainer;

	public VoidUpgradeContainer(Player player, int containerId, VoidUpgradeWrapper wrapper, UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> type) {
		super(player, containerId, wrapper, type);
		filterLogicContainer = new FilterLogicContainer<>(upgradeWrapper::getFilterLogic, this, slots::add);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_SHOULD_WORKD_IN_GUI)) {
			setShouldWorkdInGUI(data.getBoolean(DATA_SHOULD_WORKD_IN_GUI));
		} else if (data.contains(DATA_SHOULD_VOID_OVERFLOW)) {
			setShouldVoidOverflow(data.getBoolean(DATA_SHOULD_VOID_OVERFLOW));
		}
		filterLogicContainer.handlePacket(data);
	}

	public FilterLogicContainer<FilterLogic> getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		upgradeWrapper.setShouldWorkdInGUI(shouldWorkdInGUI);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHOULD_WORKD_IN_GUI, shouldWorkdInGUI));
	}

	public void setShouldVoidOverflow(boolean shouldVoidOverflow) {
		upgradeWrapper.setShouldVoidOverflow(shouldVoidOverflow);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHOULD_VOID_OVERFLOW, shouldVoidOverflow));
	}

	public boolean shouldWorkInGUI() {
		return upgradeWrapper.shouldWorkInGUI();
	}

	public boolean shouldVoidOverflow() {
		return upgradeWrapper.shouldVoidOverflow();
	}
}
