package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.PortingConstants;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface IInventoryPartHandler {
	IInventoryPartHandler EMPTY = () -> "EMPTY";

	default int getSlotLimit(int slot) {
		return 0;
	}

	default boolean isSlotAccessible(int slot) {
		return false;
	}


	default int getStackLimit(int slot, ItemVariant resource) {
		return 0;
	}
/*	default int getStackLimit(int slot, ItemStack stack) {
		return 0;
	}*/

	default long extractItem(int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext transaction) {
		return 0;
	}
/*	default ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}*/

	default long insertItem(int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext transaction, Function4<Integer, ItemVariant, Long, TransactionContext, Long> insertSuper) {
		return maxAmount;
	}
/*	default ItemStack insertItem(int slot, ItemStack stack, boolean simulate, TriFunction<Integer, ItemStack, Boolean, ItemStack> insertSuper) {
		return stack;
	}*/

	default void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		//noop
	}

	default boolean isItemValid(int slot, ItemVariant resource, long amount) {
		return false;
	}
/*	default boolean isItemValid(int slot, ItemStack stack) {
		return false;
	}*/

	default ItemVariant getVariantInSlot(int slot, IntFunction<ItemVariant> getVariantInSlotSuper) {
		return ItemVariant.blank();
	}

	default ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return ItemStack.EMPTY;
	}

	default boolean canBeReplaced() {
		return false;
	}

	default int getSlots() { return 0;}

	String getName();

	@Nullable
	default Pair<ResourceLocation, ResourceLocation> getNoItemIcon(int slot) {
		return null;
	}

	default Item getFilterItem(int slot) {
		return Items.AIR;
	}

	default void onSlotLimitChange() {
		//noop
	}

	default Set<Integer> getNoSortSlots() {
		return Set.of();
	}

	default void onSlotFilterChanged(int slot) {
		//noop
	}

	default boolean isFilterItem(Item item) {
		return false;
	}

	default Map<Item, Set<Integer>> getFilterItems() {
		return Map.of();
	}

	default void onInit() {
		//noop
	}

	class Default implements IInventoryPartHandler {
		public static final String NAME = "default";
		private final InventoryHandler parent;
		private final int slots;

		public Default(InventoryHandler parent, int slots) {
			this.parent = parent;
			this.slots = slots;
		}

		@Override
		public int getSlotLimit(int slot) {
			return parent.getBaseSlotLimit();
		}

		@Override
		public int getStackLimit(int slot, ItemVariant resource) {
			return parent.getBaseStackLimit(resource);
		}

/*		@Override
		public int getStackLimit(int slot, ItemStack stack) {
			return parent.getBaseStackLimit(stack);
		}*/

		@Override
		public long extractItem(int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext transaction) {
			return parent.extractItemInternal(slot, resource, maxAmount, transaction);
		}

/*		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return parent.extractItemInternal(slot, amount, simulate);
		}*/

		@Override
		public long insertItem(int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext transaction, Function4<Integer, ItemVariant, Long, TransactionContext, Long> insertSuper) {
			return insertSuper.apply(slot, resource, maxAmount, transaction);
		}

		/*		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, TriFunction<Integer, ItemStack, Boolean, ItemStack> insertSuper) {
			return insertSuper.apply(slot, stack, simulate);
		}*/

		@Override
		public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
			setStackInSlotSuper.accept(slot, stack);
		}

		@Override
		public boolean isItemValid(int slot, ItemVariant resource, long amount) {
			return true;
		}

/*		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return true;
		}*/

		@Override
		public ItemVariant getVariantInSlot(int slot, IntFunction<ItemVariant> getVariantInSlotSuper) {
			return getVariantInSlotSuper.apply(slot);
		}

		@Override
		public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
			return getStackInSlotSuper.apply(slot);
		}

		@Override
		public boolean canBeReplaced() {
			return true;
		}

		@Override
		public boolean isSlotAccessible(int slot) {
			return true;
		}

		@Override
		public int getSlots() {
			return slots;
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	interface Factory {
		IInventoryPartHandler create(InventoryHandler parent, InventoryPartitioner.SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings);
	}
}
