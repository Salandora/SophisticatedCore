package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.client.render.CustomParticleIcon;
import net.p3pp3rf1y.sophisticatedcore.extensions.client.particle.SophisticatedTerrainParticle;
import net.p3pp3rf1y.sophisticatedcore.util.model.ModelData;
import org.spongepowered.asm.mixin.Mixin;

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
			if (shaper.getBlockModel(state) instanceof CustomParticleIcon model) {
				ModelData data = model.getModelData(Minecraft.getInstance().level, pos, state, ModelData.EMPTY);
				this.setSprite(model.getParticleIcon(data));
			}
		}

		return this;
	}
}
