package net.p3pp3rf1y.sophisticatedcore.event.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface EntityEvents {
	Event<JoinWorld> ON_JOIN_WORLD = EventFactory.createArrayBacked(JoinWorld.class, callbacks -> (entity, world, loadedFromDisk) -> {
		for (JoinWorld callback : callbacks)
			if (!callback.onJoinWorld(entity, world, loadedFromDisk))
				return true;
		return false;
	});

	@FunctionalInterface
	interface JoinWorld {
		boolean onJoinWorld(Entity entity, Level world, boolean loadedFromDisk);
	}
}
