/*
package net.p3pp3rf1y.sophisticatedcore.mixin.common;

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
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin extends ChunkAccess {
	@Shadow
	@Final
	Level level;

	public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable LevelChunkSection[] sections, @Nullable BlendingData blendingData) {
		super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
	}

	@Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
	private void sophisticatedCore$unloadChunk(CallbackInfo ci) {
		this.blockEntities.values().forEach(BlockEntity::sophisticatedCore_onChunkUnloaded);
	}

	@Inject(method = "addAndRegisterBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;updateBlockEntityTicker(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", shift = At.Shift.AFTER))
	public void sophisticatedCore$onBlockEntityLoad(BlockEntity blockEntity, CallbackInfo ci) {
		blockEntity.sophisticatedCore_onLoad();
	}

	@Inject(method = "registerAllBlockEntitiesAfterLevelLoad", at = @At("HEAD"))
	public void sophisticatedCore$addPendingBlockEntities(CallbackInfo ci) {
		this.level.sophisticatedCore_addFreshBlockEntities(this.blockEntities.values());
	}
}
*/
