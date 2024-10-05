package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsManager;

import javax.annotation.Nullable;

public class SyncPlayerSettingsPacket implements FabricPacket {
	public static final PacketType<SyncPlayerSettingsPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "sync_player_settings"), SyncPlayerSettingsPacket::new);
	private final String playerTagName;
	@Nullable
	private final CompoundTag settingsNbt;

	public SyncPlayerSettingsPacket(String playerTagName, @Nullable CompoundTag settingsNbt) {
		this.playerTagName = playerTagName;
		this.settingsNbt = settingsNbt;
	}

	public SyncPlayerSettingsPacket(FriendlyByteBuf buffer) {
		this(buffer.readUtf(), buffer.readNbt());
	}

	public void handle(LocalPlayer player, PacketSender responseSender) {
		if (settingsNbt == null) {
			return;
		}
		SettingsManager.setPlayerSettingsTag(player, playerTagName, settingsNbt);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(playerTagName);
		buffer.writeNbt(settingsNbt);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
