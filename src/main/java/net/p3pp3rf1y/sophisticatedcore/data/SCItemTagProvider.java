package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.Tags;

import org.jetbrains.annotations.Nullable;

public class SCItemTagProvider extends FabricTagProvider.ItemTagProvider {
	public SCItemTagProvider(FabricDataGenerator output, @Nullable FabricTagProvider.BlockTagProvider blockTagProvider) {
		super(output, blockTagProvider);
	}

	@Override
	protected void generateTags() {
		getOrCreateTagBuilder(Tags.Items.STONES)
				.add(Items.ANDESITE, Items.POLISHED_ANDESITE,
						Items.DIORITE, Items.POLISHED_DIORITE,
						Items.GRANITE, Items.POLISHED_GRANITE,
						Items.STONE, Items.INFESTED_STONE,
						Items.DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.INFESTED_DEEPSLATE,
						Items.TUFF);

		getOrCreateTagBuilder(Tags.Items.WOODEN_CHESTS)
				.add(Items.CHEST, Items.TRAPPED_CHEST);

		getOrCreateTagBuilder(Tags.Items.CHESTS)
				.add(Items.CHEST)
				.add(Items.ENDER_CHEST)
				.add(Items.TRAPPED_CHEST);
	}
}
