package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaHelper;

public class UpdateMaterialListMessage implements FabricPacket {
	public static final PacketType<UpdateMaterialListMessage> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "litematica_update_material_list"), UpdateMaterialListMessage::new);
	private final int requestedContents;

	public UpdateMaterialListMessage(int requestedContents) {
		this.requestedContents = requestedContents;
	}

	public UpdateMaterialListMessage(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(this.requestedContents);
	}

	@Environment(EnvType.CLIENT)
	public void handle(LocalPlayer player, PacketSender responseSender) {
		LitematicaHelper.setRequested(this.requestedContents);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
