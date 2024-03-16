package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;

public class SCFluidTagsProvider extends FabricTagProvider.FluidTagProvider {
	public SCFluidTagsProvider(FabricDataGenerator output) {
		super(output);
	}

	@Override
	protected void generateTags() {
		getOrCreateTagBuilder(ModFluids.EXPERIENCE_TAG).add(ModFluids.XP_STILL);
	}
}
