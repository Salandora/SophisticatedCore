package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeInventoryPartBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.*;
import net.p3pp3rf1y.sophisticatedcore.init.ModFluids;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.ScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import net.p3pp3rf1y.sophisticatedcore.util.FluidHelper;
import net.p3pp3rf1y.sophisticatedcore.util.XpHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TankInventoryPart extends UpgradeInventoryPartBase<TankUpgradeContainer> {
	private static final TextureBlitData OVERLAY = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(47, 30), new Dimension(16, 18));
	private final Position pos;
	private final int height;
	private final StorageScreenBase<?> screen;

	public TankInventoryPart(int upgradeSlot, TankUpgradeContainer container, Position pos, int height, StorageScreenBase<?> screen) {
		super(upgradeSlot, container);
		this.pos = pos;
		this.height = height;
		this.screen = screen;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y(), GuiHelper.BAR_BACKGROUND_TOP);
		int yOffset = 18;
		for (int i = 0; i < (height - 36) / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + yOffset, GuiHelper.BAR_BACKGROUND_MIDDLE);
			yOffset += 18;
		}
		GuiHelper.blit(guiGraphics, getTankLeft(), pos.y() + yOffset, GuiHelper.BAR_BACKGROUND_BOTTOM);

		renderFluid(guiGraphics);

		yOffset = 0;
		for (int i = 0; i < height / 18; i++) {
			GuiHelper.blit(guiGraphics, getTankLeft() + 1, pos.y() + yOffset, OVERLAY);
			yOffset += 18;
		}
	}

	private int getTankLeft() {
		return pos.x() + 9;
	}

	@Override
	public boolean handleMouseReleased(double mouseX, double mouseY, int button) {
		if (mouseX < ((AbstractContainerScreenAccessor) screen).getGuiLeft() + getTankLeft() || mouseX >= ((AbstractContainerScreenAccessor) screen).getGuiLeft() + getTankLeft() + 18 ||
				mouseY < ((AbstractContainerScreenAccessor) screen).getGuiTop() + pos.y() || mouseY >= ((AbstractContainerScreenAccessor) screen).getGuiTop() + pos.y() + height) {
			return false;
		}

		ItemStack cursorStack = screen.getMenu().getCarried();
		if (cursorStack.getCount() > 1 || !FluidHelper.isFluidStorage(cursorStack)) {
			return false;
		}

		PacketHelper.sendToServer(new TankClickPacket(upgradeSlot));
		return true;
	}

	@Override
	public void renderErrorOverlay(GuiGraphics guiGraphics) {
		screen.renderOverlay(guiGraphics, StorageScreenBase.ERROR_SLOT_COLOR, getTankLeft() + 1, pos.y() + 1, 16, height - 2);
	}

	@Override
	public void renderTooltip(StorageScreenBase<?> screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
		FluidStack contents = container.getContents();
		long capacity = container.getTankCapacity();
		if (contents.isEmpty()) {
			contents = FluidStack.EMPTY;
		}

		int screenX = ((AbstractContainerScreenAccessor) screen).getGuiLeft() + pos.x() + 10;
		int screenY = ((AbstractContainerScreenAccessor) screen).getGuiTop() + pos.y() + 1;
		if (mouseX >= screenX && mouseX < screenX + 16 && mouseY >= screenY && mouseY < screenY + height - 2) {
			List<Component> tooltip = new ArrayList<>();
			if (!contents.isEmpty()) {
				tooltip.add(contents.getDisplayName());
			}
			tooltip.add(getContentsTooltip(contents, capacity));
			guiGraphics.renderTooltip(((ScreenAccessor) screen).getFont(), tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private MutableComponent getContentsTooltip(FluidStack contents, long capacity) {
		if (contents.getFluid().defaultFluidState().is(ModFluids.EXPERIENCE_TAG)) {
			double contentsLevels = XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(contents.getAmount()));
			double tankCapacityLevels = XpHelper.getLevelsForExperience((int) XpHelper.liquidToExperience(capacity));

			return Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tank.xp_contents_tooltip"), String.format("%.1f", contentsLevels), String.format("%.1f", tankCapacityLevels));
		}
		return Component.translatable(TranslationHelper.INSTANCE.translUpgradeKey("tank.contents_tooltip"), String.format("%,d", FluidHelper.toBuckets(contents.getAmount())), String.format("%,d", FluidHelper.toBuckets(capacity)));
	}

	private void renderFluid(GuiGraphics guiGraphics) {
		FluidStack contents = container.getContents();
		long capacity = container.getTankCapacity();
		if (contents.isEmpty()) {
			return;
		}

		Fluid fluid = contents.getFluid();
		long fill = contents.getAmount();
		int displayLevel = (int) ((height - 2) * ((float) fill / capacity));
		FluidVariant fluidVariant = FluidVariant.of(fluid);
		TextureAtlasSprite still = FluidVariantRendering.getSprite(fluidVariant);
		GuiHelper.renderTiledFluidTextureAtlas(guiGraphics, still, FluidVariantRendering.getColor(fluidVariant), pos.x() + 10, pos.y() + 1 + height - 2 - displayLevel, displayLevel);
	}

}
