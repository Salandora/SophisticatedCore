package net.p3pp3rf1y.sophisticatedcore.extensions.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public interface SophisticatedItemStack {
    // Helpers for accessing Item data
    private ItemStack self()
    {
        return (ItemStack)this;
    }

    /**
     * Called to tick armor in the armor slot. Override to do something
     */
    default void onArmorTick(Level level, Player player)
    {
        self().getItem().onArmorTick(self(), level, player);
    }

    default InteractionResult onItemUseFirst(UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
		Registry<Block> registry = player.level().registryAccess().registryOrThrow(Registries.BLOCK);
        if (!player.getAbilities().mayBuild && !self().hasAdventureModePlaceTagForBlock(registry, new BlockInWorld(context.getLevel(), pos, false))) {
            return InteractionResult.PASS;
        } else {
            Item item = self().getItem();
            InteractionResult result = item.onItemUseFirst(self(), context);
            if (result == InteractionResult.SUCCESS) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }

            return result;
        }
    }
}
