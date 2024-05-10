package net.p3pp3rf1y.sophisticatedcore.compat.litematica;

import me.pepperbell.simplenetworking.S2CPacket;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class LitematicaCompat implements ICompat {
	public static final ItemApiLookup<Function<List<UUID>, S2CPacket>, UUID> REQUEST_CONTENTS_CAPABILITY = ItemApiLookup.get(SophisticatedCore.getRL("sophisticatedcore_requestcontents"), (Class<Function<List<UUID>, S2CPacket>>)(Class<?>) Function.class, UUID.class);

	@Override
	public void setup() {
	}
}
