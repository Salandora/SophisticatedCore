package net.p3pp3rf1y.sophisticatedcore.upgrades.battery;

import com.github.salandora.sophisticatedlibrary.transfer.api.v1.ComponentItemHandler;
import com.github.salandora.sophisticatedlibrary.transfer.api.v1.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedBatteryUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleSidedEnergyContainer;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class BatteryUpgradeWrapper extends UpgradeWrapperBase<BatteryUpgradeWrapper, BatteryUpgradeItem>
		implements IRenderedBatteryUpgrade, EnergyStorage, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private Consumer<BatteryRenderInfo> updateTankRenderInfoCallback;
	private final BatteryComponentItemHandler inventory;
	private final SimpleSidedEnergyContainer energyStorage;

	protected BatteryUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new BatteryComponentItemHandler(upgrade);
		energyStorage = new SimpleSidedEnergyContainer() {
			@Override
			protected void onFinalCommit() {
				serializeEnergyStored();
			}

			@Override
			public long getCapacity() {
				return BatteryUpgradeWrapper.this.getCapacity();
			}

			@Override
			public long getMaxInsert(@Nullable Direction side) {
				return BatteryUpgradeWrapper.this.getMaxInOut();
			}

			@Override
			public long getMaxExtract(@Nullable Direction side) {
				return BatteryUpgradeWrapper.this.getMaxInOut();
			}
		};
		energyStorage.amount = getEnergyStored(upgrade);
	}

	public static long getEnergyStored(ItemStack upgrade) {
		return upgrade.sophisticatedLibrary_getOrDefault(ModCoreDataComponents.ENERGY_STORED, 0L);
	}

	public EnergyStorage getSideEnergyStorage(@Nullable Direction side) {
		return energyStorage.getSideStorage(side);
	}

	@Override
	public long insert(long maxAmount, TransactionContext ctx) {
		// This is handled through the SimpleSidedEnergyContainer for us
		//long ret = Math.min(getCapacity() - getAmount(), Math.min(getMaxInOut(), maxAmount));
		return getSideEnergyStorage(null).insert(maxAmount, ctx);
	}

	/*@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		return innerReceiveEnergy(maxReceive, simulate);
	}

	private int innerReceiveEnergy(int maxReceive, boolean simulate) {
		int ret = Math.min(getMaxEnergyStored() - energyStored, Math.min(getMaxInOut(), maxReceive));
		if (!simulate) {
			energyStored += ret;
			serializeEnergyStored();
		}
		return ret;
	}*/

	private void serializeEnergyStored() {
		upgrade.sophisticatedLibrary_set(ModCoreDataComponents.ENERGY_STORED, energyStorage.amount);
		save();
		forceUpdateBatteryRenderInfo();
	}

	@Override
	public long extract(long maxAmount, TransactionContext ctx) {
		// This is handled through the SimpleSidedEnergyContainer for us
		//long ret = Math.min(getAmount(), Math.min(getMaxInOut(), maxAmount));
		return getSideEnergyStorage(null).extract(maxAmount, ctx);
	}

	/*@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		return innerExtractEnergy(maxExtract, simulate);
	}

	private int innerExtractEnergy(int maxExtract, boolean simulate) {
		int ret = Math.min(energyStored, Math.min(getMaxInOut(), maxExtract));

		if (!simulate) {
			energyStored -= ret;
			serializeEnergyStored();
		}
		return ret;
	}*/

	@Override
	public long getAmount() {
		return energyStorage.amount;
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
		double stackMultiplier = upgradeItem.getAdjustedStackMultiplier(storageWrapper);
		int baseInOut = upgradeItem.getBatteryUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows();
		return stackMultiplier > Integer.MAX_VALUE / baseInOut ? Integer.MAX_VALUE : (int) (baseInOut * stackMultiplier);
	}

	private boolean isValidEnergyItem(ItemStack stack, boolean isOutput) {
		EnergyStorage energyStorage = ContainerItemContext.withConstant(stack).find(EnergyStorage.ITEM);

		if (energyStorage == null) {
			return false;
		}

		if (isOutput) {
			return energyStorage.supportsInsertion();
		} else {
			return energyStorage.supportsExtraction() && energyStorage.getAmount() > 0;
		}
	}

	@Override
	public void setBatteryRenderInfoUpdateCallback(Consumer<BatteryRenderInfo> updateTankRenderInfoCallback) {
		this.updateTankRenderInfoCallback = updateTankRenderInfoCallback;
	}

	@Override
	public void forceUpdateBatteryRenderInfo() {
		BatteryRenderInfo batteryRenderInfo = new BatteryRenderInfo(1f);
		batteryRenderInfo.setChargeRatio((float) getAmount() / getCapacity());
		updateTankRenderInfoCallback.accept(batteryRenderInfo);
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (getAmount() < getCapacity()) {
			EnergyStackWrapper energyContainer = new EnergyStackWrapper(INPUT_SLOT);
			EnergyStorage energyStorage = ContainerItemContext.ofSingleSlot(energyContainer).find(EnergyStorage.ITEM);
			if (energyStorage != null) {
				receiveFromStorage(energyContainer, energyStorage);

			}
		}

		if (getAmount() > 0) {
			EnergyStackWrapper energyContainer = new EnergyStackWrapper(OUTPUT_SLOT);
			EnergyStorage energyStorage = ContainerItemContext.ofSingleSlot(energyContainer).find(EnergyStorage.ITEM);
			if (energyStorage != null) {
				extractToStorage(energyContainer, energyStorage);
			}

			// TeamReborns energy system is push based so we need to add this code here
			for (Direction side : Direction.values()) {
				EnergyStorageUtil.move(
						getSideEnergyStorage(side),
						EnergyStorage.SIDED.find(level, pos.relative(side), side.getOpposite()),
						Long.MAX_VALUE,
						null
				);
			}
		}
	}

	private void extractToStorage(EnergyStackWrapper energyContainer, EnergyStorage energyStorage) {
		EnergyStorageUtil.move(
				getSideEnergyStorage(null),
				energyStorage,
				Long.MAX_VALUE,
				null
		);
	}

	/*private void extractToStorage(ItemStack energyContainer, IEnergyStorage energyStorage) {
		int toExtract = innerExtractEnergy(getMaxInOut(), true);
		if (toExtract > 0) {
			toExtract = energyStorage.receiveEnergy(toExtract, true);
			if (toExtract > 0) {
				energyStorage.receiveEnergy(toExtract, false);
				innerExtractEnergy(toExtract, false);
				inventory.setStackInSlotWithoutValidation(OUTPUT_SLOT, energyContainer);
			}
		}
	}*/

	private void receiveFromStorage(EnergyStackWrapper energyContainer, EnergyStorage energyStorage) {
		EnergyStorageUtil.move(
				energyStorage,
				getSideEnergyStorage(null),
				Long.MAX_VALUE,
				null
		);
	}

	/*private void receiveFromStorage(ItemStack energyContainer, IEnergyStorage energyStorage) {
		int toReceive = innerReceiveEnergy(getMaxInOut(), true);
		if (toReceive > 0) {
			toReceive = energyStorage.extractEnergy(toReceive, true);
			if (toReceive > 0) {
				energyStorage.extractEnergy(toReceive, false);
				innerReceiveEnergy(toReceive, false);
				inventory.setStackInSlotWithoutValidation(INPUT_SLOT, energyContainer);
			}
		}
	}*/

	public IItemHandler getInventory() {
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

	private class BatteryComponentItemHandler extends ComponentItemHandler {
		public BatteryComponentItemHandler(ItemStack upgrade) {
			super(upgrade, DataComponents.CONTAINER, 2);
		}

		@Override
		protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
			super.onContentsChanged(slot, oldStack, newStack);
			save();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot == INPUT_SLOT) {
				return stack.isEmpty() || isValidInputItem(stack);
			} else if (slot == OUTPUT_SLOT) {
				return stack.isEmpty() || isValidOutputItem(stack);
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

		public void setStackInSlotWithoutValidation(int slot, ItemStack stack) {
			super.updateContents(getContents(), stack, slot);
		}
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
