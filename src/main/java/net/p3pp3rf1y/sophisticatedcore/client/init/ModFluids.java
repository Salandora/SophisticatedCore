package net.p3pp3rf1y.sophisticatedcore.client.init;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import static net.p3pp3rf1y.sophisticatedcore.init.ModFluids.XP_FLOWING;
import static net.p3pp3rf1y.sophisticatedcore.init.ModFluids.XP_STILL;

public class ModFluids {
    public static void registerFluids() {
        FluidRenderHandlerRegistry.INSTANCE.register(XP_STILL, XP_FLOWING, new SimpleFluidRenderHandler(
                new ResourceLocation(SophisticatedCore.ID, "block/xp_still"),
                new ResourceLocation(SophisticatedCore.ID, "block/xp_flowing")
        ));
    }
}
