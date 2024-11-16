package net.p3pp3rf1y.sophisticatedcore.compat.litematica.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaCompat;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MaterialListUtils.class)
public class MaterialListUtilsMixin {
	@Inject(method="getInventoryItemCounts", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"))
	private static void sophisticatedCore$injectAdditionalChecks(Container inv, CallbackInfoReturnable<Object2IntOpenHashMap<ItemType>> cir,
			@Local Object2IntOpenHashMap<ItemType> map, @Local ItemStack stack) {
		LitematicaCompat.getWrapper(stack).ifPresent(litematicaWrapper -> sophisticatedCore$processItemStack(map, litematicaWrapper.wrapper()));
	}

	@Inject(method="getStoredItemCounts", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;addTo(Ljava/lang/Object;I)I"))
	private static void sophisticatedCore$injectAdditionalChecks(ItemStack stackShulkerBox, CallbackInfoReturnable<Object2IntOpenHashMap<ItemType>> cir,
			@Local Object2IntOpenHashMap<ItemType> map, @Local(ordinal = 1) ItemStack boxStack) {
		LitematicaCompat.getWrapper(boxStack).ifPresent(litematicaWrapper -> sophisticatedCore$processItemStack(map, litematicaWrapper.wrapper()));
	}

	@Unique
	private static void sophisticatedCore$processItemStack(Object2IntOpenHashMap<ItemType> map, IStorageWrapper wrapper) {
		// Don't trust the contents nbt as it might have been updated through our custom packet
		wrapper.onContentsNbtUpdated();

		InventoryHandler invHandler = wrapper.getInventoryHandler();
		int slots = invHandler.getSlotCount();
		for (int slot = 0; slot < slots; ++slot) {
			ItemStack invStack = invHandler.getStackInSlot(slot);
			if (!invStack.isEmpty()) {
				map.addTo(new ItemType(invStack, true, false), invStack.getCount());
				LitematicaCompat.getWrapper(invStack).ifPresent(litematicaWrapper -> sophisticatedCore$processItemStack(map, litematicaWrapper.wrapper()));
			}
		}
	}
}
