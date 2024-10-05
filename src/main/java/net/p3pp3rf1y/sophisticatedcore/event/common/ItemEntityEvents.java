package net.p3pp3rf1y.sophisticatedcore.event.common;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ItemEntityEvents {
    /**
     * Callback for item pickup
     *
     * <p> Upon return:
     * <ul><li>SUCCESS cancels further processing and returns the result.
     * <li>PASS falls back to further processing.
     * <li>FAIL cancels further processing and returns the result.
     */
    Event<CanPickup> CAN_PICKUP = EventFactory.createArrayBacked(CanPickup.class, callbacks -> (player, itemEntity, stack) -> {
        for (CanPickup event : callbacks) {
            InteractionResult result = event.canPickup(player, itemEntity, stack);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        return InteractionResult.PASS;
    });

    Event<PostPickup> POST_PICKUP = EventFactory.createArrayBacked(PostPickup.class, callbacks -> (player, itemEntity, stack) -> {
        for (PostPickup event : callbacks) {
            event.postPickup(player, itemEntity, stack);
        }
    });

    @FunctionalInterface
    interface CanPickup {
        InteractionResult canPickup(Player player, ItemEntity itemEntity, ItemStack stack);
    }

    @FunctionalInterface
    interface PostPickup {
        void postPickup(Player player, ItemEntity itemEntity, ItemStack stack);
    }
}
