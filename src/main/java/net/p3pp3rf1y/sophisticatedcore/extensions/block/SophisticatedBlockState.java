package net.p3pp3rf1y.sophisticatedcore.extensions.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface SophisticatedBlockState {
    // Helpers for accessing Item data
    private BlockState self()
    {
        return (BlockState)this;
    }

    default boolean addLandingEffects(ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        return self().getBlock().addLandingEffects(self(), level, pos, state2, entity, numberOfParticles);
    }

    default boolean addRunningEffects(Level level, BlockPos pos, Entity entity) {
        return self().getBlock().addRunningEffects(self(), level, pos, entity);
    }

	@Environment(EnvType.CLIENT)
    default boolean addHitEffects(Level level, HitResult target, ParticleEngine manager) {
        return self().getBlock().addHitEffects(self(), level, target, manager);
    }

	@Environment(EnvType.CLIENT)
    default boolean addDestroyEffects(Level level, BlockPos pos, ParticleEngine manager) {
        return self().getBlock().addDestroyEffects(self(), level, pos, manager);
    }
}
