package net.p3pp3rf1y.sophisticatedcore.client.init;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import static net.p3pp3rf1y.sophisticatedcore.init.ModFluids.XP_FLOWING;
import static net.p3pp3rf1y.sophisticatedcore.init.ModFluids.XP_STILL;

public class ModFluids {
	public static final ResourceLocation XP_STILL_TEXTURE = SophisticatedCore.getRL("block/xp_still");
	public static final ResourceLocation XP_FLOWING_TEXTURE = SophisticatedCore.getRL("block/xp_still");

    public static void registerFluids() {
        FluidRenderHandlerRegistry.INSTANCE.register(XP_STILL, XP_FLOWING, new SimpleFluidRenderHandler(XP_STILL_TEXTURE, XP_FLOWING_TEXTURE));

        ClientSpriteRegistryCallback.event(InventoryMenu.BLOCK_ATLAS).register((atlasTexture, registry) -> {
            registry.register(XP_STILL_TEXTURE);
            registry.register(XP_FLOWING_TEXTURE);
        });
    }
}
