package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.UUID;

public class PlayDiscMessage extends SimplePacketBase {
	private final boolean blockStorage;
	private final UUID storageUuid;
	private final int musicDiscItemId;
	private int entityId;
	private BlockPos pos;

	public PlayDiscMessage(UUID storageUuid, int musicDiscItemId, BlockPos pos) {
		blockStorage = true;
		this.storageUuid = storageUuid;
		this.musicDiscItemId = musicDiscItemId;
		this.pos = pos;
	}

	public PlayDiscMessage(UUID storageUuid, int musicDiscItemId, int entityId) {
		blockStorage = false;
		this.storageUuid = storageUuid;
		this.musicDiscItemId = musicDiscItemId;
		this.entityId = entityId;
	}

	@Override
	public void write(FriendlyByteBuf packetBuffer) {
		packetBuffer.writeBoolean(this.blockStorage);
		packetBuffer.writeUUID(this.storageUuid);
		packetBuffer.writeInt(this.musicDiscItemId);
		if (this.blockStorage) {
			packetBuffer.writeBlockPos(this.pos);
		} else {
			packetBuffer.writeInt(this.entityId);
		}
	}

	public PlayDiscMessage(FriendlyByteBuf packetBuffer) {
		this.blockStorage = packetBuffer.readBoolean();
		this.storageUuid = packetBuffer.readUUID();
		this.musicDiscItemId = packetBuffer.readInt();
		if (blockStorage) {
			this.pos = packetBuffer.readBlockPos();
		} else {
			this.entityId = packetBuffer.readInt();
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Item discItem = Item.byId(musicDiscItemId);
			if (!(discItem instanceof RecordItem)) {
				return;
			}

			SoundEvent soundEvent = ((RecordItem) discItem).getSound();
			if (soundEvent == null) {
				return;
			}

			UUID storageUuid1 = storageUuid;
			if (blockStorage) {
				StorageSoundHandler.playStorageSound(soundEvent, storageUuid1, pos);
			} else {
				StorageSoundHandler.playStorageSound(soundEvent, storageUuid1, entityId);
			}
		});
		return true;
	}
}
