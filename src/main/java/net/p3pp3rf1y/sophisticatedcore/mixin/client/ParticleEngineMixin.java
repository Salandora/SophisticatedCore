package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Shadow
    protected ClientLevel level;

	// TODO: Fix
/*    @Redirect(method = "destroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;shouldSpawnParticlesOnBreak()Z"))
    private boolean sophisticatedcore$addDestroyEffects(BlockState blockState, BlockPos pos) {
        return !blockState.addDestroyEffects(level, pos, MixinHelper.cast(this));
    }*/
}
