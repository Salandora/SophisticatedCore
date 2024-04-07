package net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int getGuiLeft();

	@Accessor("topPos")
	int getGuiTop();

	@Accessor
	Slot getClickedSlot();

	@Accessor
	boolean getDoubleclick();

	@Accessor
	ItemStack getDraggingItem();

	@Nullable
	@Accessor
	Slot getHoveredSlot();
	@Accessor
	void setHoveredSlot(@Nullable Slot slot);

	@Accessor
	boolean getIsSplittingStack();

	@Accessor
	ItemStack getLastQuickMoved();

	@Accessor
	int getQuickCraftingType();
	@Accessor
	int getQuickCraftingRemainder();
	@Accessor
	void setQuickCraftingRemainder(int remainder);

	@Invoker
	boolean callIsHovering(Slot slot, double pointX, double pointY);
}
