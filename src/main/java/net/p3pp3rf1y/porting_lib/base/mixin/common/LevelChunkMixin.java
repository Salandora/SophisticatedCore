// Code from https://github.com/Fabricators-of-Create/Porting-Lib/blob/1.19.2/base/src/main/java/io/github/fabricators_of_create/porting_lib/mixin/common/LevelChunkMixin.java
// to not include all of porting_lib_base just for this
package net.p3pp3rf1y.porting_lib.base.mixin.common;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;

import javax.annotation.Nullable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
	@Shadow
	@Final
	Level level;

	public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> registry, long l, @Nullable LevelChunkSection[] levelChunkSections, @Nullable BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, registry, l, levelChunkSections, blendingData);
	}

	@Inject(method = "addAndRegisterBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;updateBlockEntityTicker(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", shift = At.Shift.AFTER))
	public void sophisticatedCore$onBlockEntityLoad(BlockEntity blockEntity, CallbackInfo ci) {
		blockEntity.onLoad();
	}

	@Inject(method = "registerAllBlockEntitiesAfterLevelLoad", at = @At("HEAD"))
	public void sophisticatedCore$addPendingBlockEntities(CallbackInfo ci) {
		this.level.addFreshBlockEntities(this.blockEntities.values());
	}
}