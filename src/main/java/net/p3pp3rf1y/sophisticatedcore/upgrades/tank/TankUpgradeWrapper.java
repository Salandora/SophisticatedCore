package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import com.github.salandora.sophisticatedfabriclib.fluid.api.v1.*;
import com.github.salandora.sophisticatedfabriclib.transfer.api.v1.ComponentItemHandler;
import com.github.salandora.sophisticatedfabriclib.transfer.api.v1.IItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TankUpgradeWrapper extends UpgradeWrapperBase<TankUpgradeWrapper, TankUpgradeItem>
		implements IRenderedTankUpgrade, ITickableUpgrade, IStackableContentsUpgrade {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private Consumer<TankRenderInfo> updateTankRenderInfoCallback;
	private final TankComponentItemHandler inventory;
	private FluidStack contents;
	private long cooldownTime = 0;

	protected TankUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inventory = new TankComponentItemHandler(upgrade);
		contents = getContents(upgrade).copy();
	}

	public static SimpleFluidContent getContents(ItemStack upgrade) {
		return upgrade.sophisticatedFabricLibrary_getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
	}

	private boolean isValidFluidItem(ItemStack stack, boolean isOutput) {
		return CapabilityHelper.getFromFluidHandler(stack, fluidHandler -> isValidFluidHandler(fluidHandler, isOutput), false);
	}

	private boolean isValidFluidHandler(IFluidHandlerItem fluidHandler, boolean isOutput) {
		boolean tankEmpty = contents.isEmpty();
		for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
			FluidStack fluidInTank = fluidHandler.getFluidInTank(tank);
			if ((isOutput && (fluidInTank.isEmpty() || (!tankEmpty && FluidStack.isSameFluidSameComponents(fluidInTank, contents))))
					|| (!isOutput && !fluidInTank.isEmpty() && (tankEmpty || FluidStack.isSameFluidSameComponents(contents, fluidInTank)))
			) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setTankRenderInfoUpdateCallback(Consumer<TankRenderInfo> updateTankRenderInfoCallback) {
		this.updateTankRenderInfoCallback = updateTankRenderInfoCallback;
	}

	@Override
	public void forceUpdateTankRenderInfo() {
		TankRenderInfo renderInfo = new TankRenderInfo();
		if (!contents.isEmpty()) {
			renderInfo.setFluid(contents);
			renderInfo.setFillRatio((float) Math.round((float) contents.getAmount() / getTankCapacity() * 10) / 10);
		}
		updateTankRenderInfoCallback.accept(renderInfo);
	}

	public FluidStack getContents() {
		return contents;
	}

	// Fabric: Added for internal use to reset the content when a Transaction was cancelled
	public void setContents(FluidStack fluidStack) {
		this.contents = fluidStack;
		serializeContents();
	}

	public long getTankCapacity() {
		return upgradeItem.getTankCapacity(storageWrapper);
	}

	public IItemHandler getInventory() {
		return inventory;
	}

	private long getMaxInOut() {
		return (int) Math.max(FluidType.BUCKET_VOLUME, upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows() * upgradeItem.getAdjustedStackMultiplier(storageWrapper) * FluidUtil.BUCKET_VOLUME_IN_MILLIBUCKETS);
	}

	public long fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
		long capacity = getTankCapacity();

		if (contents.getAmount() >= capacity || (!contents.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, contents))) {
			return 0;
		}

		long toFill = Math.min(capacity - contents.getAmount(), resource.getAmount());
		if (!ignoreInOutLimit) {
			toFill = Math.min(getMaxInOut(), toFill);
		}

		if (action == IFluidHandler.FluidAction.EXECUTE) {
			if (contents.isEmpty()) {
				contents = new FluidStack(resource.getFluid(), toFill);
			} else {
				contents.setAmount(contents.getAmount() + toFill);
			}
			serializeContents();
		}

		return toFill;
	}

	private void serializeContents() {
		upgrade.sophisticatedFabricLibrary_set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(contents));
		save();
		forceUpdateTankRenderInfo();
	}

	public FluidStack drain(long maxDrain, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
		if (contents.isEmpty()) {
			return FluidStack.EMPTY;
		}

		long toDrain = Math.min(maxDrain, contents.getAmount());
		if (!ignoreInOutLimit) {
			toDrain = Math.min(getMaxInOut(), toDrain);
		}

		FluidStack ret = new FluidStack(contents.getFluid(), toDrain);
		if (action == IFluidHandler.FluidAction.EXECUTE) {
			if (toDrain == contents.getAmount()) {
				contents = FluidStack.EMPTY;
			} else {
				contents.setAmount(contents.getAmount() - toDrain);
			}
			serializeContents();
		}

		return ret;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.getGameTime() < cooldownTime) {
			return;
		}

		AtomicBoolean didSomething = new AtomicBoolean(false);
		CapabilityHelper.runOnFluidHandler(inventory.getStackInSlot(INPUT_SLOT), fluidHandler ->
				didSomething.set(drainHandler(fluidHandler, stack -> inventory.setStackInSlotWithoutValidation(INPUT_SLOT, stack)))
		);
		CapabilityHelper.runOnFluidHandler(inventory.getStackInSlot(OUTPUT_SLOT), fluidHandler ->
				didSomething.set(fillHandler(fluidHandler, stack -> inventory.setStackInSlotWithoutValidation(OUTPUT_SLOT, stack)))
		);

		if (didSomething.get()) {
			cooldownTime = level.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}

	public boolean fillHandler(IFluidHandlerItem fluidHandler, Consumer<ItemStack> updateContainerStack) {
		if (!contents.isEmpty() && isValidFluidHandler(fluidHandler, true)) {
			long filled = fluidHandler.fill(new FluidStack(contents.getFluid(), Math.min(FluidType.BUCKET_VOLUME, contents.getAmount())), IFluidHandler.FluidAction.SIMULATE);
			if (filled <= 0) { //checking for less than as well because some mods have incorrect fill logic
				return false;
			}
			FluidStack drained = drain(filled, IFluidHandler.FluidAction.EXECUTE, false);
			fluidHandler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
			updateContainerStack.accept(fluidHandler.getContainer());
			return true;
		}
		return false;
	}

	public boolean drainHandler(IFluidHandlerItem fluidHandler, Consumer<ItemStack> updateContainerStack) {
		if (isValidFluidHandler(fluidHandler, false)) {
			FluidStack extracted = contents.isEmpty() ?
					fluidHandler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE) :
					fluidHandler.drain(new FluidStack(contents.getFluid(), Math.min(FluidType.BUCKET_VOLUME, getTankCapacity() - contents.getAmount())), IFluidHandler.FluidAction.SIMULATE);
			if (extracted.isEmpty()) {
				return false;
			}
			long filled = fill(extracted, IFluidHandler.FluidAction.EXECUTE, false);
			FluidStack toExtract = filled == extracted.getAmount() ? extracted : new FluidStack(extracted.getFluid(), filled);
			fluidHandler.drain(toExtract, IFluidHandler.FluidAction.EXECUTE);
			updateContainerStack.accept(fluidHandler.getContainer());
			return true;
		}
		return false;
	}

	@Override
	public int getMinimumMultiplierRequired() {
		return (int) Math.ceil((float) contents.getAmount() / upgradeItem.getBaseCapacity(storageWrapper));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	private class TankComponentItemHandler extends ComponentItemHandler {
		public TankComponentItemHandler(ItemStack upgrade) {
			super(upgrade, DataComponents.CONTAINER, 2);
		}

		@Override
		protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
			super.onContentsChanged(slot, oldStack, newStack);
			save();
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			if (slot == INPUT_SLOT) {
				return stack.isEmpty() ||  isValidInputItem(stack);
			} else if (slot == OUTPUT_SLOT) {
				return stack.isEmpty() ||  isValidOutputItem(stack);
			}
			return false;
		}

		private boolean isValidInputItem(ItemStack stack) {
			return isValidFluidItem(stack, false);
		}

		private boolean isValidOutputItem(ItemStack stack) {
			return isValidFluidItem(stack, true);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		public void setStackInSlotWithoutValidation(int slot, ItemStack stack) {
			super.updateContents(getContents(), stack, slot);
		}
	}
}