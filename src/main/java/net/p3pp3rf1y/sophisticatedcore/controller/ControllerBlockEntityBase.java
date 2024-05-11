package net.p3pp3rf1y.sophisticatedcore.controller;

import org.apache.commons.lang3.NotImplementedException;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedSlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.porting_lib.base.util.LazyOptional;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.CachedFailedInsertInventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.IItemHandlerSimpleInserter;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import net.p3pp3rf1y.sophisticatedcore.util.WorldHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ControllerBlockEntityBase extends BlockEntity implements SlottedStackStorage {
	public static final int SEARCH_RANGE = 15;
	private List<BlockPos> storagePositions = new ArrayList<>();
	private List<Integer> baseIndexes = new ArrayList<>();
	private int totalSlots = 0;
	private final Map<ItemStackKey, Set<BlockPos>> stackStorages = new HashMap<>();
	private final Map<BlockPos, Set<ItemStackKey>> storageStacks = new HashMap<>();
	private final Map<Item, Set<ItemStackKey>> itemStackKeys = new HashMap<>();
	private final Comparator<BlockPos> distanceComparator = Comparator.<BlockPos>comparingDouble(p -> p.distSqr(getBlockPos())).thenComparing(Comparator.naturalOrder());
	private final Set<BlockPos> emptySlotsStorages = new TreeSet<>(distanceComparator);

	private final Map<Item, Set<BlockPos>> memorizedItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageMemorizedItems = new HashMap<>();
	private final Map<Integer, Set<BlockPos>> memorizedStackStorages = new HashMap<>();
	private final Map<BlockPos, Set<Integer>> storageMemorizedStacks = new HashMap<>();
	private final Map<Item, Set<BlockPos>> filterItemStorages = new HashMap<>();
	private final Map<BlockPos, Set<Item>> storageFilterItems = new HashMap<>();
	private Set<BlockPos> linkedBlocks = new TreeSet<>(distanceComparator);
	private Set<BlockPos> connectingBlocks = new TreeSet<>(distanceComparator);
	private Set<BlockPos> nonConnectingBlocks = new TreeSet<>(distanceComparator);

	@Nullable
	private LazyOptional<SlottedStackStorage> itemHandlerCap;
	@Nullable
	private LazyOptional<SlottedStackStorage> noSideItemHandlerCap;
	private boolean unloaded = false;

	protected ControllerBlockEntityBase(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
		super(blockEntityType, pos, state);

		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
			if (chunk.getBlockEntities().containsValue(this)) {
				unloaded = false;
			}
		});
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
			if (unloaded) {
				return;
			}

			if (chunk.getBlockEntities().containsValue(this)) {
				onChunkUnloaded();
			}
		});
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, world) -> {
			if (be == this) {
				invalidateCaps();
			}
		});
	}

	public boolean addLinkedBlock(BlockPos linkedPos) {
		if (level != null && !level.isClientSide() && isWithinRange(linkedPos) && !linkedBlocks.contains(linkedPos) && !storagePositions.contains(linkedPos)) {

			linkedBlocks.add(linkedPos);
			setChanged();

			WorldHelper.getBlockEntity(level, linkedPos, ILinkable.class).ifPresent(l -> {
				if (l.connectLinkedSelf()) {
					Set<BlockPos> positionsToCheck = new LinkedHashSet<>();
					positionsToCheck.add(linkedPos);
					searchAndAddBoundables(positionsToCheck, true);
				}

				searchAndAddBoundables(new LinkedHashSet<>(l.getConnectablePositions()), false);
			});
			WorldHelper.notifyBlockUpdate(this);
			return true;
		}
		return false;
	}

	public void removeLinkedBlock(BlockPos storageBlockPos) {
		linkedBlocks.remove(storageBlockPos);
		setChanged();
		verifyStoragesConnected();

		WorldHelper.notifyBlockUpdate(this);
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) {
			stackStorages.clear();
			storageStacks.clear();
			itemStackKeys.clear();
			emptySlotsStorages.clear();
			storagePositions.forEach(this::addStorageStacksAndRegisterListeners);
		}
	}

	public boolean isStorageConnected(BlockPos storagePos) {
		return storagePositions.contains(storagePos);
	}

	public void searchAndAddBoundables() {
		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			positionsToCheck.add(getBlockPos().offset(dir.getNormal()));
		}
		searchAndAddBoundables(positionsToCheck, false);
	}

	public void changeSlots(BlockPos storagePos, int newSlots, boolean hasEmptySlots) {
		updateBaseIndexesAndTotalSlots(storagePos, newSlots);
		updateEmptySlots(storagePos, hasEmptySlots);
	}

	public void updateEmptySlots(BlockPos storagePos, boolean hasEmptySlots) {
		if (emptySlotsStorages.contains(storagePos) && !hasEmptySlots) {
			emptySlotsStorages.remove(storagePos);
		} else if (!emptySlotsStorages.contains(storagePos) && hasEmptySlots) {
			emptySlotsStorages.add(storagePos);
		}
	}

	private void updateBaseIndexesAndTotalSlots(BlockPos storagePos, int newSlots) {
		int index = storagePositions.indexOf(storagePos);
		int originalSlots = getStorageSlots(index);

		int diff = newSlots - originalSlots;

		for (int i = index; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) + diff);
		}

		totalSlots += diff;
		WorldHelper.notifyBlockUpdate(this);
	}

	private int getStorageSlots(int index) {
		int previousBaseIndex = index == 0 ? 0 : baseIndexes.get(index - 1);
		return baseIndexes.get(index) - previousBaseIndex;
	}

	public int getSlots(int storageIndex) {
		if (storageIndex < 0 || storageIndex >= baseIndexes.size()) {
			return 0;
		}
		return getStorageSlots(storageIndex);
	}

	private void searchAndAddBoundables(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf) {
		Set<BlockPos> positionsChecked = new HashSet<>();

		boolean first = true;
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();

			final boolean finalFirst = first;
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllerBoundable.class).ifPresentOrElse(boundable ->
							tryToConnectStorageAndAddPositionsToCheckAround(positionsToCheck, addingLinkedSelf, positionsChecked, posToCheck, finalFirst, boundable),
					() -> positionsChecked.add(posToCheck)
			);
			first = false;
		}
	}

	private void tryToConnectStorageAndAddPositionsToCheckAround(Set<BlockPos> positionsToCheck, boolean addingLinkedSelf, Set<BlockPos> positionsChecked, BlockPos posToCheck, boolean finalFirst, IControllerBoundable boundable) {
		if (boundable.canBeConnected() || (addingLinkedSelf && finalFirst)) {
			if (boundable instanceof ILinkable linkable && linkable.isLinked() && (!addingLinkedSelf || !finalFirst)) {
				linkedBlocks.remove(posToCheck);
				linkable.setNotLinked();
			} else if (boundable instanceof IControllableStorage storage && storage.hasStorageData()) {
				addStorageData(posToCheck);
			} else {
				if (boundable.canConnectStorages()) {
					connectingBlocks.add(posToCheck);
				} else {
					nonConnectingBlocks.add(posToCheck);
				}
				boundable.registerController(this);
			}
			if (boundable.canConnectStorages()) {
				addUncheckedPositionsAround(positionsToCheck, positionsChecked, posToCheck);
			}
		}
	}

	private void addUncheckedPositionsAround(Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked, BlockPos currentPos) {
		for (Direction dir : Direction.values()) {
			BlockPos pos = currentPos.offset(dir.getNormal());
			if (!positionsChecked.contains(pos) && ((!storagePositions.contains(pos) && !connectingBlocks.contains(pos) && !nonConnectingBlocks.contains(pos)) || linkedBlocks.contains(pos)) && isWithinRange(pos)) {
				positionsToCheck.add(pos);
			}
		}
	}

	private boolean isWithinRange(BlockPos pos) {
		return Math.abs(pos.getX() - getBlockPos().getX()) <= SEARCH_RANGE && Math.abs(pos.getY() - getBlockPos().getY()) <= SEARCH_RANGE && Math.abs(pos.getZ() - getBlockPos().getZ()) <= SEARCH_RANGE;
	}

	public void addStorage(BlockPos storagePos) {
		if (storagePositions.contains(storagePos)) {
			removeStorageInventoryData(storagePos);
		}

		if (isWithinRange(storagePos)) {
			HashSet<BlockPos> positionsToCheck = new LinkedHashSet<>();
			positionsToCheck.add(storagePos);
			searchAndAddBoundables(positionsToCheck, false);
		}
	}

	private void addStorageData(BlockPos storagePos) {
		storagePositions.add(storagePos);
		totalSlots += getInventoryHandlerValueFromHolder(storagePos, SlottedStackStorage::getSlotCount).orElse(0);
		baseIndexes.add(totalSlots);
		addStorageStacksAndRegisterListeners(storagePos);

		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	public void addStorageStacksAndRegisterListeners(BlockPos storagePos) {
		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(storage -> {
			ITrackedContentsItemHandler handler = storage.getStorageWrapper().getInventoryForInputOutput();
			handler.getTrackedStacks().forEach(k -> addStorageStack(storagePos, k));
			if (handler.hasEmptySlots()) {
				emptySlotsStorages.add(storagePos);
			}
			MemorySettingsCategory memorySettings = storage.getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			memorySettings.getFilterItemSlots().keySet().forEach(i -> addStorageMemorizedItem(storagePos, i));
			memorySettings.getFilterStackSlots().keySet().forEach(stackHash -> addStorageMemorizedStack(storagePos, stackHash));

			setStorageFilterItems(storagePos, storage.getStorageWrapper().getInventoryHandler().getFilterItems());

			storage.registerController(this);
		});
	}

	public void addStorageMemorizedItem(BlockPos storagePos, Item item) {
		memorizedItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedItems.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(item);
	}

	public void addStorageMemorizedStack(BlockPos storagePos, int stackHash) {
		memorizedStackStorages.computeIfAbsent(stackHash, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageMemorizedStacks.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(stackHash);
	}

	public void removeStorageMemorizedItem(BlockPos storagePos, Item item) {
		memorizedItemStorages.computeIfPresent(item, (i, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (memorizedItemStorages.containsKey(item) && memorizedItemStorages.get(item).isEmpty()) {
			memorizedItemStorages.remove(item);
		}
		storageMemorizedItems.remove(storagePos);
	}

	public void removeStorageMemorizedStack(BlockPos storagePos, int stackHash) {
		memorizedStackStorages.computeIfPresent(stackHash, (i, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (memorizedStackStorages.containsKey(stackHash) && memorizedStackStorages.get(stackHash).isEmpty()) {
			memorizedStackStorages.remove(stackHash);
		}
		storageMemorizedStacks.remove(storagePos);
	}

	private <T> Optional<T> getInventoryHandlerValueFromHolder(BlockPos storagePos, Function<IItemHandlerSimpleInserter, T> valueGetter) {
		return getWrapperValueFromHolder(storagePos, wrapper -> valueGetter.apply(wrapper.getInventoryForInputOutput()));
	}

	private <T> Optional<T> getWrapperValueFromHolder(BlockPos storagePos, Function<IStorageWrapper, T> valueGetter) {
		return WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).map(holder -> valueGetter.apply(holder.getStorageWrapper()));
	}

	public void addStorageStack(BlockPos storagePos, ItemStackKey itemStackKey) {
		stackStorages.computeIfAbsent(itemStackKey, stackKey -> new LinkedHashSet<>()).add(storagePos);
		storageStacks.computeIfAbsent(storagePos, pos -> new HashSet<>()).add(itemStackKey);
		itemStackKeys.computeIfAbsent(itemStackKey.getStack().getItem(), item -> new LinkedHashSet<>()).add(itemStackKey);
	}

	public void removeStorageStack(BlockPos storagePos, ItemStackKey stackKey) {
		stackStorages.computeIfPresent(stackKey, (sk, positions) -> {
			positions.remove(storagePos);
			return positions;
		});
		if (stackStorages.containsKey(stackKey) && stackStorages.get(stackKey).isEmpty()) {
			stackStorages.remove(stackKey);

			itemStackKeys.computeIfPresent(stackKey.getStack().getItem(), (i, stackKeys) -> {
				stackKeys.remove(stackKey);
				return stackKeys;
			});
			if (itemStackKeys.containsKey(stackKey.getStack().getItem()) && itemStackKeys.get(stackKey.getStack().getItem()).isEmpty()) {
				itemStackKeys.remove(stackKey.getStack().getItem());
			}
		}
		storageStacks.computeIfPresent(storagePos, (pos, stackKeys) -> {
			stackKeys.remove(stackKey);
			return stackKeys;
		});
		if (storageStacks.containsKey(storagePos) && storageStacks.get(storagePos).isEmpty()) {
			storageStacks.remove(storagePos);
		}
	}

	public void removeStorageStacks(BlockPos storagePos) {
		storageStacks.computeIfPresent(storagePos, (pos, stackKeys) -> {
			stackKeys.forEach(stackKey -> {
				Set<BlockPos> storages = stackStorages.get(stackKey);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						stackStorages.remove(stackKey);
						itemStackKeys.computeIfPresent(stackKey.getStack().getItem(), (i, positions) -> {
							positions.remove(stackKey);
							return positions;
						});
						if (itemStackKeys.containsKey(stackKey.getStack().getItem()) && itemStackKeys.get(stackKey.getStack().getItem()).isEmpty()) {
							itemStackKeys.remove(stackKey.getStack().getItem());
						}
					}
				}
			});
			return stackKeys;
		});
		storageStacks.remove(storagePos);
	}

	protected boolean hasItem(Item item) {
		return itemStackKeys.containsKey(item);
	}

	protected boolean isMemorizedItem(ItemStack stack) {
		return memorizedItemStorages.containsKey(stack.getItem()) || memorizedStackStorages.containsKey(ItemStackKey.getHashCode(stack));
	}

	protected boolean isFilterItem(Item item) {
		return filterItemStorages.containsKey(item);
	}

	public void removeStorage(BlockPos storagePos) {
		removeConnectingBlock(storagePos);
		removeStorageInventoryDataAndUnregisterController(storagePos);
		verifyStoragesConnected();
	}

	private void removeConnectingBlock(BlockPos storagePos) {
		if (connectingBlocks.remove(storagePos)) {
			WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);
		}
	}

	public void removeNonConnectingBlock(BlockPos storagePos) {
		if (nonConnectingBlocks.remove(storagePos)) {
			WorldHelper.getLoadedBlockEntity(level, storagePos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController);
		}
	}

	private void removeStorageInventoryDataAndUnregisterController(BlockPos storagePos) {
		if (!storagePositions.contains(storagePos)) {
			return;
		}
		removeStorageInventoryData(storagePos);
		linkedBlocks.remove(storagePos);

		WorldHelper.getLoadedBlockEntity(level, storagePos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController);

		setChanged();
		WorldHelper.notifyBlockUpdate(this);
	}

	private void removeStorageInventoryData(BlockPos storagePos) {
		int idx = storagePositions.indexOf(storagePos);
		totalSlots -= getStorageSlots(idx);
		removeStorageStacks(storagePos);
		removeStorageMemorizedItems(storagePos);
		removeStorageMemorizedStacks(storagePos);
		removeStorageWithEmptySlots(storagePos);
		removeStorageFilterItems(storagePos);
		storagePositions.remove(idx);
		removeBaseIndexAt(idx);
	}

	private void removeStorageFilterItems(BlockPos storagePos) {
		storageFilterItems.computeIfPresent(storagePos, (pos, items) -> {
			items.forEach(item -> {
				Set<BlockPos> storages = filterItemStorages.get(item);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						filterItemStorages.remove(item);
					}
				}
			});
			return items;
		});
		storageFilterItems.remove(storagePos);
	}

	private void removeStorageMemorizedItems(BlockPos storagePos) {
		storageMemorizedItems.computeIfPresent(storagePos, (pos, items) -> {
			items.forEach(item -> {
				Set<BlockPos> storages = memorizedItemStorages.get(item);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						memorizedItemStorages.remove(item);
					}
				}
			});
			return items;
		});
		storageMemorizedItems.remove(storagePos);
	}

	private void removeStorageMemorizedStacks(BlockPos storagePos) {
		storageMemorizedStacks.computeIfPresent(storagePos, (pos, items) -> {
			items.forEach(stackHash -> {
				Set<BlockPos> storages = memorizedStackStorages.get(stackHash);
				if (storages != null) {
					storages.remove(storagePos);
					if (storages.isEmpty()) {
						memorizedStackStorages.remove(stackHash);
					}
				}
			});
			return items;
		});
		storageMemorizedStacks.remove(storagePos);
	}

	private void verifyStoragesConnected() {
		HashSet<BlockPos> toVerify = new HashSet<>(storagePositions);
		toVerify.addAll(connectingBlocks);
		toVerify.addAll(nonConnectingBlocks);

		Set<BlockPos> positionsToCheck = new HashSet<>();
		for (Direction dir : Direction.values()) {
			BlockPos offsetPos = getBlockPos().offset(dir.getNormal());
			if (toVerify.contains(offsetPos)) {
				positionsToCheck.add(offsetPos);
			}
		}
		Set<BlockPos> positionsChecked = new HashSet<>();

		verifyConnected(toVerify, positionsToCheck, positionsChecked);

		linkedBlocks.forEach(linkedPosition -> WorldHelper.getBlockEntity(getLevel(), linkedPosition, ILinkable.class).ifPresent(l -> {
			if (l.connectLinkedSelf() && toVerify.contains(linkedPosition)) {
				positionsToCheck.add(linkedPosition);
			}
			l.getConnectablePositions().forEach(p -> {
				if (toVerify.contains(p)) {
					positionsToCheck.add(p);
				}
			});
		}));

		verifyConnected(toVerify, positionsToCheck, positionsChecked);

		toVerify.forEach(storagePos -> {
			removeConnectingBlock(storagePos);
			removeNonConnectingBlock(storagePos);
			removeStorageInventoryDataAndUnregisterController(storagePos);
		});
	}

	private void verifyConnected(HashSet<BlockPos> toVerify, Set<BlockPos> positionsToCheck, Set<BlockPos> positionsChecked) {
		while (!positionsToCheck.isEmpty()) {
			Iterator<BlockPos> it = positionsToCheck.iterator();
			BlockPos posToCheck = it.next();
			it.remove();

			positionsChecked.add(posToCheck);
			WorldHelper.getLoadedBlockEntity(level, posToCheck, IControllerBoundable.class).ifPresent(h -> {
				toVerify.remove(posToCheck);
				if (h.canConnectStorages()) {
					for (Direction dir : Direction.values()) {
						BlockPos pos = posToCheck.offset(dir.getNormal());
						if (!positionsChecked.contains(pos) && toVerify.contains(pos)) {
							positionsToCheck.add(pos);
						}
					}
				}
			});
		}
	}

	private void removeBaseIndexAt(int idx) {
		if (idx >= baseIndexes.size()) {
			return;
		}
		int slotsRemoved = getStorageSlots(idx);
		baseIndexes.remove(idx);
		for (int i = idx; i < baseIndexes.size(); i++) {
			baseIndexes.set(i, baseIndexes.get(i) - slotsRemoved);
		}
	}

	@Nullable
	public <T, C> LazyOptional<T> getCapability(BlockApiLookup<T, C> cap, @Nullable C opt) {
		if (cap == ItemStorage.SIDED) {
			if (opt == null) {
				if (noSideItemHandlerCap == null) {
					noSideItemHandlerCap =  LazyOptional.of(() -> this).cast();
				}
				return noSideItemHandlerCap.cast();
			} else {
				if (itemHandlerCap == null) {
					itemHandlerCap = LazyOptional.of(() -> new CachedFailedInsertInventoryHandler(() -> this, () -> level != null ? level.getGameTime() : 0));
				}
				return itemHandlerCap.cast();
			}
		}
		return LazyOptional.empty();
	}

	public void invalidateCaps() {
		if (itemHandlerCap != null) {
			itemHandlerCap.invalidate();
			itemHandlerCap = null;
		}
		if (noSideItemHandlerCap != null) {
			noSideItemHandlerCap.invalidate();
			noSideItemHandlerCap = null;
		}
	}

	@Override
	public int getSlotCount() {
		return totalSlots;
	}

	private int getIndexForSlot(int slot) {
		if (slot < 0) {
			return -1;
		}

		for (int i = 0; i < baseIndexes.size(); i++) {
			if (slot - baseIndexes.get(i) < 0) {
				return i;
			}
		}
		return -1;
	}

	@Nullable
	protected SlottedStackStorage getHandlerFromIndex(int index) {
		if (index < 0 || index >= storagePositions.size()) {
			return null;
		}
		return getWrapperValueFromHolder(storagePositions.get(index), wrapper -> (SlottedStackStorage) wrapper.getInventoryForInputOutput()).orElse(null);
	}

	protected int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndexes.size()) {
			return slot;
		}
		return slot - baseIndexes.get(index - 1);
	}

	@Nonnull
	@Override
	public SingleSlotStorage<ItemVariant> getSlot(int slot) {
		if (isSlotIndexInvalid(slot)) {
			throw new IndexOutOfBoundsException(slot);
		}

		int handlerIndex = getIndexForSlot(slot);
		SlottedStackStorage handler = getHandlerFromIndex(handlerIndex);
		if (handler == null) {
			throw new IndexOutOfBoundsException("HandlerIndex out of range: " + handlerIndex);
		}

		slot = getSlotFromIndex(slot, handlerIndex);
		if (!validateHandlerSlotIndex(handler, handlerIndex, slot, "getStackInSlot")) {
			throw new IndexOutOfBoundsException("Slot in handler out of range: " + slot);
		}

		return handler.getSlot(slot);
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return ItemStack.EMPTY;
		}
		int handlerIndex = getIndexForSlot(slot);
		SlottedStackStorage handler = getHandlerFromIndex(handlerIndex);
		if (handler == null) {
			return ItemStack.EMPTY;
		}

		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "getStackInSlot")) {
			return handler.getStackInSlot(slot);
		}
		return ItemStack.EMPTY;
	}

	private boolean isSlotIndexInvalid(int slot) {
		return slot < 0 || slot >= totalSlots;
	}

	private boolean validateHandlerSlotIndex(SlottedStackStorage handler, int handlerIndex, int slot, String methodName) {
		if (slot >= 0 && slot < handler.getSlotCount()) {
			return true;
		}
		if (handlerIndex < 0 || handlerIndex >= storagePositions.size()) {
			SophisticatedCore.LOGGER.debug("Invalid handler index calculated {} in controller's {} method. If you see many of these messages try replacing controller at {}", handlerIndex, methodName, getBlockPos().toShortString());
		} else {
			SophisticatedCore.LOGGER.debug("Invalid slot {} passed into controller's {} method for storage at {}. If you see many of these messages try replacing controller at {}", slot, methodName, storagePositions.get(handlerIndex).toShortString(), getBlockPos().toShortString());
		}

		return false;
	}

	@Override
	public long insertSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		if (isItemValid(slot, resource, (int) maxAmount)) {return insert(resource, maxAmount, ctx, true);}

		return 0;
	}

	@Override
	public long insert(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return insert(resource, maxAmount, ctx, true);
	}

	public long insert(ItemVariant resource, long maxAmount, TransactionContext ctx, boolean insertIntoAnyEmpty) {
		ItemStackKey stackKey = ItemStackKey.of(resource.toStack());
		long remaining = maxAmount;

		remaining -= insertIntoStoragesThatMatchStack(resource, remaining, stackKey, ctx);
		if (remaining == 0) {
			return maxAmount;
		}

		remaining = insertIntoStoragesThatMatchItem(resource, remaining, ctx);
		if (remaining == 0) {
			return maxAmount;
		}

		if (memorizedItemStorages.containsKey(resource.getItem())) {
			remaining -= insertIntoStorages(memorizedItemStorages.get(resource.getItem()), resource, remaining, ctx, false);
			if (remaining == 0) {
				return maxAmount;
			}
		}

		int stackHash = stackKey.hashCode();
		if (memorizedStackStorages.containsKey(stackHash)) {
			remaining -= insertIntoStorages(memorizedStackStorages.get(stackHash), resource, remaining, ctx, false);
			if (remaining == 0) {
				return maxAmount;
			}
		}

		if (filterItemStorages.containsKey(resource.getItem())) {
			remaining -= insertIntoStorages(filterItemStorages.get(resource.getItem()), resource, remaining, ctx, false);
			if (remaining == 0) {
				return maxAmount;
			}
		}

		return (insertIntoAnyEmpty ? insertIntoStorages(emptySlotsStorages, resource, remaining, ctx, false) : maxAmount - remaining);
	}

	private long insertIntoStoragesThatMatchStack(ItemVariant resource, long maxAmount, ItemStackKey stackKey, TransactionContext ctx) {
		long remaining = maxAmount;
		if (stackStorages.containsKey(stackKey)) {
			Set<BlockPos> positions = stackStorages.get(stackKey);
			remaining -= insertIntoStorages(positions, resource, remaining, ctx, false);
		}
		return maxAmount - remaining;
	}

	private long insertIntoStoragesThatMatchItem(ItemVariant resource, long maxAmount, TransactionContext ctx) {
		long remaining = maxAmount;
		if (!emptySlotsStorages.isEmpty() && itemStackKeys.containsKey(resource.getItem())) {
			for (ItemStackKey key : itemStackKeys.get(resource.getItem())) {
				if (stackStorages.containsKey(key)) {
					Set<BlockPos> positions = stackStorages.get(key);
					remaining -= insertIntoStorages(positions, resource, remaining, ctx, true);
					if (remaining == 0) {
						return maxAmount;
					}
				}
			}
		}
		return remaining;
	}

	private long insertIntoStorages(Set<BlockPos> positions, ItemVariant resource, long maxAmount, TransactionContext ctx, boolean checkHasEmptySlotFirst) {
		long remaining = maxAmount;
		Set<BlockPos> positionsCopy = new LinkedHashSet<>(positions); //to prevent CME if stack insertion actually causes set of positions to change
		for (BlockPos storagePos : positionsCopy) {
			if (checkHasEmptySlotFirst && !emptySlotsStorages.contains(storagePos)) {
				continue;
			}
			remaining -= insertIntoStorage(storagePos, resource, remaining, ctx);
			if (remaining == 0) {
				return maxAmount;
			}
		}
		return maxAmount - remaining;
	}

	private long insertIntoStorage(BlockPos storagePos, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		return getInventoryHandlerValueFromHolder(storagePos, ins -> ins.insert(resource, maxAmount, ctx)).orElse(0L);
	}

	@Override
	public long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext ctx) {
		if (isSlotIndexInvalid(slot)) {
			return 0;
		}

		int handlerIndex = getIndexForSlot(slot);
		SlottedStackStorage handler = getHandlerFromIndex(handlerIndex);
		if (handler == null) {
			return 0;
		}

		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "extractItem(int slot, int amount, boolean simulate)")) {
			return handler.extractSlot(slot, resource, maxAmount, ctx);
		}

		return 0;
	}

	@Override
	public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
		throw new NotImplementedException();
	}

	@Override
	public int getSlotLimit(int slot) {
		if (isSlotIndexInvalid(slot)) {
			return 0;
		}
		int handlerIndex = getIndexForSlot(slot);
		SlottedStackStorage handler = getHandlerFromIndex(handlerIndex);
		if (handler == null) {
			return 0;
		}
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, localSlot, "getSlotLimit(int slot)")) {
			return handler.getSlotLimit(localSlot);
		}
		return 0;
	}

	@Override
	public boolean isItemValid(int slot, ItemVariant resource, int count) {
		if (isSlotIndexInvalid(slot)) {
			return false;
		}

		int handlerIndex = getIndexForSlot(slot);
		SlottedStackStorage handler = getHandlerFromIndex(handlerIndex);
		if (handler == null) {
			return false;
		}
		int localSlot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, localSlot, "isItemValid(int slot, ItemStack stack)")) {
			return handler.isItemValid(localSlot, resource, count);
		}
		return false;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (isSlotIndexInvalid(slot)) {
			return;
		}
		int handlerIndex = getIndexForSlot(slot);
		SlottedStackStorage handler = getHandlerFromIndex(handlerIndex);
		if (handler == null) {
			return;
		}
		slot = getSlotFromIndex(slot, handlerIndex);
		if (validateHandlerSlotIndex(handler, handlerIndex, slot, "setStackInSlot(int slot, ItemStack stack)")) {
			handler.setStackInSlot(slot, stack);
		}
	}

	public void onChunkUnloaded() {
		detachFromStoragesAndUnlinkBlocks();
		unloaded = true;
	}

	public void detachFromStoragesAndUnlinkBlocks() {
		storagePositions.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		connectingBlocks.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllableStorage.class).ifPresent(IControllableStorage::unregisterController));
		nonConnectingBlocks.forEach(pos -> WorldHelper.getLoadedBlockEntity(level, pos, IControllerBoundable.class).ifPresent(IControllerBoundable::unregisterController));
		new HashSet<>(linkedBlocks).forEach(linkedPos -> WorldHelper.getLoadedBlockEntity(level, linkedPos, ILinkable.class).ifPresent(ILinkable::unlinkFromController)); //copying into new hashset to prevent CME when these are removed
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);

		saveData(tag);
	}

	private CompoundTag saveData(CompoundTag tag) {
		NBTHelper.putList(tag, "storagePositions", storagePositions, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "connectingBlocks", connectingBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "nonConnectingBlocks", nonConnectingBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "linkedBlocks", linkedBlocks, p -> LongTag.valueOf(p.asLong()));
		NBTHelper.putList(tag, "baseIndexes", baseIndexes, IntTag::valueOf);
		tag.putInt("totalSlots", totalSlots);

		return tag;
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);

		storagePositions = NBTHelper.getCollection(tag, "storagePositions", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), ArrayList::new).orElseGet(ArrayList::new);
		connectingBlocks = NBTHelper.getCollection(tag, "connectingBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
		nonConnectingBlocks = NBTHelper.getCollection(tag, "nonConnectingBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
		baseIndexes = NBTHelper.getCollection(tag, "baseIndexes", Tag.TAG_INT, t -> Optional.of(((IntTag) t).getAsInt()), ArrayList::new).orElseGet(ArrayList::new);
		totalSlots = tag.getInt("totalSlots");
		linkedBlocks = NBTHelper.getCollection(tag, "linkedBlocks", Tag.TAG_LONG, t -> Optional.of(BlockPos.of(((LongTag) t).getAsLong())), LinkedHashSet::new).orElseGet(LinkedHashSet::new);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveData(super.getUpdateTag());
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	public void addStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotsStorages.add(storageBlockPos);
	}

	public void removeStorageWithEmptySlots(BlockPos storageBlockPos) {
		emptySlotsStorages.remove(storageBlockPos);
	}

	public Set<BlockPos> getLinkedBlocks() {
		return linkedBlocks;
	}

	public List<BlockPos> getStoragePositions() {
		return storagePositions;
	}

	public void setStorageFilterItems(BlockPos storagePos, Set<Item> filterItems) {
		removeStorageFilterItems(storagePos);
		if (filterItems.isEmpty()) {
			return;
		}

		for (Item item : filterItems) {
			filterItemStorages.computeIfAbsent(item, stackKey -> new LinkedHashSet<>()).add(storagePos);
		}
		storageFilterItems.put(storagePos, new LinkedHashSet<>(filterItems));
	}

	@Override
	public Iterator<StorageView<ItemVariant>> iterator() {
		CombinedSlottedStorage<ItemVariant, SlottedStackStorage> combinedStorage = new CombinedSlottedStorage<>(new ArrayList<>());
		for (int i = 0; i < storagePositions.size(); i++) {
			combinedStorage.parts.add(getHandlerFromIndex(i));
		}
		return combinedStorage.iterator();
	}
}
