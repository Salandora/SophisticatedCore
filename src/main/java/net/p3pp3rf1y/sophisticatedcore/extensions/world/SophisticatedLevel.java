package net.p3pp3rf1y.sophisticatedcore.extensions.world;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collection;

public interface SophisticatedLevel {
	default void sophisticatedCore_addFreshBlockEntities(Collection<BlockEntity> beList) {
		throw new RuntimeException("Should have been overriden by mixin.");
	}
}
