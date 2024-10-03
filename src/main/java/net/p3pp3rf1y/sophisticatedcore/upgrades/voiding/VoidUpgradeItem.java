package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.minecraft.world.item.CreativeModeTab;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

public class VoidUpgradeItem extends UpgradeItemBase<VoidUpgradeWrapper> {
	public static final UpgradeType<VoidUpgradeWrapper> TYPE = new UpgradeType<>(VoidUpgradeWrapper::new);
	private final VoidUpgradeConfig voidUpgradeConfig;

	public VoidUpgradeItem(VoidUpgradeConfig voidUpgradeConfig, CreativeModeTab itemGroup, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(itemGroup, upgradeTypeLimitConfig);
		this.voidUpgradeConfig = voidUpgradeConfig;
	}

	@Override
	public UpgradeType<VoidUpgradeWrapper> getType() {
		return TYPE;
	}

	public int getFilterSlotCount() {
		return voidUpgradeConfig.filterSlots.get();
	}

	public boolean isVoidAnythingEnabled() {
		return voidUpgradeConfig.voidAnythingEnabled.get();
	}
}
