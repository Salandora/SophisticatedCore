package net.p3pp3rf1y.sophisticatedcore.api;

import com.github.salandora.sophisticatedfabriclib.fluid.api.v1.FluidStack;
import com.github.salandora.sophisticatedfabriclib.fluid.api.v1.IFluidHandlerItem;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public interface IStorageFluidHandler extends IFluidHandlerItem {
	default long fill(TagKey<Fluid> fluidTag, long maxFill, Fluid fallbackFluid, FluidAction action) {
		return fill(fluidTag, maxFill, fallbackFluid, action, false);
	}

	default long fill(TagKey<Fluid> fluidTag, long maxFill, Fluid fallbackFluid, FluidAction action, boolean ignoreInOutLimit) {
		for (int tank = 0; tank < getTanks(); tank++) {
			FluidStack tankFluid = getFluidInTank(tank);
			if (tankFluid.is(fluidTag)) {
				return fill(new FluidStack(tankFluid.getFluid(), maxFill), action, ignoreInOutLimit);
			}
		}
		return fill(new FluidStack(fallbackFluid, maxFill), action, ignoreInOutLimit);
	}

	long fill(FluidStack resource, FluidAction action, boolean ignoreInOutLimit);

	FluidStack drain(TagKey<Fluid> resourceTag, long maxDrain, FluidAction action, boolean ignoreInOutLimit);

	FluidStack drain(FluidStack resource, FluidAction action, boolean ignoreInOutLimit);

	FluidStack drain(long maxDrain, FluidAction action, boolean ignoreInOutLimit);
}
