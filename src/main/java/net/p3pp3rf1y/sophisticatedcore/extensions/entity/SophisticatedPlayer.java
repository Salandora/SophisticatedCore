package net.p3pp3rf1y.sophisticatedcore.extensions.entity;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.Consumer;

@SuppressWarnings("unused") // used in fabric.mod.json for interface injection
public interface SophisticatedPlayer {
	private Player self() {
		return (Player)this;
	}

	default OptionalInt sophisticatedCore_openMenu(MenuProvider menuProvider, BlockPos pos) {
		return this.sophisticatedCore_openMenu(menuProvider, (buf) -> buf.writeBlockPos(pos));
	}

	default OptionalInt sophisticatedCore_openMenu(MenuProvider menu, Consumer<FriendlyByteBuf> context) {
		var screenHandlerFactory = new ExtendedScreenHandlerFactory() {
			@Override
			public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
				return menu.createMenu(i, inventory, player);
			}

			@Override
			public boolean shouldCloseCurrentScreen() {
				return menu.shouldCloseCurrentScreen();
			}

			@Override
			public Component getDisplayName() {
				return menu.getDisplayName();
			}

			@Override
			public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
				context.accept(buf);
			}
		};

		return this.self().openMenu(screenHandlerFactory);
	}
}