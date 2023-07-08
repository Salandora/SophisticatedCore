package net.p3pp3rf1y.sophisticatedcore.common.components;

import dev.onyxstudios.cca.api.v3.block.BlockComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.block.BlockComponentInitializer;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotExposedStorage;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.controller.ControllerBlockEntityBase;
import org.jetbrains.annotations.Nullable;

public class Components implements BlockComponentInitializer {
    public static final ComponentKey<IComponentWrapper> ITEM_HANDLER = ComponentRegistry.getOrCreate(SophisticatedCore.getRL("item_handler_component"), IComponentWrapper.class);

    @Override
    public void registerBlockComponentFactories(BlockComponentFactoryRegistry registry) {
        registry.registerFor(ControllerBlockEntityBase.class, ITEM_HANDLER, Components::createItemHandlerComponent);
    }

    private static IComponentWrapper.SimpleComponentWrapper<SlotExposedStorage, ControllerBlockEntityBase> createItemHandlerComponent(ControllerBlockEntityBase entity) {
        return new IComponentWrapper.SimpleComponentWrapper<>(entity) {
            @Nullable
            @Override
            public SlotExposedStorage get() {
                return this.object;
            }

            @Override
            public LazyOptional<SlotExposedStorage> getWrapped() {
                if (wrapped == null) {
                    wrapped =  LazyOptional.ofObject(this.object);
                }
                return wrapped;
            }
        };
    }

}
