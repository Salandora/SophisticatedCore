package net.p3pp3rf1y.sophisticatedcore.extensions.inventory;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType.ExtendedFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public interface IMenuTypeExtension<T> {
	static <T extends AbstractContainerMenu> MenuType<T> create(ExtendedFactory<T, FriendlyByteBuf> factory) {
		return new ExtendedScreenHandlerType<>((windowId, inventory, data) -> {
			FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
			T menu = factory.create(windowId, inventory, buf);
			buf.release();
			return menu;
		}, ByteBufCodecs.BYTE_ARRAY);
	}
}
