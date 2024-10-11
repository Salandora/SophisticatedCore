package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.event.common.EntityEvents;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "addPlayer", at = @At("HEAD"), cancellable = true)
	public void sophisticatedcore$addEntityEvent(ServerPlayer serverPlayer, CallbackInfo ci) {
		if (EntityEvents.ON_JOIN_WORLD.invoker().onJoinWorld(serverPlayer, MixinHelper.cast(this), false))
			ci.cancel();
	}
}
