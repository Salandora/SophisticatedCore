package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaCompat;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestContentsPacket implements FabricPacket {
	public static final PacketType<RequestContentsPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "litematica_request_contents"), RequestContentsPacket::new);

	public RequestContentsPacket() {
	}
	public RequestContentsPacket(FriendlyByteBuf buffer) {
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
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

		Map<IStorageWrapper, FabricPacket> requested = Maps.newHashMap();
		requestContents(stacks, requested);
		if (!requested.isEmpty()) {
			PacketHelper.sendToPlayer(new UpdateMaterialListMessage(requested.size()), player);
			requested.forEach((wrapper, packet) -> PacketHelper.sendToPlayer(packet, player));
		}
	}

	public static void requestContents(List<ItemStack> stacks, Map<IStorageWrapper, FabricPacket> requested) {
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
			} else if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock && shulkerBoxHasItems(stack)) {
				requestContents(getStoredItems(stack), requested);
			}
		}
	}

	public static boolean shulkerBoxHasItems(ItemStack stackShulkerBox) {
		CompoundTag nbt = stackShulkerBox.getTag();
		if (nbt != null && nbt.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
			CompoundTag tag = nbt.getCompound("BlockEntityTag");
			if (tag.contains("Items", Tag.TAG_LIST)) {
				ListTag tagList = tag.getList("Items", Tag.TAG_COMPOUND);
				return !tagList.isEmpty();
			}
		}

		return false;
	}

	public static NonNullList<ItemStack> getStoredItems(ItemStack stackIn) {
		CompoundTag nbt = stackIn.getTag();
		if (nbt != null && nbt.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
			CompoundTag tagBlockEntity = nbt.getCompound("BlockEntityTag");
			if (tagBlockEntity.contains("Items", Tag.TAG_LIST)) {
				NonNullList<ItemStack> items = NonNullList.create();
				ListTag tagList = tagBlockEntity.getList("Items", Tag.TAG_COMPOUND);

				int count = tagList.size();
				for(int i = 0; i < count; ++i) {
					ItemStack stack = ItemStack.of(tagList.getCompound(i));
					if (!stack.isEmpty()) {
						items.add(stack);
					}
				}

				return items;
			}
		}

		return NonNullList.create();
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
