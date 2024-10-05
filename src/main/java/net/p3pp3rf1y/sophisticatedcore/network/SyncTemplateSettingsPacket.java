package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;

import java.util.Map;

public class SyncTemplateSettingsPacket implements FabricPacket {
	public static final PacketType<SyncTemplateSettingsPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_template_settings"), SyncTemplateSettingsPacket::new);
	private final Map<Integer, CompoundTag> playerTemplates;
	private final Map<String, CompoundTag> playerNamedTemplates;

	public SyncTemplateSettingsPacket(Map<Integer, CompoundTag> playerTemplates, Map<String, CompoundTag> playerNamedTemplates) {
		this.playerTemplates = playerTemplates;
		this.playerNamedTemplates = playerNamedTemplates;
	}

	public SyncTemplateSettingsPacket(FriendlyByteBuf buffer) {
		this(buffer.readMap(FriendlyByteBuf::readInt, FriendlyByteBuf::readNbt), buffer.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readNbt));
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get();
		settingsTemplateStorage.clearPlayerTemplates(player);
		playerTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerTemplate(player, k, v));
		playerNamedTemplates.forEach((k, v) -> settingsTemplateStorage.putPlayerNamedTemplate(player, k, v));
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeMap(playerTemplates, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeNbt);
		buffer.writeMap(playerNamedTemplates, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeNbt);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
