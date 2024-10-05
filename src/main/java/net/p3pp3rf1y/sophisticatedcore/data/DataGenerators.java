package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

@SuppressWarnings("unused")
public class DataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
		final FabricDataGenerator.Pack pack = generator.createPack();

		SCBlockTagProvider blockTags = pack.addProvider(SCBlockTagProvider::new);
		pack.addProvider((output, wrapper) -> new SCItemTagProvider(output, wrapper, blockTags));
		pack.addProvider(SCFluidTagsProvider::new);
		pack.addProvider(SCRecipeProvider::new);
	}
}
