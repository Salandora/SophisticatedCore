package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;

import java.util.ArrayList;
import java.util.List;

public class SettingsGhostIngredientHandler<S extends SettingsScreen> implements IGhostIngredientHandler<S> {
	private S targetedScreen;

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

		gui.startMouseDragHandledByOther();
		targetedScreen = gui;

		gui.getSettingsTabControl().getOpenTab().ifPresent(tab -> {
			if (tab instanceof MemorySettingsTab) {
				gui.getMenu().getStorageInventorySlots().forEach(s -> {
					if (s.getItem().isEmpty()) {
						targets.add(new Target<>() {
							@Override
							public Rect2i getArea() {
								return new Rect2i(gui.getGuiLeft() + s.x, gui.getGuiTop() + s.y, 17, 17);
							}

							@Override
							public void accept(I i) {
								PacketHandler.sendToServer(new SetMemorySlotMessage(ghostStack, s.index));
							}
						});
					}
				});
			}
		});
		return targets;
	}

	@Override
	public void onComplete() {
		targetedScreen.stopMouseDragHandledByOther();
	}
}
