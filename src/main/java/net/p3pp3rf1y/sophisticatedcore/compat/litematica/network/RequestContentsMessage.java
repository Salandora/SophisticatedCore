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
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaCompat;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

			Map<IStorageWrapper, S2CPacket> requested = Maps.newHashMap();
			requestContents(stacks, requested);
			if (!requested.isEmpty()) {
				LitematicaPacketHandler.sendToClient(player, new UpdateMaterialListMessage(requested.size()));
				requested.forEach((wrapper, packet) -> LitematicaPacketHandler.sendToClient(player, packet));
			}
		});
		return true;
	}

	public static void requestContents(List<ItemStack> stacks, Map<IStorageWrapper, S2CPacket> requested) {
		for (ItemStack stack : stacks) {
			LitematicaCompat.LitematicaWrapper litematicaWrapper = LitematicaCompat.LITEMATICA_CAPABILITY.find(stack, null);
			if (litematicaWrapper != null) {
				IStorageWrapper wrapper = litematicaWrapper.wrapper();
				UUID uuid = wrapper.getContentsUuid().orElse(null);
				if (uuid != null) {
					requested.put(wrapper, litematicaWrapper.packetGenerator().apply(uuid));

					List<ItemStack> wrapperStacks = Lists.newArrayList();
					InventoryHandler handler = wrapper.getInventoryHandler();
					for (int slot = 0; slot < handler.getSlotCount(); slot++) {
						ItemStack wrapperStack = handler.getSlotStack(slot);
						if (!wrapperStack.isEmpty()) {
							wrapperStacks.add(wrapperStack);
						}
					}
					requestContents(wrapperStacks, requested);
				}
			} else if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock && InventoryUtils.shulkerBoxHasItems(stack)) {
				requestContents(InventoryUtils.getStoredItems(stack), requested);
			}
		}
	}
}
