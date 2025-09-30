package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import com.github.salandora.sophisticatedlibrary.network.PayloadRegistrar;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class EmiCompat implements EmiPlugin, ICompat {
    @Override
    public void register(EmiRegistry registry) {
    }

    @Override
    public void setup() {
		final PayloadRegistrar registrar = PayloadRegistrar.registrar();
		registrar.playToServer(EmiFillRecipePacket.TYPE, EmiFillRecipePacket.STREAM_CODEC, EmiFillRecipePacket::handlePayload);
		registrar.playToServer(EmiSetGhostSlotPayload.TYPE, EmiSetGhostSlotPayload.STREAM_CODEC, EmiSetGhostSlotPayload::handlePayload);
		registrar.playToServer(EmiSetMemorySlotPayload.TYPE, EmiSetMemorySlotPayload.STREAM_CODEC, EmiSetMemorySlotPayload::handlePayload);
    }
}
