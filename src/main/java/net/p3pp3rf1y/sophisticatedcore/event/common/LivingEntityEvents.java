package net.p3pp3rf1y.sophisticatedcore.event.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.Collection;

public interface LivingEntityEvents {
    Event<Drops> DROPS = EventFactory.createArrayBacked(Drops.class, callbacks -> (target, source, drops, lootingLevel, recentlyHit) -> {
        for (Drops callback : callbacks) {
            if (callback.onLivingEntityDrops(target, source, drops, lootingLevel, recentlyHit)) {
                return true;
            }
        }

        return false;
    });

    Event<Tick> TICK = EventFactory.createArrayBacked(Tick.class, callbacks -> (entity) -> {
        for (Tick callback : callbacks) {
            callback.onLivingEntityTick(entity);
        }
    });

    @FunctionalInterface
    interface Drops {
        boolean onLivingEntityDrops(LivingEntity target, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit);
    }

    @FunctionalInterface
    interface Tick {
        void onLivingEntityTick(LivingEntity entity);
    }
}
