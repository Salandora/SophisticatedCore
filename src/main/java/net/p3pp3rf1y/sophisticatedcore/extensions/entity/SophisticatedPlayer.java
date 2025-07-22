package net.p3pp3rf1y.sophisticatedcore.extensions.entity;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;
import java.util.OptionalInt;
import java.util.function.Consumer;

public interface SophisticatedPlayer {
	private Player self() {
		return (Player)this;
	}

	default OptionalInt sophisticatedCore_openMenu(MenuProvider menuProvider, BlockPos pos) {
		return this.sophisticatedCore_openMenu(menuProvider, (buf) -> buf.writeBlockPos(pos));
	}

	default OptionalInt sophisticatedCore_openMenu(MenuProvider menu, Consumer<RegistryFriendlyByteBuf> context) {
		var screenHandlerFactory = new ExtendedScreenHandlerFactory<>() {
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
			public byte[] getScreenOpeningData(ServerPlayer player) {
				final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
				context.accept(buf);
				return buf.array();
			}
		};

		return this.self().openMenu(screenHandlerFactory);
	}

	@Nullable
	default <T> T sophisticatedCore_getCapability(EntityApiLookup<T, ?> lookup) {
		return sophisticatedCore_getCapability(lookup, null);
	}

	@Nullable
	default <T, C> T sophisticatedCore_getCapability(EntityApiLookup<T, C> lookup, @Nullable C context) {
		return lookup.find(self(), context);
	}
}
