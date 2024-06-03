package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.UUID;

public class SoundStopNotificationMessage extends SimplePacketBase {
	private final UUID storageUuid;

	public SoundStopNotificationMessage(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public SoundStopNotificationMessage(FriendlyByteBuf packetBuffer) {
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
			ServerStorageSoundHandler.onSoundStopped((ServerLevel) sender.level(), storageUuid);
		});
		return true;
	}
}
