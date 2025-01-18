package net.p3pp3rf1y.sophisticatedcore.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.util.model.ModelData;

public interface CustomParticleIcon {
    default ModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData tileData) { return ModelData.EMPTY;}
    default TextureAtlasSprite getParticleIcon(ModelData data) {
        return ((BakedModel) this).getParticleIcon();
    }
}
