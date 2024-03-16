package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.extensions.inventory.SophisticatedSlot;

@Mixin(Slot.class)
public class SlotMixin implements SophisticatedSlot {
    @Unique
    private Pair<ResourceLocation, ResourceLocation> background;

    @Inject(method = "getNoItemIcon", at = @At("HEAD"), cancellable = true)
    private void sophisticatedcore$background(CallbackInfoReturnable<Pair<ResourceLocation, ResourceLocation>> cir) {
        if (background != null) {
            cir.setReturnValue(background);
        }
    }

/*    @Override
    public Slot setBackground(ResourceLocation atlas, ResourceLocation sprite) {
        this.background = Pair.of(atlas, sprite);
        return MixinHelper.cast(this);
    }*/
}
