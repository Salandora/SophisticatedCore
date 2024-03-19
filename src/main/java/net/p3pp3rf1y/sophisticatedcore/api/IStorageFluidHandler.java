package net.p3pp3rf1y.sophisticatedcore.api;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;

public interface IStorageFluidHandler extends Storage<FluidVariant> {
	default long simulateInsert(TagKey<Fluid> fluidTag, long maxFill, Fluid fallbackFluid, @Nullable TransactionContext transaction) {
		try (Transaction simulateTransaction = Transaction.openNested(transaction)) {
			return insert(fluidTag, maxFill, fallbackFluid, simulateTransaction);
		}
	}

	default long insert(TagKey<Fluid> fluidTag, long maxFill, Fluid fallbackFluid, TransactionContext ctx) {
		return insert(fluidTag, maxFill, fallbackFluid, ctx, false);
	}

	default long insert(TagKey<Fluid> fluidTag, long maxFill, Fluid fallbackFluid, TransactionContext ctx, boolean ignoreInOutLimit) {
        for (StorageView<FluidVariant> view : this.nonEmptyViews()) {
            if (view.getResource().getFluid().defaultFluidState().is(fluidTag)) {
                return insert(view.getResource(), maxFill, ctx, ignoreInOutLimit);
            }
        }

		return insert(FluidVariant.of(fallbackFluid), maxFill, ctx, ignoreInOutLimit);
	}

	long insert(FluidVariant resource, long maxFill, TransactionContext ctx, boolean ignoreInOutLimit);

	long extract(FluidVariant resource, long maxDrain, TransactionContext ctx, boolean ignoreInOutLimit);

	default FluidStack simulateExtract(TagKey<Fluid> fluidTag, long maxFill, @Nullable TransactionContext transaction) {
		try (Transaction simulateTransaction = Transaction.openNested(transaction)) {
			return extract(fluidTag, maxFill, simulateTransaction);
		}
	}

	default FluidStack extract(TagKey<Fluid> fluidTag, long maxDrain, TransactionContext ctx) {
		return extract(fluidTag, maxDrain, ctx, false);
	}

	FluidStack extract(TagKey<Fluid> resourceTag, long maxDrain, TransactionContext ctx, boolean ignoreInOutLimit);

	FluidStack extract(int maxDrain, TransactionContext ctx, boolean ignoreInOutLimit);

	FluidStack extract(FluidStack resource, TransactionContext ctx, boolean ignoreInOutLimit);
}
