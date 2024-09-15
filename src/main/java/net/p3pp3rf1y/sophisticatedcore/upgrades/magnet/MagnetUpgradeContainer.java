package net.p3pp3rf1y.sophisticatedcore.upgrades.magnet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogicContainer;

public class MagnetUpgradeContainer extends UpgradeContainerBase<MagnetUpgradeWrapper, MagnetUpgradeContainer> {
	private static final String DATA_PICKUP_ITEMS = "pickupItems";
	private static final String DATA_PICKUP_XP = "pickupXp";

	private final ContentsFilterLogicContainer filterLogicContainer;

	public MagnetUpgradeContainer(Player player, int containerId, MagnetUpgradeWrapper wrapper, UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> type) {
		super(player, containerId, wrapper, type);

		filterLogicContainer = new ContentsFilterLogicContainer(() -> upgradeWrapper.getFilterLogic(), this, slots::add);
	}

	@Override
	public void handlePacket(CompoundTag data) {
		if (data.contains(DATA_PICKUP_ITEMS)) {
			setPickupItems(data.getBoolean(DATA_PICKUP_ITEMS));
		} else if (data.contains(DATA_PICKUP_XP)) {
			setPickupXp(data.getBoolean(DATA_PICKUP_XP));
		}
		filterLogicContainer.handlePacket(data);
	}

	public ContentsFilterLogicContainer getFilterLogicContainer() {
		return filterLogicContainer;
	}

	public void setPickupItems(boolean pickupItems) {
		upgradeWrapper.setPickupItems(pickupItems);
		sendBooleanToServer(DATA_PICKUP_ITEMS, pickupItems);
	}

	public boolean shouldPickupItems() {
		return upgradeWrapper.shouldPickupItems();
	}

	public void setPickupXp(boolean pickupXp) {
		upgradeWrapper.setPickupXp(pickupXp);
		sendBooleanToServer(DATA_PICKUP_XP, pickupXp);
	}

	public boolean shouldPickupXp() {
		return upgradeWrapper.shouldPickupXp();
	}
}
