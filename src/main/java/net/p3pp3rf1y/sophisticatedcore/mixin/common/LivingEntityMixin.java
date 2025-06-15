package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.event.common.LivingEntityEvents;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(value = LivingEntity.class, priority = 500)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    protected int lastHurtByPlayerTime;

    public LivingEntityMixin(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(
			method = "dropAllDeathLoot",
			at = @At("HEAD")
	)
    private void sophisticatedCore$captureDrops(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        sophisticatedCaptureDrops(new ArrayList<>());
    }

    @Inject(
			method = "dropAllDeathLoot",
			at = @At(value = "RETURN")
	)
    private void sophisticatedCore$dropCapturedDrops(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        Collection<ItemEntity> drops = this.sophisticatedCore_captureDrops(null);
        if (!LivingEntityEvents.DROPS.invoker().onLivingEntityDrops(MixinHelper.cast(this), damageSource, drops,lastHurtByPlayerTime > 0))
            drops.forEach(level::addFreshEntity);
    }

    @Inject(
			method = "tick",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/Entity;tick()V"
			)
	)
    private void sophisticatedCore$tick(CallbackInfo ci) {
        LivingEntityEvents.TICK.invoker().onLivingEntityTick(MixinHelper.cast(this));
    }

	@WrapOperation(
			method = "checkFallDamage",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"
			)
	)
	public <T extends ParticleOptions> int sophisticatedCore$addLandingEffects(ServerLevel level, T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, Operation<Integer> original, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) BlockPos pos) {
		if (!state.sophisticatedCore_addLandingEffects(level, pos, state, MixinHelper.cast(this), particleCount)) {
			return original.call(level, type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed);
		}
		return particleCount;
	}
}
