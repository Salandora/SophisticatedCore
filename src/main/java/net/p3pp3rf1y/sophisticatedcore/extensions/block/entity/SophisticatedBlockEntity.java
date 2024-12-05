package net.p3pp3rf1y.sophisticatedcore.extensions.block.entity;

import net.fabricmc.fabric.impl.lookup.block.ServerWorldCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface SophisticatedBlockEntity {
	private BlockEntity self() {
		return (BlockEntity) this;
	}

	default void sophisticatedCore_invalidateCapabilities() {
		BlockEntity be = self();
		if (!(be.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		((ServerWorldCache) serverLevel).fabric_invalidateCache(be.getBlockPos());
	}

	default void sophisticatedCore_onLoad() {
	}

	default void sophisticatedCore_onChunkUnloaded() {
	}
}
