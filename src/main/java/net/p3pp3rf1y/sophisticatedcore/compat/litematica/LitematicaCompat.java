package net.p3pp3rf1y.sophisticatedcore.compat.litematica;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class LitematicaCompat implements ICompat {
	public record LitematicaWrapper(IStorageWrapper wrapper, Function<UUID, FabricPacket> packetGenerator) {
	}

	public static final ItemApiLookup<LitematicaWrapper, Void> LITEMATICA_CAPABILITY = ItemApiLookup.get(SophisticatedCore.getRL("sophisticatedcore_requestcontents"), LitematicaWrapper.class, Void.class);

	public static Optional<LitematicaWrapper> getWrapper(ItemStack provider) {
		return Optional.ofNullable(LITEMATICA_CAPABILITY.find(provider, null));
	}

	@Override
	public void setup() {
	}
}
