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
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;

import javax.annotation.Nullable;

public class SyncDatapackSettingsTemplatePacket implements FabricPacket {
	public static final PacketType<SyncDatapackSettingsTemplatePacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_datapack_settings_template"), SyncDatapackSettingsTemplatePacket::new);
	private final String datapack;
	private final String templateName;
	private final CompoundTag settingsNbt;

	public SyncDatapackSettingsTemplatePacket(String datapack, String templateName, @Nullable CompoundTag settingsNbt) {
		this.datapack = datapack;
		this.templateName = templateName;
		this.settingsNbt = settingsNbt;
	}

	public SyncDatapackSettingsTemplatePacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf(), buffer.readUtf(), buffer.readNbt());
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		if (settingsNbt == null) {
			return;
		}
		DatapackSettingsTemplateManager.putTemplate(datapack, templateName, settingsNbt);
		if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
			settingsContainerMenu.refreshTemplateSlots();
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(datapack);
		buffer.writeUtf(templateName);
		buffer.writeNbt(settingsNbt);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
