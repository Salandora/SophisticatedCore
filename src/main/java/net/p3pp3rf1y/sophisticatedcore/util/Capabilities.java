package net.p3pp3rf1y.sophisticatedcore.util;

import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryStorageWrapper;

import java.util.List;

public class Capabilities {
	public static class ItemHandler {
		/**
		 * Capability for the inventory of an entity.
		 * If an entity has multiple inventory "subparts", this capability should give a combined view of all the subparts.
		 */
		public static final EntityApiLookup<SlottedStorage<ItemVariant>, Void> ENTITY = EntityApiLookup.get(SophisticatedCore.getRL("entity_item_storage"), (Class<SlottedStorage<ItemVariant>>)(Class<?>)SlottedStorage.class, Void.class);

		/**
		 * Capability for an inventory of entity that should be accessible to automation,
		 * in the sense that droppers, hoppers, and similar modded devices will try to use it.
		 */
		public static final EntityApiLookup<SlottedStorage<ItemVariant>, Direction> ENTITY_AUTOMATION = EntityApiLookup.get(SophisticatedCore.getRL("entity_automation_item_storage"), (Class<SlottedStorage<ItemVariant>>)(Class<?>)SlottedStorage.class, Direction.class);

		static {
			var containerEntities = List.of(
					EntityType.CHEST_BOAT,
					EntityType.CHEST_MINECART,
					EntityType.HOPPER_MINECART);
			for (var entityType : containerEntities) {
				ENTITY.registerForType((entity, ctx) -> InventoryStorageWrapper.of(entity), entityType);
				ENTITY_AUTOMATION.registerForType((entity, direction) -> InventoryStorageWrapper.of(entity), entityType);
			}

			ENTITY.registerForType((player, ctx) -> InventoryStorageWrapper.of(player), EntityType.PLAYER);
		}
	}
}
