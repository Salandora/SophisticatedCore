package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.client.render.CustomParticleIcon;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Unique
    private static BlockPos sophisticatedcore$pos = null;

    @Redirect(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;getParticleIcon(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private static TextureAtlasSprite sophisticatedcore$renderEffectScreen$getParticleIcon(BlockModelShaper instance, BlockState state) {
        if (instance.getBlockModel(state) instanceof CustomParticleIcon model && sophisticatedcore$pos != null) {
            Minecraft mc = Minecraft.getInstance();
            TextureAtlasSprite sprite = model.getParticleIcon(state, mc.level, sophisticatedcore$pos);
            sophisticatedcore$pos = null;
            return sprite;
        }
        return instance.getParticleIcon(state);
    }

    @Inject(method = "getViewBlockingState", at = @At(value = "RETURN", ordinal = 0))
    private static void sophisticatedcore$getViewBlockingState(Player player, CallbackInfoReturnable<BlockState> cir, @Local BlockPos.MutableBlockPos mutableBlockPos) {
        sophisticatedcore$pos = mutableBlockPos.immutable();
    }
}
