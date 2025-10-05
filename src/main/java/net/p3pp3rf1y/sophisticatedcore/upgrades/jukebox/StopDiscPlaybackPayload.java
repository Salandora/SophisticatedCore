package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.github.salandora.sophisticatedlibrary.network.api.v1.IPayloadContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public record StopDiscPlaybackPayload(UUID storageUuid) implements CustomPacketPayload {
	public static final Type<StopDiscPlaybackPayload> TYPE = new Type<>(SophisticatedCore.getRL("stop_disc_playback"));
	public static final StreamCodec<ByteBuf, StopDiscPlaybackPayload> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC,
			StopDiscPlaybackPayload::storageUuid,
			StopDiscPlaybackPayload::new);


	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(StopDiscPlaybackPayload payload, IPayloadContext context) {
		StorageSoundHandler.stopStorageSound(payload.storageUuid);
	}
}
