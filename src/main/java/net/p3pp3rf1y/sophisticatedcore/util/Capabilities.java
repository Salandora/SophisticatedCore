package net.p3pp3rf1y.sophisticatedcore.util;

import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.world.entity.EntityType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.PlayerInventoryStorageWrapper;

public class Capabilities {
	public static class ItemHandler {
		public static final EntityApiLookup<PlayerInventoryStorageWrapper, Void> ENTITY = EntityApiLookup. get(SophisticatedCore.getRL("entity_api"), PlayerInventoryStorageWrapper.class, Void.class);

		static {
			ENTITY.registerForType((player, ignored) -> PlayerInventoryStorageWrapper.of(player), EntityType.PLAYER);
		}
	}
}
