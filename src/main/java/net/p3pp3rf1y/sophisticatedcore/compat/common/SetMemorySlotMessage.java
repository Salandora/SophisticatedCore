package net.p3pp3rf1y.sophisticatedcore.compat.common;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

public class SetMemorySlotMessage implements FabricPacket {
	public static final PacketType<SetMemorySlotMessage> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "set_memory_slot"), SetMemorySlotMessage::new);
	private final ItemStack stack;
	private final int slotNumber;

	public SetMemorySlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public SetMemorySlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readItem(), buffer.readShort());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeItem(stack);
		buffer.writeShort(slotNumber);
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = settingsContainerMenu.getStorageWrapper();
		storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).setFilter(slotNumber, stack);
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemChanged(slotNumber);
		storageWrapper.getInventoryHandler().onSlotFilterChanged(slotNumber);
		settingsContainerMenu.sendAdditionalSlotInfo();
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
