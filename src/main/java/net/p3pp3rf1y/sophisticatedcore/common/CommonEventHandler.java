package net.p3pp3rf1y.sophisticatedcore.common;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.p3pp3rf1y.sophisticatedcore.event.common.EntityEvents;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public class CommonEventHandler {
	public void registerHandlers() {
		ModFluids.registerHandlers();
		ModParticles.registerParticles();
		ModRecipes.registerHandlers();

		ServerTickEvents.END_SERVER_TICK.register((server) -> ItemStackKey.clearCacheOnTickEnd());
		RecipeHelper.addReloadListener();

		UseBlockCallback.EVENT.register(this::onUseBlock);

		EntityEvents.ON_JOIN_WORLD.register((entity, world, loadedFromDisk) -> {
			if (entity.getClass().equals(ItemEntity.class)) {
				ItemStack stack = ((ItemEntity)entity).getItem();
				Item item = stack.getItem();
				if (item.hasCustomEntity(stack)) {
					Entity newEntity = item.createEntity(world, entity, stack);
					if (newEntity != null) {
						entity.discard();
						world.addFreshEntity(newEntity);
						return false;
					}
				}
			}
			return true;
		});
	}

	private InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
		if (player.isSpectator()) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(hand);
		if (stack.isEmpty()) {
			return InteractionResult.PASS;
		}

		UseOnContext context = new UseOnContext(player, hand, hitResult);
		return stack.onItemUseFirst(context);
	}
}
