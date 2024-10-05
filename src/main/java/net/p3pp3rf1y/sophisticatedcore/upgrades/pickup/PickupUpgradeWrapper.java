package net.p3pp3rf1y.sophisticatedcore.upgrades.pickup;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ContentsFilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IContentsFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IPickupResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import java.util.function.Consumer;

public class PickupUpgradeWrapper extends UpgradeWrapperBase<PickupUpgradeWrapper, PickupUpgradeItem>
		implements IPickupResponseUpgrade, IContentsFilteredUpgrade {
	private final ContentsFilterLogic filterLogic;

	public PickupUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new ContentsFilterLogic(upgrade, stack -> save(), upgradeItem.getFilterSlotCount(), storageWrapper::getInventoryHandler, storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
	}

	@Override
	public ItemStack pickup(Level level, ItemStack stack, TransactionContext ctx) {
		if (!filterLogic.matchesFilter(stack)) {
			return stack;
		}

		ItemVariant resource = ItemVariant.of(stack);
		long inserted = storageWrapper.getInventoryForUpgradeProcessing().insert(resource, stack.getCount(), ctx);
		return resource.toStack(stack.getCount() - (int) inserted);
	}

	@Override
	public ContentsFilterLogic getFilterLogic() {
		return filterLogic;
	}
}
