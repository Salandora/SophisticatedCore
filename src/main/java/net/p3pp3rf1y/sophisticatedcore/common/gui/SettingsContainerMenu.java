package net.p3pp3rf1y.sophisticatedcore.common.gui;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.util.Pair;

import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor.AbstractContainerMenuAccessor;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SyncAdditionalSlotInfoMessage;
import net.p3pp3rf1y.sophisticatedcore.network.SyncContainerClientDataMessage;
import net.p3pp3rf1y.sophisticatedcore.network.SyncEmptySlotIconsMessage;
import net.p3pp3rf1y.sophisticatedcore.network.SyncTemplateSettingsMessage;
import net.p3pp3rf1y.sophisticatedcore.settings.ISettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsContainerBase;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.SettingsTemplateStorage;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.main.MainSettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsContainer;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsCategory;
import net.p3pp3rf1y.sophisticatedcore.settings.nosort.NoSortSettingsContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public abstract class SettingsContainerMenu<S extends IStorageWrapper> extends AbstractContainerMenu implements ISyncedContainer, IAdditionalSlotInfoMenu {
	private static final Map<String, ISettingsContainerFactory<?, ?>> SETTINGS_CONTAINER_FACTORIES = new HashMap<>();

	static {
		addFactory(MainSettingsCategory.NAME, MainSettingsContainer::new);
		addFactory(NoSortSettingsCategory.NAME, NoSortSettingsContainer::new);
		addFactory(MemorySettingsCategory.NAME, MemorySettingsContainer::new);
		addFactory(ItemDisplaySettingsCategory.NAME, ItemDisplaySettingsContainer::new);
	}

	protected final Player player;

	protected final S storageWrapper;

	private final List<Slot> storageInventorySlots = new ArrayList<>();
	public final NonNullList<ItemStack> lastGhostSlots = NonNullList.create();
	public final NonNullList<ItemStack> remoteGhostSlots = NonNullList.create();
	private final Map<String, SettingsContainerBase<?>> settingsContainers = new LinkedHashMap<>();
	private final TemplatePersistanceContainer templatePersistanceContainer;
	public final List<Slot> ghostSlots = new ArrayList<>();
	private boolean inventorySlotStackChanged = false;
	private final Set<Integer> inaccessibleSlots = new HashSet<>();
	private final Map<Integer, ItemStack> slotFilterItems = new HashMap<>();
	private final Map<Integer, Pair<ResourceLocation, ResourceLocation>> emptySlotIcons = new HashMap<>();

	protected SettingsContainerMenu(MenuType<?> menuType, int windowId, Player player, S storageWrapper) {
		super(menuType, windowId);
		this.player = player;
		this.storageWrapper = storageWrapper;

		addStorageInventorySlots();
		addSettingsContainers();
		templatePersistanceContainer = new TemplatePersistanceContainer(this);
	}

	public int getNumberOfStorageInventorySlots() {
		return storageWrapper.getInventoryHandler().getSlotCount();
	}

	public S getStorageWrapper() {
		return storageWrapper;
	}

	private void addSettingsContainers() {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		settingsHandler.getSettingsCategories().forEach((name, category) -> settingsContainers.put(name, instantiateContainer(this, name, category)));
	}

	private void addStorageInventorySlots() {
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();

		int slotIndex = 0;

		while (slotIndex < inventoryHandler.getSlotCount()) {
			int finalSlotIndex = slotIndex;
			storageInventorySlots.add(addSlot(new ViewOnlyStorageInventorySlot(inventoryHandler, finalSlotIndex)));

			slotIndex++;
		}
	}

	public int getColumnsTaken() {
		return storageWrapper.getColumnsTaken();
	}

	@Override
	protected Slot addSlot(Slot slot) {
		slot.index = ghostSlots.size();
		ghostSlots.add(slot);
		lastGhostSlots.add(ItemStack.EMPTY);
		remoteGhostSlots.add(ItemStack.EMPTY);
		return slot;
	}

	@Override
	public void broadcastChanges() {
		for (int slot = 0; slot < ghostSlots.size(); ++slot) {
			ItemStack itemstack = ghostSlots.get(slot).getItem();
			Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
			triggerSlotListeners(slot, itemstack, supplier);
			synchronizeSlotToRemote(slot, itemstack, supplier);
		}

		if (inventorySlotStackChanged) {
			inventorySlotStackChanged = false;
			sendAdditionalSlotInfo();
		}
	}

	@Override
	public void broadcastFullState() {
		for (int slotIndex = 0; slotIndex < ghostSlots.size(); ++slotIndex) {
			ItemStack itemstack = ghostSlots.get(slotIndex).getItem();
			triggerSlotListeners(slotIndex, itemstack, itemstack::copy);
		}

		sendAllDataToRemote();
	}

	@SuppressWarnings("java:S2177")
	private void triggerSlotListeners(int slotIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy) {
		ItemStack itemstack = lastGhostSlots.get(slotIndex);
		if (!ItemStack.matches(itemstack, slotStack)) {
			//boolean clientStackChanged = !slotStack.equals(itemstack);
			ItemStack itemstack1 = slotStackCopy.get();
			lastGhostSlots.set(slotIndex, itemstack1);

			//if (clientStackChanged) {
				for (ContainerListener containerlistener : ((AbstractContainerMenuAccessor) this).getContainerListeners()) {
					containerlistener.slotChanged(this, slotIndex, itemstack1);
				}
			//}
		}

	}

	@SuppressWarnings("java:S2177")
	private void synchronizeSlotToRemote(int slotIndex, ItemStack slotStack, Supplier<ItemStack> slotStackCopy) {
		if (!((AbstractContainerMenuAccessor) this).getSuppressRemoteUpdates()) {
			ItemStack remoteStack = remoteGhostSlots.get(slotIndex);
			if (!ItemStack.matches(remoteStack, slotStack)) {
				ItemStack stackCopy = slotStackCopy.get();
				remoteGhostSlots.set(slotIndex, stackCopy);
				if ((remoteStack.isEmpty() || slotStack.isEmpty())) {
					inventorySlotStackChanged = true;
				}
				if (((AbstractContainerMenuAccessor) this).getSynchronizer() != null) {
					((AbstractContainerMenuAccessor) this).getSynchronizer().sendSlotChange(this, slotIndex, stackCopy);
				}
			}
		}
	}

	public int getNumberOfSlots() {
		return storageWrapper.getInventoryHandler().getSlotCount();
	}

	@Override
	public void sendAllDataToRemote() {
		for (int slotIndex = 0; slotIndex < ghostSlots.size(); slotIndex++) {
			remoteGhostSlots.set(slotIndex, ghostSlots.get(slotIndex).getItem().copy());
		}

		if (((AbstractContainerMenuAccessor) this).getSynchronizer() != null) {
			((AbstractContainerMenuAccessor) this).getSynchronizer().sendInitialData(this, remoteGhostSlots, ((AbstractContainerMenuAccessor) this).getRemoteCarried(), new int[0]);
		}

		if (player instanceof ServerPlayer serverPlayer) {
			SettingsTemplateStorage settingsTemplateStorage = SettingsTemplateStorage.get();
			PacketHandler.sendToClient(serverPlayer,
					new SyncTemplateSettingsMessage(settingsTemplateStorage.getPlayerTemplates(serverPlayer), settingsTemplateStorage.getPlayerNamedTemplates(serverPlayer))
			);
		}

		sendEmptySlotIcons();
		sendAdditionalSlotInfo();
	}

	public abstract void detectSettingsChangeAndReload();

	@Override
	public void setSynchronizer(ContainerSynchronizer synchronizer) {
		if (player instanceof ServerPlayer serverPlayer && storageWrapper.getInventoryHandler().getStackSizeMultiplier() > 1) {
			super.setSynchronizer(new HighStackCountSynchronizer(serverPlayer));
			return;
		}
		super.setSynchronizer(synchronizer);
	}

	@Override
	public Slot getSlot(int slotId) {
		return ghostSlots.get(slotId);
	}

	public void onMemorizedStackAdded(int slotNumber) {
		storageWrapper.getInventoryHandler().onSlotFilterChanged(slotNumber);
		sendAdditionalSlotInfo();
	}

	public void onMemorizedStackRemoved(int slotNumber) {
		if (getSlot(slotNumber).getItem().isEmpty()) {
			storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemChanged(slotNumber);
			storageWrapper.getInventoryHandler().onSlotFilterChanged(slotNumber);
			sendAdditionalSlotInfo();
		}
	}

	public void onMemorizedItemsChanged() {
		storageWrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).itemsChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	public List<Slot> getStorageInventorySlots() {
		return storageInventorySlots;
	}

	@Override
	public void handleMessage(CompoundTag data) {
		if (data.contains("categoryName")) {
			String categoryName = data.getString("categoryName");
			if (settingsContainers.containsKey(categoryName)) {
				settingsContainers.get(categoryName).handleMessage(data);
			}
		} else if (data.contains(TemplatePersistanceContainer.TEMPLATE_PERSISTANCE_TAG, Tag.TAG_COMPOUND)) {
			templatePersistanceContainer.handleMessage(data.getCompound(TemplatePersistanceContainer.TEMPLATE_PERSISTANCE_TAG));
		}
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		//noop
	}

	@Override
	public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
		return ItemStack.EMPTY;
	}

	public void forEachSettingsContainer(BiConsumer<String, ? super SettingsContainerBase<?>> consumer) {
		settingsContainers.forEach(consumer);
	}

	public Player getPlayer() {
		return player;
	}

	public BlockPos getBlockPosition() {
		return BlockPos.ZERO;
	}


	public <T extends ISettingsCategory<?>> Optional<T> getSelectedTemplatesCategory(Class<T> categoryClass) {
		return templatePersistanceContainer.getSelectedTemplate().map(selectedTemplate -> selectedTemplate.getTypeCategory(categoryClass));
	}

	public TemplatePersistanceContainer getTemplatePersistanceContainer() {
		return templatePersistanceContainer;
	}

	public void refreshTemplateSlots() {
		templatePersistanceContainer.refreshTemplateSlots();
	}

	private class ViewOnlyStorageInventorySlot extends SlotItemHandler {
		public ViewOnlyStorageInventorySlot(SlottedStackStorage inventoryHandler, int slotIndex) {
			super(inventoryHandler, slotIndex, 0, 0);
		}

		@Override
		public boolean mayPickup(Player playerIn) {
			return false;
		}

		@Nullable
		@Override
		public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
			return inaccessibleSlots.contains(getContainerSlot()) ? StorageContainerMenuBase.INACCESSIBLE_SLOT_BACKGROUND : emptySlotIcons.getOrDefault(getContainerSlot(), null);
		}
	}

	public int getNumberOfRows() {
		return storageWrapper.getNumberOfSlotRows();
	}

	protected static <C extends ISettingsCategory<?>, T extends SettingsContainerBase<C>> void addFactory(String categoryName, ISettingsContainerFactory<C, T> factory) {
		SETTINGS_CONTAINER_FACTORIES.put(categoryName, factory);
	}

	public interface ISettingsContainerFactory<C extends ISettingsCategory<?>, T extends SettingsContainerBase<C>> {
		T create(SettingsContainerMenu<?> settingsContainer, String categoryName, C category);
	}

	private static <C extends ISettingsCategory<?>> SettingsContainerBase<C> instantiateContainer(SettingsContainerMenu<?> settingsContainer, String name, C category) {
		//noinspection unchecked
		return (SettingsContainerBase<C>) getSettingsContainerFactory(name).create(settingsContainer, name, category);
	}

	private static <C extends ISettingsCategory<?>, T extends SettingsContainerBase<C>> ISettingsContainerFactory<C, T> getSettingsContainerFactory(String name) {
		//noinspection unchecked
		return (ISettingsContainerFactory<C, T>) SETTINGS_CONTAINER_FACTORIES.get(name);
	}

	public void sendDataToServer(Supplier<CompoundTag> supplyData) {
		if (isServer()) {
			return;
		}
		CompoundTag data = supplyData.get();
		PacketHandler.sendToServer(new SyncContainerClientDataMessage(data));
	}

	protected boolean isServer() {
		return !player.level().isClientSide;
	}

	public void sendAdditionalSlotInfo() {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Set<Integer> inaccessibleSlots = new HashSet<>();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		Map<Integer, Item> slotFilterItems = new HashMap<>();
		for (int slot = 0; slot < inventoryHandler.getSlotCount(); slot++) {
			if (!inventoryHandler.isSlotAccessible(slot)) {
				inaccessibleSlots.add(slot);
			}

			if (inventoryHandler.getFilterItem(slot) != Items.AIR) {
				slotFilterItems.put(slot, inventoryHandler.getFilterItem(slot));
			}
		}
		PacketHandler.sendToClient(serverPlayer, new SyncAdditionalSlotInfoMessage(inaccessibleSlots, Map.of(), slotFilterItems));
	}

	@Override
	public void updateAdditionalSlotInfo(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Map<Integer, Item> slotFilterItems) {
		this.inaccessibleSlots.clear();
		this.inaccessibleSlots.addAll(inaccessibleSlots);

		this.slotFilterItems.clear();
		slotFilterItems.forEach((slot, item) -> this.slotFilterItems.put(slot, new ItemStack(item)));
	}

	@Override
	public void updateEmptySlotIcons(Map<ResourceLocation, Set<Integer>> emptySlotIcons) {
		this.emptySlotIcons.clear();
		emptySlotIcons.forEach((textureName, slots) -> slots.forEach(slot -> this.emptySlotIcons.put(slot, new Pair<>(InventoryMenu.BLOCK_ATLAS, textureName))));
	}

	@Override
	public void setRemoteSlotNoCopy(int slot, ItemStack stack) {
		ItemStack previous = getSlot(slot).getItem();
		super.setRemoteSlotNoCopy(slot, stack);

		if (previous.isEmpty() || stack.isEmpty()) {
			inventorySlotStackChanged = true;
		}
	}

	private void sendEmptySlotIcons() {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		Map<ResourceLocation, Set<Integer>> noItemSlotTextures = new HashMap<>();
		for (int slot = 0; slot < storageWrapper.getInventoryHandler().getSlotCount(); slot++) {
			Pair<ResourceLocation, ResourceLocation> noItemIcon = storageWrapper.getInventoryHandler().getNoItemIcon(slot);
			if (noItemIcon != null) {
				noItemSlotTextures.computeIfAbsent(noItemIcon.getSecond(), rl -> new HashSet<>()).add(slot);
			}
		}
		PacketHandler.sendToClient(serverPlayer, new SyncEmptySlotIconsMessage(noItemSlotTextures));
	}

	public ItemStack getSlotFilterItem(int slot) {
		return slotFilterItems.getOrDefault(slot, ItemStack.EMPTY);
	}
}
