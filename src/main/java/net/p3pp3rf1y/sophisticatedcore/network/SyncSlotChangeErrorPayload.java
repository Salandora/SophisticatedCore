package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;

public record SyncSlotChangeErrorPayload(UpgradeSlotChangeResult slotChangeError) implements CustomPacketPayload {
	public static final Type<SyncSlotChangeErrorPayload> TYPE = new Type<>(SophisticatedCore.getRL("sync_slot_change_error"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SyncSlotChangeErrorPayload> STREAM_CODEC = StreamCodec.composite(
			UpgradeSlotChangeResult.STREAM_CODEC,
			SyncSlotChangeErrorPayload::slotChangeError,
			SyncSlotChangeErrorPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	@Environment(EnvType.CLIENT)
	public static void handlePayload(SyncSlotChangeErrorPayload payload, ClientPlayNetworking.Context context) {
		if (!(context.player().containerMenu instanceof StorageContainerMenuBase<?> menu)) {
			return;
		}
		menu.updateSlotChangeError(payload.slotChangeError);
	}
}
