package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.util.ItemBase;

import java.util.List;
import javax.annotation.Nullable;

public abstract class UpgradeItemBase<T extends IUpgradeWrapper> extends ItemBase implements IUpgradeItem<T> {

    private final IUpgradeCountLimitConfig upgradeTypeLimitConfig;

    protected UpgradeItemBase(CreativeModeTab itemGroup, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(new Properties(), itemGroup);
        this.upgradeTypeLimitConfig = upgradeTypeLimitConfig;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.addAll(TranslationHelper.INSTANCE.getTranslatedLines(stack.getItem().getDescriptionId() + TranslationHelper.TOOLTIP_SUFFIX, null, ChatFormatting.DARK_GRAY));
    }

    @Override
    public int getUpgradesPerStorage(String storageType) {
        return upgradeTypeLimitConfig.getMaxUpgradesPerStorage(storageType, Registry.ITEM.getKey(this));
    }

    @Override
    public int getUpgradesInGroupPerStorage(String storageType) {
        if (getUpgradeGroup().isSolo()) {
            return Integer.MAX_VALUE;
        }

        return upgradeTypeLimitConfig.getMaxUpgradesInGroupPerStorage(storageType, getUpgradeGroup());
    }

    @Override
    public Component getName() {
        return Component.translatable(getDescriptionId());
    }
}
