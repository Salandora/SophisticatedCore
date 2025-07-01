package net.p3pp3rf1y.sophisticatedcore.api.client.model.loading;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;

import java.util.function.Function;

public class GeometryBakingContext implements IGeometryBakingContext {
	private final boolean useAmbientOcclusion;
	private final boolean isGui3d;
	private final boolean useBlockLight;
	private final ItemTransforms itemTransforms;
	private final Function<String, Boolean> hasTexture;
	private final Function<String, Material> getMaterial;
	private final Transformation rootTransformation;

	public GeometryBakingContext(boolean useAmbientOcclusion, boolean isGui3d, boolean useBlockLight, ItemTransforms itemTransforms, Function<String, Boolean> hasTexture, Function<String, Material> getMaterial, Transformation rootTransformation) {
		this.useAmbientOcclusion = useAmbientOcclusion;
		this.isGui3d = isGui3d;
		this.useBlockLight = useBlockLight;
		this.itemTransforms = itemTransforms;
		this.hasTexture = hasTexture;
		this.getMaterial = getMaterial;
		this.rootTransformation = rootTransformation;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return useAmbientOcclusion;
	}

	@Override
	public boolean isGui3d() {
		return isGui3d;
	}

	@Override
	public boolean useBlockLight() {
		return useBlockLight;
	}

	@Override
	public ItemTransforms getTransforms() {
		return itemTransforms;
	}

	@Override
	public boolean hasTexture(String textureName) {
		return hasTexture.apply(textureName);
	}

	@Override
	public Material getMaterial(String name) {
		return getMaterial.apply(name);
	}

	@Override
	public Transformation getRootTransform() {
		return rootTransformation;
	}

	public static Builder builder(IGeometryBakingContext parent) {
		return new Builder(parent);
	}

	public static final class Builder {
		private boolean useAmbientOcclusion;
		private boolean isGui3d;
		private boolean useBlockLight;
		private ItemTransforms itemTransforms;
		private Function<String, Boolean> hasTexture;
		private Function<String, Material> getMaterial;
		private Transformation rootTransformation;

		private Builder(IGeometryBakingContext parent) {
			this.useAmbientOcclusion = parent.useAmbientOcclusion();
			this.isGui3d = parent.isGui3d();
			this.useBlockLight = parent.useBlockLight();
			this.itemTransforms = parent.getTransforms();
			this.hasTexture = parent::hasTexture;
			this.getMaterial = parent::getMaterial;
			this.rootTransformation = parent.getRootTransform();
		}

		public Builder withUseAmbientOcclusion(boolean useAmbientOcclusion) {
			this.useAmbientOcclusion = useAmbientOcclusion;
			return this;
		}

		public Builder withGui3d(boolean isGui3d) {
			this.isGui3d = isGui3d;
			return this;
		}

		public Builder withUseBlockLight(boolean useBlockLight) {
			this.useBlockLight = useBlockLight;
			return this;
		}

		public Builder withItemTransforms(ItemTransforms itemTransforms) {
			this.itemTransforms = itemTransforms;
			return this;
		}

		public Builder withHasTexture(Function<String, Boolean> hasTexture) {
			this.hasTexture = hasTexture;
			return this;
		}

		public Builder withGetMaterial(Function<String, Material> getMaterial) {
			this.getMaterial = getMaterial;
			return this;
		}

		public Builder withRootTransformation(Transformation rootTransformation) {
			this.rootTransformation = rootTransformation;
			return this;
		}

		public IGeometryBakingContext build() {
			return new GeometryBakingContext(useAmbientOcclusion, isGui3d, useBlockLight, itemTransforms, hasTexture, getMaterial, rootTransformation);
		}
	}
}
