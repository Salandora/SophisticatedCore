package net.p3pp3rf1y.sophisticatedcore.common;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

import java.util.Optional;

public class CapabilityWrapper {
	public static final ItemApiLookup<IStorageWrapper, Void> STORAGE_WRAPPER_CAPABILITY = ItemApiLookup.get(SophisticatedCore.getRL("storage_wrapper"), IStorageWrapper.class, Void.class);

	public static Optional<IStorageWrapper> get(ItemStack provider) {
		return get(provider, IStorageWrapper.class);
	}

	public static <T> Optional<T> get(ItemStack provider, Class<T> clazz) {
		IStorageWrapper wrapper = STORAGE_WRAPPER_CAPABILITY.find(provider, null);
		if (clazz.isInstance(wrapper)) {
			return Optional.of(clazz.cast(wrapper));
		}
		return Optional.empty();
	}

    public static void register() {
        ItemStorage.SIDED.registerFallback((level, pos, state, entity, dir) -> {
            if (entity instanceof ControllerBlockEntityBase) {
				return ((ControllerBlockEntityBase) entity).getCapability(ItemStorage.SIDED, null);
            }

            return null;
        });
    }
}
