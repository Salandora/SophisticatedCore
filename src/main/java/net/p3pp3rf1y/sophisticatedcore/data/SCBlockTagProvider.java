package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.world.level.block.Blocks;
import net.p3pp3rf1y.sophisticatedcore.api.Tags;

public class SCBlockTagProvider extends FabricTagProvider.BlockTagProvider {
	public SCBlockTagProvider(FabricDataGenerator output) {
		super(output);
	}

	@Override
	protected void generateTags() {
		getOrCreateTagBuilder(Tags.Blocks.BARRELS)
				.addTag(Tags.Blocks.WOODEN_BARRELS);

		getOrCreateTagBuilder(Tags.Blocks.WOODEN_BARRELS)
				.add(Blocks.BARREL);
	}
}
