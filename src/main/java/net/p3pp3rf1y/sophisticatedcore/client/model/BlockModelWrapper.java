package net.p3pp3rf1y.sophisticatedcore.client.model;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.BlockModelGeometryBakingContext;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.IUnbakedGeometry;

import javax.annotation.Nullable;
import java.util.function.Function;

public class BlockModelWrapper extends BlockModel {
	protected final BlockModelGeometryBakingContext context;
	protected final IUnbakedGeometry wrapper;

	public BlockModelWrapper(BlockModel original) {
		this(original, null);
	}

	public BlockModelWrapper(BlockModel original, @Nullable IUnbakedGeometry wrapper) {
		super(original.parentLocation, original.getElements(), original.textureMap, original.hasAmbientOcclusion(), original.getGuiLight(), original.getTransforms(), original.getOverrides());
		this.name = original.name;
		this.context = new BlockModelGeometryBakingContext(this);
		this.wrapper = wrapper;
	}

	public IUnbakedGeometry getWrapper() {
		return wrapper;
	}

	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
		super.resolveParents(resolver);
		if (wrapper != null)
			wrapper.resolveParents(resolver, context);
	}

	@Override
	public BakedModel bake(ModelBaker baker, BlockModel model, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state, boolean guiLight3d) {
		if (wrapper != null) {
			ItemOverrides overrides = getOverrides().isEmpty() ? ItemOverrides.EMPTY : new ItemOverrides(baker, this, getOverrides());
			return wrapper.bake(context, baker, spriteGetter, state, overrides);
		}

		if (this.parent instanceof BlockModelWrapper wrapperParent) {
			return wrapperParent.bake(baker, wrapperParent, spriteGetter, state, guiLight3d);
		}

		return super.bake(baker, model, spriteGetter, state, guiLight3d);
	}
}
