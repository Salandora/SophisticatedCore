package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

public class CapabilityHelper {
	public static final EntityApiLookup<PlayerInventoryStorage, Void> ENTITY = EntityApiLookup. get(SophisticatedCore.getRL("entity_api"), PlayerInventoryStorage.class, Void.class);

	static {
		ENTITY.registerForType((player, ignored) -> PlayerInventoryStorage.of(player), EntityType.PLAYER);
	}

	public static void runOnItemHandler(Entity entity, Consumer<PlayerInventoryStorage> run) {
		runOnCapability(entity, ENTITY, null, run);
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

	private static <T> void runOnCapability(Consumer<T> run, @Nullable T t) {
		if (t != null) {
			run.accept(t);
		}
	}


	public static <T, C, U> U getFromCapability(ItemStack stack, ItemApiLookup<T, C> capability, @Nullable C context, Function<T, U> get, U defaultValue) {
		T t = capability.find(stack, context);
		if (t == null) {
			return defaultValue;
		}
		return get.apply(t);
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
		return getFromCapability(stack, FluidStorage.ITEM, null, get, defaultValue);
	}

	public static void runOnFluidHandler(ItemStack stack, Consumer<Storage<FluidVariant>> run) {
		runOnCapability(stack, FluidStorage.ITEM, null, run);
	}
}
