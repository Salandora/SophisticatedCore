package net.p3pp3rf1y.sophisticatedcore.event.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface MobSpawnEvents {
    Event<After> AFTER_FINALIZE_SPAWN = EventFactory.createArrayBacked(After.class, callbacks -> (finalizeEvent) -> {
        for (After event : callbacks) {
            event.onAfterFinalizeSpawn(finalizeEvent);
        }
    });

    @FunctionalInterface
    interface After {
        void onAfterFinalizeSpawn(FinalizeSpawn event);
    }

    class FinalizeSpawn {
        private final Entity entity;
        private final ServerLevelAccessor level;
        private final DifficultyInstance difficulty;
        private final MobSpawnType reason;
        private final SpawnGroupData spawnData;
        private final CompoundTag dataTag;

        public FinalizeSpawn(Entity entity, ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData spawnData, CompoundTag dataTag) {
            this.entity = entity;
            this.level = level;
            this.difficulty = difficulty;
            this.reason = reason;
            this.spawnData = spawnData;
            this.dataTag = dataTag;
        }

        public Entity getEntity() {
            return entity;
        }

        public ServerLevelAccessor getLevel() {
            return level;
        }

        public DifficultyInstance getDifficulty() {
            return difficulty;
        }

        public MobSpawnType getReason() {
            return reason;
        }

        public SpawnGroupData getSpawnData() {
            return spawnData;
        }

        public CompoundTag getDataTag() {
            return dataTag;
        }
    }
}
