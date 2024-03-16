package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.porting_lib.transfer.items.ItemStackHandler;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IRenderedTankUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IStackableContentsUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.FluidHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.function.Consumer;
import javax.annotation.Nullable;

public class TankUpgradeWrapper extends UpgradeWrapperBase<TankUpgradeWrapper, TankUpgradeItem>
		implements IRenderedTankUpgrade, ITickableUpgrade, IStackableContentsUpgrade, SingleSlotStorage<FluidVariant> {
	public static final int INPUT_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	private static final String CONTENTS_TAG = "contents";
	private Consumer<TankRenderInfo> updateTankRenderInfoCallback;
	private final ItemStackHandler inventory;
	private FluidStack contents;
	private long cooldownTime = 0;
	private boolean allowEmptyInputResource = false;

	protected TankUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
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
				return isValidFluidItem(stack, false);
			}

			private boolean isValidOutputItem(ItemStack stack) {
				return isValidFluidItem(stack, true);
			}

			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}
		};
		NBTHelper.getCompound(upgrade, "inventory").ifPresent(inventory::deserializeNBT);
		contents = getContents(upgrade);
	}

	public static FluidStack getContents(ItemStack upgrade) {
		return NBTHelper.getCompound(upgrade, CONTENTS_TAG).map(FluidStack::loadFluidStackFromNBT).orElse(FluidStack.EMPTY);
	}

	private boolean isValidFluidItem(ItemStack stack, boolean isOutput) {
		if (!stack.isEmpty()) {
			Storage<FluidVariant> storage = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
			if (storage == null) {
				return false;
			}
			return isValidFluidHandler(storage, isOutput);
		}
		return false;
	}

	private boolean isValidFluidHandler(Storage<FluidVariant> storage, boolean isOutput) {
		boolean tankEmpty = contents.isEmpty();
		for (StorageView<FluidVariant> view : storage) {
			if (
					(isOutput  && (view.isResourceBlank() || (!tankEmpty && view.getResource().isOf(contents.getFluid()))))
				 || (!isOutput && (!view.isResourceBlank() && (tankEmpty  || view.getResource().isOf(contents.getFluid())))
							|| (view.isResourceBlank() && allowEmptyInputResource))
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
		renderInfo.setFluid(contents);
		renderInfo.setFillRatio((float) Math.round((float) contents.getAmount() / getTankCapacity() * 10) / 10);
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
		return Math.max(FluidConstants.BUCKET, upgradeItem.getTankUpgradeConfig().maxInputOutput.get() * storageWrapper.getNumberOfSlotRows() * upgradeItem.getAdjustedStackMultiplier(storageWrapper) * FluidHelper.BUCKET_VOLUME_IN_MILLIBUCKETS);
	}

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
		upgrade.addTagElement(CONTENTS_TAG, contents.writeToNBT(new CompoundTag()));
		save();
		forceUpdateTankRenderInfo();
	}

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
	public void tick(@Nullable LivingEntity entity, Level world, BlockPos pos) {
		if (world.getGameTime() < cooldownTime) {
			return;
		}

		boolean didSomething = false;
		ContainerItemContext cic = ContainerItemContext.ofSingleSlot(inventory.getSlot(INPUT_SLOT));
		Storage<FluidVariant> storage = cic.find(FluidStorage.ITEM);
		if (storage != null) {
			didSomething = drainHandler(storage);
		}

		cic = ContainerItemContext.ofSingleSlot(inventory.getSlot(OUTPUT_SLOT));
		storage = cic.find(FluidStorage.ITEM);
		if (storage != null) {
			didSomething |= fillHandler(storage);
		}

		if (didSomething) {
			cooldownTime = world.getGameTime() + upgradeItem.getTankUpgradeConfig().autoFillDrainContainerCooldown.get();
		}
	}

	public boolean fillHandler(Storage<FluidVariant> storage) {
		if (!contents.isEmpty() && isValidFluidHandler(storage, true)) {
			long filled = storage.simulateInsert(contents.getType(), Math.min(FluidConstants.BUCKET, contents.getAmount()), null);
			if (filled <= 0) { //checking for less than as well because some mods have incorrect fill logic
				return false;
			}
			try (Transaction ctx = Transaction.openOuter()) {
				long drained = drain(filled, ctx, false);
				storage.insert(contents.getType(), drained, ctx);
				ctx.commit();
			}
			return true;
		}
		return false;
	}

	public boolean drainHandler(Storage<FluidVariant> storage) {
		if (isValidFluidHandler(storage, false)) {
			// We have confirmed that the inital item is a valid fluid handler, now it's necessary to allow empty resources due to how the ContainerItemContext
			// works, it takes care of the exchange of the item in the slot, which then will trigger the isValidItem check again.
			allowEmptyInputResource = true;

			FluidVariant resource = contents.isEmpty() ? TransferUtil.getFirstFluid(storage).getType() : contents.getType();
			long extracted = contents.isEmpty() ?
					storage.simulateExtract(resource, FluidConstants.BUCKET, null) :
					storage.simulateExtract(resource, Math.min(FluidConstants.BUCKET, getTankCapacity() - contents.getAmount()), null);
			if (extracted <= 0) {
				allowEmptyInputResource = false; // set back to false
				return false;
			}
			try (Transaction ctx = Transaction.openOuter()) {
				long filled = fill(resource, extracted, ctx, false);
				storage.extract(resource, filled, ctx);
				allowEmptyInputResource = false; // set back to false
				ctx.commit();
			}
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
		return contents.getType();
	}

	@Override
	public long getAmount() {
		return contents.getAmount();
	}

	@Override
	public long getCapacity() {
		return getMaxInOut();
	}
}
