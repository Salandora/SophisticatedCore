package net.p3pp3rf1y.sophisticatedcore.common;

import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.TickTask;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.p3pp3rf1y.sophisticatedcore.extensions.block.entity.SophisticatedBlockEntity;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;
import net.p3pp3rf1y.sophisticatedcore.init.ModRecipes;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

public class CommonEventHandler {
	public void registerHandlers() {
		ModFluids.registerHandlers();
		ModParticles.registerParticles();
		ModRecipes.registerHandlers();
		ModPayloads.registerPayloads();

		ServerTickEvents.END_SERVER_TICK.register((server) -> ItemStackKey.clearCacheOnTickEnd());
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(RecipeHelper::onDataPackSync);

		UseBlockCallback.EVENT.register(this::onUseBlock);

		ServerWorldEvents.UNLOAD.register(ServerStorageSoundHandler::onWorldUnload);
		ServerTickEvents.END_WORLD_TICK.register(ServerStorageSoundHandler::tick);

		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (blockEntity instanceof SophisticatedBlockEntity sbe) {
				// Force the onLoad to the next tick or else the game will indefinitely hang as it can't get the chunk
				world.getServer().tell(new TickTask(world.getServer().getTickCount(), sbe::sophisticatedCore_onLoad));
			}
		});
		ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
				chunk.getBlockEntities().forEach((pos, blockEntity) -> {
					if (blockEntity instanceof SophisticatedBlockEntity sbe) {
						sbe.sophisticatedCore_onChunkUnloaded();
					}
				}));
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
		return stack.sophisticatedCore_onItemUseFirst(context);
	}
}
