package net.p3pp3rf1y.sophisticatedcore.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import javax.annotation.Nullable;

public class WorldHelper {
	private WorldHelper() {}

	public static Optional<BlockEntity> getBlockEntity(@Nullable BlockGetter level, BlockPos pos) {
		return getBlockEntity(level, pos, BlockEntity.class);
	}

	public static <T> Optional<T> getLoadedBlockEntity(@Nullable Level level, BlockPos pos, Class<T> teClass) {
		if (level != null && level.isLoaded(pos)) {
			return getBlockEntity(level, pos, teClass);
		}
		return Optional.empty();
	}

	public static <T> Optional<T> getBlockEntity(@Nullable BlockGetter level, BlockPos pos, Class<T> teClass) {
		if (level == null) {
			return Optional.empty();
		}

		BlockEntity be = level.getBlockEntity(pos);

		if (teClass.isInstance(be)) {
			return Optional.of(teClass.cast(be));
		}

		return Optional.empty();
	}

	public static void notifyBlockUpdate(BlockEntity tile) {
		Level level = tile.getLevel();
		if (level == null) {
			return;
		}
		level.sendBlockUpdated(tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), 3);
	}
}
