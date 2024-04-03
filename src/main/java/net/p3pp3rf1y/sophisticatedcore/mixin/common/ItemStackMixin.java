package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.extensions.item.SophisticatedItemStack;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements SophisticatedItemStack {
}
