package net.p3pp3rf1y.sophisticatedcore.extensions.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface SophisticatedTerrainParticle {
	default Particle sophisticatedCore$updateSprite(BlockState state, @Nullable BlockPos pos) {
		throw new RuntimeException("Should have been overridden");
	}
}
