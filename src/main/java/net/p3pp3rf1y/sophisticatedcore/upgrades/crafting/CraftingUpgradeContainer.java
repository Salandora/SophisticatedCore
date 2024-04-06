package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ICraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SlotSuppliedHandler;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RecipeHelper;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

public class CraftingUpgradeContainer extends UpgradeContainerBase<CraftingUpgradeWrapper, CraftingUpgradeContainer> implements ICraftingContainer {
	private static final String DATA_SHIFT_CLICK_INTO_STORAGE = "shiftClickIntoStorage";
	private final ResultContainer craftResult = new ResultContainer();
	private final CraftingItemHandler craftMatrix;
	private final ResultSlot craftingResultSlot;
	@Nullable
	private CraftingRecipe lastRecipe = null;

	public CraftingUpgradeContainer(Player player, int upgradeContainerId, CraftingUpgradeWrapper upgradeWrapper, UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);

		int slot;
		for (slot = 0; slot < upgradeWrapper.getInventory().getSlotCount(); slot++) {
			slots.add(new SlotSuppliedHandler(upgradeWrapper::getInventory, slot, -100, -100) {
				@Override
				public void setChanged() {
					super.setChanged();
					updateCraftingResult(player.level(), player, craftMatrix, craftResult, craftingResultSlot);
					craftMatrix.setChanged();
				}
			});
		}
		craftMatrix = new CraftingItemHandler(upgradeWrapper::getInventory, this::onCraftMatrixChanged);
		craftingResultSlot = new ResultSlot(player, craftMatrix, craftResult, slot, -100, -100) {
			@Override
			public void onTake(Player thePlayer, ItemStack stack) {
				ItemStack remainingStack = getItem();
				checkTakeAchievements(stack);
				List<ItemStack> items;
				if (lastRecipe != null && lastRecipe.matches(craftMatrix, player.level())) {
					items = lastRecipe.getRemainingItems(craftMatrix);
				} else {
					items = craftMatrix.getItems();
				}
				for (int i = 0; i < items.size(); ++i) {
					ItemStack itemstack = craftMatrix.getItem(i);
					ItemStack itemstack1 = items.get(i);
					if (!itemstack.isEmpty()) {
						craftMatrix.removeItem(i, 1);
						itemstack = craftMatrix.getItem(i);
					}

					if (!itemstack1.isEmpty()) {
						if (itemstack.isEmpty()) {
							craftMatrix.setItem(i, itemstack1);
						} else if (ItemStack.isSameItemSameTags(itemstack, itemstack1)) {
							itemstack1.grow(itemstack.getCount());
							craftMatrix.setItem(i, itemstack1);
						} else if (!player.getInventory().add(itemstack1)) {
							player.drop(itemstack1, false);
						}
					}
					if (thePlayer.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
						Slot slot = slots.get(i);
						storageContainerMenu.setSlotStackToUpdate(slot.index, slot.getItem());
					}
				}

				if (!remainingStack.isEmpty()) {
					player.drop(remainingStack, false);
				}
			}
		};
		slots.add(craftingResultSlot);
	}

	@Override
	public void onInit() {
		super.onInit();
		onCraftMatrixChanged(craftMatrix);
	}

	private void onCraftMatrixChanged(Container iInventory) {
		updateCraftingResult(player.level(), player, craftMatrix, craftResult, craftingResultSlot);
	}

	private void updateCraftingResult(Level level, Player player, CraftingContainer inventory, ResultContainer inventoryResult, ResultSlot craftingResultSlot) {
		if (!level.isClientSide) {
			ServerPlayer serverplayerentity = (ServerPlayer) player;
			ItemStack itemstack = ItemStack.EMPTY;
			if (lastRecipe != null && lastRecipe.matches(inventory, level)) {
				itemstack = lastRecipe.assemble(inventory, level.registryAccess());
			} else {
				//noinspection ConstantConditions - we're on server and for sure in the world so getServer can't return null here
				Optional<CraftingRecipe> optional = RecipeHelper.safeGetRecipeFor(RecipeType.CRAFTING, inventory, level);
				if (optional.isPresent()) {
					CraftingRecipe craftingRecipe = optional.get();
					if (inventoryResult.setRecipeUsed(level, serverplayerentity, craftingRecipe)) {
						lastRecipe = craftingRecipe;
						itemstack = lastRecipe.assemble(inventory, level.registryAccess());
					} else {
						lastRecipe = null;
					}
				}
			}

			craftingResultSlot.set(itemstack);
			if (serverplayerentity.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu) {
				storageContainerMenu.setSlotStackToUpdate(craftingResultSlot.index, itemstack);
			}
		}
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains(DATA_SHIFT_CLICK_INTO_STORAGE)) {
			setShiftClickIntoStorage(data.getBoolean(DATA_SHIFT_CLICK_INTO_STORAGE));
		}
	}

	@Override
	public ItemStack getSlotStackToTransfer(Slot slot) {
		if (slot == craftingResultSlot) {
			ItemStack slotStack = slot.getItem();
			slotStack.getItem().onCraftedBy(slotStack, player.level(), player);
			return slotStack;
		}
		return super.getSlotStackToTransfer(slot);
	}

	@Override
	public List<Slot> getRecipeSlots() {
		return slots.subList(0, 9);
	}

	@Override
	public Container getCraftMatrix() {
		return craftMatrix;
	}

	public boolean shouldShiftClickIntoStorage() {
		return upgradeWrapper.shouldShiftClickIntoStorage();
	}

	public void setShiftClickIntoStorage(boolean shiftClickIntoStorage) {
		upgradeWrapper.setShiftClickIntoStorage(shiftClickIntoStorage);
		sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_SHIFT_CLICK_INTO_STORAGE, shiftClickIntoStorage));
	}

	@Override
	public boolean mergeIntoStorageFirst(Slot slot) {
		return !(slot instanceof ResultSlot) || shouldShiftClickIntoStorage();
	}

	@Override
	public boolean allowsPickupAll(Slot slot) {
		return slot != craftingResultSlot;
	}
}
