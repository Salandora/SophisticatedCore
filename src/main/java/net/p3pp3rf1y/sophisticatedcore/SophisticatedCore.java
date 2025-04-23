package net.p3pp3rf1y.sophisticatedcore;

import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import net.neoforged.fml.config.ModConfig;
import net.p3pp3rf1y.sophisticatedcore.common.CommonEventHandler;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.init.ModCompat;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.model.DynamicFluidContainerModel;
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

	public static boolean isLogicalServerThread() {
		String name = Thread.currentThread().getName();
		return name.startsWith("Server") || name.startsWith("Netty");
	}

	@Override
	public void onInitialize() {
		NeoForgeConfigRegistry.INSTANCE.register(SophisticatedCore.MOD_ID, ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
		NeoForgeConfigRegistry.INSTANCE.register(SophisticatedCore.MOD_ID, ModConfig.Type.COMMON, Config.COMMON_SPEC);
		commonEventHandler.registerHandlers();
		ModCompat.register();
		CompatRegistry.getRegistry(MOD_ID).initCompats();
		Config.COMMON.initListeners();
		ModCoreDataComponents.register();

		ServerLifecycleEvents.SERVER_STARTED.register(SophisticatedCore::serverStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(SophisticatedCore::serverStopped);

		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(DatapackSettingsTemplateManager.Loader.INSTANCE);

		CompatRegistry.getRegistry(MOD_ID).setupCompats();

		RegisterGeometryLoadersCallback.EVENT.register(loaders -> loaders.put(SophisticatedCore.getRL("fluid_container"), DynamicFluidContainerModel.Loader.INSTANCE));
	}

	private static void serverStarted(MinecraftServer server) {
		currentServer = server;
		ServerLevel world = server.getLevel(Level.OVERWORLD);
		if (world != null) {
			RecipeHelper.setLevel(world);
			StorageWrapperRepository.clearCache();
			Config.COMMON.saveIfChanged();
		}
	}

	private static void serverStopped(MinecraftServer server) {
		currentServer = null;
		StorageWrapperRepository.clearCache();
	}

	public static ResourceLocation getRL(String regName) {
		return ResourceLocation.parse(getRegistryName(regName));
	}

	public static String getRegistryName(String regName) {
		return MOD_ID + ":" + regName;
	}
}
