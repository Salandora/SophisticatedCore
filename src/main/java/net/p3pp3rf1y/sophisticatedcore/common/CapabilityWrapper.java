package net.p3pp3rf1y.sophisticatedcore.common;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

public class CapabilityWrapper {
    public static void register() {
        ItemStorage.SIDED.registerFallback((level, pos, state, entity, dir) -> {
            if (entity instanceof ControllerBlockEntityBase) {
				return ((ControllerBlockEntityBase) entity).getCapability(ItemStorage.SIDED, null);
            }

            return null;
        });
    }
}
