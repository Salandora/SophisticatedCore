package net.p3pp3rf1y.sophisticatedcore.extensions.component;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface SophisticatedMutableDataComponentHolder extends DataComponentHolder {
	<T> @Nullable T set(DataComponentType<? super T> type, @Nullable T value);

	default <T> @Nullable T set(Supplier<? extends DataComponentType<? super T>> componentType, @Nullable T value) {
		return this.set(componentType.get(), value);
	}

	<T> @Nullable T get(DataComponentType<? extends T> type);

	default <T> @Nullable T get(Supplier<? extends DataComponentType<? extends T>> componentType) {
		return this.get(componentType.get());
	}

	<T> @Nullable T remove(DataComponentType<? extends T> type);

	default <T> @Nullable T remove(Supplier<? extends DataComponentType<? extends T>> componentType) {
		return this.remove(componentType.get());
	}
}
