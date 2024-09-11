package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public class SoundStopNotificationPacket implements FabricPacket {
	public static final PacketType<SoundStopNotificationPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sound_stop_notification"), SoundStopNotificationPacket::new);
	private final UUID storageUuid;

	public SoundStopNotificationPacket(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public SoundStopNotificationPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID());
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		ServerStorageSoundHandler.onSoundStopped(player.level(), storageUuid);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(storageUuid);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
