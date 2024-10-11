package net.p3pp3rf1y.sophisticatedcore.extensions.block.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.impl.lookup.block.ServerWorldCache;

public interface CapabilityHelper {
	private BlockEntity self() {
		return (BlockEntity) this;
	}

	default void invalidateCapabilities() {
		BlockEntity be = self();
		if (!(be.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		((ServerWorldCache) serverLevel).fabric_invalidateCache(be.getBlockPos());
	}
}
