package net.p3pp3rf1y.sophisticatedcore.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class Tags {
	public static class Items {
		public static final TagKey<Item> STONES = register("stones");
		public static final TagKey<Item> WOODEN_CHESTS = register("wooden_chests");

		private static TagKey<Item> register(String tagID) {
			return TagKey.create(Registries.ITEM, new ResourceLocation("c", tagID));
		}
	}

	public static class Blocks {
		public static final TagKey<Block> BARRELS = register("barrels");
		public static final TagKey<Block> WOODEN_CHESTS = register("wooden_chests");

		private static TagKey<Block> register(String tagID) {
			return TagKey.create(Registries.BLOCK, new ResourceLocation("c", tagID));
		}
	}
}
