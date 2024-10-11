package net.p3pp3rf1y.sophisticatedcore.upgrades.compacting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.api.ISlotChangeResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.upgrades.*;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper.CompactingShape;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CompactingUpgradeWrapper extends UpgradeWrapperBase<CompactingUpgradeWrapper, CompactingUpgradeItem>
		implements IInsertResponseUpgrade, IFilteredUpgrade, ISlotChangeResponseUpgrade, ITickableUpgrade {
	private final FilterLogic filterLogic;
	private final Set<Integer> slotsToCompact = new HashSet<>();

	public CompactingUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		filterLogic = new FilterLogic(upgrade, upgradeSaveHandler, upgradeItem.getFilterSlotCount(),
				stack -> !stack.hasTag() && !RecipeHelper.getItemCompactingShapes(stack.getItem()).isEmpty());
	}

	@Override
	public long onBeforeInsert(IItemHandlerSimpleInserter inventoryHandler, int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
		return maxAmount;
	}

	@Override
	public void onAfterInsert(IItemHandlerSimpleInserter inventoryHandler, int slot, @Nullable TransactionContext ctx) {
		compactSlot(inventoryHandler, slot, ctx);
	}

	private void compactSlot(IItemHandlerSimpleInserter inventoryHandler, int slot, @Nullable TransactionContext ctx) {
		ItemStack slotStack = inventoryHandler.getStackInSlot(slot);

		if (slotStack.isEmpty() || slotStack.hasTag() || !filterLogic.matchesFilter(slotStack)) {
			return;
		}

		Item item = slotStack.getItem();

		Set<CompactingShape> shapes = RecipeHelper.getItemCompactingShapes(item);

		if (upgradeItem.shouldCompactThreeByThree() && (shapes.contains(CompactingShape.THREE_BY_THREE_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.THREE_BY_THREE)))) {
			tryCompacting(inventoryHandler, item, 3, 3, ctx);
		} else if (shapes.contains(CompactingShape.TWO_BY_TWO_UNCRAFTABLE) || (shouldCompactNonUncraftable() && shapes.contains(CompactingShape.TWO_BY_TWO))) {
			tryCompacting(inventoryHandler, item, 2, 2, ctx);
		}
	}

	private void tryCompacting(IItemHandlerSimpleInserter inventoryHandler, Item item, int width, int height, @Nullable TransactionContext ctx) {
		long totalCount = (long) width * height;
		RecipeHelper.CompactingResult compactingResult = RecipeHelper.getCompactingResult(item, width, height);
		if (!compactingResult.getResult().isEmpty()) {
			ItemVariant resource = ItemVariant.of(item);
			long extracted = StorageUtil.simulateExtract(inventoryHandler, resource, totalCount, ctx);
			if (extracted != totalCount) {
				return;
			}

			ItemVariant resultVariant = ItemVariant.of(compactingResult.getResult());
			while (extracted == totalCount) {
				List<ItemStack> remainingItemsCopy = compactingResult.getRemainingItems().isEmpty() ? Collections.emptyList() : compactingResult.getRemainingItems().stream().map(ItemStack::copy).toList();

				if (!fitsResultAndRemainingItems(inventoryHandler, remainingItemsCopy, compactingResult.getResult().copy(), ctx)) {
					break;
				}

				try (Transaction insertContext = Transaction.openNested(ctx)) {
					inventoryHandler.extract(resource, totalCount, insertContext);
					inventoryHandler.insert(resultVariant, compactingResult.getResult().getCount(), insertContext);
					InventoryHelper.insertIntoInventory(remainingItemsCopy, inventoryHandler, insertContext);
					insertContext.commit();
				}

				extracted = StorageUtil.simulateExtract(inventoryHandler, resource, totalCount, ctx);
			}
		}
	}

	private boolean fitsResultAndRemainingItems(IItemHandlerSimpleInserter inventoryHandler, List<ItemStack> remainingItems, ItemStack result, @Nullable TransactionContext ctx) {
		if (!remainingItems.isEmpty()) {
			try (Transaction insertSimulation = Transaction.openNested(ctx)) {
				return InventoryHelper.insertIntoInventory(inventoryHandler, ItemVariant.of(result), result.getCount(), insertSimulation).isEmpty()
						&& InventoryHelper.insertIntoInventory(remainingItems, inventoryHandler, insertSimulation).isEmpty();
			}
		}
		return InventoryHelper.simulateInsertIntoInventory(inventoryHandler, ItemVariant.of(result), result.getCount(), ctx).isEmpty();
	}

	@Override
	public FilterLogic getFilterLogic() {
		return filterLogic;
	}

	public boolean shouldCompactNonUncraftable() {
		return NBTHelper.getBoolean(upgrade, "compactNonUncraftable").orElse(false);
	}

	public void setCompactNonUncraftable(boolean shouldCompactNonUncraftable) {
		NBTHelper.setBoolean(upgrade, "compactNonUncraftable", shouldCompactNonUncraftable);
		save();
	}

	@Override
	public void onSlotChange(SlottedStackStorage inventoryHandler, int slot) {
		if (shouldWorkInGUI()) {
			slotsToCompact.add(slot);
		}
	}

	public void setShouldWorkdInGUI(boolean shouldWorkdInGUI) {
		NBTHelper.setBoolean(upgrade, "shouldWorkInGUI", shouldWorkdInGUI);
		save();
	}

	public boolean shouldWorkInGUI() {
		return NBTHelper.getBoolean(upgrade, "shouldWorkInGUI").orElse(false);
	}

	@Override
	public void tick(@Nullable LivingEntity entity, Level level, BlockPos pos) {
		if (slotsToCompact.isEmpty()) {
			return;
		}

		for (int slot : slotsToCompact) {
			try (Transaction ctx = Transaction.openOuter()) {
				compactSlot(storageWrapper.getInventoryHandler(), slot, ctx);
				ctx.commit();
			}
		}

		slotsToCompact.clear();
	}
}
