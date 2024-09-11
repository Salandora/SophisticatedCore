package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SyncEmptySlotIconsPacket implements FabricPacket {
	public static final PacketType<SyncEmptySlotIconsPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_empty_slot_icons"), SyncEmptySlotIconsPacket::new);
	private final Map<ResourceLocation, Set<Integer>> emptySlotIcons;

	public SyncEmptySlotIconsPacket(Map<ResourceLocation, Set<Integer>> emptySlotIcons) {
		this.emptySlotIcons = emptySlotIcons;
	}

	public SyncEmptySlotIconsPacket(FriendlyByteBuf buffer) {
		this(readEmptySlotTextures(buffer));
	}

	private void writeEmptySlotTextures(FriendlyByteBuf buffer, Map<ResourceLocation, Set<Integer>> map) {
		buffer.writeInt(map.size());

		for (Map.Entry<ResourceLocation, Set<Integer>> entry : map.entrySet()) {
			buffer.writeResourceLocation(entry.getKey());
			buffer.writeVarIntArray(entry.getValue().stream().mapToInt(i -> i).toArray());
		}
	}

	private static Map<ResourceLocation, Set<Integer>> readEmptySlotTextures(FriendlyByteBuf buffer) {
		Map<ResourceLocation, Set<Integer>> map = new HashMap<>();

		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			ResourceLocation resourceLocation = buffer.readResourceLocation();
			map.put(resourceLocation, Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()));
		}

		return map;
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
			return;
		}
		menu.updateEmptySlotIcons(emptySlotIcons);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		writeEmptySlotTextures(buffer, emptySlotIcons);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}

}
