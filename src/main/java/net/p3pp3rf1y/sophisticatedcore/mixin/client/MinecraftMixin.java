package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
	@Nullable
	public HitResult hitResult;

    @Redirect(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V"))
    private void sophisticatedcore$addBlockHitEffects(ParticleEngine manager, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (!state.addHitEffects(level, this.hitResult, manager)) {
            manager.crack(pos, side);
        }
    }
}
