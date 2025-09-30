package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.github.salandora.sophisticatedlibrary.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StorageSoundHandler {
	private static final int SOUND_STOP_CHECK_INTERVAL = 10;

	private StorageSoundHandler() {}

	private static final Map<UUID, SoundInstance> storageSounds = new ConcurrentHashMap<>();
	private static long lastPlaybackChecked = 0;

	public static void playStorageSound(UUID storageUuid, SoundInstance sound) {
		stopStorageSound(storageUuid);
		storageSounds.put(storageUuid, sound);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	public static void stopStorageSound(UUID storageUuid) {
		if (storageSounds.containsKey(storageUuid)) {
			Minecraft.getInstance().getSoundManager().stop(storageSounds.remove(storageUuid));
		}
	}

	public static void tick(ClientLevel level) {
		if (!storageSounds.isEmpty() && lastPlaybackChecked < level.getGameTime() - SOUND_STOP_CHECK_INTERVAL) {
			lastPlaybackChecked = level.getGameTime();
			storageSounds.entrySet().removeIf(entry -> {
				if (!Minecraft.getInstance().getSoundManager().isActive(entry.getValue())) {
					PacketDistributor.sendToServer(new SoundFinishedNotificationPayload(entry.getKey()));
					return true;
				}
				return false;
			});
		}
	}

	public static void playStorageSound(SoundEvent soundEvent, UUID storageUuid, BlockPos pos) {
		playStorageSound(storageUuid, SimpleSoundInstance.forJukeboxSong(soundEvent, Vec3.atCenterOf(pos)));
	}

	public static void playStorageSound(SoundEvent soundEvent, UUID storageUuid, int entityId) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}

		Entity entity = level.getEntity(entityId);
		if (!(entity instanceof Entity)) {
			stopStorageSound(storageUuid);
			return;
		}
		playStorageSound(storageUuid, new EntityBoundSoundInstance(soundEvent, SoundSource.RECORDS, 2, 1, entity, level.random.nextLong()){
			@Override
			public void tick() {
				super.tick();
				if (entity instanceof Player player) {
					Vec3 lookAngle = player.getLookAngle();
					this.x = player.getX() + lookAngle.x;
					this.y = player.getEyeY() + lookAngle.y;
					this.z = player.getZ() + lookAngle.z;
				}
			}
		});
	}

	public static void onWorldUnload(MinecraftServer minecraftServer, ServerLevel serverLevel) {
		storageSounds.clear();
		lastPlaybackChecked = 0;
	}
}
