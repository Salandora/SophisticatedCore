package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaCompat;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RequestContentsPayload() implements CustomPacketPayload {
	public static final Type<RequestContentsPayload> TYPE = new Type<>(SophisticatedCore.getRL("litematica_request_contents"));
	public static final StreamCodec<ByteBuf, RequestContentsPayload> STREAM_CODEC = StreamCodecHelper.singleton(RequestContentsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(RequestContentsPayload payload, ServerPlayNetworking.Context context) {
		List<ItemStack> stacks = Lists.newArrayList();

		ServerPlayer player = context.player();

		// Iterate over all slots cause this includes items, armor and offhand
		Container inv = player.getInventory();
		int size = inv.getContainerSize();
		for (int slot = 0; slot < size; ++slot) {
			ItemStack stack = inv.getItem(slot);
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}

		Map<IStorageWrapper, CustomPacketPayload> requested = Maps.newHashMap();
		requestContents(stacks, requested, context.server().registryAccess());
		if (!requested.isEmpty()) {
			PacketDistributor.sendToPlayer(player, new UpdateMaterialListPayload(requested.size()));
			requested.forEach((wrapper, packet) -> PacketDistributor.sendToPlayer(player, packet));
		}
	}

	public static void requestContents(List<ItemStack> stacks, Map<IStorageWrapper, CustomPacketPayload> requested, HolderLookup.Provider levelRegistry) {
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
					requestContents(wrapperStacks, requested, levelRegistry);
				}
			} else if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock && shulkerBoxHasItems(stack)) {
				requestContents(getStoredItems(stack, levelRegistry), requested, levelRegistry);
			}
		}
	}

	public static boolean shulkerBoxHasItems(ItemStack stackShulkerBox) {
		CustomData data = stackShulkerBox.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data != null && data.contains("Items")) {
			return !data.copyTag().getList("Items", Tag.TAG_COMPOUND).isEmpty();
		}

		return false;
	}

	public static NonNullList<ItemStack> getStoredItems(ItemStack stackIn, HolderLookup.Provider levelRegistry) {
		NonNullList<ItemStack> items = NonNullList.create();
		CustomData data = stackIn.get(DataComponents.BLOCK_ENTITY_DATA);
		if (data != null && data.contains("Items")) {
			ContainerHelper.loadAllItems(data.copyTag(), items, levelRegistry);
		}

		return items;
	}
}
