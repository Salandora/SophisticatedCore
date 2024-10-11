package net.p3pp3rf1y.sophisticatedcore.compat.audioplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.SoundHandler;
import de.maxhenkel.audioplayer.AudioPlayer;
import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.PlayerType;
import de.maxhenkel.audioplayer.Plugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.plugins.impl.PositionImpl;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AudioPlayerSoundHandler implements SoundHandler {
	private static final int SOUND_STOP_CHECK_INTERVAL = 10;

	private static long lastPlaybackChecked = 0;
	private static final Map<UUID, UUID> storageUUIDToChannelUUID = new HashMap<>();

	@Nullable
	public static UUID play(ServerLevel level, Vec3 pos, PlayerType type, CustomSound sound) {
		float range = sound.getRange(type);

		VoicechatServerApi api = Plugin.voicechatServerApi;
		if (api == null) {
			return null;
		}

		@Nullable UUID channelID;
		if (sound.isStaticSound() && AudioPlayer.SERVER_CONFIG.allowStaticAudio.get()) {
			channelID = SCPlayerManager.instance().playStatic(
					api,
					level,
					pos,
					sound.getSoundId(),
					null,
					range,
					type.getCategory(),
					type.getMaxDuration().get()
			);
		} else {
			channelID = SCPlayerManager.instance().playLocational(
					api,
					level,
					pos,
					sound.getSoundId(),
					null,
					range,
					type.getCategory(),
					type.getMaxDuration().get()
			);
		}

		return channelID;
	}

	@Override
	public boolean play(ServerLevel level, BlockPos position, UUID storageUuid, ItemStack discItemStack) {
		CustomSound customSound = CustomSound.of(discItemStack);
		if (customSound != null) {
			Vec3 pos = Vec3.atCenterOf(position);
			UUID channel = play(level, pos, PlayerType.MUSIC_DISC, customSound);
			if (channel != null) {
				storageUUIDToChannelUUID.put(storageUuid, channel);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean play(ServerLevel level, Vec3 position, UUID storageUuid, int entityId, ItemStack discItemStack) {
		CustomSound customSound = CustomSound.of(discItemStack);
		if (customSound != null) {
			UUID channel = play(level, position, PlayerType.MUSIC_DISC, customSound);
			if (channel != null) {
				storageUUIDToChannelUUID.put(storageUuid, channel);
				return true;
			}
		}
		return false;
	}

	@Override
	public void stop(ServerLevel level, Vec3 position, UUID storageUuid) {
		if (!storageUUIDToChannelUUID.containsKey(storageUuid)) {
			return;
		}

		UUID channelID = storageUUIDToChannelUUID.remove(storageUuid);
		SCPlayerManager.instance().stop(channelID);
	}

	@Override
	public void update(UUID storageUuid, Vec3 position) {
		if (!storageUUIDToChannelUUID.containsKey(storageUuid)) {
			return;
		}

		UUID channelID = storageUUIDToChannelUUID.get(storageUuid);
		LocationalAudioChannel channel = SCPlayerManager.instance().getAudioChannel(channelID);
		if (channel != null) {
			channel.updateLocation(new PositionImpl(position.x, position.y, position.z));
		}
	}

	public static void tick(ServerLevel level) {
		if (!storageUUIDToChannelUUID.isEmpty() && lastPlaybackChecked < level.getGameTime() - SOUND_STOP_CHECK_INTERVAL) {
			lastPlaybackChecked = level.getGameTime();
			storageUUIDToChannelUUID.entrySet().removeIf(entry -> {
				if (!SCPlayerManager.instance().isPlaying(entry.getValue())) {
					ServerStorageSoundHandler.onSoundStopped(level, entry.getKey());
					return true;
				}
				return false;
			});
		}
	}
}
