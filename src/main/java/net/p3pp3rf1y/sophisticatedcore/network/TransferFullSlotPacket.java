package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class TransferFullSlotPacket implements FabricPacket {
	public static final PacketType<TransferFullSlotPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "transfer_full_slot"), TransferFullSlotPacket::new);
	private final int slotId;

	public TransferFullSlotPacket(int slotId) {
		this.slotId = slotId;
	}

	public TransferFullSlotPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainer)) {
			return;
		}
		Slot slot = storageContainer.getSlot(slotId);
		ItemStack transferResult;
		do {
			transferResult = storageContainer.quickMoveStack(player, slotId);
		} while (!transferResult.isEmpty() && ItemStack.isSameItem(slot.getItem(), transferResult));
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(slotId);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
