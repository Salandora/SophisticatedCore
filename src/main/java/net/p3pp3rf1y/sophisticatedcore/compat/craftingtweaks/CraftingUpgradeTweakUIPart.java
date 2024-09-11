package net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks;

import net.blay09.mods.craftingtweaks.CraftingTweaksProviderManager;
import net.blay09.mods.craftingtweaks.api.CraftingTweaksClientAPI;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.ScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.ICraftingUIPart;

import java.util.ArrayList;
import java.util.List;

public class CraftingUpgradeTweakUIPart implements ICraftingUIPart {
    @Environment(EnvType.CLIENT)
	private StorageScreenBase<?> storageScreen;

	private final List<Button> buttons = new ArrayList<>();

	public static void register() {
		StorageScreenBase.setCraftingUIPart(new CraftingUpgradeTweakUIPart());
	}

    @Environment(EnvType.CLIENT)
	private void addButton(Button button) {
		buttons.add(button);
		((ScreenAccessor) storageScreen).callAddRenderableWidget(button);
	}

	@Override
    @Environment(EnvType.CLIENT)
	public void onCraftingSlotsHidden() {
		if (buttons.isEmpty()) {
			return;
		}

		buttons.forEach(storageScreen.children()::remove);
		buttons.forEach(storageScreen.renderables::remove);
		buttons.clear();
	}

	@Override
	public int getWidth() {
		return 18;
	}

	@Override
    @Environment(EnvType.CLIENT)
	public void setStorageScreen(StorageScreenBase<?> screen) {
		storageScreen = screen;
	}

	@Override
	public void onCraftingSlotsDisplayed(List<Slot> slots) {
		if (slots.isEmpty() || storageScreen == null) {
			return;
		}
		Slot firstSlot = slots.get(0);
		CraftingTweaksProviderManager.getDefaultCraftingGrid(storageScreen.getMenu()).ifPresent(craftingGrid -> {
			addButton(CraftingTweaksClientAPI.createRotateButtonRelative(craftingGrid, storageScreen, getButtonX(firstSlot), getButtonY(firstSlot, 0)));
			addButton(CraftingTweaksClientAPI.createBalanceButtonRelative(craftingGrid, storageScreen, getButtonX(firstSlot), getButtonY(firstSlot, 1)));
			addButton(CraftingTweaksClientAPI.createClearButtonRelative(craftingGrid, storageScreen, getButtonX(firstSlot), getButtonY(firstSlot, 2)));
		});
	}

    @Environment(EnvType.CLIENT)
	private int getButtonX(Slot firstSlot) {
		return firstSlot.x - 19;
	}

    @Environment(EnvType.CLIENT)
	private int getButtonY(Slot firstSlot, int index) {
		return firstSlot.y + 18 * index;
	}
}
