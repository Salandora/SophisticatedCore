package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.event.common.ItemEntityEvents;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getItem();

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I"), cancellable = true)
    private void sophisticatedcore$playerTouch(Player player, CallbackInfo ci, @Share("cachedStack") LocalRef<ItemStack> cachedStack) {
        cachedStack.set(getItem().copy());
        var canPickup = ItemEntityEvents.CAN_PICKUP.invoker().canPickup(player, MixinHelper.cast(this), getItem());
        if (canPickup != InteractionResult.PASS) {
            ci.cancel();
        }
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"))
    private void sophisticatedcore$playerTouchPickup(Player player, CallbackInfo ci, @Share("cachedStack") LocalRef<ItemStack> cachedStack) {
        if (cachedStack.get() != null) {
            ItemEntityEvents.POST_PICKUP.invoker().postPickup(player, MixinHelper.cast(this), cachedStack.get());
        }
    }

}
