package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.p3pp3rf1y.sophisticatedcore.Config;

public class BlockItemBase extends BlockItem {
	public BlockItemBase(Block pBlock, Properties properties) {
		super(pBlock, properties);
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
		if (Config.COMMON.enabledItems.isItemEnabled(this) && getBlock() instanceof BlockBase blockBase) {
			super.fillItemCategory(group, items);
		}
	}
}