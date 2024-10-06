package net.p3pp3rf1y.sophisticatedcore.compat.chipped;

import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import earth.terrarium.chipped.common.recipes.ChippedRecipe;

import java.util.List;
import java.util.function.Supplier;

public class BlockTransformationUpgradeItem extends UpgradeItemBase<BlockTransformationUpgradeWrapper> {
	private static final UpgradeType<BlockTransformationUpgradeWrapper> TYPE = new UpgradeType<>(BlockTransformationUpgradeWrapper::new);
	private final Supplier<RecipeType<ChippedRecipe>> getRecipeType;
	public BlockTransformationUpgradeItem(Supplier<RecipeType<ChippedRecipe>> getRecipeType, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(upgradeTypeLimitConfig);
		this.getRecipeType = getRecipeType;
	}

	@Override
	public UpgradeType<BlockTransformationUpgradeWrapper> getType() {
		return TYPE;
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return List.of();
	}

	public RecipeType<ChippedRecipe> getRecipeType() {
		return getRecipeType.get();
	}

}
