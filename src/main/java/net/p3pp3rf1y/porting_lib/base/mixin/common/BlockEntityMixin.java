package net.p3pp3rf1y.porting_lib.base.mixin.common;

import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.BlockEntityExtensions;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements BlockEntityExtensions {
}
