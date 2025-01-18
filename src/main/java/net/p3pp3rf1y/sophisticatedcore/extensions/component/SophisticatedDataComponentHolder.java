package net.p3pp3rf1y.sophisticatedcore.extensions.component;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface SophisticatedDataComponentHolder {
	private DataComponentHolder self() {
		return (DataComponentHolder)this;
	}

	default <T> @Nullable T sophisticatedCore_get(Supplier<? extends DataComponentType<? extends T>> componentType) {
		return this.self().get(componentType.get());
	}

	default <T> T sophisticatedCore_getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T defaultValue) {
		return this.self().getOrDefault(type.get(), defaultValue);
	}

	default <T extends DataComponentType<?>> boolean sophisticatedCore_has(Supplier<T> type) {
		return this.self().has(type.get());
	}
}
