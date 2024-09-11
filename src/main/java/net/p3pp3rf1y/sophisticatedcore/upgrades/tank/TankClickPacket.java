package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

public class TankClickPacket implements FabricPacket {
	public static final PacketType<TankClickPacket> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "tank_click"), TankClickPacket::new);
	private final int upgradeSlot;

	public TankClickPacket(int upgradeSlot) {
		this.upgradeSlot = upgradeSlot;
	}

	public TankClickPacket(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		if (!(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu)) {
			return;
		}

		AbstractContainerMenu containerMenu = player.containerMenu;
		UpgradeContainerBase<?, ?> upgradeContainer = storageContainerMenu.getUpgradeContainers().get(upgradeSlot);
		if (!(upgradeContainer instanceof TankUpgradeContainer tankContainer)) {
			return;
		}

		ContainerItemContext cic = ContainerItemContext.ofPlayerCursor(player, containerMenu);
		Storage<FluidVariant> storage = cic.find(FluidStorage.ITEM);
		if (storage != null) {
			TankUpgradeWrapper tankWrapper = tankContainer.getUpgradeWrapper();
			io.github.fabricators_of_create.porting_lib.fluids.FluidStack tankContents = tankWrapper.getContents();
			if (tankContents.isEmpty()) {
				tankWrapper.drainHandler(storage);
			} else {
				if (!tankWrapper.fillHandler(storage)) {
					tankWrapper.drainHandler(storage);
				}
			}
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(upgradeSlot);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
