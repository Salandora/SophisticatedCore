package net.p3pp3rf1y.sophisticatedcore.common;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.porting_lib.base.util.LazyOptional;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

public class CapabilityWrapper {
	public static final ItemApiLookup<LazyOptional<IStorageWrapper>, Void> STORAGE_WRAPPER_CAPABILITY = ItemApiLookup.get(SophisticatedCore.getRL("storage_wrapper"), (Class<LazyOptional<IStorageWrapper>>)(Class<?>)LazyOptional.class, Void.class);

	public static LazyOptional<IStorageWrapper> get(ItemStack provider) {
		LazyOptional<IStorageWrapper> wrapper = STORAGE_WRAPPER_CAPABILITY.find(provider, null);
		if (wrapper != null) {
			return wrapper;
		}
		return LazyOptional.empty();
	}

    public static void register() {
        ItemStorage.SIDED.registerFallback((level, pos, state, entity, dir) -> {
            if (entity instanceof ControllerBlockEntityBase) {
				return ((ControllerBlockEntityBase) entity).getCapability(ItemStorage.SIDED, null).getValueUnsafer();
            }

            return null;
        });
    }
}
