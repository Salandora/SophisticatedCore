package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.p3pp3rf1y.sophisticatedcore.event.common.EntityEvents;

@Mixin(PersistentEntitySectionManager.class)
public class PersistentEntitySectionManagerMixin<T extends EntityAccess> {
	@Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
	public void sophisticatedcore$addEntityEvent(T entityAccess, boolean loadedFromDisk, CallbackInfoReturnable<Boolean> cir) {
		if (entityAccess instanceof Entity entity && EntityEvents.ON_JOIN_WORLD.invoker().onJoinWorld(entity, entity.level(), loadedFromDisk))
			cir.setReturnValue(false);
	}
}
