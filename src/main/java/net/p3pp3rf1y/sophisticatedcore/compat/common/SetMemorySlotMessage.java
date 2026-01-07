package net.p3pp3rf1y.sophisticatedcore.compat.common;

import com.github.salandora.sophisticatedfabriclib.network.api.v0.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SetMemorySlotMessage {
	private final ItemStack stack;
	private final int slotNumber;

	public SetMemorySlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public static void encode(SetMemorySlotMessage msg, FriendlyByteBuf buffer) {
		buffer.writeItem(msg.stack);
		buffer.writeShort(msg.slotNumber);
	}

	public static SetMemorySlotMessage decode(FriendlyByteBuf buffer) {
		return new SetMemorySlotMessage(buffer.readItem(), buffer.readShort());
	}

	public static void onMessage(SetMemorySlotMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	public static void handleMessage(@Nullable ServerPlayer sender, SetMemorySlotMessage msg) {
		if (sender == null || !(sender.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu)) {
			return;
		}
		IStorageWrapper storageWrapper = settingsContainerMenu.getStorageWrapper();
		storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).setFilter(msg.slotNumber, msg.stack);
		storageWrapper.getInventoryHandler().onSlotFilterChanged(msg.slotNumber);
		settingsContainerMenu.sendAdditionalSlotInfo();
	}

}
