package net.p3pp3rf1y.sophisticatedcore.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.Tags;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

public class SCItemTagProvider extends FabricTagProvider.ItemTagProvider {
	public SCItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture, @Nullable FabricTagProvider.BlockTagProvider blockTagProvider) {
		super(output, completableFuture, blockTagProvider);
	}

	@Override
	protected void addTags(HolderLookup.Provider arg) {
		getOrCreateTagBuilder(Tags.Items.STONES)
				.add(Items.ANDESITE, Items.POLISHED_ANDESITE,
						Items.DIORITE, Items.POLISHED_DIORITE,
						Items.GRANITE, Items.POLISHED_GRANITE,
						Items.STONE, Items.INFESTED_STONE,
						Items.DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.INFESTED_DEEPSLATE,
						Items.TUFF);

		getOrCreateTagBuilder(Tags.Items.WOODEN_CHESTS)
				.add(Items.CHEST, Items.TRAPPED_CHEST);
	}
}
