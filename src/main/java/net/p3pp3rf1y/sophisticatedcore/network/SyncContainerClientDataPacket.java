package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;

import javax.annotation.Nullable;

public class SyncContainerClientDataPacket implements FabricPacket {
	public static final PacketType<SyncContainerClientDataPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_container_client_data"), SyncContainerClientDataPacket::new);
	@Nullable
	private final CompoundTag data;

	public SyncContainerClientDataPacket(@Nullable CompoundTag data) {
		this.data = data;
	}

	public SyncContainerClientDataPacket(FriendlyByteBuf buffer) {
		this(buffer.readNbt());
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		if (data == null) {
			return;
		}

		if (player.containerMenu instanceof ISyncedContainer container) {
			container.handlePacket(data);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeNbt(data);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
