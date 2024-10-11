package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.client.render.CustomParticleIcon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

    @Redirect(method = "renderScreenEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockModelShaper;getParticleIcon(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private static TextureAtlasSprite sophisticatedcore$renderEffectScreen$getParticleIcon(BlockModelShaper instance, BlockState state, @Share("pos") LocalRef<BlockPos> pos) {
        if (instance.getBlockModel(state) instanceof CustomParticleIcon model && pos.get() != null) {
            Minecraft mc = Minecraft.getInstance();
			return model.getParticleIcon(state, mc.level, pos.get());
        }
        return instance.getParticleIcon(state);
    }

    @Inject(method = "getViewBlockingState", at = @At(value = "RETURN", ordinal = 0))
    private static void sophisticatedcore$getViewBlockingState(Player player, CallbackInfoReturnable<BlockState> cir, @Local BlockPos.MutableBlockPos mutableBlockPos, @Share("pos") LocalRef<BlockPos> pos) {
		pos.set(mutableBlockPos);
    }
}
