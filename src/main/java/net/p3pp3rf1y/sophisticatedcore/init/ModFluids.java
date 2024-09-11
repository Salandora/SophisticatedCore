package net.p3pp3rf1y.sophisticatedcore.init;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.fluid.XPFluid;

public class ModFluids {
	private ModFluids() {
	}

	public static final ResourceLocation EXPERIENCE_TAG_NAME = new ResourceLocation("c:experience");
	public static final TagKey<Fluid> EXPERIENCE_TAG = TagKey.create(Registries.FLUID, EXPERIENCE_TAG_NAME);

	public static final FlowingFluid XP_STILL = register("xp_still", new XPFluid.Still());
	public static final FlowingFluid XP_FLOWING = register("xp_flowing", new XPFluid.Flowing());


	public static <T extends Fluid> T register(String id, T value) {
		return Registry.register(BuiltInRegistries.FLUID, SophisticatedCore.getRL(id), value);
	}

	public static void registerHandlers() {
		FluidVariantAttributes.register(XP_STILL, new FluidVariantAttributeHandler() {
			@Override
			public Component getName(FluidVariant fluidVariant) {
				return Component.translatable(Util.makeDescriptionId("fluid", BuiltInRegistries.FLUID.getKey(fluidVariant.getFluid())));
			}
		});
	}
}
