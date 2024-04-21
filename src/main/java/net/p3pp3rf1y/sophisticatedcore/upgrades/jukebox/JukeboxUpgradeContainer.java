package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.porting_lib.transfer.items.SCSlotItemHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

public class JukeboxUpgradeContainer extends UpgradeContainerBase<JukeboxUpgradeItem.Wrapper, JukeboxUpgradeContainer> {

	private static final String ACTION_DATA = "action";

	public JukeboxUpgradeContainer(Player player, int upgradeContainerId, JukeboxUpgradeItem.Wrapper upgradeWrapper, UpgradeContainerType<JukeboxUpgradeItem.Wrapper, JukeboxUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);
		slots.add(new SCSlotItemHandler(upgradeWrapper.getDiscInventory(), 0, -100, -100) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (upgradeWrapper.isPlaying()) {
					upgradeWrapper.stop(player);
				}
			}
		});
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(ACTION_DATA)) {
			String actionName = data.getString(ACTION_DATA);
			if (actionName.equals("play")) {
				if (player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
					storageContainerMenu.getBlockPosition().ifPresentOrElse(pos -> upgradeWrapper.play(player.level(), pos), () -> upgradeWrapper.play(player));
				}
			} else if (actionName.equals("stop")) {
				upgradeWrapper.stop(player);
			}
		}
	}

	public void play() {
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_DATA, "play"));
	}

	public void stop() {
		sendDataToServer(() -> NBTHelper.putString(new CompoundTag(), ACTION_DATA, "stop"));
	}
}
