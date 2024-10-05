package net.p3pp3rf1y.sophisticatedcore.extensions.block;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface SophisticatedBlock {
    default boolean addLandingEffects(BlockState state1, ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
    {
        return false;
    }

    default boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        return false;
    }

	@Environment(EnvType.CLIENT)
    default boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) { return false; }

	@Environment(EnvType.CLIENT)
    default boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
		return !state.shouldSpawnTerrainParticles();
	}
}
