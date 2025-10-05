package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import com.github.salandora.sophisticatedlibrary.network.api.v1.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

public class JeiCompat implements ICompat {
	@Override
	public void setup() {
		final PayloadRegistrar registrar = PayloadRegistrar.registrar();
		registrar.playToServer(TransferRecipePayload.TYPE, TransferRecipePayload.STREAM_CODEC, TransferRecipePayload::handlePayload);
		registrar.playToServer(SetGhostSlotPayload.TYPE, SetGhostSlotPayload.STREAM_CODEC, SetGhostSlotPayload::handlePayload);
		registrar.playToServer(SetMemorySlotPayload.TYPE, SetMemorySlotPayload.STREAM_CODEC, SetMemorySlotPayload::handlePayload);
	}
}
