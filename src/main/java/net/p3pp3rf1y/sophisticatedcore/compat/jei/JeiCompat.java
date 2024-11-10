package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;

public class JeiCompat implements ICompat {
	@Override
	public void setup() {
		ModPayloads.registerC2S(TransferRecipePayload.TYPE, TransferRecipePayload.STREAM_CODEC, TransferRecipePayload::handlePayload);
		ModPayloads.registerC2S(SetGhostSlotPayload.TYPE, SetGhostSlotPayload.STREAM_CODEC, SetGhostSlotPayload::handlePayload);
		ModPayloads.registerC2S(SetMemorySlotPayload.TYPE, SetMemorySlotPayload.STREAM_CODEC, SetMemorySlotPayload::handlePayload);
	}
}
