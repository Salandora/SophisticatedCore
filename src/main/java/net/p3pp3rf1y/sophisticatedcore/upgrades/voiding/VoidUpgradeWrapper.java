package net.p3pp3rf1y.sophisticatedcore.upgrades.voiding;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IFilteredUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ISlotLimitUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.PrimaryMatch;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class VoidUpgradeWrapper extends UpgradeWrapperBase<VoidUpgradeWrapper, VoidUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade, IOverflowResponseUpgrade, ISlotLimitUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToVoid = new HashSet<>();
	private boolean shouldVoidOverflow;

	public VoidUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount());
		filterLogic.setAllowByDefault(true);
		setShouldVoidOverflowDefaultOrLoadFromNbt(false);
	}

	@Override
	public long onBeforeInsert(IItemHandlerSimpleInserter inventoryHandler, int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
		ItemStack stack = resource.toStack((int) maxAmount);
		if (shouldVoidOverflow && inventoryHandler.getStackInSlot(slot).isEmpty() && (!filterLogic.shouldMatchNbt() || !filterLogic.shouldMatchDurability() || filterLogic.getPrimaryMatch() != PrimaryMatch.ITEM) && filterLogic.matchesFilter(stack)) {
			for (int s = 0; s < inventoryHandler.getSlotCount(); s++) {
				if (s == slot) {
					continue;
				}
				if (stackMatchesFilterStack(inventoryHandler.getStackInSlot(s), stack)) {
					return 0;
				}
			}
			return maxAmount;
		}

		return !shouldVoidOverflow && filterLogic.matchesFilter(stack) ? 0 : maxAmount;
	}

	@Override
	public void onAfterInsert(IItemHandlerSimpleInserter inventoryHandler, int slot, @Nullable TransactionContext ctx) {
		//noop
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		NBTHelper.setBoolean(upgrade, "shouldWorkInGUI", shouldWorkdInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		return NBTHelper.getBoolean(upgrade, "shouldWorkInGUI").orElse(false);
	}

	public void setShouldVoidOverflow(boolean shouldVoidOverflow) {
		if (!shouldVoidOverflow && !upgradeItem.isVoidAnythingEnabled()) {
			return;
		}

		this.shouldVoidOverflow = shouldVoidOverflow;
		NBTHelper.setBoolean(upgrade, "shouldVoidOverflow", shouldVoidOverflow);
		save();
	}

	public void setShouldVoidOverflowDefaultOrLoadFromNbt(boolean shouldVoidOverflowDefault) {
		shouldVoidOverflow = !upgradeItem.isVoidAnythingEnabled() || NBTHelper.getBoolean(upgrade, "shouldVoidOverflow").orElse(shouldVoidOverflowDefault);
	}

	public boolean shouldVoidOverflow() {
		return !upgradeItem.isVoidAnythingEnabled() || shouldVoidOverflow;
	}

	@Override
	public void onSlotChange(SlottedStackStorage inventoryHandler, int slot) {
		if (!shouldWorkInGUI() || shouldVoidOverflow()) {
			return;
		}

		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);
		if (filterLogic.matchesFilter(slotStack)) {
			slotsToVoid.add(slot);
		}
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level world, BlockPos pos) {
		if (slotsToVoid.isEmpty()) {
			return;
		}

		InventoryHandler storageInventory = storageWrapper.getInventoryHandler();
		for (int slot : slotsToVoid) {
			ItemStack stack = storageInventory.getStackInSlot(slot);
			try (Transaction outer = Transaction.openOuter()) {
				storageInventory.extractSlot(slot, ItemVariant.of(stack), stack.getCount(), outer);
				outer.commit();
			}
		}

		slotsToVoid.clear();
	}

	@Override
	public boolean worksInGui() {
		return shouldWorkInGUI();
	}

	@Override
	public ItemStack onOverflow(ItemStack stack) {
		return filterLogic.matchesFilter(stack) ? ItemStack.EMPTY : stack;
	}

	@Override
	public boolean stackMatchesFilter(ItemStack stack) {
		return filterLogic.matchesFilter(stack);
	}

	public boolean isVoidAnythingEnabled() {
		return upgradeItem.isVoidAnythingEnabled();
	}

	@Override
	public int getSlotLimit() {
		return Integer.MAX_VALUE;
	}
}
