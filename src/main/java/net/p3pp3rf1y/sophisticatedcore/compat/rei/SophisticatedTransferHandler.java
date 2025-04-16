package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.ArrayList;
import java.util.List;

public class SophisticatedTransferHandler<C extends StorageContainerMenuBase<?>, D extends Display> implements SimpleTransferHandler {
	public static <C extends StorageContainerMenuBase<?>> TransferHandler crafting(Class<? extends C> containerClass) {
		return new SophisticatedTransferHandler<>(containerClass, BuiltinPlugin.CRAFTING, RecipeType.CRAFTING);
	}

	private final Class<? extends C> containerClass;
	private final CategoryIdentifier<D> categoryIdentifier;
	private final RecipeType<? extends Recipe<?>> recipeType;

	public SophisticatedTransferHandler(Class<? extends C> containerClass, CategoryIdentifier<D> categoryIdentifier, RecipeType<? extends Recipe<?>> recipeType) {
		this.containerClass = containerClass;
		this.categoryIdentifier = categoryIdentifier;
		this.recipeType = recipeType;
	}

	@Override
	public ApplicabilityResult checkApplicable(Context context) {
		if (!containerClass.isInstance(context.getMenu())
				|| !categoryIdentifier.equals(context.getDisplay().getCategoryIdentifier())
				|| context.getContainerScreen() == null) {
			return ApplicabilityResult.createNotApplicable();
		}

		if (context.isActuallyCrafting()) {
			C storageContainerMenuBase = (C) context.getMenu();
			storageContainerMenuBase.getOpenOrFirstCraftingContainer(recipeType).ifPresent(openOrFirstCraftingContainer -> {
				if (!openOrFirstCraftingContainer.isOpen()) {
					storageContainerMenuBase.getOpenContainer().ifPresent(c -> {
						c.setIsOpen(false);
						storageContainerMenuBase.setOpenTabId(-1);
					});
					openOrFirstCraftingContainer.setIsOpen(true);
					storageContainerMenuBase.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
				}
			});
		}

		return ApplicabilityResult.createApplicable();
	}

	@Override
	public Iterable<SlotAccessor> getInputSlots(Context context) {
		StorageContainerMenuBase<?> storageContainerMenuBase = (StorageContainerMenuBase<?>) context.getMenu();
		return storageContainerMenuBase.getOpenOrFirstCraftingContainer(recipeType)
				.map(c -> c.getRecipeSlots().stream().map(SophisticatedSlotAccessor::fromSlot).toList())
				.orElse(List.of());
	}

	@Override
	public Iterable<SlotAccessor> getInventorySlots(Context context) {
		StorageContainerMenuBase<?> storageContainerMenuBase = (StorageContainerMenuBase<?>) context.getMenu();
		return storageContainerMenuBase.realInventorySlots.stream().map(SophisticatedSlotAccessor::fromSlot).toList();
	}
}
