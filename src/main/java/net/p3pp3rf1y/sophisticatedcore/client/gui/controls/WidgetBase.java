package net.p3pp3rf1y.sophisticatedcore.client.gui.controls;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public abstract class WidgetBase extends GuiComponent implements Widget, GuiEventListener, NarratableEntry {
	protected final int x;

	protected final int y;
	protected final Minecraft minecraft;
	protected final Font font;
	private int height;
	private int width;
	protected boolean isHovered;
	private boolean visible = true;

	protected WidgetBase(Position position, Dimension dimension) {
		x = position.x();
		y = position.y();
		width = dimension.width();
		height = dimension.height();
		minecraft = Minecraft.getInstance();
		font = minecraft.font;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}

		isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
		RenderSystem.enableDepthTest();
		renderBg(matrixStack, minecraft, mouseX, mouseY);
		renderWidget(matrixStack, mouseX, mouseY, partialTicks);
	}

	@Override
	public NarrationPriority narrationPriority() {
		return isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
	}

	protected abstract void renderBg(PoseStack matrixStack, Minecraft minecraft, int mouseX, int mouseY);

	public abstract void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks);

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	protected void updateDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	protected int getCenteredX(int elementWidth) {
		return (getWidth() - elementWidth) / 2;
	}

	public void renderTooltip(Screen screen, PoseStack poseStack, int mouseX, int mouseY) {
		//noop
	}

	public void setFocus(boolean focused) {
		//noop
	}
}
