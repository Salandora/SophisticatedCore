package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.settings.DatapackSettingsTemplateManager;

import javax.annotation.Nullable;

public class SyncDatapackSettingsTemplateMessage extends SimplePacketBase {
	private final String datapack;
	private final String templateName;
	private final CompoundTag settingsNbt;

	public SyncDatapackSettingsTemplateMessage(String datapack, String templateName, @Nullable CompoundTag settingsNbt) {
		this.datapack = datapack;
		this.templateName = templateName;
		this.settingsNbt = settingsNbt;
	}

	public SyncDatapackSettingsTemplateMessage(FriendlyByteBuf packetBuffer) {
		this(packetBuffer.readUtf(), packetBuffer.readUtf(), packetBuffer.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(this.datapack);
		buffer.writeUtf(this.templateName);
		buffer.writeNbt(this.settingsNbt);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Player player = context.getClientPlayer();
			if (player == null) {
				return;
			}

			DatapackSettingsTemplateManager.putTemplate(this.datapack, this.templateName, this.settingsNbt);
			if (player.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu) {
				settingsContainerMenu.refreshTemplateSlots();
			}
		});
		return true;
	}
}
