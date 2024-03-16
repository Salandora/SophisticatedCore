package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyItem;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.porting_lib.transfer.items.ItemStackHandler;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.function.Consumer;
import javax.annotation.Nullable;

public class BatteryUpgradeWrapper extends UpgradeWrapperBase<BatteryUpgradeWrapper, BatteryUpgradeItem>
		implements IRenderedBatteryUpgrade, EnergyStorage, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	public static final String ENERGY_STORED_TAG = SimpleEnergyItem.ENERGY_KEY;
	private Consumer<BatteryRenderInfo> updateTankRenderInfoCallback;
	private final ItemStackHandler inventory;
	private final BatteryUpgradeEnergyStorage energyStorage;

	protected BatteryUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new ItemStackHandler(2) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				upgrade.addTagElement("inventory", serializeNBT());
				save();
			}

			@Override
			public boolean isItemValid(int slot, ItemVariant resource) {
				if (slot == INPUT_SLOT) {
					return isValidInputItem(resource.toStack());
				} else if (slot == OUTPUT_SLOT) {
					return isValidOutputItem(resource.toStack());
				}
				return false;
			}

			private boolean isValidInputItem(ItemStack stack) {
				return isValidEnergyItem(stack, false);
			}

			private boolean isValidOutputItem(ItemStack stack) {
				return isValidEnergyItem(stack, true);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}
		};
		NBTHelper.getCompound(upgrade, "inventory").ifPresent(inventory::deserializeNBT);
		energyStorage = new BatteryUpgradeEnergyStorage(getEnergyStored(upgrade)) {
			@Override
			protected void onFinalCommit() {
				serializeEnergyStored();
			}
		};
	}

	public static long getEnergyStored(ItemStack upgrade) {
		return NBTHelper.getLong(upgrade, ENERGY_STORED_TAG).orElse(0L);
	}

	@Override
	public long insert(long maxAmount, @Nullable TransactionContext ctx) {
		try (Transaction nested = Transaction.openNested(ctx)) {
			long ret = Math.min(getCapacity() - getAmount(), Math.min(getMaxInOut(), maxAmount));
			return energyStorage.insert(ret, nested);
		}
	}

	private void serializeEnergyStored() {
		NBTHelper.setLong(upgrade, ENERGY_STORED_TAG, energyStorage.amount);
		save();
		forceUpdateBatteryRenderInfo();
	}

	@Override
	public long extract(long maxAmount, @Nullable TransactionContext ctx) {
		try (Transaction nested = Transaction.openNested(ctx)) {
			long ret = Math.min(getAmount(), Math.min(getMaxInOut(), maxAmount));
			return energyStorage.extract(ret, nested);
		}
	}

	@Override
	public long getAmount() {
		return energyStorage.getAmount();
	}

	@Override
	public long getCapacity() {
		return upgradeItem.getMaxEnergyStored(storageWrapper);
	}

	@Override
	public boolean supportsExtraction() {
		return true;
	}

	@Override
	public boolean supportsInsertion() {
		return true;
	}

	private int getMaxInOut() {
		int stackMultiplier = upgradeItem.getAdjustedStackMultiplier(storageWrapper);
		int baseInOut = upgradeItem.getBatteryUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows();
		return stackMultiplier > Integer.MAX_VALUE / baseInOut ? Integer.MAX_VALUE : baseInOut * stackMultiplier;
	}

	private boolean isValidEnergyItem(ItemStack stack, boolean isOutput) {
		return isOutput || EnergyStorageUtil.isEnergyStorage(stack);
	}

	@Override
	public void setBatteryRenderInfoUpdateCallback(Consumer<BatteryRenderInfo> updateTankRenderInfoCallback) {
		this.updateTankRenderInfoCallback = updateTankRenderInfoCallback;
	}

	@Override
	public void forceUpdateBatteryRenderInfo() {
		BatteryRenderInfo batteryRenderInfo = new BatteryRenderInfo(1f);
		batteryRenderInfo.setChargeRatio((float) Math.round((float) getAmount() / getCapacity() * 4) / 4);
		updateTankRenderInfoCallback.accept(batteryRenderInfo);
	}

	public void tick(@Nullable LivingEntity entity, Level world, BlockPos pos) {
		if (getAmount() < getCapacity()) {
			EnergyStorageUtil.move(
					ContainerItemContext.ofSingleSlot(new EnergyStackWrapper(INPUT_SLOT)).find(EnergyStorage.ITEM),
					energyStorage,
					Long.MAX_VALUE,
					null
			);
		}

		if (getAmount() > 0) {
			EnergyStorageUtil.move(
					energyStorage,
					ContainerItemContext.ofSingleSlot(new EnergyStackWrapper(OUTPUT_SLOT)).find(EnergyStorage.ITEM),
					Long.MAX_VALUE,
					null
			);
		}
	}

	public SlottedStackStorage getInventory() {
		return inventory;
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) getAmount() / upgradeItem.getMaxEnergyBase(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	private class EnergyStackWrapper extends SingleStackStorage {
		private final int slot;

		public EnergyStackWrapper(int slot) {
			this.slot = slot;
		}

		@Override
		protected ItemStack getStack() {
			return inventory.getStackInSlot(slot);
		}

		@Override
		protected void setStack(ItemStack stack) {
			inventory.setStackInSlot(slot, stack);
		}
	}
}
