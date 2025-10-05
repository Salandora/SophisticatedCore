package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import com.github.salandora.sophisticatedlibrary.network.api.v1.PacketDistributor;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.registry.EmiRecipeFiller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmiGridMenuInfo<T extends StorageContainerMenuBase<?>> implements StandardRecipeHandler<T> {
	private final RecipeType<? extends Recipe<?>> recipeType;

	public static <T extends StorageContainerMenuBase<?>> EmiGridMenuInfo<T> crafting() {
		return new EmiGridMenuInfo<>(RecipeType.CRAFTING);
	}
	public static <T extends StorageContainerMenuBase<?>> EmiGridMenuInfo<T> smithing() {
		return new EmiGridMenuInfo<>(RecipeType.SMITHING);
	}
	public static <T extends StorageContainerMenuBase<?>> EmiGridMenuInfo<T> stonecutting() {
		return new EmiGridMenuInfo<>(RecipeType.STONECUTTING);
	}

	private EmiGridMenuInfo(RecipeType<? extends Recipe<?>> recipeType) {
		this.recipeType = recipeType;
	}

    @Override
    public List<Slot> getInputSources(T handler) {
        List<Slot> slots = new ArrayList<>(handler.realInventorySlots);
        handler.getOpenOrFirstCraftingContainer(recipeType).ifPresent(c -> slots.addAll(c.getRecipeSlots()));
        return slots;
    }

    @Override
    public List<Slot> getCraftingSlots(T handler) {
        List<Slot> slots = new ArrayList<>();
        handler.getOpenOrFirstCraftingContainer(recipeType).ifPresent(c -> slots.addAll(c.getRecipeSlots()));
        return slots;
    }

    @Override
    public @Nullable Slot getOutputSlot(T handler) {
        return handler.getOpenOrFirstCraftingContainer(recipeType).map(c -> c.getSlots().get(c.getSlots().size() - 1)).orElse(null);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return VanillaEmiRecipeCategories.CRAFTING.equals(recipe.getCategory()) && recipe.supportsRecipeTree();
    }

	@Override
	public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
		return context.getScreenHandler().getOpenOrFirstCraftingContainer(recipeType).isPresent() && StandardRecipeHandler.super.canCraft(recipe, context);
	}

	@Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
		T container = context.getScreenHandler();
		Optional<? extends UpgradeContainerBase<?, ?>> potentialCraftingContainer = container.getOpenOrFirstCraftingContainer(recipeType);

        List<ItemStack> stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), context.getAmount());
        if (stacks != null) {
			// Can be suppressed cause emi does a canCraft check before calling the craft method, and we test for a crafting container there
			//noinspection OptionalGetWithoutIsPresent
			UpgradeContainerBase<?, ?> openOrFirstCraftingContainer = potentialCraftingContainer.get();
			if (!openOrFirstCraftingContainer.isOpen()) {
				container.getOpenContainer().ifPresent(c -> {
					c.setIsOpen(false);
					container.setOpenTabId(-1);
				});
				openOrFirstCraftingContainer.setIsOpen(true);
				container.setOpenTabId(openOrFirstCraftingContainer.getUpgradeContainerId());
			}

            Minecraft.getInstance().setScreen(context.getScreen());
            if (!EmiClient.onServer) {
                return EmiRecipeFiller.clientFill(this, recipe, context.getScreen(), stacks, context.getDestination());
            } else {
                sendFillRecipe(this, context.getScreen(), switch(context.getDestination()) {
                    case NONE -> 0;
                    case CURSOR -> 1;
                    case INVENTORY -> 2;
                }, stacks, recipe);
            }
            return true;
        }
        return false;
    }

    public static <T extends AbstractContainerMenu> void sendFillRecipe(StandardRecipeHandler<T> handler, AbstractContainerScreen<T> screen,
                                                                        int action, List<ItemStack> stacks, EmiRecipe recipe) {
        T screenHandler = screen.getMenu();
        List<Slot> crafting = handler.getCraftingSlots(recipe, screenHandler);
        Slot output = handler.getOutputSlot(screenHandler);
        PacketDistributor.sendToServer(
				new EmiFillRecipePacket(
						screenHandler.containerId,
						action,
						handler.getInputSources(screenHandler).stream().map(s -> s == null ? -1 : s.index).toList(),
						crafting.stream().map(s -> s == null ? -1 : s.index).toList(),
						output == null ? -1 : output.index,
						stacks
				)
		);
    }
}
