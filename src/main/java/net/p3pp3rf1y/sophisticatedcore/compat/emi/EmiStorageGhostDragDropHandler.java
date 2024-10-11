package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.List;

public class EmiStorageGhostDragDropHandler<T extends StorageScreenBase<?>> extends EmiDragDropHandler.SlotBased<T> {
    public EmiStorageGhostDragDropHandler() {
        super(
            screen -> {
                List<Slot> slots = new ArrayList<>();
                screen.getMenu().getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
                    if (s instanceof IFilterSlot) {
                        slots.add(s);
                    }
                }));
                return slots;
            },
            (screen, slot, ingredient) -> {
                List<EmiStack> stacks = ingredient.getEmiStacks();
                if (stacks.size() != 1)
                    return;

                ItemStack stack = stacks.get(0).getItemStack();
                if (slot.mayPlace(stack)) {
                    PacketHelper.sendToServer(new SetGhostSlotMessage(stack, slot.index));
                }
            }
        );
    }
}
