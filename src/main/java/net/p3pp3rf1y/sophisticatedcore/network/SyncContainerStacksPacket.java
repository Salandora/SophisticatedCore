package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.ArrayList;
import java.util.List;

public class SyncContainerStacksPacket implements FabricPacket {
	public static final PacketType<SyncContainerStacksPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_container_stacks"), SyncContainerStacksPacket::new);
	private final int windowId;
	private final int stateId;
	private final List<ItemStack> itemStacks;
	private final ItemStack carriedStack;

	public SyncContainerStacksPacket(int windowId, int stateId, List<ItemStack> itemStacks, ItemStack carriedStack) {
		this.windowId = windowId;
		this.stateId = stateId;
		this.itemStacks = itemStacks;
		this.carriedStack = carriedStack;
	}

	public SyncContainerStacksPacket(FriendlyByteBuf buffer) {
		this(buffer.readUnsignedByte(), buffer.readVarInt(), buffer.readCollection(ArrayList::new, PacketHelper::readOversizedItemStack), buffer.readItem());
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase) || player.containerMenu.containerId != windowId) {
			return;
		}
		player.containerMenu.initializeContents(stateId, itemStacks, carriedStack);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeByte(windowId);
		buffer.writeVarInt(stateId);
		buffer.writeCollection(itemStacks, (buf, itemStack) -> PacketHelper.writeOversizedItemStack(itemStack, buf));
		buffer.writeItem(carriedStack);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
