package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetGhostSlotMessage;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import java.util.ArrayList;
import java.util.List;

public class StorageGhostIngredientHandler<S extends StorageScreenBase<?>> implements IGhostIngredientHandler<S> {

	@Override
	public <I> List<Target<I>> getTargets(S gui, I ingredient, boolean doStart) {
		return null;
	}

	@Override
	public <I> List<Target<I>> getTargetsTyped(S gui, ITypedIngredient<I> ingredient, boolean doStart) {
		List<Target<I>> targets = new ArrayList<>();
		if (!(ingredient.getIngredient() instanceof ItemStack ghostStack)) {
			return targets;
		}
		StorageContainerMenuBase<?> container = gui.getMenu();
		container.getOpenContainer().ifPresent(c -> c.getSlots().forEach(s -> {
			if (s instanceof IFilterSlot && s.mayPlace(ghostStack)) {
				targets.add(new Target<>() {
					@Override
					public Rect2i getArea() {
						return new Rect2i(gui.getGuiLeft() + s.x, gui.getGuiTop() + s.y, 17, 17);
					}

					@Override
					public void accept(I i) {
						PacketHandler.sendToServer(new SetGhostSlotMessage(ghostStack, s.index));
					}
				});
			}
		}));
		return targets;
	}

	@Override
	public void onComplete() {
		//noop
	}
}
