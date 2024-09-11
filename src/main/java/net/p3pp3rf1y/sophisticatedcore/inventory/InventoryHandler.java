package net.p3pp3rf1y.sophisticatedcore.inventory;

import com.mojang.datafixers.util.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandlerSlot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IInsertResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IOverflowResponseUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ISlotLimitUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeConfig;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.MathHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public abstract class InventoryHandler extends ItemStackHandler implements ITrackedContentsItemHandler {
	public static final String INVENTORY_TAG = "inventory";
	private static final String PARTITIONER_TAG = "partitioner";
	private static final String REAL_COUNT_TAG = "realCount";
	protected final IStorageWrapper storageWrapper;
	private final CompoundTag contentsNbt;
	private final Runnable saveHandler;
	private final List<IntConsumer> onContentsChangedListeners = new ArrayList<>();
	private boolean persistent = true;
	private final Map<Integer, CompoundTag> stackNbts = new LinkedHashMap<>();

	private ISlotTracker slotTracker = new ISlotTracker.Noop();

	private int baseSlotLimit;
	private int slotLimit;
	private double maxStackSizeMultiplier;
	private boolean isInitializing;
	private final StackUpgradeConfig stackUpgradeConfig;
	private final InventoryPartitioner inventoryPartitioner;
	private Consumer<Set<Item>> filterItemsChangeListener = s -> {};
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
		deserializeNBT(contentsNbt.getCompound(INVENTORY_TAG));
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
			CompoundTag itemTag = getSlotsStackNbt(slot, slotStack);
			if (!stackNbts.containsKey(slot) || !stackNbts.get(slot).equals(itemTag)) {
				stackNbts.put(slot, itemTag);
				return true;
			}
		}
		return false;
	}

	private CompoundTag getSlotsStackNbt(int slot, ItemStack slotStack) {
		CompoundTag itemTag = new CompoundTag();
		itemTag.putInt("Slot", slot);
		itemTag.putInt(REAL_COUNT_TAG, slotStack.getCount());
		slotStack.save(itemTag);
		return itemTag;
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		slotTracker.clear();
		setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : getSlotCount());
		ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < tagList.size(); i++) {
			CompoundTag itemTags = tagList.getCompound(i);
			int slot = itemTags.getInt("Slot");

			if (slot >= 0 && slot < getSlotCount()) {
				this.getSlot(slot).load(itemTags);
				/*if (itemTags.contains(REAL_COUNT_TAG)) {
					super.getStackInSlot(slot).setCount(itemTags.getInt(REAL_COUNT_TAG));
				}*/
			}
		}
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

	public int getBaseStackLimit(ItemVariant resource) {
		if (!stackUpgradeConfig.canStackItem(resource.getItem())) {
			return resource.getItem().getMaxStackSize();
		}

		int limit = MathHelper.intMaxCappedMultiply(resource.getItem().getMaxStackSize(), (baseSlotLimit / 64));
		int remainder = baseSlotLimit % 64;
		if (remainder > 0) {
			limit = MathHelper.intMaxCappedAddition(limit, remainder * resource.getItem().getMaxStackSize() / 64);
		}
		return limit;
	}

	@Override
	public int getStackLimit(int slot, ItemVariant resource) {
		return inventoryPartitioner.getPartBySlot(slot).getStackLimit(slot, resource);
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

	public long extractItemInternal(int slot, ItemVariant resource, long amount, TransactionContext ctx) {
		long extracted = super.extractSlot(slot, resource, amount, ctx);
		TransactionCallback.onSuccess(ctx, () -> {
			slotTracker.removeAndSetSlotIndexes(this, slot, getSlotStack(slot));
			onContentsChanged(slot);
		});
		return extracted;
	}

	@Override
	public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return inventoryPartitioner.getPartBySlot(slot).extractItem(slot, resource, maxAmount, ctx);
	}

	public ItemStack getSlotStack(int slot) {
		return ((InventoryHandlerSlot) this.getSlot(slot)).getInternalStack();
	}

	public void setSlotStack(int slot, ItemStack stack) {
		((InventoryHandlerSlot) this.getSlot(slot)).setInternalNewStack(stack);
		slotTracker.removeAndSetSlotIndexes(this, slot, stack);
		onContentsChanged(slot);
	}

	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		initSlotTracker();
		return maxAmount - slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, slot, resource, maxAmount, ctx);
	}

	public long insertItemOnlyToSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		initSlotTracker();
		if (ItemStack.isSameItemSameTags(getStackInSlot(slot), resource.toStack())) {
			return maxAmount - triggerOverflowUpgrades(resource.toStack((int)(maxAmount - insertItemInternal(slot, resource, maxAmount, ctx)))).getCount();
		}

		return insertItemInternal(slot, resource, maxAmount, ctx);
	}

	private void initSlotTracker() {
		if (!(slotTracker instanceof InventoryHandlerSlotTracker)) {
			slotTracker = new InventoryHandlerSlotTracker(storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), filterItemSlots);
			slotTracker.refreshSlotIndexesFrom(this);
			slotTracker.setShouldInsertIntoEmpty(shouldInsertIntoEmpty);
		}
	}

	private long insertItemInternal(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		long remaining = runOnBeforeInsert(slot, resource, maxAmount, ctx, this, storageWrapper);
		if (remaining <= 0) {
			return maxAmount;
		}

		remaining -= inventoryPartitioner.getPartBySlot(slot).insertItem(slot, resource, maxAmount, ctx, super::insertSlot);
		TransactionCallback.onSuccess(ctx, () -> slotTracker.removeAndSetSlotIndexes(this, slot, getStackInSlot(slot)));

		if (remaining == maxAmount) {
			return 0;
		}

		runOnAfterInsert(slot, ctx, this, storageWrapper);

		return maxAmount - remaining;
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

	private void runOnAfterInsert(int slot, TransactionContext ctx, IItemHandlerSimpleInserter handler, IStorageWrapper storageWrapper) {
		// TODO: should this be simulated at all?
		storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class).forEach(u -> u.onAfterInsert(handler, slot, ctx));
	}

	private long runOnBeforeInsert(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx, IItemHandlerSimpleInserter handler, IStorageWrapper storageWrapper) {
		List<IInsertResponseUpgrade> wrappers = storageWrapper.getUpgradeHandler().getWrappersThatImplementFromMainStorage(IInsertResponseUpgrade.class);
		long toInsert = maxAmount;
		for (IInsertResponseUpgrade upgrade : wrappers) {
			toInsert = upgrade.onBeforeInsert(handler, slot, resource, toInsert, ctx);
			if (toInsert <= 0) {
				return 0;
			}
		}
		return toInsert;
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
	public boolean isItemValid(int slot, ItemVariant resource, int count) {
		return inventoryPartitioner.getPartBySlot(slot).isItemValid(slot, resource, count) && isAllowed(resource) && storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(slot, resource);
	}

	@Nonnull
	@Override
	public ItemVariant getVariantInSlot(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getVariantInSlot(slot, super::getVariantInSlot);
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return inventoryPartitioner.getPartBySlot(slot).getStackInSlot(slot, super::getStackInSlot);
	}

	protected abstract boolean isAllowed(ItemVariant resource);

	public void saveInventory() {
		contentsNbt.put(INVENTORY_TAG, serializeNBT());
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
	public CompoundTag serializeNBT() {
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
	public long insert(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		initSlotTracker();
		return maxAmount - slotTracker.insertItemIntoHandler(this, this::insertItemInternal, this::triggerOverflowUpgrades, resource, maxAmount, ctx);
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
		var previousSlots = new ArrayList<>(getSlots());

		super.setSize(previousSlots.size() + diff);
		for (int i = 0; i < previousSlots.size() && i < getSlotCount(); i++) {
			CompoundTag tag = ((ItemStackHandlerSlot) previousSlots.get(i)).save();
			if (tag != null) {
				getSlot(i).load(tag);
			}
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
		filterItemsChangeListener = s -> {};
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

	@Override
	protected ItemStackHandlerSlot makeSlot(int index, ItemStack stack) {
		return new InventoryHandlerSlot(index, this, stack);
	}

	// Make the "get stack" functions return a copy of the item due to how the insertion and extraction is handled in the part inventory handler implementations.
	private class InventoryHandlerSlot extends ItemStackHandlerSlot {
		public InventoryHandlerSlot(int index, InventoryHandler handler, ItemStack initial) {
			super(index, handler, initial);
			super.setStack(initial);
		}

		protected ItemStack getInternalStack() {
			return super.getStack().copy();
		}

		protected void setInternalNewStack(ItemStack stack) {
			super.setStack(stack);
			this.onFinalCommit();
		}

		@Override
		public ItemStack getStack() {
			if (inventoryPartitioner == null) {
				return super.getStack().copy();
			}

			return inventoryPartitioner.getPartBySlot(getIndex()).getStackInSlot(getIndex(), (s) -> super.getStack()).copy();
		}

		@Override
		protected void setStack(ItemStack stack) {
			if (inventoryPartitioner == null) {
				super.setStack(stack);
				return;
			}

			inventoryPartitioner.getPartBySlot(getIndex()).setStackInSlot(getIndex(), stack, (slot, stck) -> super.setStack(stack));
		}

		@Override
		public long insert(ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
			long inserted = super.insert(insertedVariant, maxAmount, transaction);
			TransactionCallback.onSuccess(transaction, () -> {
				slotTracker.removeAndSetSlotIndexes(InventoryHandler.this, getIndex(), getStack());
				this.onFinalCommit();
			});
			return inserted;
		}

		@Override
		public long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
			long extracted = super.extract(variant, maxAmount, transaction);
			TransactionCallback.onSuccess(transaction, () -> {
				slotTracker.removeAndSetSlotIndexes(InventoryHandler.this, getIndex(), getStack());
				this.onFinalCommit();
			});
			return extracted;
		}

		@Nullable
		@Override
		public CompoundTag save() {
			CompoundTag itemTag = super.save();
			if (itemTag != null) {
				itemTag.putInt(REAL_COUNT_TAG, getStack().getCount());
			}
			return itemTag;
		}

		@Override
		public void load(CompoundTag tag) {
			ItemStack stack = ItemStack.of(tag);
			if (tag.contains(REAL_COUNT_TAG)) {
				stack.setCount(tag.getInt(REAL_COUNT_TAG));
			}
			super.setStack(stack);
			onStackChange();
		}
	}
}
