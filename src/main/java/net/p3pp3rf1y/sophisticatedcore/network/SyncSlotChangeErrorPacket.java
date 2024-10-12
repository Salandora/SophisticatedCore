package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SyncSlotChangeErrorPacket implements FabricPacket {
	public static final PacketType<SyncSlotChangeErrorPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_slot_change_error"), SyncSlotChangeErrorPacket::new);
	private final UpgradeSlotChangeResult slotChangeError;

	public SyncSlotChangeErrorPacket(UpgradeSlotChangeResult slotChangeError) {
		this.slotChangeError = slotChangeError;
	}

	public SyncSlotChangeErrorPacket(FriendlyByteBuf buffer) {
		this(new UpgradeSlotChangeResult.Fail(buffer.readComponent(),
				Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()),
				Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()),
				Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet())));
	}

	private void writeSlotChangeResult(FriendlyByteBuf buffer, UpgradeSlotChangeResult slotChangeResult) {
		buffer.writeComponent(slotChangeResult.getErrorMessage().orElse(Component.empty()));
		buffer.writeVarIntArray(slotChangeResult.getErrorUpgradeSlots().stream().mapToInt(i -> i).toArray());
		buffer.writeVarIntArray(slotChangeResult.getErrorInventorySlots().stream().mapToInt(i -> i).toArray());
		buffer.writeVarIntArray(slotChangeResult.getErrorInventoryParts().stream().mapToInt(i -> i).toArray());
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
			return;
		}
		menu.updateSlotChangeError(slotChangeError);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		writeSlotChangeResult(buffer, slotChangeError);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}