package net.p3pp3rf1y.sophisticatedcore.upgrades.infinity;

import com.mojang.datafixers.util.Function4;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.inventory.IInventoryPartHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.util.SlotRange;
import net.p3pp3rf1y.sophisticatedcore.util.TriPredicate;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

public abstract class InfinityInventoryPart implements IInventoryPartHandler {
	private final InventoryHandler parent;
	private final SlotRange slotRange;
	private final int permissionLevel;
	private final Map<Integer, ItemStack> cachedStacks = new HashMap<>();

	protected InfinityInventoryPart(InventoryHandler parent, SlotRange slotRange, int permissionLevel) {
		this.parent = parent;
		this.slotRange = slotRange;
		this.permissionLevel = permissionLevel;
	}

	@Override
	public boolean isInfinite(int slot) {
		return !parent.getSlotStack(slot).isEmpty();
	}

	@Override
	public int getSlotLimit(int slot) {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isItemValid(int slot, ItemVariant resource, int count, @Nullable Player player, TriPredicate<Integer, ItemVariant, Integer> isItemValidSuper) {
		return player != null && player.hasPermissions(permissionLevel) && parent.getSlotStack(slot).isEmpty() && isItemValidSuper.test(slot, resource, count);
	}

	@Override
	public ItemVariant getVariantInSlot(int slot, IntFunction<ItemVariant> getVariantInSlotSuper) {
		return ItemVariant.of(parent.getSlotStack(slot));
	}

	@Override
	public boolean isSlotAccessible(int slot) {
		return true;
	}

	@Override
	public int getStackLimit(int slot, ItemVariant stack) {
		return Integer.MAX_VALUE;
	}

	@Override
	public long extractItem(int slot, ItemVariant resource, long amount, @Nullable TransactionContext ctx) {
		return parent.getSlotStack(slot).copyWithCount((int) amount).getCount();
	}

	@Override
	public long insertItem(int slot, ItemVariant resource, long maxAmount, @Nullable TransactionContext ctx, Function4<Integer, ItemVariant, Long, TransactionContext, Long> insertSuper) {
		if (!parent.getSlotStack(slot).isEmpty()) {
			return 0;
		}
		cachedStacks.remove(slot);
		return insertSuper.apply(slot, resource, maxAmount, ctx);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		if (parent.getSlotStack(slot).isEmpty()) {
			cachedStacks.remove(slot);
			parent.setSlotStack(slot, stack);
		}
	}

	@Override
	public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		if (cachedStacks.containsKey(slot) && cachedStacks.get(slot).isEmpty() != parent.getSlotStack(slot).isEmpty()) {
			cachedStacks.remove(slot);
		}

		return cachedStacks.computeIfAbsent(slot, s ->  parent.getSlotStack(s).copyWithCount(Integer.MAX_VALUE));
	}

	@Override
	public int getSlots() {
		return slotRange.numberOfSlots();
	}

	public static class Admin extends InfinityInventoryPart {
		public static final String NAME = "infinity";

		protected Admin(InventoryHandler parent, SlotRange slotRange) {
			super(parent, slotRange, 2);
		}

		@Override
		public String getName() {
			return NAME;
		}
	}

	public static class Survival extends InfinityInventoryPart {
		public static final String NAME = "survival_infinity";

		protected Survival(InventoryHandler parent, SlotRange slotRange) {
			super(parent, slotRange, 0);
		}

		@Override
		public String getName() {
			return NAME;
		}
	}
}
