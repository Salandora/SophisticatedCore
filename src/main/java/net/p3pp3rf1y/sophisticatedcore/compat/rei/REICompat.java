package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class REICompat implements ICompat {
	public REICompat() {
	}

	@Override
	public void setup() {
		PacketHandler.INSTANCE.registerMessage(SetGhostSlotMessage.class, SetGhostSlotMessage::encode, SetGhostSlotMessage::decode, SetGhostSlotMessage::onMessage);
		PacketHandler.INSTANCE.registerMessage(SetMemorySlotMessage.class, SetMemorySlotMessage::encode, SetMemorySlotMessage::decode, SetMemorySlotMessage::onMessage);
	}
}