package net.p3pp3rf1y.sophisticatedcore.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageFluidHandler;

import java.util.Iterator;

public class EmptyFluidHandler implements IStorageFluidHandler {
	public static EmptyFluidHandler INSTANCE = new EmptyFluidHandler();

	@Override
	public long insert(FluidVariant resource, long maxFill, TransactionContext ctx, boolean ignoreInOutLimit) {
		return 0;
	}

	@Override
	public long extract(FluidVariant resource, long maxDrain, TransactionContext ctx, boolean ignoreInOutLimit) {
		return 0;
	}

	@Override
	public FluidStack extract(TagKey<Fluid> resourceTag, long maxDrain, TransactionContext ctx, boolean ignoreInOutLimit) {
		return null;
	}

	@Override
	public FluidStack extract(int maxDrain, TransactionContext ctx, boolean ignoreInOutLimit) {
		return null;
	}

	@Override
	public FluidStack extract(FluidStack resource, TransactionContext ctx, boolean ignoreInOutLimit) {
		return null;
	}

	@Override
	public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public Iterator<StorageView<FluidVariant>> iterator() {
		return null;
	}
}
