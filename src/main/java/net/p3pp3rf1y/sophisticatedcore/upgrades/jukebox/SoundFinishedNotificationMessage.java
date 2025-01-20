package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.UUID;

public class SoundFinishedNotificationMessage extends SimplePacketBase {
	private final UUID storageUuid;

	public SoundFinishedNotificationMessage(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public SoundFinishedNotificationMessage(FriendlyByteBuf packetBuffer) {
		this(packetBuffer.readUUID());
	}

	@Override
	public void write(FriendlyByteBuf packetBuffer) {
		packetBuffer.writeUUID(this.storageUuid);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null) {
				return;
			}
			ServerStorageSoundHandler.onSoundFinished((ServerLevel) sender.level(), storageUuid);
		});
		return true;
	}
}
