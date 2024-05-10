package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import fi.dy.masa.malilib.util.InventoryUtils;
import me.pepperbell.simplenetworking.S2CPacket;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedcore.common.CapabilityWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaCompat;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class RequestContentsMessage extends SimplePacketBase {

	public RequestContentsMessage() {
	}
	public RequestContentsMessage(FriendlyByteBuf buffer) {
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null) {
				return;
			}
			List<ItemStack> stacks = Lists.newArrayList();

			// Iterate over all slots cause this includes items, armor and offhand
			Container inv = player.getInventory();
			int size = inv.getContainerSize();
			for (int slot = 0; slot < size; ++slot) {
				ItemStack stack = inv.getItem(slot);
				if (!stack.isEmpty()) {
					stacks.add(stack);
				}
			}

			Map<Function<List<UUID>, S2CPacket>, List<UUID>> requested = Maps.newHashMap();
			requestContents(stacks, requested);
			if (!requested.isEmpty()) {
				int count = 0;
				for (List<UUID> uuids : requested.values()) {
					count += uuids.size();
				}
				LitematicaPacketHandler.sendToClient(player, new UpdateMaterialListMessage(count));
				requested.forEach((function, uuids) -> LitematicaPacketHandler.sendToClient(player, function.apply(uuids)));
			}
		});
		return true;
	}

	public static void requestContents(List<ItemStack> stacks, Map<Function<List<UUID>, S2CPacket>, List<UUID>> requested) {
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				CapabilityWrapper.get(stack).ifPresent(wrapper -> wrapper.getContentsUuid().ifPresent(uuid -> {
					Function<List<UUID>, S2CPacket> function = LitematicaCompat.REQUEST_CONTENTS_CAPABILITY.find(stack, uuid);
					if (function != null) {
						requested.compute(function, (k, v) -> {
							if (v == null) {
								v = Lists.newArrayList();
							}
							v.add(uuid);
							return v;
						});
					}

					List<ItemStack> wrapperStacks = Lists.newArrayList();
					InventoryHandler handler = wrapper.getInventoryHandler();
					for (int slot = 0; slot < handler.getSlotCount(); slot++) {
						ItemStack wrapperStack = handler.getSlotStack(slot);
						if (!wrapperStack.isEmpty()) {
							wrapperStacks.add(wrapperStack);
						}
					}
					requestContents(wrapperStacks, requested);
				}));
				if (stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock && InventoryUtils.shulkerBoxHasItems(stack)) {
					requestContents(InventoryUtils.getStoredItems(stack), requested);
				}
			}
		}
	}
}
