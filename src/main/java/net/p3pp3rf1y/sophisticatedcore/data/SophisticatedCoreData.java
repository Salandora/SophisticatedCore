package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

@SuppressWarnings("unused")
public class SophisticatedCoreData implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
		SCBlockTagProvider blockTags = generator.addProvider(SCBlockTagProvider::new);
		generator.addProvider(output -> new SCItemTagProvider(output, blockTags));
		generator.addProvider(SCFluidTagsProvider::new);
		generator.addProvider(SCRecipeProvider::new);
	}
}
