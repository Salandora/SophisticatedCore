package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public class SyncSlotStackPacket implements FabricPacket {
	public static final PacketType<SyncSlotStackPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_slot_stack"), SyncSlotStackPacket::new);
	private final int windowId;
	private final int stateId;
	private final int slotNumber;
	private final ItemStack stack;

	public SyncSlotStackPacket(int windowId, int stateId, int slotNumber, ItemStack stack) {
		this.windowId = windowId;
		this.stateId = stateId;
		this.slotNumber = slotNumber;
		this.stack = stack;
	}

	public SyncSlotStackPacket(FriendlyByteBuf buffer) {
		this(buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readShort(), PacketHelper.readOversizedItemStack(buffer));
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase || player.containerMenu instanceof SettingsContainerMenu) || player.containerMenu.containerId != windowId) {
			return;
		}
		player.containerMenu.setItem(slotNumber, stateId, stack);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeByte(windowId);
		buffer.writeVarInt(stateId);
		buffer.writeShort(slotNumber);
		PacketHelper.writeOversizedItemStack(stack, buffer);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
