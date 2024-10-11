package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;

public class REICompat implements ICompat {
	public REICompat() {
	}

	@Override
	public void setup() {
		ServerPlayNetworking.registerGlobalReceiver(SetGhostSlotMessage.TYPE, SetGhostSlotMessage::handle);
		ServerPlayNetworking.registerGlobalReceiver(SetMemorySlotMessage.TYPE, SetMemorySlotMessage::handle);
	}
}