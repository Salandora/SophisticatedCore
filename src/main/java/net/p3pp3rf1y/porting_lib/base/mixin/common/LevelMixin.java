// Code from https://github.com/Fabricators-of-Create/Porting-Lib/blob/1.20.1/modules/base/src/main/java/io/github/fabricators_of_create/porting_lib/mixin/common/LevelMixin.java
// copied to not include all of "base" for this
package net.p3pp3rf1y.porting_lib.base.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.BlockEntityExtensions;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.LevelExtensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(value = Level.class, priority = 1100) // need to apply after lithium
public abstract class LevelMixin implements LevelAccessor, LevelExtensions {
	@Unique
	private final ArrayList<BlockEntity> sophisticatedCore$freshBlockEntities = new ArrayList<>();
	@Unique
	private final ArrayList<BlockEntity> sophisticatedCore$pendingFreshBlockEntities = new ArrayList<>();

	@Shadow
	private boolean tickingBlockEntities;

	@Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", shift = Shift.AFTER))
	public void port_lib$pendingBlockEntities(CallbackInfo ci) {
		if (!this.sophisticatedCore$pendingFreshBlockEntities.isEmpty()) {
			this.sophisticatedCore$freshBlockEntities.addAll(this.sophisticatedCore$pendingFreshBlockEntities);
			this.sophisticatedCore$pendingFreshBlockEntities.clear();
		}
	}

	@Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	public void port_lib$onBlockEntitiesLoad(CallbackInfo ci) {
		if (!this.sophisticatedCore$freshBlockEntities.isEmpty()) {
			this.sophisticatedCore$freshBlockEntities.forEach(BlockEntityExtensions::onLoad);
			this.sophisticatedCore$freshBlockEntities.clear();
		}
	}

	@Unique
	@Override
	public void addFreshBlockEntities(Collection<BlockEntity> beList) {
		if (this.tickingBlockEntities) {
			this.sophisticatedCore$pendingFreshBlockEntities.addAll(beList);
		} else {
			this.sophisticatedCore$freshBlockEntities.addAll(beList);
		}
	}
}
