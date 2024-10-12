package net.p3pp3rf1y.sophisticatedcore.upgrades.tank;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

public record TankClickPayload(int upgradeSlot) implements CustomPacketPayload {
	public static final Type<TankClickPayload> TYPE = new Type<>(SophisticatedCore.getRL("tank_click"));
	public static final StreamCodec<ByteBuf, TankClickPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			TankClickPayload::upgradeSlot,
			TankClickPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TankClickPayload payload, ServerPlayNetworking.Context context) {
		Player player = context.player();
		if (!(player instanceof ServerPlayer) || !(player.containerMenu instanceof StorageContainerMenuBase<?> storageContainerMenu)) {
			return;
		}
		AbstractContainerMenu containerMenu = player.containerMenu;
		UpgradeContainerBase<?, ?> upgradeContainer = storageContainerMenu.getUpgradeContainers().get(payload.upgradeSlot);
		if (!(upgradeContainer instanceof TankUpgradeContainer tankContainer)) {
			return;
		}

		ContainerItemContext cic = ContainerItemContext.ofPlayerCursor(player, containerMenu);
		Storage<FluidVariant> storage = cic.find(FluidStorage.ITEM);
		if (storage != null) {
			TankUpgradeWrapper tankWrapper = tankContainer.getUpgradeWrapper();
			FluidStack tankContents = tankWrapper.getContents();
			if (tankContents.isEmpty()) {
				tankWrapper.drainHandler(storage);
			} else {
				if (!tankWrapper.fillHandler(storage)) {
					tankWrapper.drainHandler(storage);
				}
			}
		}

		// TODO:
		/*ItemStack cursorStack = containerMenu.getCarried();
		IFluidHandlerItem fluidHandler = cursorStack.getCapability(Capabilities.FluidHandler.ITEM);
		if (fluidHandler == null) {
			return;
		}

		TankUpgradeWrapper tankWrapper = tankContainer.getUpgradeWrapper();
		FluidStack tankContents = tankWrapper.getContents();
		if (tankContents.isEmpty()) {
			drainHandler(serverPlayer, containerMenu, fluidHandler, tankWrapper);
		} else {
			if (!tankWrapper.fillHandler(fluidHandler, itemStackIn -> {
				containerMenu.setCarried(itemStackIn);
				serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, containerMenu.getCarried()));
			})) {
				drainHandler(serverPlayer, containerMenu, fluidHandler, tankWrapper);
			}
		}*/
	}

	/*private static void drainHandler(ServerPlayer player, AbstractContainerMenu containerMenu, IFluidHandlerItem fluidHandler, TankUpgradeWrapper tankWrapper) {
		tankWrapper.drainHandler(fluidHandler, itemStackIn -> {
			containerMenu.setCarried(itemStackIn);
			player.connection.send(new ClientboundContainerSetSlotPacket(-1, containerMenu.incrementStateId(), -1, containerMenu.getCarried()));
		});
	}*/
}
