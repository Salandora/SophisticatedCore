package net.p3pp3rf1y.sophisticatedcore.compat.common;

import com.github.salandora.sophisticatedlibrary.network.api.v0.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SetGhostSlotMessage {
	private final ItemStack stack;
	private final int slotNumber;

	public SetGhostSlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public static void encode(SetGhostSlotMessage msg, FriendlyByteBuf buffer) {
		buffer.writeItem(msg.stack);
		buffer.writeShort(msg.slotNumber);
	}

	public static SetGhostSlotMessage decode(FriendlyByteBuf buffer) {
		return new SetGhostSlotMessage(buffer.readItem(), buffer.readShort());
	}

	public static void onMessage(SetGhostSlotMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

	private static void handleMessage(@Nullable ServerPlayer sender, SetGhostSlotMessage msg) {
		if (sender == null || !(sender.containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		sender.containerMenu.getSlot(msg.slotNumber).set(msg.stack);
	}

}
