package net.p3pp3rf1y.sophisticatedcore.common;

import com.github.salandora.sophisticatedlibrary.transfer.api.v1.wrapper.fabric.FabricItemHandlerWrapper;
import com.github.salandora.sophisticatedlibrary.util.Capabilities;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;

public class CapabilityWrapper {
    public static void register() {
        Capabilities.ItemHandler.SIDED.registerFallback((level, pos, state, entity, dir) -> {
            if (entity instanceof ControllerBlockEntityBase controller) {
                return controller.getCapability(Capabilities.ItemHandler.SIDED, null).orElse(null);
            }

            return null;
        });

        ItemStorage.SIDED.registerFallback((level, pos, state, entity, dir) -> {
            if (entity instanceof ControllerBlockEntityBase controller) {
				return FabricItemHandlerWrapper.of(controller.getCapability(Capabilities.ItemHandler.SIDED, null).orElse(null));
            }

            return null;
        });
    }
}
