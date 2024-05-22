package net.p3pp3rf1y.sophisticatedcore.network;

import me.pepperbell.simplenetworking.C2SPacket;
import me.pepperbell.simplenetworking.S2CPacket;
import me.pepperbell.simplenetworking.SimpleChannel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.PlayDiscMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.SoundStopNotificationMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.StopDiscPlaybackMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.tank.TankClickMessage;

import java.util.function.Function;

public class PacketHandler {
	private static int index = 0;
	public static final ResourceLocation CHANNEL_NAME = SophisticatedCore.getRL("channel");
	private static final SimpleChannel channel = new SimpleChannel(CHANNEL_NAME);

	public static void init() {
		channel.initServerListener();

		registerC2SMessage(SyncContainerClientDataMessage.class, SyncContainerClientDataMessage::new);
		registerC2SMessage(TransferFullSlotMessage.class, TransferFullSlotMessage::new);
		registerC2SMessage(SoundStopNotificationMessage.class, SoundStopNotificationMessage::new);
		registerC2SMessage(TankClickMessage.class, TankClickMessage::new);

		registerS2CMessage(SyncContainerStacksMessage.class, SyncContainerStacksMessage::new);
		registerS2CMessage(SyncSlotStackMessage.class, SyncSlotStackMessage::new);
		registerS2CMessage(SyncPlayerSettingsMessage.class, SyncPlayerSettingsMessage::new);
		registerS2CMessage(PlayDiscMessage.class, PlayDiscMessage::new);
		registerS2CMessage(StopDiscPlaybackMessage.class, StopDiscPlaybackMessage::new);
		registerS2CMessage(SyncTemplateSettingsMessage.class, SyncTemplateSettingsMessage::new);
		registerS2CMessage(SyncAdditionalSlotInfoMessage.class, SyncAdditionalSlotInfoMessage::new);
		registerS2CMessage(SyncEmptySlotIconsMessage.class, SyncEmptySlotIconsMessage::new);
		registerS2CMessage(SyncSlotChangeErrorMessage.class, SyncSlotChangeErrorMessage::new);
		registerS2CMessage(SyncDatapackSettingsTemplateMessage.class, SyncDatapackSettingsTemplateMessage::new);
	}

	public static <T extends SimplePacketBase> void registerC2SMessage(Class<T> type, Function<FriendlyByteBuf, T> factory) {
		getChannel().registerC2SPacket(type, index++, factory);
	}
	public static <T extends SimplePacketBase> void registerS2CMessage(Class<T> type, Function<FriendlyByteBuf, T> factory) {
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

	public static void sendToAllNear(ServerLevel world, Vec3 pos, int range, Object message) {
		getChannel().sendToClientsAround((S2CPacket) message, world, pos, range);
	}
}
