package net.p3pp3rf1y.sophisticatedcore.upgrades.pump;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IServerUpdater;

import java.util.function.Supplier;

public class FluidFilterContainer {
	private final Player player;
	private final IServerUpdater serverUpdater;
	private final Supplier<FluidFilterLogic> fluidFilterLogic;
	private static final String DATA_FLUID = "setFluid";

	public FluidFilterContainer(Player player, IServerUpdater serverUpdater, Supplier<FluidFilterLogic> fluidFilterLogic) {
		this.player = player;
		this.serverUpdater = serverUpdater;
		this.fluidFilterLogic = fluidFilterLogic;
	}

	public FluidStack getFluid(int index) {
		return fluidFilterLogic.get().getFluid(index);
	}

	private void setFluid(int index, FluidStack fluid) {
		fluidFilterLogic.get().setFluid(index, fluid);
		serverUpdater.sendDataToServer(() -> serializeSetFluidData(index, fluid));
	}

	private CompoundTag serializeSetFluidData(int index, FluidStack fluid) {
		CompoundTag ret = new CompoundTag();
		CompoundTag fluidNbt = new CompoundTag();
		fluidNbt.putInt("index", index);
		fluidNbt.put("fluid", fluid.writeToNBT(new CompoundTag()));
		ret.put(DATA_FLUID, fluidNbt);
		return ret;
	}

	public boolean handleMessage(CompoundTag data) {
		if (data.contains(DATA_FLUID)) {
			CompoundTag fluidData = data.getCompound(DATA_FLUID);
			FluidStack fluid = FluidStack.loadFluidStackFromNBT(data.getCompound("fluid"));
			if (!fluid.isEmpty()) {
				setFluid(fluidData.getInt("index"), fluid);
			}
			return true;
		}
		return false;
	}

	public int getNumberOfFluidFilters() {
		return fluidFilterLogic.get().getNumberOfFluidFilters();
	}

	public void slotClick(int index) {
		ItemStack carried = player.containerMenu.getCarried();
		if (carried.isEmpty()) {
			setFluid(index, FluidStack.EMPTY);
			return;
		}

        Storage<FluidVariant> storage = ContainerItemContext.withConstant(carried).find(FluidStorage.ITEM);
		if (storage != null) {
			FluidStack containedFluid = TransferUtil.simulateExtractAnyFluid(storage, FluidConstants.BUCKET);
			if (!containedFluid.isEmpty()) {
				setFluid(index, containedFluid);
			}
		}
	}
}
