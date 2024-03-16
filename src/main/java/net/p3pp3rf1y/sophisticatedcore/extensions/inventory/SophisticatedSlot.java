package net.p3pp3rf1y.sophisticatedcore.extensions.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.inventory.Slot;

public interface SophisticatedSlot {
    default boolean isSameInventory(Slot other) {
        return ((Slot)this).container == other.container;
    }

    default ItemVariant getItemVariant() {
        return ItemVariant.of(((Slot)this).getItem());
    }

    // Slot setBackground(ResourceLocation atlas, ResourceLocation sprite);
}
