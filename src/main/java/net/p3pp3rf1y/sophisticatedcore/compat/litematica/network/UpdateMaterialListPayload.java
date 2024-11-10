package net.p3pp3rf1y.sophisticatedcore.compat.litematica.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaHelper;

public record UpdateMaterialListPayload(int requestedContents) implements CustomPacketPayload {
	public static final Type<UpdateMaterialListPayload> TYPE = new Type<>(SophisticatedCore.getRL("litematica_update_material_list"));
	public static final StreamCodec<ByteBuf, UpdateMaterialListPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, UpdateMaterialListPayload::requestedContents,
			UpdateMaterialListPayload::new
	);

	@Environment(EnvType.CLIENT)
	public static void handlePayload(UpdateMaterialListPayload payload, ClientPlayNetworking.Context context) {
		LitematicaHelper.setRequested(payload.requestedContents);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
