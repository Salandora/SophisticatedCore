package net.p3pp3rf1y.sophisticatedcore.extensions.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

public interface SophisticatedAbstractContainerScreen {
    int slotColor = -2130706433;
    default int sophisticatedCore_getSlotColor(int slotId) {
        return slotColor;
    }

    default int sophisticatedCore_getXSize() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    default int sophisticatedCore_getGuiLeft() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    default int sophisticatedCore_getGuiTop() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    @Nullable
    default Slot sophisticatedCore_getSlotUnderMouse() {
        throw new RuntimeException("Should have been overriden by mixin.");
    }

    default void sophisticatedCore_superRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) { throw new RuntimeException("Should have been overriden by mixin."); }
}
