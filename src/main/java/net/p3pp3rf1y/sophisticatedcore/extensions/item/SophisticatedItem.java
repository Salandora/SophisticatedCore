package net.p3pp3rf1y.sophisticatedcore.extensions.item;

import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import javax.annotation.Nullable;

public interface SophisticatedItem {
	default float sophisticatedCore_getXpRepairRatio(ItemStack stack) {
		return 1.0F;
	}

	default boolean sophisticatedCore_onDroppedByPlayer(ItemStack stack, Player player) {
		return true;
	}

	@OverrideOnly
	default int sophisticatedCore_getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
		Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
		return burnTime != null ? burnTime : 0;
	}

    default InteractionResult sophisticatedCore_onItemUseFirst(ItemStack stack, UseOnContext context) {
        return InteractionResult.PASS;
    }

	default @Nullable FoodProperties sophisticatedCore_getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
		return stack.get(DataComponents.FOOD);
	}

	@OverrideOnly
	default int sophisticatedCore_getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
		return stack.getEnchantments().getLevel(enchantment);
	}

	default boolean sophisticatedCore_shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !oldStack.equals(newStack);
	}

	default boolean sophisticatedCore_makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
		return stack.getItem() instanceof ArmorItem && ((ArmorItem)stack.getItem()).getMaterial() == ArmorMaterials.GOLD;
	}
}
