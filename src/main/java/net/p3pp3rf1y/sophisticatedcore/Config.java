package net.p3pp3rf1y.sophisticatedcore;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.api.fml.event.config.ModConfigEvents;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SortButtonsPosition;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Config {
	private static final Map<ModConfig.Type, BaseConfig> CONFIGS = new EnumMap<>(ModConfig.Type.class);

	public static Client CLIENT = register(Client::new, ModConfig.Type.CLIENT);
	public static Common COMMON = register(Common::new, ModConfig.Type.SERVER);

	public static class BaseConfig {
		public ForgeConfigSpec specification;

		public void onConfigLoad() { }
		public void onConfigReload() { }
	}

	public static class Client extends BaseConfig {
		public final ForgeConfigSpec.EnumValue<SortButtonsPosition> sortButtonsPosition;
		public final ForgeConfigSpec.BooleanValue playButtonSound;

		Client(ForgeConfigSpec.Builder builder) {
			builder.comment("Client Settings").push("client");
			sortButtonsPosition = builder.comment("Positions where sort buttons can display to help with conflicts with controls from other mods").defineEnum("sortButtonsPosition", SortButtonsPosition.TITLE_LINE_RIGHT);
			playButtonSound = builder.comment("Whether click sound should play when custom buttons are clicked in gui").define("playButtonSound", true);
			builder.pop();
		}
	}

	public static class Common extends BaseConfig {
		public final EnabledItems enabledItems;

		@SuppressWarnings("unused") //need the Event parameter for forge reflection to understand what event this listens to
		public void onConfigReload() {
			enabledItems.enabledMap.clear();
		}

		Common(ForgeConfigSpec.Builder builder) {
			builder.comment("Common Settings").push("common");

			enabledItems = new EnabledItems(builder);
		}

		public static class EnabledItems {
			private final ForgeConfigSpec.ConfigValue<List<String>> itemsEnableList;
			private final Map<ResourceLocation, Boolean> enabledMap = new ConcurrentHashMap<>();

			EnabledItems(ForgeConfigSpec.Builder builder) {
				itemsEnableList = builder.comment("Disable / enable any items here (disables their recipes)").define("enabledItems", new ArrayList<>());
			}

			public boolean isItemEnabled(Item item) {
				return RegistryHelper.getRegistryName(Registry.ITEM, item).map(this::isItemEnabled).orElse(false);
			}

			public boolean isItemEnabled(ResourceLocation itemRegistryName) {
				if (!COMMON.specification.isLoaded()) {
					return true;
				}
				if (enabledMap.isEmpty()) {
					loadEnabledMap();
				}
				return enabledMap.computeIfAbsent(itemRegistryName, irn -> {
					addEnabledItemToConfig(itemRegistryName);
					return true;
				});
			}

			private void addEnabledItemToConfig(ResourceLocation itemRegistryName) {
				List<String> list = itemsEnableList.get();
				list.add(itemRegistryName + "|true");
				itemsEnableList.set(list);
			}

			private void loadEnabledMap() {
				for (String itemEnabled : itemsEnableList.get()) {
					String[] data = itemEnabled.split("\\|");
					if (data.length == 2) {
						enabledMap.put(new ResourceLocation(data[0]), Boolean.valueOf(data[1]));
					} else {
						SophisticatedCore.LOGGER.error("Wrong data for enabledItems - expected registry name|true/false when {} was provided", itemEnabled);
					}
				}
			}
		}
	}

	private static <T extends BaseConfig> T register(Function<ForgeConfigSpec.Builder, T> factory, ModConfig.Type side) {
		Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(factory);

		T config = specPair.getLeft();
		config.specification = specPair.getRight();
		CONFIGS.put(side, config);
		return config;
	}

	public static void register() {
		for (Map.Entry<ModConfig.Type, BaseConfig> pair : CONFIGS.entrySet()) {
			ModLoadingContext.registerConfig(SophisticatedCore.ID, pair.getKey(), pair.getValue().specification);
		}

		ModConfigEvents.loading(SophisticatedCore.ID).register(Config::onConfigLoad);
		ModConfigEvents.reloading(SophisticatedCore.ID).register(Config::onConfigReload);
	}

	public static void onConfigLoad(ModConfig modConfig) {
		for (BaseConfig config : CONFIGS.values())
			if (config.specification == modConfig.getSpec())
				config.onConfigLoad();
	}

	public static void onConfigReload(ModConfig modConfig) {
		for (BaseConfig config : CONFIGS.values())
			if (config.specification == modConfig.getSpec())
				config.onConfigReload();
	}
}
