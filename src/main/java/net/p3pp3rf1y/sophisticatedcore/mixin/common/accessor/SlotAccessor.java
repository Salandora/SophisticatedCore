package net.p3pp3rf1y.sophisticatedcore.mixin.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.inventory.Slot;

@Mixin(Slot.class)
public interface SlotAccessor {
	@Mutable
	@Accessor("x")
	void setX(int value);

	@Mutable
	@Accessor("y")
	void setY(int value);

	@Invoker("onSwapCraft")
	void callOnSwapCraft(int amount);
}
