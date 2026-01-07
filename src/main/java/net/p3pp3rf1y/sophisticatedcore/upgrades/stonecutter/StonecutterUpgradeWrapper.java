package net.p3pp3rf1y.sophisticatedcore.upgrades.stonecutter;

import com.github.salandora.sophisticatedfabriclib.transfer.api.v1.IItemHandlerModifiable;
import com.github.salandora.sophisticatedfabriclib.transfer.api.v1.ItemStackHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.SimpleItemContent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public class StonecutterUpgradeWrapper extends UpgradeWrapperBase<StonecutterUpgradeWrapper, StonecutterUpgradeItem> {
	private final IItemHandlerModifiable inputInventory;

	protected StonecutterUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);

		inputInventory = new ItemStackHandler(1) {
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				if (slot == 0) {
					upgrade.sophisticatedLibrary_set(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.copyOf(getStackInSlot(0)));
				}
				save();
			}
		};
		inputInventory.setStackInSlot(0, upgrade.sophisticatedLibrary_getOrDefault(ModCoreDataComponents.INPUT_ITEM, SimpleItemContent.EMPTY).copy());
	}

	public IItemHandlerModifiable getInputInventory() {
		return inputInventory;
	}

	public void setRecipeId(@Nullable ResourceLocation recipeId) {
		if (recipeId == null) {
			upgrade.sophisticatedLibrary_remove(ModCoreDataComponents.RECIPE_ID);
			return;
		}
		upgrade.sophisticatedLibrary_set(ModCoreDataComponents.RECIPE_ID, recipeId);
		save();
	}

	public Optional<ResourceLocation> getRecipeId() {
		return Optional.ofNullable(upgrade.sophisticatedLibrary_get(ModCoreDataComponents.RECIPE_ID));
	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgrade.sophisticatedLibrary_getOrDefault(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, true);
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgrade.sophisticatedLibrary_set(ModCoreDataComponents.SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage);
		save();
	}
}
