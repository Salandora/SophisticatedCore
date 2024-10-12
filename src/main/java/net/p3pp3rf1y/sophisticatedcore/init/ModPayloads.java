package net.p3pp3rf1y.sophisticatedcore.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.network.LitematicaPayloads;
import net.p3pp3rf1y.sophisticatedcore.network.*;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.PlayDiscPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.SoundStopNotificationPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackPayload;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickPayload;

public class ModPayloads {
	private ModPayloads() {
	}

	public static void registerPayloads() {
		registerC2S(SyncContainerClientDataPayload.TYPE, SyncContainerClientDataPayload.STREAM_CODEC, SyncContainerClientDataPayload::handlePayload);
		registerC2S(TransferFullSlotPayload.TYPE, TransferFullSlotPayload.STREAM_CODEC, TransferFullSlotPayload::handlePayload);
		registerC2S(SoundStopNotificationPayload.TYPE, SoundStopNotificationPayload.STREAM_CODEC, SoundStopNotificationPayload::handlePayload);
		registerC2S(TankClickPayload.TYPE, TankClickPayload.STREAM_CODEC, TankClickPayload::handlePayload);

		PayloadTypeRegistry.playS2C().register(SyncContainerStacksPayload.TYPE, SyncContainerStacksPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncSlotStackPayload.TYPE, SyncSlotStackPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncPlayerSettingsPayload.TYPE, SyncPlayerSettingsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PlayDiscPayload.TYPE, PlayDiscPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(StopDiscPlaybackPayload.TYPE, StopDiscPlaybackPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncTemplateSettingsPayload.TYPE, SyncTemplateSettingsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncAdditionalSlotInfoPayload.TYPE, SyncAdditionalSlotInfoPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncEmptySlotIconsPayload.TYPE, SyncEmptySlotIconsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncSlotChangeErrorPayload.TYPE, SyncSlotChangeErrorPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(SyncDatapackSettingsTemplatePayload.TYPE, SyncDatapackSettingsTemplatePayload.STREAM_CODEC);

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			ClientPlayNetworking.registerGlobalReceiver(SyncContainerStacksPayload.TYPE, SyncContainerStacksPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncSlotStackPayload.TYPE, SyncSlotStackPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncPlayerSettingsPayload.TYPE, SyncPlayerSettingsPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(PlayDiscPayload.TYPE, PlayDiscPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(StopDiscPlaybackPayload.TYPE, StopDiscPlaybackPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncTemplateSettingsPayload.TYPE, SyncTemplateSettingsPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncAdditionalSlotInfoPayload.TYPE, SyncAdditionalSlotInfoPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncEmptySlotIconsPayload.TYPE, SyncEmptySlotIconsPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncSlotChangeErrorPayload.TYPE, SyncSlotChangeErrorPayload::handlePayload);
			ClientPlayNetworking.registerGlobalReceiver(SyncDatapackSettingsTemplatePayload.TYPE, SyncDatapackSettingsTemplatePayload::handlePayload);
		}

		LitematicaPayloads.registerPackets();
	}

	public static <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> id, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
		PayloadTypeRegistry.playC2S().register(id, codec);
		ServerPlayNetworking.registerGlobalReceiver(id, handler);
	}
}
