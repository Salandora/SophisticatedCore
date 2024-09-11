package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class PacketHelper {
	private PacketHelper() {
	}

	/*public static ItemStack readOversizedItemStack(FriendlyByteBuf buffer) {
		if (!buffer.readBoolean()) {
			return ItemStack.EMPTY;
		} else {
			Item item = buffer.readById(BuiltInRegistries.ITEM);
			int count = buffer.readInt();
			return item == null ? ItemStack.EMPTY : AttachmentInternals.reconstructItemStack(item, count, buffer.readNbt());
		}
	}

	public static void writeOversizedItemStack(ItemStack stack, FriendlyByteBuf buffer) {
		if (stack.isEmpty()) {
			buffer.writeBoolean(false);
		} else {
			buffer.writeBoolean(true);
			Item item = stack.getItem();
			buffer.writeId(BuiltInRegistries.ITEM, item);
			buffer.writeInt(stack.getCount());
			CompoundTag compoundtag = null;
			if (item.isDamageable(stack) || item.shouldOverrideMultiplayerNbt()) {
				compoundtag = stack.getTag();
			}
			compoundtag = AttachmentInternals.addAttachmentsToTag(compoundtag, stack, false);

			buffer.writeNbt(compoundtag);
		}
	}*/

	public static ItemStack readOversizedItemStack(FriendlyByteBuf packetBuffer) {
		if (!packetBuffer.readBoolean()) {
			return ItemStack.EMPTY;
		} else {
			int i = packetBuffer.readVarInt();
			int j = packetBuffer.readInt();
			ItemStack itemstack = new ItemStack(Item.byId(i), j);
			itemstack.setTag(packetBuffer.readNbt());
			return itemstack;
		}
	}

	public static void writeOversizedItemStack(ItemStack stack, FriendlyByteBuf packetBuffer) {
		if (stack.isEmpty()) {
			packetBuffer.writeBoolean(false);
		} else {
			packetBuffer.writeBoolean(true);
			Item item = stack.getItem();
			packetBuffer.writeVarInt(Item.getId(item));
			packetBuffer.writeInt(stack.getCount());
			CompoundTag compoundnbt = null;
			if (item.canBeDepleted() || item.shouldOverrideMultiplayerNbt()) {
				compoundnbt = stack.getTag();
			}

			packetBuffer.writeNbt(compoundnbt);
		}
	}

	public static <T extends FabricPacket> void sendToServer(T packet) {
		ClientPlayNetworking.send(packet);
	}

	public static <T extends FabricPacket> void sendToAllNear(T packet, Entity entity, double range) {
		for (ServerPlayer player : PlayerLookup.around((ServerLevel) entity.level(), entity.position(), range)) {
			ServerPlayNetworking.send(player, packet);
		}
	}

	public static <T extends FabricPacket> void sendToAllNear(ServerLevel level, Vec3 pos, int range, T message) {
		for (ServerPlayer player : PlayerLookup.around(level, pos, range)) {
			ServerPlayNetworking.send(player, message);
		}
	}

	public static <T extends FabricPacket> void sendToPlayer(T packet, Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, packet);
		}
	}
	public static <T extends FabricPacket> void sendToPlayer(T packet, ServerPlayer player) {
		ServerPlayNetworking.send(player, packet);
	}
}
