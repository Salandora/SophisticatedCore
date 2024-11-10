package net.p3pp3rf1y.sophisticatedcore.extensions.client.gui.screens.inventory;

import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

public interface SophisticatedAbstractContainerScreen {
    int slotColor = -2130706433;
    default int sophisticatedCore$getSlotColor(int slotId) {
        return slotColor;
    }

    default int getXSize() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    default int getGuiLeft() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    default int getGuiTop() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    @Nullable
    default Slot getSlotUnderMouse() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }
}
