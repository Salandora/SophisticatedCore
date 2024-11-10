package net.p3pp3rf1y.sophisticatedcore.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import javax.annotation.Nullable;

public record SyncPlayerSettingsPayload(String playerTagName,
										@Nullable CompoundTag settingsNbt) implements CustomPacketPayload {
	public static final Type<SyncPlayerSettingsPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_player_settings"));
	public static final StreamCodec<ByteBuf, SyncPlayerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8,
			SyncPlayerSettingsPayload::playerTagName,
			StreamCodecHelper.ofNullable(ByteBufCodecs.COMPOUND_TAG),
			SyncPlayerSettingsPayload::settingsNbt,
			SyncPlayerSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	@Environment(EnvType.CLIENT)
	public static void handlePayload(SyncPlayerSettingsPayload payload, ClientPlayNetworking.Context context) {
		if (payload.settingsNbt == null) {
			return;
		}
		SettingsManager.setPlayerSettingsTag(context.player(), payload.playerTagName, payload.settingsNbt);
	}
}
