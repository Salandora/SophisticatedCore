package net.p3pp3rf1y.sophisticatedcore.extensions.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public interface SophisticatedItem {
    // Helpers for accessing Item data
    private Item self()
    {
        return (Item)this;
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

	@Nullable
	default EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return null;
	}
}
