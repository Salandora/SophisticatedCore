package net.p3pp3rf1y.sophisticatedcore.extensions.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public interface SophisticatedItem {
	/**
	 * Called when a player drops the item into the world, returning false from this will prevent the item from being removed from the players inventory and spawning in the world
	 * Params:
	 * @param stack - The item stack, before the item is removed. The item stack, before the item is removed.
	 * @param player – The player that dropped the item
	 */
	default boolean onDroppedByPlayer(ItemStack stack, Player player) {
		return true;
	}

    /**
     * Called to tick armor in the armor slot. Override to do something
     */
    default void onArmorTick(ItemStack stack, Level level, Player player)
    {
    }

    /**
     * This is called when the item is used, before the block is activated.
     *
     * @return Return PASS to allow vanilla handling, any other to skip normal code.
     */
    default InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        return InteractionResult.PASS;
    }
}
