package net.p3pp3rf1y.sophisticatedcore.upgrades;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

public interface IPickupResponseUpgrade {
	ItemStack pickup(Level level, ItemStack stack, TransactionContext ctx);
}
