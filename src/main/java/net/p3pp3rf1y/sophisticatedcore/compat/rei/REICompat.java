package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SetGhostSlotPayload;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.SetMemorySlotPayload;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;

public class REICompat implements ICompat {
	public REICompat() {
	}

	@Override
	public void setup() {
		ModPayloads.registerC2S(SetGhostSlotPayload.TYPE, SetGhostSlotPayload.STREAM_CODEC, SetGhostSlotPayload::handlePayload);
		ModPayloads.registerC2S(SetMemorySlotPayload.TYPE, SetMemorySlotPayload.STREAM_CODEC, SetMemorySlotPayload::handlePayload);
	}
}