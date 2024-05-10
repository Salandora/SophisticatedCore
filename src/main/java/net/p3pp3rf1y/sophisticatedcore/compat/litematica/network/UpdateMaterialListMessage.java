package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import me.pepperbell.simplenetworking.S2CPacket;
import me.pepperbell.simplenetworking.SimpleChannel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaHelper;

public class UpdateMaterialListMessage implements S2CPacket {
	private final int requestedContents;

	public UpdateMaterialListMessage(int requestedContents) {
		this.requestedContents = requestedContents;
	}

	public UpdateMaterialListMessage(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	@Override
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeInt(this.requestedContents);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void handle(Minecraft client, ClientPacketListener listener, PacketSender responseSender, SimpleChannel channel) {
		client.execute(() -> {
			if (client.player == null) {
				return;
			}

			LitematicaHelper.setRequested(this.requestedContents);
		});
	}
}
