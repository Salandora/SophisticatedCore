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

public interface SophisticatedBlockState {
    // Helpers for accessing Item data
    private BlockState self()
    {
        return (BlockState)this;
    }

    default boolean sophisticatedCore_addLandingEffects(ServerLevel level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        return self().getBlock().sophisticatedCore_addLandingEffects(self(), level, pos, state2, entity, numberOfParticles);
    }

    default boolean sophisticatedCore_addRunningEffects(Level level, BlockPos pos, Entity entity) {
        return self().getBlock().sophisticatedCore_addRunningEffects(self(), level, pos, entity);
    }

	@Environment(EnvType.CLIENT)
    default boolean sophisticatedCore_addHitEffects(Level level, HitResult target, ParticleEngine manager) {
        return self().getBlock().sophisticatedCore_addHitEffects(self(), level, target, manager);
    }

	@Environment(EnvType.CLIENT)
    default boolean sophisticatedCore_addDestroyEffects(Level level, BlockPos pos, ParticleEngine manager) {
        return self().getBlock().sophisticatedCore_addDestroyEffects(self(), level, pos, manager);
    }
}
