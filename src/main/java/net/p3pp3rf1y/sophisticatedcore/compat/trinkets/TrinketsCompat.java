package net.p3pp3rf1y.sophisticatedcore.compat.trinkets;

import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

public class TrinketsCompat implements ICompat {
	@Override
	public void setup() {
		addInventoryItemHandler();
	}

	private void addInventoryItemHandler() {
		InventoryHelper.registerPlayerInventoryProvider(player ->
				TrinketsApi.getTrinketComponent(player)
						.map(comp ->
								comp.getInventory().values().stream()
										.flatMap(group -> group.values().stream())
										.map(inv -> InventoryStorage.of(inv, null))
										.toList()
						)
						.map(list -> (SlottedStorage<ItemVariant>) new CombinedSlottedStorage<>(list))
						.orElse(null));
	}
}
