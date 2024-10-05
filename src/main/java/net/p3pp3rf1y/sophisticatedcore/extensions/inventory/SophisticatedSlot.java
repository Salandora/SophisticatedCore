package net.p3pp3rf1y.sophisticatedcore.extensions.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

public interface SophisticatedSlot {
    default boolean isSameInventory(Slot other) {
        return ((Slot)this).container == other.container;
    }

    default ItemVariant getItemVariant() {
        return ItemVariant.of(((Slot)this).getItem());
    }

    default Slot sophisticatedcore$setBackground(ResourceLocation atlas, ResourceLocation sprite) {
        throw new RuntimeException("Should have been overriden by mixin.");
    }
}
