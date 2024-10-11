package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.extensions.entity.SophisticatedEntity;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;

import java.util.Collection;

@Mixin(Entity.class)
public class EntityMixin implements SophisticatedEntity {
	@Unique
	private static final String SOPHISTICATEDCOREDATA_NBT_KEY = "SophisticatedCoreData";

    @Shadow public Level level;

	@Unique
	private Collection<ItemEntity> sophisticatedCore$captureDrops = null;

	@WrapWithCondition(
			method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
			)
	)
	public boolean sophisticatedCore$captureDrops(Level level, Entity entity) {
		if (sophisticatedCaptureDrops() != null && entity instanceof ItemEntity item) {
			sophisticatedCaptureDrops().add(item);
			return false;
		}
		return true;
	}

	@Unique
	@Override
	public Collection<ItemEntity> sophisticatedCaptureDrops() {
		return this.sophisticatedCore$captureDrops;
	}

	@Unique
	@Override
	public Collection<ItemEntity> sophisticatedCaptureDrops(Collection<ItemEntity> value) {
		Collection<ItemEntity> ret = this.sophisticatedCore$captureDrops;
		this.sophisticatedCore$captureDrops = value;
		return ret;
	}

	@Unique
	private CompoundTag sophisticatedCore$customData;

    @Inject(method = "spawnSprintParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void sophisticatedCore$addRunningEffects(CallbackInfo ci, int i, int j, int k, BlockPos blockPos, BlockState blockState) {
        if (blockState.addRunningEffects(level, blockPos, MixinHelper.cast(this))) {
            ci.cancel();
        }
    }

	@Override
	public CompoundTag getSophisticatedCustomData() {
		if (this.sophisticatedCore$customData == null) {
			this.sophisticatedCore$customData = new CompoundTag();
		}
		return this.sophisticatedCore$customData;
	}

	@Inject(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
	public void sophisticatedCore$saveAdditionalData(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
		if (this.sophisticatedCore$customData != null && !this.sophisticatedCore$customData.isEmpty()) {
			compound.put(SOPHISTICATEDCOREDATA_NBT_KEY, this.sophisticatedCore$customData);
		}
	}

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V"))
	public void sophisticatedCore$readAdditionalData(CompoundTag compound, CallbackInfo ci) {
		if (compound.contains(SOPHISTICATEDCOREDATA_NBT_KEY)) {
			this.sophisticatedCore$customData = compound.getCompound(SOPHISTICATEDCOREDATA_NBT_KEY);
		}
	}
}
