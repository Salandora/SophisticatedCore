/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.p3pp3rf1y.fabricmc.fabric.api.transfer.v1.storage;

import com.google.common.collect.Iterators;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A {@link Storage} implementation made of indexed slots.
 *
 * <p>Please note that some storages may not implement this interface.
 * It is up to the storage implementation to decide whether to implement this interface or not.
 * Checking whether a storage is slotted can be done using {@code instanceof}.
 *
 * @param <T> The type of the stored resources.
 *
 * <b>Experimental feature</b>, we reserve the right to remove or change it without further notice.
 * The transfer API is a complex addition, and we want to be able to correct possible design mistakes.
 */
@ApiStatus.Experimental
public interface SlottedStorage<T> extends Storage<T> {
	/**
	 * Retrieve the number of slots in this storage.
	 */
	int getSlotCount();

	/**
	 * Retrieve a specific slot of this storage.
	 *
	 * @throws IndexOutOfBoundsException If the slot index is out of bounds.
	 */
	SingleSlotStorage<T> getSlot(int slot);

	/**
	 * Retrieve a list containing all the slots of this storage. <b>The list must not be modified.</b>
	 *
	 * <p>This function can be used to interface with code that requires a slot list,
	 * for example {@link StorageUtil#insertStacking} or {@link ContainerItemContext#getAdditionalSlots()}.
	 *
	 * <p>It is guaranteed that calling this function is fast.
	 * The default implementation returns a view over the storage that delegates to {@link #getSlotCount} and {@link #getSlot}.
	 */
	@UnmodifiableView
	default List<SingleSlotStorage<T>> getSlots() {
		return new AbstractList<>() {
			@Override
			public SingleSlotStorage<T> get(int index) {
				return getSlot(index);
			}

			@Override
			public int size() {
				return getSlotCount();
			}
		};
	}

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
	default Iterator<StorageView<T>> nonEmptyIterator() {
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
	default Iterable<StorageView<T>> nonEmptyViews() {
		return this::nonEmptyIterator;
	}
}
