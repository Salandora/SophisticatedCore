package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.init.ModPayloads;

public class EmiCompat implements EmiPlugin, ICompat {
    @Override
    public void register(EmiRegistry registry) {
    }

    @Override
    public void setup() {
		ModPayloads.registerC2S(EmiFillRecipePacket.TYPE, EmiFillRecipePacket.STREAM_CODEC, EmiFillRecipePacket::handlePayload);
		ModPayloads.registerC2S(EmiSetGhostSlotPayload.TYPE, EmiSetGhostSlotPayload.STREAM_CODEC, EmiSetGhostSlotPayload::handlePayload);
		ModPayloads.registerC2S(EmiSetMemorySlotPayload.TYPE, EmiSetMemorySlotPayload.STREAM_CODEC, EmiSetMemorySlotPayload::handlePayload);
    }
}
