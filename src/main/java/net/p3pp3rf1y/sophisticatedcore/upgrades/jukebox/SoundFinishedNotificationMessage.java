package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.github.salandora.sophisticatedlibrary.network.api.v0.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

public class SoundFinishedNotificationMessage {
	private final UUID storageUuid;

	public SoundFinishedNotificationMessage(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public static void encode(SoundFinishedNotificationMessage msg, FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(msg.storageUuid);
	}

	public static SoundFinishedNotificationMessage decode(FriendlyByteBuf packetBuffer) {
		return new SoundFinishedNotificationMessage(packetBuffer.readUUID());
	}

	public static void onMessage(SoundFinishedNotificationMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer sender, SoundFinishedNotificationMessage msg) {
		if (sender == null) {
			return;
		}
		ServerStorageSoundHandler.onSoundFinished((ServerLevel) sender.level(), msg.storageUuid);
	}
}
