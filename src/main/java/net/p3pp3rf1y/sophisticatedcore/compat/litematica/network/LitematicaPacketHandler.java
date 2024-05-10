package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import me.pepperbell.simplenetworking.C2SPacket;
import me.pepperbell.simplenetworking.S2CPacket;
import me.pepperbell.simplenetworking.SimpleChannel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.function.Function;

public class LitematicaPacketHandler {
	private static int index = 0;
	public static final ResourceLocation CHANNEL_NAME = SophisticatedCore.getRL("litematica_channel");
	private static final SimpleChannel channel = new SimpleChannel(CHANNEL_NAME);

	public static void init() {
		channel.initServerListener();

		registerC2SMessage(RequestContentsMessage.class, RequestContentsMessage::new);
		registerS2CMessage(UpdateMaterialListMessage.class, UpdateMaterialListMessage::new);
	}

	public static <T extends C2SPacket> void registerC2SMessage(Class<T> type, Function<FriendlyByteBuf, T> factory) {
		getChannel().registerC2SPacket(type, index++, factory);
	}
	public static <T extends S2CPacket> void registerS2CMessage(Class<T> type, Function<FriendlyByteBuf, T> factory) {
		getChannel().registerS2CPacket(type, index++, factory);
	}

	public static SimpleChannel getChannel() {
		return channel;
	}

	public static void sendToServer(Object message) {
		getChannel().sendToServer((C2SPacket) message);
	}

	public static void sendToClient(ServerPlayer player, Object message) {
		getChannel().sendToClient((S2CPacket) message, player);
	}
}
