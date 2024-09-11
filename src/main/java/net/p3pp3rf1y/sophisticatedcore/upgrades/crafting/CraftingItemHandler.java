package net.p3pp3rf1y.sophisticatedcore.upgrades.crafting;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CraftingItemHandler extends TransientCraftingContainer {
	private final Supplier<SlottedStackStorage> supplyInventory;
	private final Consumer<Container> onCraftingMatrixChanged;
	private boolean itemsInitialized = false;
	private List<ItemStack> items = List.of();

	public CraftingItemHandler(Supplier<SlottedStackStorage> supplyInventory, Consumer<Container> onCraftingMatrixChanged) {
		super(new AbstractContainerMenu(null, -1) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player playerIn) {
				return false;
			}
		}, 3, 3);
		this.supplyInventory = supplyInventory;
		this.onCraftingMatrixChanged = onCraftingMatrixChanged;
	}

	@Override
	public int getContainerSize() {
		return supplyInventory.get().getSlotCount();
	}

	@Override
	public boolean isEmpty() {
		return InventoryHelper.isEmpty(supplyInventory.get());
	}

	@Override
	public ItemStack getItem(int index) {
		SlottedStackStorage itemHandler = supplyInventory.get();
		return index >= itemHandler.getSlotCount() ? ItemStack.EMPTY : itemHandler.getStackInSlot(index);
	}

	@Override
	public List<ItemStack> getItems() {
		if (!itemsInitialized) {
			items = new ArrayList<>();
			for (int slot = 0; slot < supplyInventory.get().getSlotCount(); slot++) {
				items.add(supplyInventory.get().getStackInSlot(slot));
			}
			itemsInitialized = true;
		}
		return items;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return InventoryHelper.getAndRemove(supplyInventory.get(), index);
	}

	@Override
	public ItemStack removeItem(int index, int count) {
		ItemVariant resource = ItemVariant.of(supplyInventory.get().getStackInSlot(index));

		long amount;
		try (Transaction ctx = Transaction.openOuter()) {
			amount = supplyInventory.get().extractSlot(index, resource, count, ctx);
			ctx.commit();
		}
		if (amount > 0) {
			itemsInitialized = false;
			onCraftingMatrixChanged.accept(this);
		}

		return resource.toStack((int) amount);
	}

	@Override
	public void setItem(int index, ItemStack stack) {
		supplyInventory.get().setStackInSlot(index, stack);
		onCraftingMatrixChanged.accept(this);
		itemsInitialized = false;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		itemsInitialized = false;
	}

	@Override
	public void fillStackedContents(StackedContents helper) {
		for (var view : supplyInventory.get().nonEmptyViews()) {
			ItemStack stack = view.getResource().toStack((int) view.getAmount());
			helper.accountSimpleStack(stack);
		}
	}

}
