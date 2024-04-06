package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.client.render.CustomParticleIcon;
import net.p3pp3rf1y.sophisticatedcore.extensions.client.particle.SophisticatedTerrainParticle;

import org.jetbrains.annotations.Nullable;

@Mixin(TerrainParticle.class)
public abstract class TerrainParticleMixin extends TextureSheetParticle implements SophisticatedTerrainParticle {
	protected TerrainParticleMixin(ClientLevel clientLevel, double d, double e, double f) {
		super(clientLevel, d, e, f);
	}

	@Override
	public Particle sophisticatedCore$updateSprite(BlockState state, @Nullable BlockPos pos) {
		if (pos != null) {
			BlockModelShaper shaper = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper();
			BakedModel model = shaper.getBlockModel(state);
			if (model instanceof CustomParticleIcon sophModel) {
				this.setSprite(sophModel.getParticleIcon(state, level, pos));
			}
		}

		return this;
	}
}
