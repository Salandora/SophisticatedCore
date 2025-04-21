package net.p3pp3rf1y.sophisticatedcore.compat.jei.subtypes;

import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public abstract class PropertyBasedSubtypeInterpreter implements ISubtypeInterpreter<ItemStack> {
	private final List<IPropertyDefinition<?>> propertyDefinitions = new ArrayList<>();

	protected <T> void addOptionalProperty(Function<ItemStack, Optional<T>> propertyGetter, String propertyName,
										   Function<T, String> propertyValueSerializer) {
		Function<ItemStack, @Nullable T> nullableGetter = propertyGetter.andThen(i -> i.orElse(null));
		addDefinition(nullableGetter, propertyName, propertyValueSerializer);
	}

	private <T> void addDefinition(Function<ItemStack, @Nullable T> getter, String propertyName, Function<T, String> propertyValueSerializer) {
		this.propertyDefinitions.add(new IPropertyDefinition<T>() {
			@Override
			public @Nullable T getPropertyValue(ItemStack itemStack) {
				return getter.apply(itemStack);
			}

			@Override
			public String getPropertyName() {
				return propertyName;
			}

			@Override
			public String serializePropertyValue(@Nullable T property) {
				return property != null ? propertyValueSerializer.apply(property) : "";
			}
		});
	}

	protected <T> void addProperty(Function<ItemStack, @Nullable T> propertyGetter, String propertyName, Function<T, String> propertyValueSerializer) {
		addDefinition(propertyGetter, propertyName, propertyValueSerializer);
	}

	@Override
	public final @Nullable Object getSubtypeData(ItemStack ingredient, UidContext context) {
		boolean allNulls = true;
		List<@Nullable Object> results = new ArrayList<>(propertyDefinitions.size());
		for (IPropertyDefinition<?> definition : propertyDefinitions) {
			@Nullable Object value = definition.getPropertyValue(ingredient);
			if (value != null) {
				allNulls = false;
			}
			results.add(value);
		}
		if (allNulls) {
			return null;
		}
		return results;
	}

	@Override
	public String getLegacyStringSubtypeInfo(ItemStack itemStack, UidContext context) {
		StringBuilder result = new StringBuilder();
		for (IPropertyDefinition<?> definition : propertyDefinitions) {
			@Nullable Object value = definition.getPropertyValue(itemStack);
			if (value != null) {
				String serializedValue = getSerializedPropertyValue(definition, value);
				if (!result.isEmpty()) {
					result.append(',');
				}
				result.append(definition.getPropertyName()).append(':').append(serializedValue);
			}
		}
		return "{" + result + "}";
	}

	private <T> String getSerializedPropertyValue(IPropertyDefinition<T> definition, Object value) {
		//noinspection unchecked
		return definition.serializePropertyValue((T) value);
	}

	public String getRegistrySanitizedItemString(ItemStack stack) {
		StringBuilder result = new StringBuilder();
		for (IPropertyDefinition<?> definition : propertyDefinitions) {
			@Nullable Object value = definition.getPropertyValue(stack);
			if (value != null) {
				String serializedValue = sanitize(getSerializedPropertyValue(definition, value));
				if (!result.isEmpty()) {
					result.append('_');
				}
				result.append(definition.getPropertyName().toLowerCase(Locale.ROOT)).append('_').append(serializedValue);
			}
		}
		return getItemPath(stack) + "_" + result;
	}

	private String sanitize(String value) {
		return value.replaceAll(":", "_").toLowerCase(Locale.ROOT);
	}

	private static @NotNull String getItemPath(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
	}


	public interface IPropertyDefinition<T> {
		@Nullable
		T getPropertyValue(ItemStack itemStack);

		String getPropertyName();

		String serializePropertyValue(@Nullable T property);
	}
}
