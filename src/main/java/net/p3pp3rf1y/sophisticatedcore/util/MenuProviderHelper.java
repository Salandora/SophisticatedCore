package net.p3pp3rf1y.sophisticatedcore.util;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public class MenuProviderHelper {
    public static <T> ExtendedScreenHandlerFactory<T> createMenuProvider(MenuConstructor<T> menuConstructor, T context, Component name) {
        return new ExtendedScreenHandlerFactory<>() {
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return menuConstructor.createMenu(i, inventory, player);
            }

			@Override
			public T getScreenOpeningData(ServerPlayer player) {
				return context;
			}

            @Override
            public Component getDisplayName() {
                return name;
            }
        };
    }

	@FunctionalInterface
	public interface MenuConstructor<T> {
		AbstractContainerMenu createMenu(int i, Inventory inventory, Player player);
	}
}
