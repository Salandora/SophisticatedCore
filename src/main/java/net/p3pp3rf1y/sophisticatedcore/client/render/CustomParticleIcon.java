package net.p3pp3rf1y.sophisticatedcore.client.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;

public interface CustomParticleIcon {
    default TextureAtlasSprite getParticleIcon(Object modelData) {
        return ((BakedModel) this).getParticleIcon();
    }
}
