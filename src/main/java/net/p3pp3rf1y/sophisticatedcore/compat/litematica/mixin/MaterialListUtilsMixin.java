package net.p3pp3rf1y.sophisticatedcore.compat.litematica.mixin;

import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.CapabilityWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

@Mixin(MaterialListUtils.class)
public class MaterialListUtilsMixin {
	@Inject(method = "getInventoryItemCounts", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;addTo(Ljava/lang/Object;I)I", ordinal = 0, shift = At.Shift.AFTER, remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
	private static void sophisticatedCore$injectStorageBlockBase(Container inv, CallbackInfoReturnable<Object2IntOpenHashMap<ItemType>> cir, Object2IntOpenHashMap<ItemType> map, int slots, int slot, ItemStack stack) {
		CapabilityWrapper.get(stack).ifPresent(wrapper -> sophisticatedCore$processItemStack(map, wrapper));
	}

	@Unique
	private static void sophisticatedCore$processItemStack(Object2IntOpenHashMap<ItemType> map, IStorageWrapper wrapper) {
		InventoryHandler invHandler = wrapper.getInventoryHandler();
		int slots = invHandler.getSlotCount();
		for (int slot = 0; slot < slots; ++slot) {
			ItemStack invStack = invHandler.getStackInSlot(slot);
			if (!invStack.isEmpty()) {
				map.addTo(new ItemType(invStack, true, false), invStack.getCount());
				CapabilityWrapper.get(invStack).ifPresent(w -> sophisticatedCore$processItemStack(map, w));
			}
		}
	}
}
