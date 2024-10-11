package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.List;

public class EmiSettingsGhostDragDropHandler<T extends SettingsScreen> extends EmiDragDropHandler.SlotBased<T> {
    public EmiSettingsGhostDragDropHandler() {
        super(
            screen -> {
                List<Slot> slots = new ArrayList<>();
                screen.getSettingsTabControl().getOpenTab().ifPresent(tab -> {
                    if (tab instanceof MemorySettingsTab) {
                        screen.getMenu().getStorageInventorySlots().forEach(s -> {
                            if (s.getItem().isEmpty()) {
                                slots.add(s);
                            }
                        });
                    }
                });
                return slots;
            },
            (screen, slot, ingredient) -> {
                List<EmiStack> stacks = ingredient.getEmiStacks();
                if (stacks.size() != 1)
                    return;

                ItemStack stack = stacks.get(0).getItemStack();
                if (slot.mayPlace(stack)) {
					PacketHelper.sendToServer(new SetMemorySlotMessage(stack, slot.index));
                }
            }
        );
    }
}
