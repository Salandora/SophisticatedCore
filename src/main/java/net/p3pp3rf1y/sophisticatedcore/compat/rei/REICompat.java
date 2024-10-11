package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

public class REICompat implements ICompat {
	public REICompat() {
	}

	@Override
	public void setup() {
		PacketHandler.registerC2SMessage(SetGhostSlotMessage.class, SetGhostSlotMessage::new);
		PacketHandler.registerC2SMessage(SetMemorySlotMessage.class, SetMemorySlotMessage::new);
	}
}