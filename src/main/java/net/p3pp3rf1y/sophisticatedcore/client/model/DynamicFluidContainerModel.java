package net.p3pp3rf1y.sophisticatedcore.client.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.GeometryBakingContext;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.IGeometryBakingContext;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.IGeometryLoader;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.IUnbakedGeometry;
import net.p3pp3rf1y.sophisticatedcore.fluid.FluidUtil;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class DynamicFluidContainerModel implements IUnbakedGeometry {
	private static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());

	private final Fluid fluid;
	private DynamicFluidContainerModel(Fluid fluid) {
		this.fluid = fluid;
	}

	public DynamicFluidContainerModel withFluid(Fluid newFluid) {
		return new DynamicFluidContainerModel(newFluid);
	}

	public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
		Material particleLocation = context.hasTexture("particle") ? context.getMaterial("particle") : null;
		Material baseLocation = context.hasTexture("base") ? context.getMaterial("base") : null;
		Material fluidMaskLocation = context.hasTexture("fluid") ? context.getMaterial("fluid") : null;

		TextureAtlasSprite baseSprite = baseLocation != null ? spriteGetter.apply(baseLocation) : null;
		TextureAtlasSprite templateSprite = fluidMaskLocation != null ? spriteGetter.apply(fluidMaskLocation) : null;

		TextureAtlasSprite particleSprite = particleLocation != null ? spriteGetter.apply(particleLocation) : null;

		var itemContext = GeometryBakingContext.builder(context)
				.withGui3d(false)
				.withUseBlockLight(false)
				.build();
		var overrideHandler = new ContainedFluidOverrideHandler(overrides, baker, itemContext, this);

		// It is necessary to use a LazyBakedModel here because fluid textures are not loaded yet on game start
		// and would lead to fluid containers without fluids.
		return new LazyBakedModel(itemContext, baseSprite, templateSprite, particleSprite, modelState, overrideHandler);
	}

	public final class LazyBakedModel implements BakedModel {
		private final IGeometryBakingContext itemContext;
		private final TextureAtlasSprite baseSprite;
		private final TextureAtlasSprite templateSprite;
		private final TextureAtlasSprite particleSprite;
		private final ModelState modelState;
		private final ItemOverrides overrides;

		private BakedModel compositeModel;

		private LazyBakedModel(IGeometryBakingContext itemContext, TextureAtlasSprite baseSprite, TextureAtlasSprite templateSprite, TextureAtlasSprite particleSprite, ModelState modelState, ItemOverrides overrides) {
			this.itemContext = itemContext;
			this.baseSprite = baseSprite;
			this.templateSprite = templateSprite;
			this.particleSprite = particleSprite;
			this.modelState = modelState;
			this.overrides = overrides;
		}

		private BakedModel wrapped() {
			if (compositeModel == null) {
				compositeModel = initializeWrappedModel();
			}

			return compositeModel;
		}

		private BakedModel initializeWrappedModel() {
			ModelState modelState = this.modelState;

			// Initializer must be in the if statement to make it usable in lambdas
			TextureAtlasSprite fluidSprite;
			if (fluid != Fluids.EMPTY) {
				fluidSprite = FluidVariantRendering.getSprite(FluidVariant.of(fluid));
			} else {
				fluidSprite = null;
			}

			var modelBuilder = CompositeModel.Baked.builder(itemContext, particleSprite, overrides, itemContext.getTransforms());

			TextureAtlasSprite particleSprite = this.particleSprite;
			if (particleSprite == null) particleSprite = fluidSprite;
			if (particleSprite == null) particleSprite = baseSprite;

			if (baseSprite != null) {
				// Base texture
				var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite.contents());
				var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> baseSprite, modelState);
				modelBuilder.addQuads(RenderType.translucent(), quads);
			}

			if (templateSprite != null && fluidSprite != null) {
				// Fluid layer
				var transformedState = new SimpleModelState(modelState.getRotation().compose(FLUID_TRANSFORM), modelState.isUvLocked());
				var unbaked = UnbakedGeometryHelper.createUnbakedItemMaskElements(1, templateSprite.contents()); // Use template as mask
				var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> fluidSprite, transformedState); // Bake with fluid texture

				modelBuilder.addQuads(RenderType.solid(), quads);
			}

			modelBuilder.setParticle(particleSprite);

			return modelBuilder.build();
		}

		@Override
		public boolean isVanillaAdapter() {
			// Need to use fabrics rendering api because with the getQuads function water textures still break the rendering
			return false;
		}

		@Override
		public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
			wrapped().emitBlockQuads(blockView, state, pos, randomSupplier, context);
		}

		@Override
		public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
			wrapped().emitItemQuads(stack, randomSupplier, context);
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
			return List.of();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return wrapped().useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return wrapped().isGui3d();
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return wrapped().getParticleIcon();
		}

		@Override
		public boolean usesBlockLight() {
			return wrapped().usesBlockLight();
		}

		@Override
		public ItemTransforms getTransforms() {
			return wrapped().getTransforms();
		}

		@Override
		public ItemOverrides getOverrides() {
			return wrapped().getOverrides();
		}
	}

	public static final class Loader implements IGeometryLoader<DynamicFluidContainerModel> {
		public static final Loader INSTANCE = new Loader();

		private Loader() {}

		@Override
		public DynamicFluidContainerModel read(JsonObject jsonObject) {
			if (!jsonObject.has("fluid"))
				throw new RuntimeException("Bucket model requires 'fluid' value.");

			ResourceLocation fluidName = ResourceLocation.parse(jsonObject.get("fluid").getAsString());

			Fluid fluid = BuiltInRegistries.FLUID.get(fluidName);

			// create new model with correct liquid
			return new DynamicFluidContainerModel(fluid);
		}
	}

	private static final class ContainedFluidOverrideHandler extends ItemOverrides {
		private final Map<String, BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change
		private final ItemOverrides nested;
		private final ModelBaker baker;
		private final IGeometryBakingContext owner;
		private final DynamicFluidContainerModel parent;

		private ContainedFluidOverrideHandler(ItemOverrides nested, ModelBaker baker, IGeometryBakingContext owner, DynamicFluidContainerModel parent) {
			this.nested = nested;
			this.baker = baker;
			this.owner = owner;
			this.parent = parent;
		}

		@Override
		public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			BakedModel overridden = nested.resolve(originalModel, stack, level, entity, seed);
			if (overridden != originalModel) return overridden;
			return FluidUtil.getFluidContained(stack)
					.map(f -> {
						Fluid fluid = f.resource().getFluid();
						String name = BuiltInRegistries.FLUID.getKey(fluid).toString();

						if (!cache.containsKey(name)) {
							DynamicFluidContainerModel unbaked = this.parent.withFluid(fluid);
							BakedModel bakedModel = unbaked.bake(owner, baker, Material::sprite, BlockModelRotation.X0_Y0, this);
							cache.put(name, bakedModel);
							return bakedModel;
						}

						return cache.get(name);
					})
					// not a fluid item apparently
					.orElse(originalModel); // empty bucket
		}
	}
}
