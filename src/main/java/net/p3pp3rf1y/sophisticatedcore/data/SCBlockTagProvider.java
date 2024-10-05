package net.p3pp3rf1y.sophisticatedcore.data;

import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.p3pp3rf1y.sophisticatedcore.api.Tags;

import java.util.concurrent.CompletableFuture;

public class SCBlockTagProvider extends FabricTagProvider.BlockTagProvider {
	public SCBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider arg) {
		getOrCreateTagBuilder(Tags.Blocks.BARRELS)
				.forceAddTag(ConventionalBlockTags.WOODEN_BARRELS);
	}
}
