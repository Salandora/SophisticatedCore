package net.p3pp3rf1y.sophisticatedcore.util;

import org.apache.commons.lang3.Validate;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public class RegistryHelper {
	private RegistryHelper() {}

	public static ResourceLocation getItemKey(Item item) {
		ResourceLocation itemKey = Registry.ITEM.getKey(item);
		Validate.notNull(itemKey, "itemKey");
		return itemKey;
	}

	public static <V> Optional<ResourceLocation> getRegistryName(Registry<V> registry, V registryEntry) {
		return Optional.ofNullable(registry.getKey(registryEntry));
	}

	public static Optional<Item> getItemFromName(String itemName) {
		ResourceLocation key = new ResourceLocation(itemName);
		if (Registry.ITEM.containsKey(key)) {
			//noinspection ConstantConditions - checked above with containsKey
			return Optional.of(Registry.ITEM.get(key));
		}
		return Optional.empty();
	}
}