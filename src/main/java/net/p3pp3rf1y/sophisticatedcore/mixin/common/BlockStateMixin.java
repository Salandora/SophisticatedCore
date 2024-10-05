package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedcore.extensions.block.SophisticatedBlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public class BlockStateMixin implements SophisticatedBlockState {
}
