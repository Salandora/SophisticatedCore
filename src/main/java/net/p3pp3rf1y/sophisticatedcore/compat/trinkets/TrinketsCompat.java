package net.p3pp3rf1y.sophisticatedcore.compat.trinkets;

import com.github.salandora.sophisticatedlibrary.transfer.api.v1.EmptyItemHandler;
import com.github.salandora.sophisticatedlibrary.transfer.api.v1.IItemHandler;
import com.github.salandora.sophisticatedlibrary.transfer.api.v1.IItemHandlerModifiable;
import com.github.salandora.sophisticatedlibrary.transfer.api.v1.wrapper.CombinedInvWrapper;
import com.github.salandora.sophisticatedlibrary.transfer.api.v1.wrapper.InvWrapper;
import dev.emi.trinkets.api.TrinketsApi;
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
									.flatMap(group -> group.values().stream().map(InvWrapper::of))
									.toArray(IItemHandlerModifiable[]::new)
						)
						.<IItemHandler>map(CombinedInvWrapper::new)
						.orElse(EmptyItemHandler.INSTANCE));
	}
}
