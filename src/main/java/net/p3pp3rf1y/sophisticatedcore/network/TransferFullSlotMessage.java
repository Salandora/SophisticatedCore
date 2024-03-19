package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class TransferFullSlotMessage extends SimplePacketBase {
	private final int slotId;

	public TransferFullSlotMessage(int slotId) {
		this.slotId = slotId;
	}

	public TransferFullSlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(slotId);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null || !(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainer)) {
				return;
			}
			Slot slot = storageContainer.getSlot(slotId);
			ItemStack transferResult;
			do {
				transferResult = storageContainer.quickMoveStack(player, slotId);
			} while (!transferResult.isEmpty() && ItemStack.isSameItem(slot.getItem(), transferResult));
		});
		return true;
	}

}
