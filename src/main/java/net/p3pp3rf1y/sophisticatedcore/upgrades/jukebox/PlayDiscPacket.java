package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.UUID;

public class PlayDiscPacket implements FabricPacket {
	public static final PacketType<PlayDiscPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "play_disc"), PlayDiscPacket::read);
	private final boolean blockStorage;
	private final UUID storageUuid;
	private final int musicDiscItemId;
	private int entityId;
	private BlockPos pos;

	public PlayDiscPacket(UUID storageUuid, int musicDiscItemId, BlockPos pos) {
		blockStorage = true;
		this.storageUuid = storageUuid;
		this.musicDiscItemId = musicDiscItemId;
		this.pos = pos;
	}

	public PlayDiscPacket(UUID storageUuid, int musicDiscItemId, int entityId) {
		blockStorage = false;
		this.storageUuid = storageUuid;
		this.musicDiscItemId = musicDiscItemId;
		this.entityId = entityId;
	}

	public static PlayDiscPacket read(FriendlyByteBuf buffer) {
		if (buffer.readBoolean()) {
			return new PlayDiscPacket(buffer.readUUID(), buffer.readInt(), buffer.readBlockPos());
		}
		return new PlayDiscPacket(buffer.readUUID(), buffer.readInt(), buffer.readInt());
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		Item discItem = Item.byId(musicDiscItemId);
		if (!(discItem instanceof RecordItem)) {
			return;
		}

		SoundEvent soundEvent = ((RecordItem) discItem).getSound();
		if (soundEvent == null) {
			return;
		}

		if (blockStorage) {
			StorageSoundHandler.playStorageSound(soundEvent, storageUuid, pos);
		} else {
			StorageSoundHandler.playStorageSound(soundEvent, storageUuid, entityId);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(blockStorage);
		buffer.writeUUID(storageUuid);
		buffer.writeInt(musicDiscItemId);
		if (blockStorage) {
			buffer.writeBlockPos(pos);
		} else {
			buffer.writeInt(entityId);
		}
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
