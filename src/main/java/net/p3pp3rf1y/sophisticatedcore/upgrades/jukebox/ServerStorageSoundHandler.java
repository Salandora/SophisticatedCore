package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class ServerStorageSoundHandler {
	private ServerStorageSoundHandler() {
	}

	private static final int KEEP_ALIVE_CHECK_INTERVAL = 10;
	private static final Map<ResourceKey<Level>, Long> lastWorldCheck = new HashMap<>();
	private static final Map<ResourceKey<Level>, Map<UUID, KeepAliveInfo>> worldStorageSoundKeepAlive = new HashMap<>();

	private static final List<SoundHandler> soundHandlers = Lists.newArrayList();

	public static void registerSoundHandler(SoundHandler handler) {
		soundHandlers.add(handler);
	}

	static {
		registerSoundHandler(new SoundHandler() {
			@Override
			public boolean play(ServerLevel level, BlockPos position, UUID storageUuid, ItemStack discItemStack) {
				PacketHelper.sendToAllNear(new PlayDiscPacket(storageUuid, Item.getId(discItemStack.getItem()), position), level, Vec3.atCenterOf(position), 128);
				return true;
			}

			@Override
			public boolean play(ServerLevel level, Vec3 position, UUID storageUuid, int entityId, ItemStack discItemStack) {
				PacketHelper.sendToAllNear(new PlayDiscPacket(storageUuid, Item.getId(discItemStack.getItem()), entityId), level, position, 128);
				return true;
			}

			@Override
			public void stop(ServerLevel level, Vec3 position, UUID storageUuid) {
				sendStopMessage(level, position, storageUuid);
			}

			@Override
			public void update(UUID storageUuid, Vec3 position) {
				// noop
			}
		});
	}

	public static void tick(ServerLevel world) {
		if (world.isClientSide()) {
			return;
		}
		ResourceKey<Level> dim = world.dimension();
		if (lastWorldCheck.computeIfAbsent(dim, key -> world.getGameTime()) > world.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL || !worldStorageSoundKeepAlive.containsKey(dim)) {
			return;
		}
		lastWorldCheck.put(dim, world.getGameTime());

		worldStorageSoundKeepAlive.get(dim).entrySet().removeIf(entry -> {
			if (entry.getValue().getLastKeepAliveTime() < world.getGameTime() - KEEP_ALIVE_CHECK_INTERVAL) {
				entry.getValue().getSoundHandler().stop(world, entry.getValue().getLastPosition(), entry.getKey());
				return true;
			}
			return false;
		});
	}

	public static void updateKeepAlive(UUID storageUuid, Level level, Vec3 position, Runnable onNoLongerRunning) {
		ResourceKey<Level> dim = level.dimension();
		if (!worldStorageSoundKeepAlive.containsKey(dim) || !worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			onNoLongerRunning.run();
			return;
		}
		if (worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			worldStorageSoundKeepAlive.get(dim).get(storageUuid).update(level.getGameTime(), position, storageUuid);
		}
	}

	public static void onSoundStopped(Level level, UUID storageUuid) {
		removeKeepAliveInfo(level, storageUuid);
	}

	private static class KeepAliveInfo {
		private final WeakReference<Runnable> onStopHandler;
		private long lastKeepAliveTime;
		private Vec3 lastPosition;
		private final SoundHandler handler;

		private KeepAliveInfo(Runnable onStopHandler, long lastKeepAliveTime, Vec3 lastPosition, SoundHandler handler) {
			this.onStopHandler = new WeakReference<>(onStopHandler);
			this.lastKeepAliveTime = lastKeepAliveTime;
			this.lastPosition = lastPosition;
			this.handler = handler;
		}

		public long getLastKeepAliveTime() {
			return lastKeepAliveTime;
		}

		public Vec3 getLastPosition() {
			return lastPosition;
		}

		public SoundHandler getSoundHandler() {
			return handler;
		}

		public void update(long gameTime, Vec3 position, UUID storageUuid) {
			lastKeepAliveTime = gameTime;
			lastPosition = position;
			handler.update(storageUuid, position);
		}

		public void runOnStop() {
			Runnable handler = onStopHandler.get();
			if (handler != null) {
				handler.run();
			}
		}
	}

	private static void runSoundHandler(ServerLevel serverWorld, Vec3 position, UUID storageUuid, Runnable onStopHandler, Function<SoundHandler, Boolean> onHandler) {
		for (SoundHandler handler : Lists.reverse(soundHandlers)) {
			if (onHandler.apply(handler)) {
				putKeepAliveInfo(serverWorld, storageUuid, onStopHandler, position, handler);
				return;
			}
		}
	}

	public static void startPlayingDisc(ServerLevel serverWorld, BlockPos position, UUID storageUuid, ItemStack discItemStack, Runnable onStopHandler) {
		Vec3 pos = Vec3.atCenterOf(position);
		runSoundHandler(serverWorld, pos, storageUuid, onStopHandler, (handler) -> handler.play(serverWorld, position, storageUuid, discItemStack));
	}

	public static void startPlayingDisc(ServerLevel serverWorld, Vec3 position, UUID storageUuid, int entityId, ItemStack discItemStack, Runnable onStopHandler) {
		runSoundHandler(serverWorld, position, storageUuid, onStopHandler, (handler) -> handler.play(serverWorld, position, storageUuid, entityId, discItemStack));
	}

	private static void putKeepAliveInfo(ServerLevel serverLevel, UUID storageUuid, Runnable onStopHandler, Vec3 pos, SoundHandler handler) {
		worldStorageSoundKeepAlive.computeIfAbsent(serverLevel.dimension(), dim -> new HashMap<>()).put(storageUuid, new KeepAliveInfo(onStopHandler, serverLevel.getGameTime(), pos, handler));
	}

	public static void stopPlayingDisc(ServerLevel serverWorld, Vec3 position, UUID storageUuid) {
		ResourceKey<Level> dim = serverWorld.dimension();
		if (worldStorageSoundKeepAlive.containsKey(dim) && worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			worldStorageSoundKeepAlive.get(dim).get(storageUuid).getSoundHandler().stop(serverWorld, position, storageUuid);
		}
		removeKeepAliveInfo(serverWorld, storageUuid);
	}

	private static void removeKeepAliveInfo(Level level, UUID storageUuid) {
		ResourceKey<Level> dim = level.dimension();
		if (worldStorageSoundKeepAlive.containsKey(dim) && worldStorageSoundKeepAlive.get(dim).containsKey(storageUuid)) {
			worldStorageSoundKeepAlive.get(dim).remove(storageUuid).runOnStop();
		}
	}

	private static void sendStopMessage(ServerLevel serverWorld, Vec3 position, UUID storageUuid) {
		PacketHelper.sendToAllNear(new StopDiscPlaybackPacket(storageUuid), serverWorld, position, 128);
	}
}
