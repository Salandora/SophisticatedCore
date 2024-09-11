package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IPickupResponseUpgrade {
	ItemStack pickup(Level level, ItemStack stack, TransactionContext ctx);
}
