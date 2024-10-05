package net.p3pp3rf1y.sophisticatedcore.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.p3pp3rf1y.sophisticatedcore.network.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.PlayDiscPacket;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.SoundStopNotificationPacket;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackPacket;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickPacket;

public class ModPackets {
	private ModPackets() {
	}

	public static void registerPackets() {
		ServerPlayNetworking.registerGlobalReceiver(SyncContainerClientDataPacket.TYPE, SyncContainerClientDataPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(TransferFullSlotPacket.TYPE, TransferFullSlotPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(SoundStopNotificationPacket.TYPE, SoundStopNotificationPacket::handle);
		ServerPlayNetworking.registerGlobalReceiver(TankClickPacket.TYPE, TankClickPacket::handle);
	}

	@Environment(EnvType.CLIENT)
	public static void registerClientPackets() {
		ClientPlayNetworking.registerGlobalReceiver(SyncContainerStacksPacket.TYPE, SyncContainerStacksPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncSlotStackPacket.TYPE, SyncSlotStackPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncPlayerSettingsPacket.TYPE, SyncPlayerSettingsPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(PlayDiscPacket.TYPE, PlayDiscPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(StopDiscPlaybackPacket.TYPE, StopDiscPlaybackPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncTemplateSettingsPacket.TYPE, SyncTemplateSettingsPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncAdditionalSlotInfoPacket.TYPE, SyncAdditionalSlotInfoPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncEmptySlotIconsPacket.TYPE, SyncEmptySlotIconsPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncSlotChangeErrorPacket.TYPE, SyncSlotChangeErrorPacket::handle);
		ClientPlayNetworking.registerGlobalReceiver(SyncDatapackSettingsTemplatePacket.TYPE, SyncDatapackSettingsTemplatePacket::handle);
	}
}
