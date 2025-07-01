package net.p3pp3rf1y.sophisticatedcore.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.IGeometryBakingContext;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class CompositeModel {
	public static class Baked implements BakedModel {
		private final boolean isAmbientOcclusion;
		private final boolean isGui3d;
		private final boolean isSideLit;
		private final TextureAtlasSprite particle;
		private final ItemOverrides overrides;
		private final ItemTransforms transforms;
		private final ImmutableMap<String, BakedModel> children;

		public Baked(boolean isGui3d, boolean isSideLit, boolean isAmbientOcclusion, TextureAtlasSprite particle, ItemTransforms transforms, ItemOverrides overrides, ImmutableMap<String, BakedModel> children) {
			this.children = children;
			this.isAmbientOcclusion = isAmbientOcclusion;
			this.isGui3d = isGui3d;
			this.isSideLit = isSideLit;
			this.particle = particle;
			this.overrides = overrides;
			this.transforms = transforms;
		}

		@Override
		public boolean isVanillaAdapter() {
			return false;
		}

		@Override
		public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
			for (Map.Entry<String, BakedModel> entry : children.entrySet()) {
				entry.getValue().emitBlockQuads(blockView, state, pos, randomSupplier, context);
			}
		}

		@Override
		public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
			for (Map.Entry<String, BakedModel> entry : children.entrySet()) {
				entry.getValue().emitItemQuads(stack, randomSupplier, context);
			}
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
			return List.of();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return isAmbientOcclusion;
		}

		@Override
		public boolean isGui3d() {
			return isGui3d;
		}

		@Override
		public boolean usesBlockLight() {
			return isSideLit;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return particle;
		}

		@Override
		public ItemOverrides getOverrides() {
			return overrides;
		}

		@Override
		public ItemTransforms getTransforms() {
			return transforms;
		}

		public static Builder builder(IGeometryBakingContext owner, TextureAtlasSprite particle, ItemOverrides overrides, ItemTransforms cameraTransforms) {
			return builder(owner.useAmbientOcclusion(), owner.isGui3d(), owner.useBlockLight(), particle, overrides, cameraTransforms);
		}

		public static Builder builder(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit, TextureAtlasSprite particle, ItemOverrides overrides, ItemTransforms cameraTransforms) {
			return new Builder(isAmbientOcclusion, isGui3d, isSideLit, particle, overrides, cameraTransforms);
		}

		protected static class SimpleModelBuilder {
			private final MeshBuilder builder;
			private final boolean hasAmbientOcclusion;
			private final boolean usesBlockLight;
			private final boolean isGui3d;
			private final ItemTransforms transforms;
			private final ItemOverrides overrides;
			private final TextureAtlasSprite particle;
			private final RenderMaterial material;

			private SimpleModelBuilder(boolean hasAmbientOcclusion1, boolean usesBlockLight, boolean isGui3d, ItemTransforms transforms, ItemOverrides overrides, TextureAtlasSprite particle, RenderType renderType) {
				this.builder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
				this.hasAmbientOcclusion = hasAmbientOcclusion1;
				this.usesBlockLight = usesBlockLight;
				this.isGui3d = isGui3d;
				this.transforms = transforms;
				this.overrides = overrides;
				this.particle = particle;
				this.material = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(BlendMode.fromRenderLayer(renderType)).find();
			}

			public SimpleModelBuilder addUnculledFace(BakedQuad quad) {
				builder.getEmitter().fromVanilla(quad, material, null).emit();
				return this;
			}

			public BakedModel build() {
				return new MeshBakedModel(builder.build(), hasAmbientOcclusion, usesBlockLight, isGui3d, particle, transforms, overrides);
			}
		}

		public static class Builder {
			private final boolean isAmbientOcclusion;
			private final boolean isGui3d;
			private final boolean isSideLit;
			private final List<BakedModel> children = new ArrayList<>();
			private final List<BakedQuad> quads = new ArrayList<>();
			private final ItemOverrides overrides;
			private final ItemTransforms transforms;
			private TextureAtlasSprite particle;
			private RenderType lastRenderType = null;

			private Builder(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit, TextureAtlasSprite particle, ItemOverrides overrides, ItemTransforms transforms) {
				this.isAmbientOcclusion = isAmbientOcclusion;
				this.isGui3d = isGui3d;
				this.isSideLit = isSideLit;
				this.particle = particle;
				this.overrides = overrides;
				this.transforms = transforms;
			}

			private void addLayer(RenderType renderType, List<BakedQuad> quads) {
				var modelBuilder = new SimpleModelBuilder(isAmbientOcclusion, isSideLit, isGui3d, transforms, overrides, particle, renderType);
				quads.forEach(modelBuilder::addUnculledFace);
				children.add(modelBuilder.build());
			}

			private void flushQuads(RenderType renderType) {
				if (!Objects.equals(renderType, lastRenderType)) {
					if (quads.size() > 0) {
						addLayer(lastRenderType, quads);
						quads.clear();
					}
					lastRenderType = renderType;
				}
			}

			public Builder setParticle(TextureAtlasSprite particleSprite) {
				this.particle = particleSprite;
				return this;
			}

			public Builder addQuads(RenderType renderType, Collection<BakedQuad> quadsToAdd) {
				flushQuads(renderType);
				quads.addAll(quadsToAdd);
				return this;
			}

			public BakedModel build() {
				if (quads.size() > 0) {
					addLayer(lastRenderType, quads);
				}
				var childrenBuilder = ImmutableMap.<String, BakedModel>builder();
				var itemPassesBuilder = ImmutableList.<BakedModel>builder();
				int i = 0;
				for (var model : this.children) {
					childrenBuilder.put("model_" + (i++), model);
					itemPassesBuilder.add(model);
				}
				return new Baked(isGui3d, isSideLit, isAmbientOcclusion, particle, transforms, overrides, childrenBuilder.build());
			}
		}
	}
}
