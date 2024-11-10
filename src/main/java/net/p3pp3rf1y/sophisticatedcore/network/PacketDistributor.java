package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PacketDistributor {
	private PacketDistributor() {
	}

	public static <T extends CustomPacketPayload> void sendToServer(T packet) {
		ClientPlayNetworking.send(packet);
	}

	public static <T extends CustomPacketPayload> void sendToAllNear(T packet, Entity entity, double range) {
		for (ServerPlayer player : PlayerLookup.around((ServerLevel) entity.level(), entity.position(), range)) {
			ServerPlayNetworking.send(player, packet);
		}
	}
	public static <T extends CustomPacketPayload> void sendToAllNear(T message, Level level, Vec3 pos, int range) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		sendToAllNear(message, serverLevel, pos, range);
	}
	public static <T extends CustomPacketPayload> void sendToAllNear(T message, ServerLevel level, Vec3 pos, int range) {
		for (ServerPlayer player : PlayerLookup.around(level, pos, range)) {
			ServerPlayNetworking.send(player, message);
		}
	}

	public static <T extends CustomPacketPayload> void sendToPlayer(Player player, T packet) {
		if (player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, packet);
		}
	}
	public static <T extends CustomPacketPayload> void sendToPlayer(ServerPlayer player, T packet) {
		ServerPlayNetworking.send(player, packet);
	}
}
