package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.extensions.block.entity.SophisticatedBlockEntity;
import net.p3pp3rf1y.sophisticatedcore.extensions.world.SophisticatedLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(Level.class)
public abstract class LevelMixin  implements LevelAccessor, SophisticatedLevel {
	@Shadow private boolean tickingBlockEntities;

	@Unique
	private final ArrayList<BlockEntity> sophisticatedCore_freshBlockEntities = new ArrayList<>();
	@Unique
	private final ArrayList<BlockEntity> sophisticatedCore_pendingFreshBlockEntities = new ArrayList<>();

	@Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", shift = At.Shift.AFTER))
	public void port_lib$pendingBlockEntities(CallbackInfo ci) {
		if (!this.sophisticatedCore_pendingFreshBlockEntities.isEmpty()) {
			this.sophisticatedCore_freshBlockEntities.addAll(this.sophisticatedCore_pendingFreshBlockEntities);
			this.sophisticatedCore_pendingFreshBlockEntities.clear();
		}
	}

	@Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	public void port_lib$onBlockEntitiesLoad(CallbackInfo ci) {
		if (!this.sophisticatedCore_freshBlockEntities.isEmpty()) {
			this.sophisticatedCore_freshBlockEntities.forEach(SophisticatedBlockEntity::sophisticatedCore_onLoad);
			this.sophisticatedCore_freshBlockEntities.clear();
		}
	}

	@Unique
	@Override
	public void sophisticatedCore_addFreshBlockEntities(Collection<BlockEntity> beList) {
		if (this.tickingBlockEntities) {
			this.sophisticatedCore_pendingFreshBlockEntities.addAll(beList);
		} else {
			this.sophisticatedCore_freshBlockEntities.addAll(beList);
		}
	}
}
