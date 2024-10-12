package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;

public class LitematicaPayloads {
	public static void registerPackets() {
		ModPayloads.registerC2S(RequestContentsPayload.TYPE, RequestContentsPayload.CODEC, RequestContentsPayload::handlePayload);

		PayloadTypeRegistry.playS2C().register(UpdateMaterialListPayload.TYPE, UpdateMaterialListPayload.STREAM_CODEC);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			ClientPlayNetworking.registerGlobalReceiver(UpdateMaterialListPayload.TYPE, UpdateMaterialListPayload::handlePayload);
		}
	}
}

