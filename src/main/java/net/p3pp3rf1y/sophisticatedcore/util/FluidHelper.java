package net.p3pp3rf1y.sophisticatedcore.util;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class FluidHelper {
    public static final long BUCKET_VOLUME_IN_MILLIBUCKETS = FluidConstants.BUCKET / 1000;

    public static int toBuckets(long amount) {
        return (int) (amount / BUCKET_VOLUME_IN_MILLIBUCKETS);
    }

    public static boolean isFluidStorage(ItemStack stack) {
        return ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null;
    }

    public static boolean placeFluid(@Nullable Player player, @NotNull Level level, @NotNull BlockPos pos, Storage<FluidVariant> source, FluidVariant resource, long maxAmount) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        Fluid fluid = resource.getFluid();
        if (fluid == Fluids.EMPTY) {
            return false;
        }

        // check that we can place the fluid at the destination
        BlockState state = level.getBlockState(pos);
        boolean waterlog = state.hasProperty(WATERLOGGED);
        if (!waterlog && !state.canBeReplaced()) {
            return false;
        }
        if (waterlog && fluid != Fluids.WATER) {
            return false;
        }

        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty() && fluidState.getType() != fluid) {
            return false;
        }

        try (Transaction extraction = Transaction.openOuter()) {
			long result = source.extract(resource, maxAmount, extraction);
			if (result == 0) {
				return false;
			}
			extraction.commit();
		}

		if (level.dimensionType().ultraWarm() && fluid.defaultFluidState().is(FluidTags.WATER)) {
			level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
			for (int i = 0; i < 8; ++i) {
				level.addParticle(ParticleTypes.LARGE_SMOKE, (double) pos.getX() + Math.random(), (double) pos.getY() + Math.random(), (double) pos.getZ() + Math.random(), 0.0, 0.0, 0.0);
			}
			return true;
		}

		if (waterlog) {
			level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
			level.scheduleTick(pos, Fluids.WATER, 1);
			return true;
		}

		if (!level.isClientSide && state.canBeReplaced(fluid) && !state.liquid()) {
			level.destroyBlock(pos, true);
		}

		if (level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 3) || fluidState.isSource()) {
			playFluidSound(player, level, pos, resource, false);
			return true;
		}

		return false;
    }

    public static void playFluidSound(@Nullable Player player, LevelAccessor level, BlockPos pos, FluidVariant resource, boolean fill) {
        SoundEvent sound = fill ? FluidVariantAttributes.getFillSound(resource) : FluidVariantAttributes.getEmptySound(resource);
        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}
