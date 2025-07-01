package net.p3pp3rf1y.sophisticatedcore.api.client.model.loading;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;

public class BlockModelGeometryBakingContext implements IGeometryBakingContext {
	private final BlockModel parent;

	public BlockModelGeometryBakingContext(BlockModel parent) {
		this.parent = parent;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return parent.hasAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean useBlockLight() {
		return parent.getGuiLight().lightLikeBlock();
	}

	@Override
	public ItemTransforms getTransforms() {
		return parent.getTransforms();
	}

	@Override
	public boolean hasTexture(String textureName) {
		return parent.hasTexture(textureName);
	}

	@Override
	public Material getMaterial(String name) {
		return parent.getMaterial(name);
	}

	@Override
	public Transformation getRootTransform() {
		return Transformation.identity();
	}
}
