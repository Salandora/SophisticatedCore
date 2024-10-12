package net.p3pp3rf1y.sophisticatedcore.fluid;

import net.minecraft.world.item.ItemStack;

public class FluidActionResult {
	public static final FluidActionResult FAILURE = new FluidActionResult(false, ItemStack.EMPTY);

	private final boolean success;
	private final ItemStack result;

	public FluidActionResult(ItemStack result) {
		this(true, result);
	}

	public FluidActionResult(boolean success, ItemStack result) {
		this.success = success;
		this.result = result;
	}

	public boolean isSuccess() {
		return success;
	}

	public ItemStack getResult() {
		return result;
	}
}
