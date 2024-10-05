package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.client.renderer.Rect2i;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.compat.common.SetMemorySlotMessage;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsTab;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;

import java.util.ArrayList;
import java.util.List;

public class SettingsGhostIngredientHandler<S extends SettingsScreen> implements IGhostIngredientHandler<S> {
	private S targetedScreen;

	@Override
	public <I> List<Target<I>> getTargetsTyped(S gui, ITypedIngredient<I> ingredient, boolean doStart) {
		List<Target<I>> targets = new ArrayList<>();
		if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
			return targets;
		}

		gui.startMouseDragHandledByOther();
		targetedScreen = gui;

		gui.getSettingsTabControl().getOpenTab().ifPresent(tab -> {
			if (tab instanceof MemorySettingsTab) {
				ingredient.getItemStack().ifPresent(ghostStack ->
						gui.getMenu().getStorageInventorySlots().forEach(s -> {
							if (s.getItem().isEmpty()) {
								targets.add(new Target<>() {
									@Override
									public Rect2i getArea() {
										return new Rect2i(((AbstractContainerScreenAccessor) gui).getGuiLeft() + s.x, ((AbstractContainerScreenAccessor) gui).getGuiTop() + s.y, 17, 17);
									}

									@Override
									public void accept(I i) {
										PacketHelper.sendToServer(new SetMemorySlotMessage(ghostStack, s.index));
									}
								});
							}
						})
				);
			}
		});
		return targets;
	}

	@Override
	public void onComplete() {
		targetedScreen.stopMouseDragHandledByOther();
	}
}
