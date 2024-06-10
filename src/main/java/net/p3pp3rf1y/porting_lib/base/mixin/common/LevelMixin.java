package net.p3pp3rf1y.porting_lib.base.mixin.common;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.fabricators_of_create.porting_lib.PortingConstants;
import io.github.fabricators_of_create.porting_lib.extensions.BlockEntityExtensions;
import io.github.fabricators_of_create.porting_lib.extensions.LevelExtensions;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

@Mixin(value = Level.class)
public abstract class LevelMixin implements LevelAccessor, LevelExtensions {
	// only non-null during transactions. Is set back to null in
	// onFinalCommit on commits, and through snapshot rollbacks on aborts.
	@Unique
	private List<ChangedPosData> port_lib$modifiedStates = null;
	@Unique
	private final ArrayList<BlockEntity> sophisticatedCore$freshBlockEntities = new ArrayList<>();
	@Unique
	private final ArrayList<BlockEntity> sophisticatedCore$pendingFreshBlockEntities = new ArrayList<>();

	@Shadow
	private boolean tickingBlockEntities;

	@Shadow
	public abstract BlockState getBlockState(BlockPos blockPos);

	@Shadow
	public abstract void setBlocksDirty(BlockPos pos, BlockState old, BlockState updated);

	@Shadow
	@Final
	public boolean isClientSide;

	@Shadow
	public abstract void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags);

	@Shadow
	public abstract void updateNeighbourForOutputSignal(BlockPos pos, Block block);

	@Shadow
	public abstract void onBlockStateChange(BlockPos pos, BlockState blockState, BlockState newState);

	@Unique
	private final SnapshotParticipant<LevelSnapshotData> port_lib$snapshotParticipant = new SnapshotParticipant<>() {

		@Override
		protected LevelSnapshotData createSnapshot() {
			LevelSnapshotData data = new LevelSnapshotData(port_lib$modifiedStates);
			if (port_lib$modifiedStates == null) port_lib$modifiedStates = new LinkedList<>();
			return data;
		}

		@Override
		protected void readSnapshot(LevelSnapshotData snapshot) {
			port_lib$modifiedStates = snapshot.changedStates();
		}

		@Override
		protected void onFinalCommit() {
			super.onFinalCommit();
			List<ChangedPosData> modifications = port_lib$modifiedStates;
			port_lib$modifiedStates = null;
			for (ChangedPosData data : modifications) {
				setBlock(data.pos(), data.state(), data.flags());
			}
		}
	};

	@Override
	public SnapshotParticipant<LevelSnapshotData> snapshotParticipant() {
		return port_lib$snapshotParticipant;
	}

	@Inject(method = "getBlockState", at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
			target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"), cancellable = true)
	private void port_lib$getBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		if (port_lib$modifiedStates != null) {
			// iterate in reverse order - latest changes priority
			for (ChangedPosData data : port_lib$modifiedStates) {
				if (data.pos().equals(pos)) {
					BlockState state = data.state();
					if (state == null) {
						PortingConstants.LOGGER.error("null blockstate stored in snapshots at " + pos);
						new Throwable().printStackTrace();
					} else {
						cir.setReturnValue(state);
					}
					return;
				}
			}
		}
	}

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
			at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/world/level/Level;getChunkAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/LevelChunk;"), cancellable = true)
	private void port_lib$setBlock(BlockPos pos, BlockState state, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
		if (state == null) {
			PortingConstants.LOGGER.error("Setting null blockstate at " + pos);
			new Throwable().printStackTrace();
		}
		if (port_lib$modifiedStates != null) {
			port_lib$modifiedStates.add(new ChangedPosData(pos, state, flags));
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", shift = At.Shift.AFTER))
	public void port_lib$pendingBlockEntities(CallbackInfo ci) {
		if (!this.sophisticatedCore$pendingFreshBlockEntities.isEmpty()) {
			this.sophisticatedCore$freshBlockEntities.addAll(this.sophisticatedCore$pendingFreshBlockEntities);
			this.sophisticatedCore$pendingFreshBlockEntities.clear();
		}
	}

	@Inject(method = "tickBlockEntities", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
	public void port_lib$onBlockEntitiesLoad(CallbackInfo ci) {
		if (!this.sophisticatedCore$freshBlockEntities.isEmpty()) {
			this.sophisticatedCore$freshBlockEntities.forEach(BlockEntityExtensions::onLoad);
			this.sophisticatedCore$freshBlockEntities.clear();
		}
	}

	@Unique
	@Override
	public void addFreshBlockEntities(Collection<BlockEntity> beList) {
		if (this.tickingBlockEntities) {
			this.sophisticatedCore$pendingFreshBlockEntities.addAll(beList);
		} else {
			this.sophisticatedCore$freshBlockEntities.addAll(beList);
		}
	}

	@Unique
	@Override
	public void markAndNotifyBlock(BlockPos pos, @Nullable LevelChunk levelchunk, BlockState oldState, BlockState newState, int flags, int recursionLeft) {
		Block block = newState.getBlock();
		BlockState blockstate1 = getBlockState(pos);
		if (blockstate1 == newState) {
			if (oldState != blockstate1) {
				this.setBlocksDirty(pos, oldState, blockstate1);
			}

			if ((flags & 2) != 0 && (!this.isClientSide || (flags & 4) == 0) && (this.isClientSide || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(ChunkHolder.FullChunkStatus.TICKING))) {
				this.sendBlockUpdated(pos, oldState, newState, flags);
			}

			if ((flags & 1) != 0) {
				this.blockUpdated(pos, oldState.getBlock());
				if (!this.isClientSide && newState.hasAnalogOutputSignal()) {
					this.updateNeighbourForOutputSignal(pos, block);
				}
			}

			if ((flags & 16) == 0 && recursionLeft > 0) {
				int i = flags & -34;
				oldState.updateIndirectNeighbourShapes(this, pos, i, recursionLeft - 1);
				newState.updateNeighbourShapes(this, pos, i, recursionLeft - 1);
				newState.updateIndirectNeighbourShapes(this, pos, i, recursionLeft - 1);
			}

			this.onBlockStateChange(pos, oldState, blockstate1);
		}
	}
}