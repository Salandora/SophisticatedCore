package net.p3pp3rf1y.sophisticatedcore.util;

import io.github.fabricators_of_create.porting_lib.transfer.callbacks.TransactionCallback;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;

import java.util.Objects;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class FluidHelper {
    public static final long BUCKET_VOLUME_IN_MILLIBUCKETS = FluidConstants.BUCKET / 1000;

    public static int toBuckets(long amount) {
        return (int) (amount / BUCKET_VOLUME_IN_MILLIBUCKETS);
    }

    public static boolean isFluidStorage(ItemStack stack) {
        return ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM) != null;
    }

    public static boolean placeFluid(@Nullable Player player, @NotNull Level level, @NotNull BlockPos pos, Storage<FluidVariant> source, FluidVariant resource, long maxAmount, @Nullable TransactionContext ctx) {
        if (!level.isLoaded(pos)) {
            return false;
        }

        Fluid fluid = resource.getFluid();
        if (fluid == Fluids.EMPTY) {
            return false;
        }

        // check that we can place the fluid at the destination
        BlockState state = level.getBlockState(pos);
        Material material = state.getMaterial();
        boolean waterlog = state.hasProperty(WATERLOGGED);
        if (!waterlog && !material.isReplaceable()) {
            return false;
        }
        if (waterlog && fluid != Fluids.WATER) {
            return false;
        }

        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty() && fluidState.getType() != fluid) {
            return false;
        }

        try (Transaction nested = Transaction.openNested(ctx)) {
            long result = source.extract(resource, maxAmount, nested);
            if (result == 0) {
                return false;
            }

            level.updateSnapshots(nested);
            if (level.dimensionType().ultraWarm() && fluid.defaultFluidState().is(FluidTags.WATER)) {
                TransactionCallback.onSuccess(nested, () -> {
                    level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f + (level.random.nextFloat() - level.random.nextFloat()) * 0.8f);
                    for (int i = 0; i < 8; ++i) {
                        level.addParticle(ParticleTypes.LARGE_SMOKE, (double) pos.getX() + Math.random(), (double) pos.getY() + Math.random(), (double) pos.getZ() + Math.random(), 0.0, 0.0, 0.0);
                    }
                });
                nested.commit();
                return true;
            }

            if (waterlog) {
                level.setBlock(pos, state.setValue(WATERLOGGED, true), 3);
                TransactionCallback.onSuccess(nested, () -> level.scheduleTick(pos, Fluids.WATER, 1));
                nested.commit();
                return true;
            }

            if (!level.isClientSide && state.canBeReplaced(fluid) && !material.isLiquid()) {
                level.destroyBlock(pos, true);
            }

            if (level.setBlock(pos, fluid.defaultFluidState().createLegacyBlock(), 3) || fluidState.isSource()) {
                TransactionCallback.onSuccess(nested, () -> playFluidSound(player, level, pos, resource, false));
                nested.commit();
                return true;
            }

            return false;
        }
    }

    public static void playFluidSound(@Nullable Player player, LevelAccessor level, BlockPos pos, FluidVariant resource, boolean fill) {
        SoundEvent sound = fill ? FluidVariantAttributes.getFillSound(resource) : FluidVariantAttributes.getEmptySound(resource);
        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }

    // region Copied from fabric transfer api, modified to mimic forge behaviour and add fill parameter
    /**
     * Try to make the item in a player hand "interact" with a fluid storage.
     * This can be used when a player right-clicks a tank, for example.
     *
     * <p>More specifically, this function tries to find a fluid storing item in the player's hand.
     * Then, it tries to fill that item from the storage. If that fails, it tries to fill the storage from that item.
     *
     * <p>Only up to one fluid variant will be moved, and the corresponding emptying/filling sound will be played.
     *
     * @param storage The storage that the player is interacting with.
     * @param player The player.
     * @param hand The hand that the player used.
     * @return True if some fluid was moved.
     */
    public static boolean interactWithFluidStorage(Storage<FluidVariant> storage, Player player, InteractionHand hand, boolean fill) {
        // Check if hand is a fluid container.
        // To keep same behaviour as in forge we need to use a non creative context even in creative more,
        // otherwise buckets will not get filled nor emptied
        Storage<FluidVariant> handStorage = ContainerItemContext.ofPlayerHand(player, hand).find(FluidStorage.ITEM);
        if (handStorage == null) return false;

        Item handItem = player.getItemInHand(hand).getItem();
        try {
            if (fill) {
                return moveWithSound(storage, handStorage, player, true, handItem);
            } else {
                return moveWithSound(handStorage, storage, player, false, handItem);
            }
        } catch (Exception e) {
            CrashReport report = CrashReport.forThrowable(e, "Interacting with fluid storage");
            report.addCategory("Interaction details")
                    //.setDetail("Player", () -> DebugMessages.forPlayer(player))
                    .setDetail("Hand", hand)
                    .setDetail("Hand item", handItem::toString)
                    .setDetail("Fluid storage", () -> Objects.toString(storage, null));
            throw new ReportedException(report);
        }
    }
    private static boolean moveWithSound(Storage<FluidVariant> from, Storage<FluidVariant> to, Player player, boolean fill, Item handItem) {
        for (StorageView<FluidVariant> view : from) {
            if (view.isResourceBlank()) continue;
            FluidVariant resource = view.getResource();
            long maxExtracted;

            // check how much can be extracted
            try (Transaction extractionTestTransaction = Transaction.openOuter()) {
                maxExtracted = view.extract(resource, Long.MAX_VALUE, extractionTestTransaction);
            }

            try (Transaction transferTransaction = Transaction.openOuter()) {
                // check how much can be inserted
                long accepted = to.insert(resource, maxExtracted, transferTransaction);

                // extract it, or rollback if the amounts don't match
                if (accepted > 0 && view.extract(resource, accepted, transferTransaction) == accepted) {
                    transferTransaction.commit();

                    SoundEvent sound = fill ? FluidVariantAttributes.getFillSound(resource) : FluidVariantAttributes.getEmptySound(resource);

                    if (resource.isOf(Fluids.WATER)) {
                        if (fill && handItem == Items.GLASS_BOTTLE) sound = SoundEvents.BOTTLE_FILL;
                        if (!fill && handItem == Items.POTION) sound = SoundEvents.BOTTLE_EMPTY;
                    }

                    player.playNotifySound(sound, SoundSource.BLOCKS, 1, 1);

                    return true;
                }
            }
        }

        return false;
    }
    // endregion
}
