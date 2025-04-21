package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;

public class REICompat implements ICompat {
	public REICompat() {
	}

	@Override
	public void setup() {
		ModPayloads.registerC2S(REISetGhostSlotPayload.TYPE, REISetGhostSlotPayload.STREAM_CODEC, REISetGhostSlotPayload::handlePayload);
		ModPayloads.registerC2S(REISetMemorySlotPayload.TYPE, REISetMemorySlotPayload.STREAM_CODEC, REISetMemorySlotPayload::handlePayload);

		ModPayloads.registerC2S(REIMoveItemsPayload.TYPE, REIMoveItemsPayload.STREAM_CODEC, REIMoveItemsPayload::handlePayload);
	}
}