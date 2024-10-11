package net.p3pp3rf1y.porting_lib.base.mixin.common;

import net.minecraft.world.level.block.state.BlockState;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.BlockStateExtensions;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateExtensions {
}
