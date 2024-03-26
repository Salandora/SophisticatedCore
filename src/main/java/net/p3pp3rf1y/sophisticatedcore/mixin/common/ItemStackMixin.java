package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import io.github.fabricators_of_create.porting_lib.extensions.ItemStackExtensions;
import io.github.fabricators_of_create.porting_lib.util.ToolAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.extensions.item.SophisticatedItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements SophisticatedItemStack, ItemStackExtensions {
	@Shadow
	public abstract Item getItem();

	@Unique
	@Override
	public boolean canPerformAction(ToolAction toolAction) {
		return getItem().canPerformAction((ItemStack) (Object) this, toolAction);
	}
}
