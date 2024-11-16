package net.p3pp3rf1y.sophisticatedcore.extensions.component;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface SophisticatedMutableDataComponentHolder extends DataComponentHolder {
	<T> @Nullable T sophisticatedCore_set(DataComponentType<? super T> type, @Nullable T value);

	default <T> @Nullable T sophisticatedCore_set(Supplier<? extends DataComponentType<? super T>> componentType, @Nullable T value) {
		return this.sophisticatedCore_set(componentType.get(), value);
	}

	default <T> @Nullable T sophisticatedCore_get(Supplier<? extends DataComponentType<? extends T>> componentType) {
		return this.get(componentType.get());
	}

	<T> @Nullable T sophisticatedCore_remove(DataComponentType<? extends T> type);

	default <T> @Nullable T sophisticatedCore_remove(Supplier<? extends DataComponentType<? extends T>> componentType) {
		return this.sophisticatedCore_remove(componentType.get());
	}
}
