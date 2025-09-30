package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import com.github.salandora.sophisticatedlibrary.network.handling.IPayloadContext;
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

public record REISetMemorySlotPayload(ItemStack stack, int slotNumber) implements CustomPacketPayload {
	public static final Type<REISetMemorySlotPayload> TYPE = new Type<>(SophisticatedCore.getRL("rei_set_memory_slot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, REISetMemorySlotPayload> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC,
			REISetMemorySlotPayload::stack,
			ByteBufCodecs.INT,
			REISetMemorySlotPayload::slotNumber,
			REISetMemorySlotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(REISetMemorySlotPayload payload, IPayloadContext context) {
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
