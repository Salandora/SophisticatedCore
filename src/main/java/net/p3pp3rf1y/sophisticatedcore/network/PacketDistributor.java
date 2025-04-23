package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PacketDistributor {
	private PacketDistributor() {
	}

	public static <T extends CustomPacketPayload> void sendToServer(T packet) {
		ClientPlayNetworking.send(packet);
	}

	public static <T extends CustomPacketPayload> void sendToAllNear(T packet, Entity entity, double range) {
		sendToAllNear(packet, (ServerLevel) entity.level(), entity.position(), range);
	}
	public static <T extends CustomPacketPayload> void sendToAllNear(T message, Level level, Vec3 pos, double range) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		sendToAllNear(message, serverLevel, pos, range);
	}
	public static <T extends CustomPacketPayload> void sendToAllNear(T message, ServerLevel level, Vec3 pos, double range) {
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

	@SafeVarargs
	public static <T extends CustomPacketPayload> void sendToPlayersNear(ServerLevel level, @Nullable ServerPlayer excluded, double x, double y, double z, double radius, T payload, T... payloads) {
		Packet<?> packet = makeClientboundPacket(payload, payloads);
		PlayerLookup.around(level, new Vec3(x, y, z), radius)
				.stream()
				.filter(p -> excluded == null || p != excluded)
				.forEach(player -> player.connection.send(packet));
	}

	private static <T extends CustomPacketPayload> Packet<?> makeClientboundPacket(T payload, T... payloads) {
		if (payloads.length > 0) {
			List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>(payloads.length + 1);
			packets.add(ServerPlayNetworking.createS2CPacket(payload));
			Arrays.stream(payloads).map(ServerPlayNetworking::createS2CPacket).forEach(packets::add);
			return new ClientboundBundlePacket(packets);
		}

		return ServerPlayNetworking.createS2CPacket(payload);
	}
}
