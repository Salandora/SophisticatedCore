package net.p3pp3rf1y.porting_lib.loot.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import io.github.fabricators_of_create.porting_lib.extensions.ItemStackExtensions;
import io.github.fabricators_of_create.porting_lib.util.ToolAction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackExtensions {
	@Shadow
	public abstract Item getItem();

	@Unique
	@Override
	public boolean canPerformAction(ToolAction toolAction) {
		return getItem().canPerformAction((ItemStack) (Object) this, toolAction);
	}
}
