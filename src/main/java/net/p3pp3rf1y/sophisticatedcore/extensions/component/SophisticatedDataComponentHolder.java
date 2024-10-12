package net.p3pp3rf1y.sophisticatedcore.extensions.component;

import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;

import java.util.function.Supplier;

public interface SophisticatedDataComponentHolder {
	private DataComponentHolder self() {
		return (DataComponentHolder)this;
	}

	default <T> T getOrDefault(Supplier<? extends DataComponentType<? extends T>> type, T defaultValue) {
		return this.self().getOrDefault(type.get(), defaultValue);
	}

	default <T extends DataComponentType<?>> boolean has(Supplier<T> type) {
		return this.self().has(type.get());
	}
}
