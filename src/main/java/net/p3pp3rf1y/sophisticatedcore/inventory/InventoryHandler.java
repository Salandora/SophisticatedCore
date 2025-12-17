package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.datafixers.util.Pair;
import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerSlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ISlotLimitUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.util.CodecHelper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public abstract class InventoryHandler extends ItemStackHandler implements ITrackedContentsItemHandler {
	public static final String INVENTORY_TAG = "inventory";
	private static final String PARTITIONER_TAG = "partitioner";
	protected final IStorageWrapper storageWrapper;
	private final CompoundTag contentsNbt;
	private final Runnable saveHandler;
	private final List<IntConsumer> onContentsChangedListeners = new ArrayList<>();
	private boolean persistent = true;
	private final Map<Integer, Tag> stackNbts = new LinkedHashMap<>();

	private ISlotTracker slotTracker = new ISlotTracker.Noop();

	private int baseSlotLimit;
	private int slotLimit;
	private double maxStackSizeMultiplier;
	private boolean isInitializing;
	private final StackUpgradeConfig stackUpgradeConfig;
	private final InventoryPartitioner inventoryPartitioner;
	private Consumer<Set<Item>> filterItemsChangeListener = s -> {
	};
	private final Map<Item, Set<Integer>> filterItemSlots = new HashMap<>();
	private BooleanSupplier shouldInsertIntoEmpty = () -> true;
	private boolean slotLimitInitialized = false;

	protected InventoryHandler(int numberOfInventorySlots, IStorageWrapper storageWrapper, CompoundTag contentsNbt, Runnable saveHandler, int baseSlotLimit, StackUpgradeConfig stackUpgradeConfig) {
		super(numberOfInventorySlots);
		this.stackUpgradeConfig = stackUpgradeConfig;
		isInitializing = true;
		this.storageWrapper = storageWrapper;
		this.contentsNbt = contentsNbt;
		this.saveHandler = saveHandler;
		setBaseSlotLimit(baseSlotLimit);
		RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> deserializeNBT(registryAccess, contentsNbt.getCompound(INVENTORY_TAG)));
		inventoryPartitioner = new InventoryPartitioner(contentsNbt.getCompound(PARTITIONER_TAG), this, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class));
		initStackNbts();

		isInitializing = false;
	}

	public ISlotTracker getSlotTracker() {
		initSlotTracker();
		return slotTracker;
	}

	@Override
	public void setSize(int size) {
		super.setSize(this.getSlotCount());
	}

	private void initStackNbts() {
		stackNbts.clear();
		for (int slot = 0; slot < this.getSlotCount(); slot++) {
			ItemStack slotStack = this.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				stackNbts.put(slot, getSlotsStackNbt(slot, slotStack));
			}
		}
	}

	@Override
	public void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		if (persistent && updateSlotNbt(slot)) {
			saveInventory();
			triggerOnChangeListeners(slot);
		}
	}

	public void triggerOnChangeListeners(int slot) {
		for (IntConsumer onContentsChangedListener : onContentsChangedListeners) {
			onContentsChangedListener.accept(slot);
		}
	}

	@SuppressWarnings("java:S3824")
	//compute use here would be difficult as then there's no way of telling that value was newly created vs different than the one that needs to be set
	private boolean updateSlotNbt(int slot) {
		ItemStack slotStack = getSlotStack(slot);
		if (slotStack.isEmpty()) {
			if (stackNbts.containsKey(slot)) {
				stackNbts.remove(slot);
				return true;
			}
		} else {
			Tag itemTag = getSlotsStackNbt(slot, slotStack);
			if (!stackNbts.containsKey(slot) || !stackNbts.get(slot).equals(itemTag)) {
				stackNbts.put(slot, itemTag);
				return true;
			}
		}
		return false;
	}

	private Tag getSlotsStackNbt(int slot, ItemStack slotStack) {
		CompoundTag itemTag = new CompoundTag();
		itemTag.putInt("Slot", slot);
		return RegistryHelper.getRegistryAccess().map(registryAccess -> CodecHelper.OVERSIZED_ITEM_STACK_CODEC.encode(slotStack, registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag).getOrThrow()).orElse(itemTag);
	}

	private Optional<ItemStack> getStackFromNbt(Tag itemTag, HolderLookup.Provider lookupProvider) {
		return CodecHelper.OVERSIZED_ITEM_STACK_CODEC.parse(lookupProvider.createSerializationContext(NbtOps.INSTANCE), itemTag)
				.resultOrPartial(itemName -> SophisticatedCore.LOGGER.error("Tried to load invalid item: '{}'", itemName));
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
		slotTracker.clear();
		setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : getSlotCount());
		ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
		RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> {
			for (int i = 0; i < tagList.size(); i++) {
				CompoundTag itemTag = tagList.getCompound(i);
				int slot = itemTag.getInt("Slot");
				if (slot >= 0 && slot < getSlotCount()) {
					// Changed to call onStackChange in the load function
					this.getSlot(slot).load(registryAccess, itemTag);
				}
			}
		});
		slotTracker.refreshSlotIndexesFrom(this);
		onLoad();
	}

	public int getBaseSlotLimit() {
		return baseSlotLimit;
	}

	@Override
	public int getInternalSlotLimit(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getSlotLimit(slot);
	}

	@Override
	public int getSlotLimit(int slot) {
		if (!slotLimitInitialized) {
			slotLimitInitialized = true;
			updateSlotLimit();
			inventoryPartitioner.onSlotLimitChange();
		}

		return slotLimit > baseSlotLimit ? slotLimit : inventoryPartitioner.getPartBySlot(slot).getSlotLimit(slot);
	}

	public int getBaseStackLimit(ItemStack stack) {
		if (!stackUpgradeConfig.canStackItem(stack.getItem())) {
			return stack.getMaxStackSize();
		}
		int maxStackSize = stack.isEmpty() ? getBaseSlotLimit() : stack.getMaxStackSize();

		if (baseSlotLimit < 64) {
			return (int) Math.max(1, (double) maxStackSize * baseSlotLimit / 64);
		}

		int limit = MathHelper.intMaxCappedMultiply(maxStackSize, baseSlotLimit / 64);
		int remainder = baseSlotLimit % 64;
		if (remainder > 0) {
			limit = MathHelper.intMaxCappedAddition(limit, remainder * maxStackSize / 64);
		}
		return limit;
	}

	@Override
	protected int getStackLimit(int slot, ItemVariant resource) {
		return getStackLimit(slot, resource.toStack());
	}

	public int getStackLimit(int slot, ItemStack stack) {
		return inventoryPartitioner.getPartBySlot(slot).getStackLimit(slot, stack);
	}

	public Item getFilterItem(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getFilterItem(slot);
	}

	public boolean isFilterItem(Item item) {
		return inventoryPartitioner.isFilterItem(item);
	}

	public void setBaseSlotLimit(int baseSlotLimit) {
		slotLimitInitialized = false; // not the most ideal of places to do this, but base slot limit is set when upgrades change and that's when slot limit needs to be reinitialized as well
		this.baseSlotLimit = baseSlotLimit;
		maxStackSizeMultiplier = baseSlotLimit / 64f;

		if (inventoryPartitioner != null) {
			inventoryPartitioner.onSlotLimitChange();
		}

		if (!isInitializing) {
			slotTracker.refreshSlotIndexesFrom(this);
		}
	}

	private void updateSlotLimit() {
		AtomicInteger slotLimitOverride = new AtomicInteger(baseSlotLimit);
		storageWrapper.getUpgradeHandler().getWrappersThatImplement(ISlotLimitUpgrade.class).forEach(slu -> {
			if (slu.getSlotLimit() > slotLimitOverride.get()) {
				slotLimitOverride.set(slu.getSlotLimit());
			}
		});
		slotLimit = slotLimitOverride.get();
	}

	public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
		if (amount == 0) {
			return ItemStack.EMPTY;
		}

		ItemStack existing = getSlotStack(slot);

		if (existing.isEmpty()) {
			return ItemStack.EMPTY;
		}

		int toExtract = Math.min(amount, existing.getMaxStackSize());

		if (existing.getCount() <= toExtract) {
			if (!simulate) {
				setSlotStack(slot, ItemStack.EMPTY);
				return existing;
			} else {
				return existing.copy();
			}
		} else {
			if (!simulate) {
				setSlotStack(slot, existing.copyWithCount(existing.getCount() - toExtract));
			}

			return existing.copyWithCount(toExtract);
		}
	}

	@Override
	@Nonnull
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return inventoryPartitioner.getPartBySlot(slot).extractItem(slot, amount, simulate);
	}

	@Override
	public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		TransactionCallback.onSuccess(ctx, () -> inventoryPartitioner.getPartBySlot(slot).extractItem(slot, (int) maxAmount, false));
		return inventoryPartitioner.getPartBySlot(slot).extractItem(slot, (int) maxAmount, true).getCount();
	}

	public ItemStack getSlotStack(int slot) {
		return ((InventoryHandlerSlot) this.getSlot(slot)).getInternalStack();
	}

	public void setSlotStack(int slot, ItemStack stack) {
		((InventoryHandlerSlot) this.getSlot(slot)).setInternalNewStack(stack);
		slotTracker.removeAndSetSlotIndexes(this, slot, stack);
		onContentsChanged(slot);
	}

	/// Do not call from an open transaction
	@Override
	public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
		initSlotTracker();
		return slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, slot, stack, simulate);
	}

	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		initSlotTracker();
		TransactionCallback.onSuccess(ctx, () -> slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, slot, resource.toStack((int) maxAmount), false));
		return maxAmount - slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, slot, resource.toStack((int) maxAmount), true).getCount();
	}

	@Nonnull
	public ItemStack insertItemOnlyToSlot(int slot, ItemStack stack, boolean simulate) {
		initSlotTracker();
		if (ItemStack.isSameItemSameComponents(getStackInSlot(slot), stack)) {
			return triggerOverflowUpgrades(insertItemInternal(slot, stack, simulate));
		}

		return insertItemInternal(slot, stack, simulate);
	}

	private void initSlotTracker() {
		if (!(slotTracker instanceof InventoryHandlerSlotTracker)) {
			slotTracker = new InventoryHandlerSlotTracker(storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), filterItemSlots);
			slotTracker.refreshSlotIndexesFrom(this);
			slotTracker.setShouldInsertIntoEmpty(shouldInsertIntoEmpty);
		}
	}

	private ItemStack superInsertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;

		if (!isItemValid(slot, stack))
			return stack;

		if (slot < 0 || slot >= getSlotCount())
			throw new RuntimeException("Slot " + slot + " not in valid range - [0," + getSlotCount() + ")");

		ItemStack existing = this.getSlotStack(slot);

		int limit = getStackLimit(slot, stack);

		if (!existing.isEmpty()) {
			if (!ItemStack.isSameItemSameComponents(stack, existing))
				return stack;

			limit -= existing.getCount();
		}

		if (limit <= 0)
			return stack;

		boolean reachedLimit = stack.getCount() > limit;

		if (!simulate) {
			ItemStack result;
			if (existing.isEmpty()) {
				result = reachedLimit ? stack.copyWithCount(limit) : stack;
			} else {
				existing.grow(reachedLimit ? limit : stack.getCount());
				result = existing;
			}
			this.getSlot(slot).setNewStack(result);
		}

		return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
	}

	private ItemStack insertItemInternal(int slot, ItemStack stack, boolean simulate) {
		ItemStack ret = runOnBeforeInsert(slot, stack, simulate, this, storageWrapper);
		if (ret.isEmpty()) {
			return ret;
		}

		ret = inventoryPartitioner.getPartBySlot(slot).insertItem(slot, ret, simulate, this::superInsertItem);

		if (!simulate) {
			slotTracker.removeAndSetSlotIndexes(this, slot, getStackInSlot(slot));
		}

		if (ret == stack) {
			return ret;
		}

		runOnAfterInsert(slot, simulate, this, storageWrapper);

		return ret;
	}

	private ItemStack triggerOverflowUpgrades(ItemStack ret) {
		for (IOverflowResponseUpgrade overflowUpgrade : storageWrapper.getUpgradeHandler().getWrappersThatImplement(IOverflowResponseUpgrade.class)) {
			ret = overflowUpgrade.onOverflow(ret);
			if (ret.isEmpty()) {
				break;
			}
		}
		return ret;
	}

	private void runOnAfterInsert(int slot, boolean simulate, IItemHandlerSimpleInserter handler, IStorageWrapper storageWrapper) {
		if (!simulate) {
			storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class).forEach(u -> u.onAfterInsert(handler, slot));
		}
	}

	private ItemStack runOnBeforeInsert(int slot, ItemStack stack, boolean simulate, IItemHandlerSimpleInserter handler, IStorageWrapper storageWrapper) {
		List<IInsertResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class);
		ItemStack remaining = stack;
		for (IInsertResponseUpgrade upgrade : wrappers) {
			remaining = upgrade.onBeforeInsert(handler, slot, remaining, simulate);
			if (remaining.isEmpty()) {
				return ItemStack.EMPTY;
			}
		}
		return remaining;
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		inventoryPartitioner.getPartBySlot(slot).setStackInSlot(slot, stack, super::setStackInSlot);
		slotTracker.removeAndSetSlotIndexes(this, slot, stack);
	}

	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return isItemValid(slot, stack, null);
	}

	public boolean isItemValid(int slot, ItemStack stack, @Nullable Player player) {
		return isItemValid(slot, ItemVariant.of(stack), stack.getCount(), player);
	}

	@Override
	public boolean isItemValid(int slot, ItemVariant resource, int count) {
		return isItemValid(slot, resource, count, null);
	}

	public boolean isItemValid(int slot, ItemVariant resource, int count, @Nullable Player player) {
		ItemStack stack = resource.toStack(count);
		return inventoryPartitioner.getPartBySlot(slot).isItemValid(slot, resource, count, player, super::isItemValid)
				&& isAllowed(stack) && storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(slot, stack);
	}

	@Override
	public ItemVariant getVariantInSlot(int slot) {
		return ItemVariant.of(getStackInSlot(slot));
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getStackInSlot(slot, super::getStackInSlot);
	}

	protected final boolean isAllowed(ItemVariant resource) {
		return isAllowed(resource.toStack());
	}

	protected abstract boolean isAllowed(ItemStack stack);

	public void saveInventory() {
		RegistryHelper.getRegistryAccess().ifPresent(registryAccess -> contentsNbt.put(INVENTORY_TAG, serializeNBT(registryAccess)));
		if (inventoryPartitioner != null) {
			//inventory parts may affect inventory slots during their initialization in Inventory Partitioner deserialize,
			// but there's no reason to serialize partitioner at that point as its nbt can't during init/deserialization.
			contentsNbt.put(PARTITIONER_TAG, inventoryPartitioner.serializeNBT());
		}
		saveHandler.run();
	}

	@Nullable
	public Pair<ResourceLocation, ResourceLocation> getNoItemIcon(int slotIndex) {
		return inventoryPartitioner.getNoItemIcon(slotIndex);
	}

	public void copyStacksTo(InventoryHandler otherHandler) {
		InventoryHelper.copyTo(this, otherHandler);
	}

	public void addListener(IntConsumer onContentsChanged) {
		onContentsChangedListeners.add(onContentsChanged);
	}

	public void clearListeners() {
		onContentsChangedListeners.clear();
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider registries) {
		ListTag nbtTagList = new ListTag();
		nbtTagList.addAll(stackNbts.values());
		CompoundTag nbt = new CompoundTag();
		nbt.put("Items", nbtTagList);
		nbt.putInt("Size", getSlotCount());
		return nbt;
	}

	public double getStackSizeMultiplier() {
		return maxStackSizeMultiplier;
	}

	@Override
	@NotNull
	public ItemStack insertItem(ItemStack stack, boolean simulate) {
		initSlotTracker();
		return slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, stack, simulate);
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		initSlotTracker();
		TransactionCallback.onSuccess(ctx, () -> slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, resource.toStack((int) maxAmount), false));
		return maxAmount - slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, resource.toStack((int) maxAmount), true).getCount();
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		long remaining = maxAmount;
		for (int i = 0 ; i < this.getSlotCount() && remaining > 0; i++) {
            if (!getVariantInSlot(i).equals(resource)) {
                continue;
            }

			remaining -= this.extractSlot(i, resource, remaining, ctx);
        }
		return maxAmount - remaining;
	}

	public void changeSlots(int diff) {
		NonNullList<ItemStack> previousStacks = NonNullList.of(ItemStack.EMPTY, IntStream.range(0, getSlotCount()).mapToObj(this::getStackInSlot).toArray(ItemStack[]::new));
		super.setSize(previousStacks.size() + diff);
		for (int slot = 0; slot < previousStacks.size() && slot < getSlotCount(); slot++) {
			((InventoryHandlerSlot) this.getSlot(slot)).setInternalNewStack(previousStacks.get(slot));
		}
		initStackNbts();
		saveInventory();
		slotTracker.refreshSlotIndexesFrom(this);
	}

	@Override
	public Set<ItemStackKey> getTrackedStacks() {
		initSlotTracker();
		HashSet<ItemStackKey> ret = new HashSet<>(slotTracker.getFullStacks());
		ret.addAll(slotTracker.getPartialStacks());
		return ret;
	}

	@Override
	public void registerTrackingListeners(Consumer<ItemStackKey> onAddStackKey, Consumer<ItemStackKey> onRemoveStackKey, Runnable onAddFirstEmptySlot, Runnable onRemoveLastEmptySlot) {
		initSlotTracker();
		slotTracker.registerListeners(onAddStackKey, onRemoveStackKey, onAddFirstEmptySlot, onRemoveLastEmptySlot);
	}

	@Override
	public void unregisterStackKeyListeners() {
		slotTracker.unregisterStackKeyListeners();
	}

	@Override
	public boolean hasEmptySlots() {
		return slotTracker.hasEmptySlots();
	}

	public InventoryPartitioner getInventoryPartitioner() {
		return inventoryPartitioner;
	}

	public boolean isSlotAccessible(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).isSlotAccessible(slot);
	}

	public Set<Integer> getNoSortSlots() {
		return inventoryPartitioner.getNoSortSlots();
	}

	public void onSlotFilterChanged(int slot) {
		inventoryPartitioner.getPartBySlot(slot).onSlotFilterChanged(slot);
	}

	public void registerFilterItemsChangeListener(Consumer<Set<Item>> listener) {
		filterItemsChangeListener = listener;
	}

	public void unregisterFilterItemsChangeListener() {
		filterItemsChangeListener = s -> {
		};
	}

	public void initFilterItems() {
		filterItemSlots.putAll(inventoryPartitioner.getFilterItems());
	}

	public void onFilterItemsChanged() {
		if (inventoryPartitioner == null) {
			return;
		}
		filterItemSlots.clear();
		filterItemSlots.putAll(inventoryPartitioner.getFilterItems());

		filterItemsChangeListener.accept(filterItemSlots.keySet());
	}

	public Set<Item> getFilterItems() {
		return filterItemSlots.keySet();
	}

	public void onInit() {
		if (inventoryPartitioner == null) {
			return;
		}
		inventoryPartitioner.onInit();
		slotTracker = new ISlotTracker.Noop();
	}

	public void setShouldInsertIntoEmpty(BooleanSupplier shouldInsertIntoEmpty) {
		this.shouldInsertIntoEmpty = shouldInsertIntoEmpty;
		slotTracker.setShouldInsertIntoEmpty(shouldInsertIntoEmpty);
	}

	public boolean isInfinite(int slot) {
		return inventoryPartitioner.isInfinite(slot);
	}

	// Fabric
	private List<InventoryHandlerSlot> backingList;

	@Override
	protected ItemStackHandlerSlot makeSlot(int index, ItemStack stack) {
		if (backingList == null) {
			this.backingList = new ArrayList<>();
		}
		while (backingList.size() <= index) {
			backingList.add(new InventoryHandlerSlot(index, this, ItemStack.EMPTY));
		}

		InventoryHandlerSlot slot = backingList.get(index);
		slot.setInternalNewStack(stack);
		return slot;
	}

	private class InventoryHandlerSlot extends ItemStackHandlerSlot {
		public InventoryHandlerSlot(int index, InventoryHandler handler, ItemStack initial) {
			super(index, handler, initial);
		}

		// Make the "get stack" functions return a copy of the item due to how the insertion and extraction is handled in the part inventory handler implementations.
		protected ItemStack getInternalStack() {
			return super.getStack().copy();
		}

		protected void setInternalNewStack(ItemStack stack) {
			super.setStack(stack);
			onStackChange();
		}

		@Override
		public long insert(ItemVariant variant, long maxAmount, TransactionContext ctx) {
			if (variant.isBlank() || maxAmount < 0) {
				return 0;
			}

			return InventoryHandler.this.insertSlot(getIndex(), variant, maxAmount, ctx);
		}

		@Override
		public long extract(ItemVariant variant, long maxAmount, TransactionContext ctx) {
			if (variant.isBlank() || maxAmount < 0) {
				return 0;
			}

			return InventoryHandler.this.extractSlot(getIndex(), variant, maxAmount, ctx);
		}

		@Override
		public void load(HolderLookup.Provider provider, CompoundTag tag) {
			getStackFromNbt(tag, provider).ifPresent(this::setStack);
			onStackChange();
		}
	}
}
