package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class LitematicaPackets {
	public static void registerPackets() {
		ServerPlayNetworking.registerGlobalReceiver(RequestContentsPacket.TYPE, RequestContentsPacket::handle);
	}

	@Environment(EnvType.CLIENT)
	public static void registerClientPackets() {
		ClientPlayNetworking.registerGlobalReceiver(UpdateMaterialListMessage.TYPE, UpdateMaterialListMessage::handle);
	}
}

