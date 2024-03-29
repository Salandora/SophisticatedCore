package net.p3pp3rf1y.sophisticatedcore.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.Config;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.Button;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.InventoryScrollPanel;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SortBy;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageBackgroundProperties;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageInventorySlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.TransferFullSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.ICraftingUIPart;
import net.p3pp3rf1y.sophisticatedcore.util.ColorHelper;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

public abstract class StorageScreenBase<S extends StorageContainerMenuBase<?>> extends AbstractContainerScreen<S>
		implements InventoryScrollPanel.IInventoryScreen {
	public static final int ERROR_BACKGROUND_COLOR = 0xF0100010;
	public static final int ERROR_BORDER_COLOR = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors()) | 0xFF000000;
	private static final int DISABLED_SLOT_COLOR = -1072689136;
	private static final int UPGRADE_TOP_HEIGHT = 7;
	private static final int UPGRADE_SLOT_HEIGHT = 16;
	private static final int UPGRADE_BOTTOM_HEIGHT = 6;
	public static final int UPGRADE_INVENTORY_OFFSET = 21;
	public static final int DISABLED_SLOT_X_POS = -1000;
	static final int SLOTS_Y_OFFSET = 17;
	static final int SLOTS_X_OFFSET = 7;
	public static final int ERROR_SLOT_COLOR = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors()) | 0xAA000000;
	private static final int ERROR_TEXT_COLOR = ColorHelper.getColor(DyeColor.RED.getTextureDiffuseColors());
	public static final int HEIGHT_WITHOUT_STORAGE_SLOTS = 114;

	private UpgradeSettingsTabControl settingsTabControl;
	private final int numberOfUpgradeSlots;
	@Nullable
	private Button sortButton = null;
	@Nullable
	private ToggleButton<SortBy> sortByButton = null;

	private InventoryScrollPanel inventoryScrollPanel = null;
	private final Set<ToggleButton<Boolean>> upgradeSwitches = new HashSet<>();

	private final Map<Integer, UpgradeInventoryPartBase<?>> inventoryParts = new LinkedHashMap<>();

	private static ICraftingUIPart craftingUIPart = ICraftingUIPart.NOOP;

	private StorageBackgroundProperties storageBackgroundProperties;

	public static void setCraftingUIPart(ICraftingUIPart part) {
		craftingUIPart = part;
	}

	private static final Set<IButtonFactory> buttonFactories = new HashSet<>();

	public static void addButtonFactory(IButtonFactory buttonFactory) {
		buttonFactories.add(buttonFactory);
	}

	protected StorageScreenBase(S pMenu, Inventory pPlayerInventory, Component pTitle) {
		super(pMenu, pPlayerInventory, pTitle);
		numberOfUpgradeSlots = getMenu().getNumberOfUpgradeSlots();
		updateDimensionsAndSlotPositions(Minecraft.getInstance().getWindow().getGuiScaledHeight());
		passEvents = true;
	}

	public ICraftingUIPart getCraftingUIAddition() {
		return craftingUIPart;
	}

	@Override
	public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
		updateDimensionsAndSlotPositions(pHeight);
		super.resize(pMinecraft, pWidth, pHeight);
	}

	private void updateDimensionsAndSlotPositions(int pHeight) {
		int displayableNumberOfRows = Math.min((pHeight - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
		int newImageHeight = HEIGHT_WITHOUT_STORAGE_SLOTS + getStorageInventoryHeight(displayableNumberOfRows);
		storageBackgroundProperties = (getMenu().getNumberOfStorageInventorySlots() + getMenu().getColumnsTaken() * getMenu().getNumberOfRows()) <= 81 ? StorageBackgroundProperties.REGULAR_9_SLOT : StorageBackgroundProperties.REGULAR_12_SLOT;

		imageWidth = storageBackgroundProperties.getSlotsOnLine() * 18 + 14;
		updateStorageSlotsPositions();
		if (displayableNumberOfRows < getMenu().getNumberOfRows()) {
			storageBackgroundProperties = storageBackgroundProperties == StorageBackgroundProperties.REGULAR_9_SLOT ? StorageBackgroundProperties.WIDER_9_SLOT : StorageBackgroundProperties.WIDER_12_SLOT;
			imageWidth += 6;
		}
		imageHeight = newImageHeight;
		inventoryLabelY = imageHeight - 94;
		inventoryLabelX = 8 + storageBackgroundProperties.getPlayerInventoryXOffset();
		updatePlayerSlotsPositions();
		updateUpgradeSlotsPositions();
	}

	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return displayableNumberOfRows * 18;
	}

	@Override
	public Slot getSlot(int slotIndex) {
		return getMenu().getSlot(slotIndex);
	}

	private void updateUpgradeSlotsPositions() {
		int yPosition = 6;
		for (int slotIndex = 0; slotIndex < numberOfUpgradeSlots; slotIndex++) {
			Slot slot = getMenu().getSlot(getMenu().getFirstUpgradeSlot() + slotIndex);
			slot.y = yPosition;
			yPosition += UPGRADE_SLOT_HEIGHT;
		}
	}

	protected void updateStorageSlotsPositions() {
		int yPosition = 18;

		int slotIndex = 0;
		while (slotIndex < getMenu().getNumberOfStorageInventorySlots()) {
			Slot slot = getMenu().getSlot(slotIndex);
			int lineIndex = slotIndex % getSlotsOnLine();
			slot.x = 8 + lineIndex * 18;
			slot.y = yPosition;

			slotIndex++;
			if (slotIndex % getSlotsOnLine() == 0) {
				yPosition += 18;
			}
		}
	}

	protected void updatePlayerSlotsPositions() {
		int playerInventoryXOffset = storageBackgroundProperties.getPlayerInventoryXOffset();

		int yPosition = inventoryLabelY + 12;

		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				int slotIndex = j + i * 9;
				int xPosition = playerInventoryXOffset + 8 + j * 18;
				Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + slotIndex);
				slot.x = xPosition;
				slot.y = yPosition;
			}
			yPosition += 18;
		}

		yPosition += 4;

		for (int slotIndex = 0; slotIndex < 9; ++slotIndex) {
			int xPosition = playerInventoryXOffset + 8 + slotIndex * 18;
			Slot slot = getMenu().getSlot(getMenu().getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + 3 * 9 + slotIndex);
			slot.x = xPosition;
			slot.y = yPosition;
		}
	}

	@Override
	protected void init() {
		super.init();
		updateInventoryScrollPanel();
		craftingUIPart.setStorageScreen(this);
		initUpgradeSettingsControl();
		initUpgradeInventoryParts();
		addUpgradeSwitches();
		getMenu().setUpgradeChangeListener(c -> {
			updateStorageSlotsPositions();
			updatePlayerSlotsPositions();
			updateUpgradeSlotsPositions();
			updateInventoryScrollPanel();
			children().remove(settingsTabControl);
			craftingUIPart.onCraftingSlotsHidden();
			initUpgradeSettingsControl();
			initUpgradeInventoryParts();
			addUpgradeSwitches();
		});
		if (shouldShowSortButtons()) {
			addSortButtons();
		}
		addAdditionalButtons();
	}

	protected boolean shouldShowSortButtons() {
		return true;
	}

	private void addAdditionalButtons() {
		buttonFactories.forEach(factory -> addRenderableWidget(factory.instantiateButton(this)));
	}

	private void updateInventoryScrollPanel() {
		if (inventoryScrollPanel != null) {
			removeWidget(inventoryScrollPanel);
		}

		int numberOfVisibleRows = getNumberOfVisibleRows();
		if (numberOfVisibleRows < getMenu().getNumberOfRows()) {
			inventoryScrollPanel = new InventoryScrollPanel(Minecraft.getInstance(), this, 0, getMenu().getNumberOfStorageInventorySlots(), getSlotsOnLine(), numberOfVisibleRows * 18, this.getGuiTop() + 17, this.getGuiLeft() + 7);
			addRenderableWidget(inventoryScrollPanel);
			inventoryScrollPanel.updateSlotsYPosition();
		} else {
			inventoryScrollPanel = null;
		}
	}

	private int getNumberOfVisibleRows() {
		return Math.min((imageHeight - HEIGHT_WITHOUT_STORAGE_SLOTS) / 18, getMenu().getNumberOfRows());
	}

	public int getSlotsOnLine() {
		return storageBackgroundProperties.getSlotsOnLine() - getMenu().getColumnsTaken();
	}

	private void initUpgradeInventoryParts() {
		inventoryParts.clear();
		if (getMenu().getColumnsTaken() == 0) {
			return;
		}

		int numberOfVisibleRows = getNumberOfVisibleRows();
		int scrollBarOffset = numberOfVisibleRows < getMenu().getNumberOfRows() ? 6 : 0;
		AtomicReference<Position> pos = new AtomicReference<>(new Position(SLOTS_X_OFFSET + getSlotsOnLine() * 18 + scrollBarOffset, SLOTS_Y_OFFSET));
		int height = numberOfVisibleRows * 18;
		for (Map.Entry<Integer, UpgradeContainerBase<?, ?>> entry : getMenu().getUpgradeContainers().entrySet()) {
			UpgradeContainerBase<?, ?> container = entry.getValue();
			UpgradeGuiManager.getInventoryPart(entry.getKey(), container, pos.get(), height, this).ifPresent(part -> {
				inventoryParts.put(entry.getKey(), part);
				pos.set(new Position(pos.get().x() + 36, pos.get().y()));
			});
		}
	}

	private void addUpgradeSwitches() {
		upgradeSwitches.clear();
		int switchTop = topPos + 8;
		for (int slot = 0; slot < numberOfUpgradeSlots; slot++) {
			if (menu.canDisableUpgrade(slot)) {
				int finalSlot = slot;
				ToggleButton<Boolean> upgradeSwitch = new ToggleButton<>(new Position(leftPos - 22, switchTop), ButtonDefinitions.UPGRADE_SWITCH,
						button -> getMenu().setUpgradeEnabled(finalSlot, !getMenu().getUpgradeEnabled(finalSlot)), () -> getMenu().getUpgradeEnabled(finalSlot));
				addWidget(upgradeSwitch);
				upgradeSwitches.add(upgradeSwitch);
			}
			switchTop += UPGRADE_SLOT_HEIGHT;
		}
	}

	private void addSortButtons() {
		SortButtonsPosition sortButtonsPosition = Config.CLIENT.sortButtonsPosition.get();
		if (sortButtonsPosition == SortButtonsPosition.HIDDEN) {
			return;
		}

		Position pos = getSortButtonsPosition(sortButtonsPosition);

		sortButton = new Button(new Position(pos.x(), pos.y()), ButtonDefinitions.SORT, button -> {
			if (button == 0) {
				getMenu().sort();
				//noinspection ConstantConditions - by this point player can't be null
				Minecraft.getInstance().player.displayClientMessage(Component.literal("Sorted"), true);
			}
		});
		addWidget(sortButton);
		sortByButton = new ToggleButton<>(new Position(pos.x() + 14, pos.y()), ButtonDefinitions.SORT_BY, button -> {
			if (button == 0) {
				getMenu().setSortBy(getMenu().getSortBy().next());
			}
		}, () -> getMenu().getSortBy());
		addWidget(sortByButton);

	}

	private Position getSortButtonsPosition(SortButtonsPosition sortButtonsPosition) {
		return switch (sortButtonsPosition) {
			case BELOW_UPGRADES ->
					new Position(leftPos - UPGRADE_INVENTORY_OFFSET - 2, topPos + getUpgradeHeightWithoutBottom() + UPGRADE_BOTTOM_HEIGHT + 2);
			case BELOW_UPGRADE_TABS ->
					settingsTabControl == null ? new Position(0, 0) : new Position(settingsTabControl.getX() + 2, settingsTabControl.getY() + Math.max(0, settingsTabControl.getHeight() + 2));
			default -> new Position(leftPos + imageWidth - 34, topPos + 4);
		};
	}

	private void initUpgradeSettingsControl() {
		settingsTabControl = new UpgradeSettingsTabControl(new Position(leftPos + imageWidth, topPos + 4), this, getStorageSettingsTabTooltip());
		addWidget(settingsTabControl);
	}

	protected abstract String getStorageSettingsTabTooltip();

	public int getUpgradeHeight() {
		return getUpgradeHeightWithoutBottom() + UPGRADE_TOP_HEIGHT;
	}

	protected int getUpgradeHeightWithoutBottom() {
		return UPGRADE_BOTTOM_HEIGHT + numberOfUpgradeSlots * UPGRADE_SLOT_HEIGHT;
	}

	public Optional<Rect2i> getSortButtonsRectangle() {
		if (sortButton == null || sortByButton == null) {
			return Optional.empty();
		}
		return GuiHelper.getPositiveRectangle(sortButton.getX(), sortButton.getY(), sortByButton.getX() + sortByButton.getWidth() - sortButton.getX(), sortByButton.getY() + sortByButton.getHeight() - sortButton.getY());
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		if (menu.detectSettingsChangeAndReload()) {
			updateStorageSlotsPositions();
			updatePlayerSlotsPositions();
			updateInventoryScrollPanel();
		}
		poseStack.pushPose();
		poseStack.translate(0, 0, -20);
		renderBackground(poseStack);
		poseStack.popPose();
		settingsTabControl.render(poseStack, mouseX, mouseY, partialTicks);
		poseStack.pushPose();

		super.render(poseStack, mouseX, mouseY, partialTicks);

		settingsTabControl.renderTooltip(this, poseStack, mouseX, mouseY);
		if (sortButton != null && sortByButton != null) {
			sortButton.render(poseStack, mouseX, mouseY, partialTicks);
			sortByButton.render(poseStack, mouseX, mouseY, partialTicks);
		}
		upgradeSwitches.forEach(us -> us.render(poseStack, mouseX, mouseY, partialTicks));
		renderErrorOverlay(poseStack);
		renderTooltip(poseStack, mouseX, mouseY);
		poseStack.popPose();
	}

	@Override
	protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
		super.renderLabels(matrixStack, mouseX, mouseY);
		renderUpgradeInventoryParts(matrixStack, mouseX, mouseY);
		renderUpgradeSlots(matrixStack, mouseX, mouseY);
		if (inventoryScrollPanel == null) {
			renderStorageInventorySlots(matrixStack, mouseX, mouseY);
		}
	}

	private void renderUpgradeInventoryParts(PoseStack matrixStack, int mouseX, int mouseY) {
		inventoryParts.values().forEach(ip -> ip.render(matrixStack, mouseX, mouseY));
	}

	private void renderStorageInventorySlots(PoseStack matrixStack, int mouseX, int mouseY) {
		renderStorageInventorySlots(matrixStack, mouseX, mouseY, true);
	}

	private void renderStorageInventorySlots(PoseStack matrixStack, int mouseX, int mouseY, boolean canShowHover) {
		for (int slotId = 0; slotId < menu.realInventorySlots.size(); ++slotId) {
			Slot slot = menu.realInventorySlots.get(slotId);
			renderSlot(matrixStack, slot);

			if (canShowHover && isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotOverlay(matrixStack, slot, getSlotColor(slotId));
			}
		}
	}

	private void renderUpgradeSlots(PoseStack matrixStack, int mouseX, int mouseY) {
		for (int slotId = 0; slotId < menu.upgradeSlots.size(); ++slotId) {
			Slot slot = menu.upgradeSlots.get(slotId);
			if (slot.x != DISABLED_SLOT_X_POS) {
				renderSlot(matrixStack, slot);
				if (!slot.isActive()) {
					renderSlotOverlay(matrixStack, slot, DISABLED_SLOT_COLOR);
				}
			}

			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				hoveredSlot = slot;
				renderSlotOverlay(matrixStack, slot, getSlotColor(slotId));
			}
		}
	}

	@Override
	protected void renderSlot(PoseStack poseStack, Slot slot) {
		int i = slot.x;
		int j = slot.y;
		ItemStack stackToRender = slot.getItem();
		boolean flag = false;
		boolean rightClickDragging = slot == clickedSlot && !draggingItem.isEmpty() && !isSplittingStack;
		ItemStack carriedStack = getMenu().getCarried();
		String stackCountText = null;
		if (slot == clickedSlot && !draggingItem.isEmpty() && isSplittingStack && !stackToRender.isEmpty()) {
			stackToRender = stackToRender.copy();
			stackToRender.setCount(stackToRender.getCount() / 2);
		} else if (isQuickCrafting && quickCraftSlots.contains(slot) && !carriedStack.isEmpty()) {
			if (quickCraftSlots.size() == 1) {
				return;
			}

			if (StorageContainerMenuBase.canItemQuickReplace(slot, carriedStack) && menu.canDragTo(slot)) {
				stackToRender = carriedStack.copy();
				flag = true;
				AbstractContainerMenu.getQuickCraftSlotCount(quickCraftSlots, quickCraftingType, stackToRender, slot.getItem().isEmpty() ? 0 : slot.getItem().getCount());
				int slotLimit = slot.getMaxStackSize(stackToRender);
				if (stackToRender.getCount() > slotLimit) {
					stackCountText = ChatFormatting.YELLOW + CountAbbreviator.abbreviate(slotLimit);
					stackToRender.setCount(slotLimit);
				}
			} else {
				quickCraftSlots.remove(slot);
				recalculateQuickCraftRemaining();
			}
		}

		setBlitOffset(100);
		itemRenderer.blitOffset = 100.0F;
		if (stackToRender.isEmpty() && slot.isActive()) {
			renderSlotBackground(poseStack, slot, i, j);
		} else if (!rightClickDragging) {
			renderStack(poseStack, i, j, stackToRender, flag, stackCountText);
		}

		itemRenderer.blitOffset = 0.0F;
		setBlitOffset(0);
	}

	private void renderStack(PoseStack poseStack, int i, int j, ItemStack itemstack, boolean flag, @Nullable String stackCountText) {
		if (flag) {
			fill(poseStack, i, j, i + 16, j + 16, -2130706433);
		}

		RenderSystem.enableDepthTest();
		itemRenderer.renderAndDecorateItem(itemstack, i, j);
		if (shouldUseSpecialCountRender(itemstack)) {
			itemRenderer.renderGuiItemDecorations(font, itemstack, i, j, "");
			if (stackCountText == null) {
				stackCountText = CountAbbreviator.abbreviate(itemstack.getCount());
			}
			renderStackCount(stackCountText, i, j);
		} else {
			itemRenderer.renderGuiItemDecorations(font, itemstack, i, j, stackCountText);
		}
	}

	private void renderSlotBackground(PoseStack poseStack, Slot slot, int i, int j) {
		Optional<ItemStack> memorizedStack = getMenu().getMemorizedStackInSlot(slot.index);
		if (memorizedStack.isPresent()) {
			itemRenderer.renderAndDecorateItem(memorizedStack.get(), i, j);
			drawStackOverlay(poseStack, i, j);
		} else if (!getMenu().getSlotFilterItem(slot.index).isEmpty()) {
			itemRenderer.renderAndDecorateItem(getMenu().getSlotFilterItem(slot.index), i, j);
			drawStackOverlay(poseStack, i, j);
		} else {
			Pair<ResourceLocation, ResourceLocation> pair = slot.getNoItemIcon();
			if (pair != null) {
				//noinspection ConstantConditions - by this point minecraft isn't null
				TextureAtlasSprite textureatlassprite = minecraft.getTextureAtlas(pair.getFirst()).apply(pair.getSecond());
				RenderSystem.setShaderTexture(0, textureatlassprite.atlas().location());
				blit(poseStack, i, j, getBlitOffset(), 16, 16, textureatlassprite);
			}
		}
	}

	private void drawStackOverlay(PoseStack poseStack, int x, int y) {
		poseStack.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, GuiHelper.GUI_CONTROLS);
		blit(poseStack, x, y, 77, 0, 16, 16);
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		poseStack.popPose();
	}

	private boolean shouldUseSpecialCountRender(ItemStack itemstack) {
		return itemstack.getCount() > 99;
	}

	private void renderSlotOverlay(PoseStack matrixStack, Slot slot, int slotColor) {
		renderSlotOverlay(matrixStack, slot, slotColor, 0, 16);
	}

	private void renderSlotOverlay(PoseStack matrixStack, Slot slot, int slotColor, int yOffset, int height) {
		renderOverlay(matrixStack, slotColor, slot.x, slot.y + yOffset, 16, height);
	}

	public void renderOverlay(PoseStack matrixStack, int slotColor, int xPos, int yPos, int width, int height) {
		RenderSystem.disableDepthTest();
		RenderSystem.colorMask(true, true, true, false);
		fillGradient(matrixStack, xPos, yPos, xPos + width, yPos + height, slotColor, slotColor);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.enableDepthTest();
	}

	protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		drawInventoryBg(matrixStack, x, y, storageBackgroundProperties.getTextureName());
		if (inventoryScrollPanel == null) {
			drawSlotBg(matrixStack, x, y);
			drawSlotOverlays(matrixStack);
		}
		drawUpgradeBackground(matrixStack);
	}

	protected void drawSlotBg(PoseStack matrixStack, int x, int y) {
		int inventorySlots = getMenu().getNumberOfStorageInventorySlots();
		int slotsOnLine = getSlotsOnLine();
		int slotRows = inventorySlots / slotsOnLine;
		int remainingSlots = inventorySlots % slotsOnLine;
		GuiHelper.renderSlotsBackground(matrixStack, x + StorageScreenBase.SLOTS_X_OFFSET, y + StorageScreenBase.SLOTS_Y_OFFSET, slotsOnLine, slotRows, remainingSlots);
	}

	private void drawSlotOverlays(PoseStack matrixStack) {
		matrixStack.pushPose();
		matrixStack.translate(this.getGuiLeft(), this.getGuiTop(), 0.0F);
		for (int slotNumber = 0; slotNumber < menu.getNumberOfStorageInventorySlots(); slotNumber++) {
			List<Integer> colors = menu.getSlotOverlayColors(slotNumber);
			if (!colors.isEmpty()) {
				int stripeHeight = 16 / colors.size();
				int i = 0;
				for (int slotColor : colors) {
					int yOffset = i * stripeHeight;
					renderSlotOverlay(matrixStack, menu.getSlot(slotNumber), slotColor | (80 << 24), yOffset, i == colors.size() - 1 ? 16 - yOffset : stripeHeight);
					i++;
				}
			}
		}
		matrixStack.popPose();
	}

	@Override
	protected void renderTooltip(PoseStack poseStack, int x, int y) {
		poseStack.pushPose();
		inventoryParts.values().forEach(part -> part.renderTooltip(this, poseStack, x, y));
		if (getMenu().getCarried().isEmpty() && hoveredSlot != null) {
			if (hoveredSlot.hasItem()) {
				renderTooltip(poseStack, hoveredSlot.getItem(), x, y);
			} else if (hoveredSlot instanceof INameableEmptySlot emptySlot && emptySlot.hasEmptyTooltip()) {
				renderComponentTooltip(poseStack, Collections.singletonList(emptySlot.getEmptyTooltip()), x, y);
			}
		}
		if (sortButton != null) {
			sortButton.renderTooltip(this, poseStack, x, y);
		}
		if (sortByButton != null) {
			sortByButton.renderTooltip(this, poseStack, x, y);
		}
		poseStack.popPose();
	}

	@Override
	public List<Component> getTooltipFromItem(ItemStack itemStack) {
		List<Component> ret = super.getTooltipFromItem(itemStack);
		if (itemStack.getCount() > 999) {
			ret.add(Component.translatable("gui.sophisticatedcore.tooltip.stack_count",
					Component.literal(NumberFormat.getNumberInstance().format(itemStack.getCount())).withStyle(ChatFormatting.DARK_AQUA))
					.withStyle(ChatFormatting.GRAY)
			);
		}
		return ret;
	}

	public void drawInventoryBg(PoseStack matrixStack, int x, int y, ResourceLocation textureName) {
		StorageGuiHelper.renderStorageBackground(new Position(x, y), matrixStack, textureName, imageWidth, imageHeight - HEIGHT_WITHOUT_STORAGE_SLOTS);
	}

	private void drawUpgradeBackground(PoseStack matrixStack) {
		if (numberOfUpgradeSlots == 0) {
			return;
		}

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, GuiHelper.GUI_CONTROLS);

		int heightWithoutBottom = getUpgradeHeightWithoutBottom();

		blit(matrixStack, leftPos - UPGRADE_INVENTORY_OFFSET, topPos, 0, 0, 26, 4, 256, 256);
		blit(matrixStack, leftPos - UPGRADE_INVENTORY_OFFSET, topPos + 4, 0, 4, 25, heightWithoutBottom - 4, 256, 256);
		blit(matrixStack, leftPos - UPGRADE_INVENTORY_OFFSET, topPos + heightWithoutBottom, 0, 198, 25, UPGRADE_BOTTOM_HEIGHT, 256, 256);

		boolean previousHasSwitch = false;
		for (int slot = 0; slot < numberOfUpgradeSlots; slot++) {
			if (menu.canDisableUpgrade(slot)) {
				int y = topPos + 5 + slot * UPGRADE_SLOT_HEIGHT + (previousHasSwitch ? 1 : 0);

				blit(matrixStack, leftPos - UPGRADE_INVENTORY_OFFSET - 4, y, 0, 204 + (previousHasSwitch ? 1 : 0), 7, 18 - (previousHasSwitch ? 1 : 0), 256, 256);
				previousHasSwitch = true;
			} else {
				previousHasSwitch = false;
			}
		}
	}

	public UpgradeSettingsTabControl getUpgradeSettingsControl() {
		if (settingsTabControl == null) {
			settingsTabControl = new UpgradeSettingsTabControl(new Position(leftPos + imageWidth, topPos + 4), this, getStorageSettingsTabTooltip());
		}
		return settingsTabControl;
	}

	@Nullable
	@Override
	public Slot findSlot(double mouseX, double mouseY) {
		for (int i = 0; i < menu.upgradeSlots.size(); ++i) {
			Slot slot = menu.upgradeSlots.get(i);
			if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
				return slot;
			}
		}

		if (inventoryScrollPanel != null) {
			Optional<Slot> result = inventoryScrollPanel.findSlot(mouseX, mouseY);
			if (result.isPresent()) {
				return result.get();
			}
			Slot slot = super.findSlot(mouseX, mouseY);

			return slot == null || menu.isStorageInventorySlot(slot.index) ? null : slot; //if super finds inventory slot that's hidden inside the scroll panel just return null
		} else {
			for (int i = 0; i < menu.realInventorySlots.size(); ++i) {
				Slot slot = menu.realInventorySlots.get(i);
				if (isHovering(slot, mouseX, mouseY) && slot.isActive()) {
					return slot;
				}
			}
			return super.findSlot(mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (UpgradeInventoryPartBase<?> inventoryPart : inventoryParts.values()) {
			if (inventoryPart.handleMouseReleased(mouseX, mouseY, button)) {
				return true;
			}
		}

		handleQuickMoveAll(mouseX, mouseY, button);

		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void handleQuickMoveAll(double mouseX, double mouseY, int button) {
		Slot slot = findSlot(mouseX, mouseY);
		if (doubleclick && !getMenu().getCarried().isEmpty() && slot != null && button == 0 && menu.canTakeItemForPickAll(ItemStack.EMPTY, slot) && hasShiftDown() && !lastQuickMoved.isEmpty()) {
			for (Slot slot2 : menu.realInventorySlots) {
				tryQuickMoveSlot(button, slot, slot2);
			}
		}
	}

	private void tryQuickMoveSlot(int button, Slot slot, Slot slot2) {
		//noinspection ConstantConditions - by this point minecraft isn't null
		if (slot2.mayPickup(minecraft.player) && slot2.hasItem() && slot2.isSameInventory(slot)) {
			ItemStack slotItem = slot2.getItem();
			if (ItemStack.isSameItemSameTags(lastQuickMoved, slotItem)) {
				if (slotItem.getCount() > slotItem.getMaxStackSize()) {
					PacketHandler.sendToServer(new TransferFullSlotMessage(slot2.index));
				} else {
					slotClicked(slot2, slot2.index, button, ClickType.QUICK_MOVE);
				}
			}
		}
	}

	@Override
	protected void slotClicked(Slot slot, int slotNumber, int mouseButton, ClickType type) {
		if (type == ClickType.PICKUP_ALL && !menu.getSlotUpgradeContainer(slot).map(c -> c.allowsPickupAll(slot)).orElse(true)) {
			type = ClickType.PICKUP;
		}

		handleInventoryMouseClick(slotNumber, mouseButton, type);
	}

	private void handleInventoryMouseClick(int slotNumber, int mouseButton, ClickType type) {
		StorageContainerMenuBase<?> menu = getMenu();
		List<ItemStack> realInventoryItems = new ArrayList<>(menu.realInventorySlots.size());
		menu.realInventorySlots.forEach(slot -> realInventoryItems.add(slot.getItem().copy()));
		List<ItemStack> upgradeItems = new ArrayList<>(menu.upgradeSlots.size());
		menu.upgradeSlots.forEach(slot -> upgradeItems.add(slot.getItem().copy()));

		//noinspection ConstantConditions - by this point minecraft isn't null
		menu.clicked(slotNumber, mouseButton, type, minecraft.player);
		Int2ObjectMap<ItemStack> changedSlotIndexes = new Int2ObjectOpenHashMap<>();

		int inventorySlotsToCheck = Math.min(realInventoryItems.size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS, menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS);

		for (int i = 0; i < inventorySlotsToCheck; i++) {
			ItemStack itemstack = realInventoryItems.get(i);
			ItemStack slotStack = menu.getSlot(i).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotIndexes.put(i, slotStack.copy());
			}
		}

		for (int i = 0; i < StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS; i++) {
			ItemStack itemstack = realInventoryItems.get(realInventoryItems.size() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i);
			int slotIndex = menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i;
			ItemStack slotStack = menu.getSlot(slotIndex).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotIndexes.put(slotIndex, slotStack.copy());
			}
		}

		int lastChecked = 0;
		int upgradeSlotsToCheck = Math.min(menu.getUpgradeSlotsSize(), upgradeItems.size());

		for (; lastChecked < upgradeSlotsToCheck; lastChecked++) {
			ItemStack itemstack = upgradeItems.get(lastChecked);
			ItemStack slotStack = menu.getSlot(menu.getInventorySlotsSize() + lastChecked).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				break;
			}
		}

		for (int i = upgradeSlotsToCheck - 1; i >= lastChecked; i--) {
			ItemStack itemstack = upgradeItems.get(i);
			int slotIndex = menu.getInventorySlotsSize() + i;
			ItemStack slotStack = menu.getSlot(slotIndex).getItem();
			if (!ItemStack.matches(itemstack, slotStack)) {
				changedSlotIndexes.put(slotIndex, slotStack.copy());
			}
		}

		minecraft.player.connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), slotNumber, mouseButton, type, menu.getCarried().copy(), changedSlotIndexes));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		Slot slot = findSlot(mouseX, mouseY);
		if (hasShiftDown() && hasControlDown() && slot instanceof StorageInventorySlot && button == 0) {
			PacketHandler.sendToServer(new TransferFullSlotMessage(slot.index));
			return true;
		}
		GuiEventListener focused = getFocused();
		if (focused != null && !focused.isMouseOver(mouseX, mouseY) && (focused instanceof WidgetBase widgetBase)) {
				widgetBase.setFocus(false);

		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		for (GuiEventListener child : children()) {
			if (child.isMouseOver(mouseX, mouseY) && child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				return true;
			}
		}
		Slot slot = findSlot(mouseX, mouseY);
		ItemStack itemstack = getMenu().getCarried();
		if (isQuickCrafting) {
			if (slot != null && !itemstack.isEmpty()
					&& (itemstack.getCount() > quickCraftSlots.size() || quickCraftingType == 2)
					&& StorageContainerMenuBase.canItemQuickReplace(slot, itemstack) && slot.mayPlace(itemstack)
					&& menu.canDragTo(slot)
					&& isAllowedSlotCombination(slot, itemstack)) {
				quickCraftSlots.add(slot);
				recalculateQuickCraftRemaining();
			}
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	private boolean isAllowedSlotCombination(Slot slot, ItemStack carried) {
		if (quickCraftSlots.isEmpty() || !(carried.getItem() instanceof UpgradeItemBase<?> upgradeItem) || upgradeItem.getInventoryColumnsTaken() == 0) {
			return true;
		}
		return quickCraftSlots.contains(slot) || (!(quickCraftSlots.iterator().next() instanceof StorageContainerMenuBase.StorageUpgradeSlot) && !(slot instanceof StorageContainerMenuBase.StorageUpgradeSlot));
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeftIn, int guiTopIn, int mouseButton) {
		return super.hasClickedOutside(mouseX, mouseY, guiLeftIn, guiTopIn, mouseButton) && hasClickedOutsideOfUpgradeSlots(mouseX, mouseY)
				&& hasClickedOutsideOfUpgradeSettings(mouseX, mouseY);
	}

	private boolean hasClickedOutsideOfUpgradeSettings(double mouseX, double mouseY) {
		return settingsTabControl.getTabRectangles().stream().noneMatch(r -> r.contains((int) mouseX, (int) mouseY));
	}

	private boolean hasClickedOutsideOfUpgradeSlots(double mouseX, double mouseY) {
		return !getUpgradeSlotsRectangle().map(r -> r.contains((int) mouseX, (int) mouseY)).orElse(false);
	}

	public Optional<Rect2i> getUpgradeSlotsRectangle() {
		return numberOfUpgradeSlots == 0 ? Optional.empty() : GuiHelper.getPositiveRectangle(leftPos - UPGRADE_INVENTORY_OFFSET + 4, topPos, UPGRADE_INVENTORY_OFFSET + 4, getUpgradeHeight());
	}

	private void renderStackCount(String count, int x, int y) {
		PoseStack poseStrack = new PoseStack();
		poseStrack.translate(0.0D, 0.0D, itemRenderer.blitOffset + 200.0F);
		float scale = Math.min(1f, (float) 16 / font.width(count));
		if (scale < 1f) {
			poseStrack.scale(scale, scale, 1.0F);
		}
		MultiBufferSource.BufferSource renderBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		font.drawInBatch(count, (x + 19 - 2 - (font.width(count) * scale)) / scale,
				(y + 6 + 3 + (1 / (scale * scale) - 1)) / scale, 16777215, true, poseStrack.last().pose(), renderBuffer, false, 0, 15728880);
		renderBuffer.endBatch();
	}

	@Override
	protected void recalculateQuickCraftRemaining() {
		ItemStack cursorStack = getMenu().getCarried();
		if (!cursorStack.isEmpty() && isQuickCrafting) {
			if (quickCraftingType == 2) {
				quickCraftingRemainder = cursorStack.getMaxStackSize();
			} else {
				quickCraftingRemainder = cursorStack.getCount();

				for (Slot slot : quickCraftSlots) {
					ItemStack itemstack1 = cursorStack.copy();
					ItemStack slotStack = slot.getItem();
					int slotStackCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
					AbstractContainerMenu.getQuickCraftSlotCount(quickCraftSlots, quickCraftingType, itemstack1, slotStackCount);
					int j = slot.getMaxStackSize(itemstack1);
					if (itemstack1.getCount() > j) {
						itemstack1.setCount(j);
					}

					quickCraftingRemainder -= itemstack1.getCount() - slotStackCount;
				}
			}
		}
	}

	private void renderErrorOverlay(PoseStack matrixStack) {
		menu.getErrorUpgradeSlotChangeResult().ifPresent(upgradeSlotChangeResult -> upgradeSlotChangeResult.getErrorMessage().ifPresent(overlayErrorMessage -> {
			matrixStack.pushPose();
			matrixStack.translate(this.getGuiLeft(), this.getGuiTop(), 0.0F);
			upgradeSlotChangeResult.getErrorUpgradeSlots().forEach(slotIndex -> renderSlotOverlay(matrixStack, menu.getSlot(menu.getFirstUpgradeSlot() + slotIndex), ERROR_SLOT_COLOR));
			upgradeSlotChangeResult.getErrorInventorySlots().forEach(slotIndex -> {
				Slot slot = menu.getSlot(slotIndex);
				//noinspection ConstantConditions
				if (slot != null) {
					renderSlotOverlay(matrixStack, slot, ERROR_SLOT_COLOR);
				}
			});
			upgradeSlotChangeResult.getErrorInventoryParts().forEach(partIndex -> {
				if (inventoryParts.size() > partIndex) {
					UpgradeInventoryPartBase<?> inventoryPart = inventoryParts.get(partIndex);
					if (inventoryPart != null) {
						inventoryPart.renderErrorOverlay(matrixStack);
					}
				}
			});
			matrixStack.popPose();

			renderErrorMessage(matrixStack, overlayErrorMessage);
		}));
	}

	private void renderErrorMessage(PoseStack matrixStack, Component overlayErrorMessage) {
		matrixStack.pushPose();
		RenderSystem.disableDepthTest();
		matrixStack.translate((float) width / 2, (double) topPos + inventoryLabelY + 4, 300F);
		Font fontrenderer = Minecraft.getInstance().font;

		int tooltipWidth = font.width(overlayErrorMessage);

		List<FormattedText> wrappedTextLines = new ArrayList<>();
		int maxLineWidth = 260;
		if (tooltipWidth > maxLineWidth) {
			int wrappedTooltipWidth = 0;
			List<FormattedText> wrappedLine = font.getSplitter().splitLines(overlayErrorMessage, maxLineWidth, Style.EMPTY);

			for (FormattedText line : wrappedLine) {
				int lineWidth = font.width(line);
				if (lineWidth > wrappedTooltipWidth) {wrappedTooltipWidth = lineWidth;}
				wrappedTextLines.add(line);
			}
			tooltipWidth = wrappedTooltipWidth;
		} else {
			wrappedTextLines.add(overlayErrorMessage);
		}

		int tooltipHeight = 8;
		if (wrappedTextLines.size() > 1) {
			tooltipHeight += 2 + (wrappedTextLines.size() - 1) * 10;
		}

		Matrix4f matrix4f = matrixStack.last().pose();
		float leftX = (float) -tooltipWidth / 2;

		GuiHelper.renderTooltipBackground(matrix4f, tooltipWidth, (int) leftX, 0, tooltipHeight, StorageScreenBase.ERROR_BACKGROUND_COLOR, StorageScreenBase.ERROR_BORDER_COLOR, StorageScreenBase.ERROR_BORDER_COLOR);
		MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		matrixStack.translate(0.0D, 0.0D, 400.0D);
		GuiHelper.writeTooltipLines(wrappedTextLines, fontrenderer, leftX, 0, matrix4f, renderTypeBuffer, ERROR_TEXT_COLOR);
		renderTypeBuffer.endBatch();
		matrixStack.popPose();
	}

	@Override
	public void renderInventorySlots(PoseStack matrixStack, int mouseX, int mouseY, boolean canShowHover) {
		renderStorageInventorySlots(matrixStack, mouseX, mouseY, canShowHover);
	}

	@Override
	public boolean isMouseOverSlot(Slot pSlot, double pMouseX, double pMouseY) {
		return isHovering(pSlot, pMouseX, pMouseY);
	}

	@Override
	public void drawSlotBg(PoseStack matrixStack) {
		drawSlotBg(matrixStack, (width - imageWidth) / 2, (height - imageHeight) / 2);
		drawSlotOverlays(matrixStack);
	}

	@Override
	public int getTopY() {
		return this.getGuiTop();
	}

	@Override
	public int getLeftX() {
		return this.getGuiLeft();
	}

	public Position getRightTopAbovePlayersInventory() {
		return new Position(storageBackgroundProperties.getPlayerInventoryXOffset() + 8 + 9 * 18, inventoryLabelY);
	}
}
