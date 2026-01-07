package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import com.github.salandora.sophisticatedfabriclib.network.api.v1.IPayloadContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public record REISetGhostSlotPayload(ItemStack stack, int slotNumber) implements CustomPacketPayload {
	public static final Type<REISetGhostSlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("rei_set_ghost_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, REISetGhostSlotPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			REISetGhostSlotPayload::stack,
			ByteBufCodecs.INT,
			REISetGhostSlotPayload::slotNumber,
			REISetGhostSlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(REISetGhostSlotPayload payload, IPayloadContext context) {
		if (!(context.player().containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		context.player().containerMenu.getSlot(payload.slotNumber).set(payload.stack);
	}
}
