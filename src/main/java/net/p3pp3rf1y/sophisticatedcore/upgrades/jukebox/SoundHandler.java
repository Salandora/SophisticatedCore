package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface SoundHandler {
	boolean play(ServerLevel level, BlockPos position, UUID storageUuid, ItemStack discItemStack);
	boolean play(ServerLevel level, Vec3 position, UUID storageUuid, int entityId, ItemStack discItemStack);
	void stop(ServerLevel level, Vec3 position, UUID storageUuid);
	void update(UUID storageUuid, Vec3 position);
}
