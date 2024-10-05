package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

import java.util.concurrent.CompletableFuture;

public class SCFluidTagsProvider extends FabricTagProvider.FluidTagProvider {
	public SCFluidTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
		super(output, completableFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider arg) {
		getOrCreateTagBuilder(ModFluids.EXPERIENCE_TAG).add(ModFluids.XP_STILL);
	}
}
