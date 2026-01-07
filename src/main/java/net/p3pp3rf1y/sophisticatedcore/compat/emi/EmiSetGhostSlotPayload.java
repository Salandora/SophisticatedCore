package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import com.github.salandora.sophisticatedfabriclib.network.api.v1.IPayloadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record EmiSetGhostSlotPayload(ItemStack stack, int slotNumber) implements CustomPacketPayload {
	public static final Type<EmiSetGhostSlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("emi_set_ghost_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmiSetGhostSlotPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			EmiSetGhostSlotPayload::stack,
			ByteBufCodecs.INT,
			EmiSetGhostSlotPayload::slotNumber,
			EmiSetGhostSlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(EmiSetGhostSlotPayload payload, IPayloadContext context) {
		if (!(context.player().containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		context.player().containerMenu.getSlot(payload.slotNumber).set(payload.stack);
	}
}
