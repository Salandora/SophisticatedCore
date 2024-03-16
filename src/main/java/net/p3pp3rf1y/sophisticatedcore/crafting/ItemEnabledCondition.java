package net.p3pp3rf1y.sophisticatedcore.crafting;

import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

public class ItemEnabledCondition implements ConditionJsonProvider {
	public static final ResourceLocation ID = SophisticatedCore.getRL("item_enabled");
	private final ResourceLocation itemRegistryName;

	public ItemEnabledCondition(Item item) {
		this.itemRegistryName = Registry.ITEM.getKey(item);
	}

	@Override
	public ResourceLocation getConditionId() {
		return ID;
	}

	@Override
	public void writeParameters(JsonObject json) {
		json.addProperty("itemRegistryName", itemRegistryName.toString());
	}

	public static boolean test(JsonObject json) {
		return Config.COMMON.enabledItems.isItemEnabled(new ResourceLocation(GsonHelper.getAsString(json, "itemRegistryName")));
	}
}
