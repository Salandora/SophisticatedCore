package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

public class SetGhostSlotMessage extends SimplePacketBase {
	private final ItemStack stack;
	private final int slotNumber;

	public SetGhostSlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public SetGhostSlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readItem(), buffer.readShort());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeItem(stack);
		buffer.writeShort(slotNumber);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null || !(sender.containerMenu instanceof StorageContainerMenuBase<?>)) {
				return;
			}
			sender.containerMenu.getSlot(slotNumber).set(stack);
		});
		return true;
	}

}
