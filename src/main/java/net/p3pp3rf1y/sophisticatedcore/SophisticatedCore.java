package net.p3pp3rf1y.sophisticatedcore;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticatedcore.common.CapabilityWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedcore.init.ModCompat;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;


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
		Config.COMMON.initListeners();
		PacketHandler.INSTANCE.init();
		PacketHandler.INSTANCE.initServerListener();
		ModCompat.initCompats();
		CapabilityWrapper.register();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> currentServer = server);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> RecipeHelper.setLevel(server.getLevel(Level.OVERWORLD)));
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(DatapackSettingsTemplateManager.Loader.INSTANCE);
	}

	public static ResourceLocation getRL(String regName) {
		return new ResourceLocation(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
