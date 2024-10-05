package net.p3pp3rf1y.sophisticatedcore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.p3pp3rf1y.sophisticatedcore.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.network.LitematicaPackets;
import net.p3pp3rf1y.sophisticatedcore.init.ModCompat;
import net.p3pp3rf1y.sophisticatedcore.init.ModPackets;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import fuzs.forgeconfigapiport.api.config.v3.ForgeConfigRegistry;
import net.neoforged.fml.config.ModConfig;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SophisticatedCore implements ModInitializer {
	public static final String MOD_ID = "sophisticatedcore";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public final CommonEventHandler commonEventHandler = new CommonEventHandler();

	private static MinecraftServer currentServer = null;

	@Nullable
	public static MinecraftServer getCurrentServer() {
		return currentServer;
	}

	@Override
	public void onInitialize() {
		ForgeConfigRegistry.INSTANCE.register(SophisticatedCore.MOD_ID, ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		ForgeConfigRegistry.INSTANCE.register(SophisticatedCore.MOD_ID, ModConfig.Type.COMMON, Config.COMMON_SPEC);
		commonEventHandler.registerHandlers();
		ModCompat.register();
		CompatRegistry.initCompats();
		Config.COMMON.initListeners();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> RecipeHelper.setLevel(server.getLevel(Level.OVERWORLD)));

		ModPackets.registerPackets();
		LitematicaPackets.registerPackets();

		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(DatapackSettingsTemplateManager.Loader.INSTANCE);

		CompatRegistry.setupCompats();
	}

	public static ResourceLocation getRL(String regName) {
		return new ResourceLocation(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
