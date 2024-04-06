package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Shadow
    protected ClientLevel level;

    @Redirect(method = "destroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean sophisticatedcore$addDestroyEffects(BlockState blockState, BlockPos pos) {
        return !blockState.addDestroyEffects(level, pos, MixinHelper.cast(this));
    }

	// lambda inside destroy
	@ModifyArgs(method = "method_34020", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;add(Lnet/minecraft/client/particle/Particle;)V"))
	private void sophisticatedcore$updateSprite(Args args, BlockPos pos, BlockState state, double i, double j, double k, double l, double m, double n) {
		Particle p = args.get(0);
		if (p instanceof TerrainParticle tp) {
			tp.sophisticatedCore$updateSprite(state, pos);
		}
	}
}
