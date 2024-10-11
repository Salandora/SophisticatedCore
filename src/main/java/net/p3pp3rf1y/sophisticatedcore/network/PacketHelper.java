package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class PacketHelper {
	private PacketHelper() {
	}

	public static ItemStack readOversizedItemStack(FriendlyByteBuf buffer) {
		if (!buffer.readBoolean()) {
			return ItemStack.EMPTY;
		} else {
			Item item = buffer.readById(BuiltInRegistries.ITEM);
			int count = buffer.readInt();
			if (item == null) {
				return ItemStack.EMPTY;
			}

			ItemStack itemstack = new ItemStack(item, count);
			itemstack.setTag(buffer.readNbt());
			return itemstack;
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
			if (item.canBeDepleted() || item.shouldOverrideMultiplayerNbt()) {
				compoundtag = stack.getTag();
			}

			buffer.writeNbt(compoundtag);
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
	public static <T extends FabricPacket> void sendToAllNear(T message, ServerLevel level, Vec3 pos, int range) {
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
