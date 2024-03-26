package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.extensions.BlockStateExtensions;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.extensions.block.SophisticatedBlockState;

@Mixin(BlockState.class)
public class BlockStateMixin implements SophisticatedBlockState, BlockStateExtensions {
}
