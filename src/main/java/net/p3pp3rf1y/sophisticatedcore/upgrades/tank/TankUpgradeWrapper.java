package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import com.github.salandora.sophisticatedlibrary.fluid.FluidStack;
import com.github.salandora.sophisticatedlibrary.fluid.SimpleFluidContent;
import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ComponentItemHandler;
import net.p3pp3rf1y.sophisticatedcore.fluid.FluidUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TankUpgradeWrapper extends UpgradeWrapperBase<TankUpgradeWrapper, TankUpgradeItem>
		implements IRenderedTankUpgrade, ITickableUpgrade, IStackableContentsUpgrade, SingleSlotStorage<FluidVariant> {
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
		return upgrade.sophisticatedCore_getOrDefault(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.EMPTY);
	}

	private boolean isValidFluidItem(ItemStack stack, boolean isOutput) {
		return CapabilityHelper.getFromFluidHandler(stack, fluidHandler -> isValidFluidHandler(fluidHandler, isOutput), false);
	}

	private boolean isValidFluidHandler(Storage<FluidVariant> storage, boolean isOutput) {
		boolean tankEmpty = contents.isEmpty();
		for (StorageView<FluidVariant> view : storage) {
			FluidStack fluidInTank = new FluidStack(view);
			if ((isOutput && (view.isResourceBlank() || (!tankEmpty && FluidStack.isSameFluidSameComponents(fluidInTank, contents))))
				|| (!isOutput && (!view.isResourceBlank() && (tankEmpty || FluidStack.isSameFluidSameComponents(contents, fluidInTank))))
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

	public long getTankCapacity() {
		return upgradeItem.getTankCapacity(storageWrapper);
	}

	public SlottedStackStorage getInventory() {
		return inventory;
	}

	private long getMaxInOut() {
		return (int) Math.max(FluidConstants.BUCKET, upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows() * upgradeItem.getAdjustedStackMultiplier(storageWrapper) * FluidUtil.BUCKET_VOLUME_IN_MILLIBUCKETS);
	}

	/*public int fill(FluidStack resource, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
		int capacity = getTankCapacity();

		if (contents.getAmount() >= capacity || (!contents.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, contents))) {
			return 0;
		}

		int toFill = Math.min(capacity - contents.getAmount(), resource.getAmount());
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
	}*/

	public long fill(FluidVariant resource, long maxFill, TransactionContext ctx, boolean ignoreInOutLimit) {
		long capacity = getTankCapacity();
		if (contents.getAmount() >= capacity || (!contents.isEmpty() && !resource.isOf(contents.getFluid()))) {
			return 0;
		}

		long toFill = Math.min(capacity - contents.getAmount(), maxFill);
		if (!ignoreInOutLimit) {
			toFill = Math.min(getMaxInOut(), toFill);
		}

		long finalToFill = toFill;
		TransactionCallback.onSuccess(ctx, () -> {
			if (contents.isEmpty()) {
				contents = new FluidStack(resource, finalToFill);
			} else {
				contents.setAmount(contents.getAmount() + finalToFill);
			}
			serializeContents();
		});

		return toFill;
	}

	private void serializeContents() {
		upgrade.sophisticatedCore_set(ModCoreDataComponents.FLUID_CONTENTS, SimpleFluidContent.copyOf(contents));
		save();
		forceUpdateTankRenderInfo();
	}

	/*public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action, boolean ignoreInOutLimit) {
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
	}*/

	public long drain(long maxDrain, TransactionContext ctx, boolean ignoreInOutLimit) {
		if (contents.isEmpty()) {
			return 0;
		}

		long toDrain = Math.min(maxDrain, contents.getAmount());
		if (!ignoreInOutLimit) {
			toDrain = Math.min(getMaxInOut(), toDrain);
		}

		long finalToDrain = toDrain;
		TransactionCallback.onSuccess(ctx, () -> {
			if (finalToDrain == contents.getAmount()) {
				contents = FluidStack.EMPTY;
			} else {
				contents.setAmount(contents.getAmount() - finalToDrain);
			}
			serializeContents();
		});

		return toDrain;
	}

	@Override
	public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
		if (level.getGameTime() < cooldownTime) {
			return;
		}

		AtomicBoolean didSomething = new AtomicBoolean(false);
		CapabilityHelper.runOnFluidHandler(inventory.getStackInSlot(INPUT_SLOT), (cic, fluidHandler) ->
				didSomething.set(drainHandler(cic, fluidHandler, stack -> inventory.setStackInSlotWithoutValidation(INPUT_SLOT, stack)))
		);
		CapabilityHelper.runOnFluidHandler(inventory.getStackInSlot(OUTPUT_SLOT), (cic, fluidHandler) ->
				didSomething.set(fillHandler(cic, fluidHandler, stack -> inventory.setStackInSlotWithoutValidation(OUTPUT_SLOT, stack)))
		);

		if (didSomething.get()) {
			cooldownTime = level.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}

	public boolean fillHandler(ContainerItemContext cic, Storage<FluidVariant> fluidHandler, Consumer<ItemStack> updateContainerStack) {
		if (!contents.isEmpty() && isValidFluidHandler(fluidHandler, true)) {
			long filled = StorageUtil.simulateInsert(fluidHandler, contents.getVariant(), Math.min(FluidConstants.BUCKET, contents.getAmount()), null);
			if (filled <= 0) { //checking for less than as well because some mods have incorrect fill logic
				return false;
			}
			try (Transaction ctx = Transaction.openOuter()) {
				long drained = drain(filled, ctx, false);
				fluidHandler.insert(contents.getVariant(), drained, ctx);
				ctx.commit();
			}
			updateContainerStack.accept(cic.getItemVariant().toStack((int) cic.getAmount()));
			return true;
		}
		return false;
	}

	public boolean drainHandler(ContainerItemContext cic, Storage<FluidVariant> fluidHandler, Consumer<ItemStack> updateContainerStack) {
		if (isValidFluidHandler(fluidHandler, false)) {
			FluidVariant resource = contents.isEmpty() ? StorageUtil.findStoredResource(fluidHandler) : contents.getVariant();
			long extracted = contents.isEmpty() ?
					StorageUtil.simulateExtract(fluidHandler, resource, FluidConstants.BUCKET, null) :
					StorageUtil.simulateExtract(fluidHandler, resource, Math.min(FluidConstants.BUCKET, getTankCapacity() - contents.getAmount()), null);
			if (extracted <= 0) {
				return false;
			}
			try (Transaction ctx = Transaction.openOuter()) {
				long filled = fill(resource, extracted, ctx, false);
				fluidHandler.extract(resource, filled, ctx);
				ctx.commit();
			}
			updateContainerStack.accept(cic.getItemVariant().toStack((int) cic.getAmount()));
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

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		return fill(resource, maxAmount, transaction, false);
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		if (contents == null || !resource.isOf(contents.getFluid())) {
			return 0;
		}

		return drain(maxAmount, transaction, false);
	}

	@Override
	public boolean isResourceBlank() {
		return contents == null || contents.isEmpty();
	}

	@Override
	public FluidVariant getResource() {
		return contents.isEmpty() ? FluidVariant.blank() : contents.getVariant();
	}

	@Override
	public long getAmount() {
		return contents.getAmount();
	}

	@Override
	public long getCapacity() {
		return getMaxInOut();
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