package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;

@SuppressWarnings("unused")
public class JeiCompat implements ICompat {
	@Override
	public void setup() {
		ServerPlayNetworking.registerGlobalReceiver(TransferRecipeMessage.TYPE, TransferRecipeMessage::handle);
		ServerPlayNetworking.registerGlobalReceiver(SetGhostSlotMessage.TYPE, SetGhostSlotMessage::handle);
		ServerPlayNetworking.registerGlobalReceiver(SetMemorySlotMessage.TYPE, SetMemorySlotMessage::handle);
	}
}
