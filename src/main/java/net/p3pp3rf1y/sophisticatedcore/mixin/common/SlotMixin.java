package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.extensions.inventory.SophisticatedSlot;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotMixin implements SophisticatedSlot {
    @Unique
    private Pair<ResourceLocation, ResourceLocation> sophisticatedcore$background;

    @Inject(method = "getNoItemIcon", at = @At("HEAD"), cancellable = true)
    private void sophisticatedcore$background(CallbackInfoReturnable<Pair<ResourceLocation, ResourceLocation>> cir) {
        if (sophisticatedcore$background != null) {
            cir.setReturnValue(sophisticatedcore$background);
        }
    }

    @Override
    public Slot sophisticatedcore$setBackground(ResourceLocation atlas, ResourceLocation sprite) {
        this.sophisticatedcore$background = Pair.of(atlas, sprite);
        return MixinHelper.cast(this);
    }
}
