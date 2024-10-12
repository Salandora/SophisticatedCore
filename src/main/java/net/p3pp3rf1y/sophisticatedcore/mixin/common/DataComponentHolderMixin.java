package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.core.component.DataComponentHolder;
import net.p3pp3rf1y.sophisticatedcore.extensions.component.SophisticatedDataComponentHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DataComponentHolder.class)
public interface DataComponentHolderMixin extends SophisticatedDataComponentHolder {
}
