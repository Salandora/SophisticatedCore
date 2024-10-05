package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public class StopDiscPlaybackPacket implements FabricPacket {
	public static final PacketType<StopDiscPlaybackPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "stop_disc_playback"), StopDiscPlaybackPacket::new);
	private final UUID storageUuid;

	public StopDiscPlaybackPacket(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	public StopDiscPlaybackPacket(FriendlyByteBuf buffer) {
		this(buffer.readUUID());
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		StorageSoundHandler.stopStorageSound(storageUuid);
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
