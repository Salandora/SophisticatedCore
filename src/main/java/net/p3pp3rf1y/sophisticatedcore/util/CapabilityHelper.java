package net.p3pp3rf1y.sophisticatedcore.util;

import com.github.salandora.sophisticatedlibrary.transfer.MutableContainerItemContext;
import com.github.salandora.sophisticatedlibrary.transfer.SlottedStackStorage;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CapabilityHelper {
	public static void runOnItemHandler(Entity entity, Consumer<SlottedStackStorage> run) {
		runOnCapability(entity, Capabilities.ItemHandler.ENTITY, null, run);
	}

	public static <T> T getFromItemHandler(Level level, BlockPos pos, @Nullable Direction context, Function<Storage<ItemVariant>, T> get, T defaultValue) {
		return getFromCapability(level, pos, ItemStorage.SIDED, context, get, defaultValue);
	}
	public static <T> T getFromItemHandler(Level level, BlockPos pos, Function<Storage<ItemVariant>, T> get, T defaultValue) {
		return getFromItemHandler(level, pos, null, get, defaultValue);
	}

	public static <T, C> void runOnCapability(Entity entity, EntityApiLookup<T, C> capability, @Nullable C context, Consumer<T> run) {
		runOnCapability(run, capability.find(entity, context));
	}
	public static <T, C> void runOnCapability(ItemStack stack, ItemApiLookup<T, C> capability, @Nullable C context, Consumer<T> run) {
		runOnCapability(run, capability.find(stack, context));
	}
	public static <T, C> void runOnCapability(ItemStack stack, ItemApiLookup<T, C> capability, @Nullable C context, BiConsumer<C, T> run) {
		runOnCapability(run, context, capability.find(stack, context));
	}

	private static <T> void runOnCapability(Consumer<T> run, @Nullable T t) {
		if (t != null) {
			run.accept(t);
		}
	}
	private static <T, C> void runOnCapability(BiConsumer<C, T> run, @Nullable C context, @Nullable T t) {
		if (t != null) {
			run.accept(context, t);
		}
	}

	public static <U> U getFromCapability(ItemApiLookup<Storage<FluidVariant>, ContainerItemContext> capability, ContainerItemContext context, Function<Storage<FluidVariant>, U> get, U defaultValue) {
		Storage<FluidVariant> fluidHandler = context.find(capability);
		if (fluidHandler == null) {
			return defaultValue;
		}

		return get.apply(fluidHandler);
	}

	public static <T, C, U> U getFromCapability(Level level, BlockPos pos, BlockApiLookup<T, C> capability, @Nullable C context, Function<T, U> get, U defaultValue) {
		return getFromCapability(level, pos, null, null, capability, context, get, defaultValue);
	}

	public static <T, C, U> U getFromCapability(BlockEntity blockEntity, BlockApiLookup<T, C> capability, @Nullable C context, Function<T, U> get, U defaultValue) {
		if (blockEntity.getLevel() == null) {
			return defaultValue;
		}

		return getFromCapability(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, capability, context, get, defaultValue);
	}

	public static <T, C, U> U getFromCapability(Level level, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity blockEntity, BlockApiLookup<T, C> capability, @Nullable C context, Function<T, U> get, U defaultValue) {
		T t = capability.find(level, pos, context);
		if (t == null) {
			return defaultValue;
		}
		return get.apply(t);
	}

	public static <T> T getFromFluidHandler(BlockEntity be, Direction side, Function<Storage<FluidVariant>, T> get, T defaultValue) {
		return getFromCapability(be, FluidStorage.SIDED, side, get, defaultValue);
	}

	public static <T> T getFromFluidHandler(ItemStack stack, Function<Storage<FluidVariant>, T> get, T defaultValue) {
		return getFromCapability(FluidStorage.ITEM, ContainerItemContext.withConstant(stack), get, defaultValue);
	}

	public static <T> T getFromFluidHandler(Player player, InteractionHand hand, Function<Storage<FluidVariant>, T> get, T defaultValue) {
		return getFromCapability(FluidStorage.ITEM, ContainerItemContext.forPlayerInteraction(player, hand), get, defaultValue);
	}

	public static void runOnFluidHandler(ItemStack stack, BiConsumer<ContainerItemContext, Storage<FluidVariant>> run) {
		runOnCapability(stack, FluidStorage.ITEM, new MutableContainerItemContext(stack), run);
	}
}
