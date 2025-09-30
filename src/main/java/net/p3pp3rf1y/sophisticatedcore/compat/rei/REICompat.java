package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import com.github.salandora.sophisticatedlibrary.network.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class REICompat implements ICompat {
	public REICompat() {
	}

	@Override
	public void setup() {
		final PayloadRegistrar registrar = PayloadRegistrar.registrar();
		registrar.playToServer(REISetGhostSlotPayload.TYPE, REISetGhostSlotPayload.STREAM_CODEC, REISetGhostSlotPayload::handlePayload);
		registrar.playToServer(REISetMemorySlotPayload.TYPE, REISetMemorySlotPayload.STREAM_CODEC, REISetMemorySlotPayload::handlePayload);
		registrar.playToServer(REIMoveItemsPayload.TYPE, REIMoveItemsPayload.STREAM_CODEC, REIMoveItemsPayload::handlePayload);
	}
}