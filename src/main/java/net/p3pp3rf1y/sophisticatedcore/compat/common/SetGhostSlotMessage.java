package net.p3pp3rf1y.sophisticatedcore.compat.common;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class SetGhostSlotMessage implements FabricPacket {
	public static final PacketType<SetGhostSlotMessage> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "set_ghost_slot"), SetGhostSlotMessage::new);
	private final ItemStack stack;
	private final int slotNumber;

	public SetGhostSlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public SetGhostSlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readItem(), buffer.readShort());
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?>)) {
			return;
		}
		player.containerMenu.getSlot(slotNumber).set(stack);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeItem(stack);
		buffer.writeShort(slotNumber);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}

}
