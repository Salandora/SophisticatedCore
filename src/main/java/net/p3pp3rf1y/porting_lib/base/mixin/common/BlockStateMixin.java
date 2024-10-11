package net.p3pp3rf1y.porting_lib.base.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.extensions.BlockStateExtensions;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateExtensions {
}