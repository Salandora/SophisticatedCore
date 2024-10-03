package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import me.shedaniel.rei.api.common.display.SimpleGridMenuDisplay;
import me.shedaniel.rei.api.common.transfer.info.MenuInfoContext;
import me.shedaniel.rei.api.common.transfer.info.MenuSerializationContext;
import me.shedaniel.rei.api.common.transfer.info.simple.SimpleGridMenuInfo;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.stream.IntStream;

public class ReiGridMenuInfo<T extends StorageContainerMenuBase<?>, D extends SimpleGridMenuDisplay> implements SimpleGridMenuInfo<T, D> {
    private final D display;

    public ReiGridMenuInfo(D display) {
        this.display = display;
    }

    @Override
    public D getDisplay() {
        return this.display;
    }

    @Override
    public int getCraftingWidth(T menu) {
        var potentialCraftingContainer = menu.getOpenOrFirstCraftingContainer();
        if (potentialCraftingContainer.isEmpty()) {
            return 0;
        }

        return potentialCraftingContainer.map(c -> ((CraftingContainer)c.getCraftMatrix()).getWidth()).orElse(0);
    }

    @Override
    public int getCraftingHeight(T menu) {
        var potentialCraftingContainer = menu.getOpenOrFirstCraftingContainer();
        if (potentialCraftingContainer.isEmpty()) {
            return 0;
        }

        return potentialCraftingContainer.map(c -> ((CraftingContainer)c.getCraftMatrix()).getHeight()).orElse(0);
    }

    @Override
    public void clearInputSlots(T menu) {
        var potentialCraftingContainer = menu.getOpenOrFirstCraftingContainer();
        if (potentialCraftingContainer.isEmpty()) {
            return;
        }

        potentialCraftingContainer.get().getCraftMatrix().clearContent();
    }

    @Override
    public Iterable<SlotAccessor> getInventorySlots(MenuInfoContext<T, ?, D> context) {
        return context.getMenu().realInventorySlots.stream().map(SlotAccessor::fromSlot).toList();
    }

    @Override
    public IntStream getInputStackSlotIds(MenuInfoContext<T, ?, D> context) {
        var potentialCraftingContainer = context.getMenu().getOpenOrFirstCraftingContainer();
        if (potentialCraftingContainer.isEmpty()) {
            return IntStream.empty();
        }

        return potentialCraftingContainer.map(c -> c.getRecipeSlots().stream().mapToInt(s -> s == null ? -1 : s.index)).get();
    }

    @Override
    public int getCraftingResultSlotIndex(T menu) {
        var potentialCraftingContainer = menu.getOpenOrFirstCraftingContainer();
        if (potentialCraftingContainer.isEmpty()) {
            return 0;
        }

        return potentialCraftingContainer.map(c -> c.getSlots().get(c.getSlots().size() - 1).index).orElse(-1);
    }

    @Override
    public void markDirty(MenuInfoContext<T, ? extends ServerPlayer, D> context) {
        SimpleGridMenuInfo.super.markDirty(context);
        context.getMenu().broadcastChanges();
    }

    @Override
    public CompoundTag save(MenuSerializationContext<T, ?, D> context, D display) {
        // This is a bit hacky to do here, but it prevents us from implementing a custom TransferHandler just to set the tab id
        context.getMenu().getOpenOrFirstCraftingContainer().ifPresent(openOrFirstCraftingContainer -> {
            if (!openOrFirstCraftingContainer.isOpen()) {
				T container = context.getMenu();
				container.getOpenContainer().ifPresent(c -> {
                    c.setIsOpen(false);
					container.setOpenTabId(-1);
                });
                openOrFirstCraftingContainer.setIsOpen(true);
				container.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
            }
        });
        // For some reason with REI this needs to be called after the tab id was set
        Minecraft.getInstance().setScreen(Minecraft.getInstance().screen);

        return SimpleGridMenuInfo.super.save(context, display);
    }
}
