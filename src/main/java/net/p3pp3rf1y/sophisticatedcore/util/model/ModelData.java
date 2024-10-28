package net.p3pp3rf1y.sophisticatedcore.util.model;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class ModelData {
	public static final ModelData EMPTY = builder().build();
	private final Map<ModelProperty<?>, Object> properties;

	private ModelData(Map<ModelProperty<?>, Object> properties) {
		this.properties = properties;
	}

	public Set<ModelProperty<?>> getProperties() {
		return this.properties.keySet();
	}

	public boolean has(ModelProperty<?> property) {
		return this.properties.containsKey(property);
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T get(ModelProperty<T> property) {
		return (T) this.properties.get(property);
	}

	public static Builder derive(ModelData data) {
		return new Builder(data.properties);
	}

	public static Builder builder() {
		return new Builder(new IdentityHashMap<>());
	}

	public static final class Builder {
		private final Map<ModelProperty<?>, Object> properties;

		private Builder(@Nullable Map<ModelProperty<?>, Object> properties) {
			this.properties = new IdentityHashMap<>();
			if (properties != null) {
				this.properties.putAll(properties);
			}
		}

		public <T> Builder with(ModelProperty<T> property, T value) {
			this.properties.put(property, value);
			return this;
		}

		public ModelData build() {
			return new ModelData(Collections.unmodifiableMap(this.properties));
		}
	}
}
