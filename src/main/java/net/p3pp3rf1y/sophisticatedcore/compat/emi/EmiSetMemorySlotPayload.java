package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

public record EmiSetMemorySlotPayload(ItemStack stack, int slotNumber) implements CustomPacketPayload {
	public static final Type<EmiSetMemorySlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("emi_set_memory_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmiSetMemorySlotPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			EmiSetMemorySlotPayload::stack,
			ByteBufCodecs.INT,
			EmiSetMemorySlotPayload::slotNumber,
			EmiSetMemorySlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(EmiSetMemorySlotPayload payload, ServerPlayNetworking.Context context) {
		if (!(context.player().containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = settingsContainerMenu.getStorageWrapper();
		storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).setFilter(payload.slotNumber, payload.stack);
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemChanged(payload.slotNumber);
		storageWrapper.getInventoryHandler().onSlotFilterChanged(payload.slotNumber);
		settingsContainerMenu.sendAdditionalSlotInfo();
	}
}
