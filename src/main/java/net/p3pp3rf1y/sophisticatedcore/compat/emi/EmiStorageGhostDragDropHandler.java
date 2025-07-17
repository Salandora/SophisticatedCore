package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import com.github.salandora.sophisticatedlibrary.fluid.FluidStack;
import com.google.common.collect.Maps;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.runtime.EmiDrawContext;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pump.PumpUpgradeTab;
import net.p3pp3rf1y.sophisticatedcore.util.CapabilityHelper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class EmiStorageGhostDragDropHandler<T extends StorageScreenBase<?>> implements EmiDragDropHandler<T> {
    private final BiFunction<T, EmiIngredient, Map<Bounds, Consumer<EmiIngredient>>> bounds;

    public EmiStorageGhostDragDropHandler() {
        this.bounds = (screen, ingredient) -> {
            Map<Bounds, Consumer<EmiIngredient>> map = Maps.newHashMap();
            if (ingredient.getEmiStacks().isEmpty()) {
                return map;
            }

            ItemStack ghostStack = ingredient.getEmiStacks().getFirst().getItemStack();
            if (!ghostStack.isEmpty()) {
                FluidStack fluidStack = CapabilityHelper.getFromFluidHandler(ghostStack,
                        fluidHandler -> Optional.ofNullable(StorageUtil.findExtractableContent(fluidHandler, null)).map(FluidStack::new).orElse(FluidStack.EMPTY), FluidStack.EMPTY);

                if (!fluidStack.isEmpty()) {
                    screen.getUpgradeSettingsControl().getOpenTab().filter(tab -> tab instanceof PumpUpgradeTab.Advanced).map(PumpUpgradeTab.Advanced.class::cast).ifPresent(pumpUpgradeTab -> {
                        addFluidTargets(pumpUpgradeTab, fluidStack, map);
                    });
                }
                screen.getMenu().getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
                    if (s instanceof IFilterSlot && s.mayPlace(ghostStack)) {
                        map.put(
                                new Bounds(screen.getLeftX() + s.x, screen.getTopY() + s.y, 18, 18),
                                (i) -> PacketDistributor.sendToServer(new EmiSetGhostSlotPayload(ghostStack, s.index)));
                    }
                }));
            } else if (ingredient instanceof FluidEmiStack fluidStack) {
                screen.getUpgradeSettingsControl().getOpenTab().filter(tab -> tab instanceof PumpUpgradeTab.Advanced).map(PumpUpgradeTab.Advanced.class::cast).ifPresent(pumpUpgradeTab -> {
                    Fluid fluid = fluidStack.getKeyOfType(Fluid.class);
                    addFluidTargets(pumpUpgradeTab, new FluidStack(fluid, 1), map);
                });
            }
            return map;
        };
    }

    @Override
    public boolean dropStack(T screen, EmiIngredient stack, int x, int y) {
        Map<Bounds, Consumer<EmiIngredient>> bounds = this.bounds.apply(screen, stack);
        for (Bounds b : bounds.keySet()) {
            if (b.contains(x, y)) {
                bounds.get(b).accept(stack);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(T screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        EmiDrawContext context = EmiDrawContext.wrap(draw);
        for (Bounds b : this.bounds.apply(screen, dragged).keySet()) {
            context.fill(b.x(), b.y(), b.width(), b.height(), 0x8822BB33);
        }
    }

    private void addFluidTargets(PumpUpgradeTab.Advanced pumpUpgradeTab, FluidStack ghostFluid, Map<Bounds, Consumer<EmiIngredient>> map) {
        List<Position> slotTopLeftPositions = pumpUpgradeTab.getFluidFilterControl().getSlotTopLeftPositions();
        AtomicInteger slot = new AtomicInteger();
        for (slot.set(0); slot.get() < slotTopLeftPositions.size(); slot.incrementAndGet()) {
            Position position = slotTopLeftPositions.get(slot.get());
            final int slotIndex = slot.get();
            map.put(
                    new Bounds(position.x(), position.y(), 17, 17),
                    ingredient -> pumpUpgradeTab.getFluidFilterControl().setFluid(slotIndex, ghostFluid));
        }
    }
}
