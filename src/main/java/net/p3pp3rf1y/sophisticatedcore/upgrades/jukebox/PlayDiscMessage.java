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

	public PlayDiscMessage(FriendlyByteBuf buffer) {
		this.blockStorage = buffer.readBoolean();
		this.storageUuid = buffer.readUUID();
		this.musicDiscItemId = buffer.readInt();
		if (blockStorage) {
			this.pos = buffer.readBlockPos();
		} else {
			this.entityId = buffer.readInt();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(this.blockStorage);
		buffer.writeUUID(this.storageUuid);
		buffer.writeInt(this.musicDiscItemId);
		if (this.blockStorage) {
			buffer.writeBlockPos(this.pos);
		} else {
			buffer.writeInt(this.entityId);
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
