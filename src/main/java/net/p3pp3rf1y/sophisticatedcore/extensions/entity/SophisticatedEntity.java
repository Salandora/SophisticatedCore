package net.p3pp3rf1y.sophisticatedcore.extensions.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nullable;
import java.util.Collection;

public interface SophisticatedEntity {
	default CompoundTag sophisticatedCore_getCustomData() {
		throw new RuntimeException("This should have been implemented via mixin.");
	}

	@Nullable
	default Collection<ItemEntity> sophisticatedCore_captureDrops() {
		throw new RuntimeException("This should have been implemented via mixin.");
	}

	default Collection<ItemEntity> sophisticatedCore_captureDrops(@Nullable Collection<ItemEntity> value) {
		throw new RuntimeException("This should have been implemented via mixin.");
	}
}
