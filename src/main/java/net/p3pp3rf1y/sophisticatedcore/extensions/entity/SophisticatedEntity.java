package net.p3pp3rf1y.sophisticatedcore.extensions.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collection;

public interface SophisticatedEntity {
	default CompoundTag getSophisticatedCustomData() {
		throw new RuntimeException("This should have been implemented via mixin.");
	}

	default Collection<ItemEntity> sophisticatedCaptureDrops() {
		throw new RuntimeException("This should have been implemented via mixin.");
	}

	default Collection<ItemEntity> sophisticatedCaptureDrops(Collection<ItemEntity> value) {
		throw new RuntimeException("This should have been implemented via mixin.");
	}
}
