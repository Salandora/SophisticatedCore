package net.p3pp3rf1y.sophisticatedcore.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.p3pp3rf1y.sophisticatedcore.util.model.ModelData;

public interface CustomParticleIcon {
    default TextureAtlasSprite getParticleIcon(ModelData data) {
        return ((BakedModel) this).getParticleIcon();
    }
}
