package net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

@Mixin(ShapedRecipe.class)
public interface ShapedRecipeAccessor {
	@Accessor("pattern")
	ShapedRecipePattern getPattern();

	@Accessor("result")
	ItemStack getResult();
}
