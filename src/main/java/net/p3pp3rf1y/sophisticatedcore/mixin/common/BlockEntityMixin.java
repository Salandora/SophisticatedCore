package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedcore.extensions.block.entity.CapabilityHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements CapabilityHelper {
}
