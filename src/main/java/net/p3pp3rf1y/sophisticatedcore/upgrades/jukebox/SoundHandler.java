package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public interface SoundHandler {
	boolean play(Level level, BlockPos position, UUID storageUuid, ItemStack discItemStack, Holder<JukeboxSong> song);
	boolean play(Level level, Vec3 position, UUID storageUuid, int entityId, ItemStack discItemStack, Holder<JukeboxSong> song);
	void stop(Level level, Vec3 position, UUID storageUuid);
	void update(UUID storageUuid, Vec3 position);
}
