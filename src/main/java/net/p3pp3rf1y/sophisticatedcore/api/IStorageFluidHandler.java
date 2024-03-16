package net.p3pp3rf1y.sophisticatedcore.api;

import com.google.common.collect.Iterators;

import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.Iterator;
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

	/**
	 * Same as {@link #iterator()}, but the iterator is guaranteed to skip over empty views,
	 * i.e. views that {@linkplain StorageView#isResourceBlank() contain blank resources} or have a zero {@linkplain StorageView#getAmount() amount}.
	 *
	 * <p>This can provide a large performance benefit over {@link #iterator()} if the caller is only interested in non-empty views,
	 * for example because it is trying to extract resources from the storage.
	 *
	 * <p>This function should only be overridden if the storage is able to provide an optimized iterator over non-empty views,
	 * for example because it is keeping an index of non-empty views.
	 * Otherwise, the default implementation simply calls {@link #iterator()} and filters out empty views.
	 *
	 * <p>When implementing this function, note that the guarantees of {@link #iterator()} still apply.
	 * In particular, {@link #insert} and {@link #extract} may be called safely during iteration.
	 *
	 * @return An iterator over the non-empty views of this storage. Calling remove on the iterator is not allowed.
	 */
	default Iterator<StorageView<FluidVariant>> nonEmptyIterator() {
		return Iterators.filter(iterator(), view -> view.getAmount() > 0 && !view.isResourceBlank());
	}

	/**
	 * Convenient helper to get an {@link Iterable} over the {@linkplain #nonEmptyIterator() non-empty views} of this storage, for use in for-each loops.
	 *
	 * <p><pre>{@code
	 * for (StorageView<T> view : storage.nonEmptyViews()) {
	 *     // Do something with the view
	 * }
	 * }</pre>
	 */
	default Iterable<StorageView<FluidVariant>> nonEmptyViews() {
		return this::nonEmptyIterator;
	}
}
