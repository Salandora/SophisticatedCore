package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.sophisticatedcore.util.Capabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
	@Shadow
	private Direction facing;

	@Inject(
			method = "ejectItems",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private static void sophisticatedCore_ejectItems(Level world, BlockPos pos, HopperBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
		Direction direction = ((HopperBlockEntityMixin) (Object) blockEntity).facing;
		Vec3 searchPos = new Vec3(pos.getX() + direction.getStepX() + 0.5, pos.getY() + direction.getStepY() + 0.5, pos.getZ() + direction.getStepZ() + 0.5);
		List<Entity> list = world.getEntities(
				(Entity)null,
				new AABB(
						searchPos.x() - (double)0.5F, searchPos.y() - (double)0.5F, searchPos.z() - (double)0.5F,
						searchPos.x() + (double)0.5F, searchPos.y() + (double)0.5F, searchPos.z() + (double)0.5F
				),
				EntitySelector.ENTITY_STILL_ALIVE
		);
		if (list.isEmpty()) {
			return;
		}

		Collections.shuffle(list);
		for (Entity entity : list) {
			Storage<ItemVariant> target = Capabilities.ItemHandler.ENTITY_AUTOMATION.find(entity, direction.getOpposite());
			if (target != null) {
				long moved = StorageUtil.move(
						InventoryStorage.of(blockEntity, direction),
						target,
						iv -> true,
						1,
						null
				);
				cir.setReturnValue(moved == 1);
				return;
			}
		}
	}

	@Inject(
			method = "suckInItems",
			at = @At(value = "HEAD"),
			cancellable = true
	)
	private static void sophisticatedCore_suckInItems(Level world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
		Vec3 searchPos = new Vec3(hopper.getLevelX(), hopper.getLevelY() + 1.0F, hopper.getLevelZ());
		List<Entity> list = world.getEntities(
				(Entity)null,
				new AABB(
						searchPos.x() - (double)0.5F, searchPos.y() - (double)0.5F, searchPos.z() - (double)0.5F,
						searchPos.x() + (double)0.5F, searchPos.y() + (double)0.5F, searchPos.z() + (double)0.5F
				),
				EntitySelector.ENTITY_STILL_ALIVE
		);
		if (list.isEmpty()) {
			return;
		}

		Collections.shuffle(list);
		for (Entity entity : list) {
			Storage<ItemVariant> source = Capabilities.ItemHandler.ENTITY_AUTOMATION.find(entity, Direction.DOWN);
			if (source != null) {
				long moved = StorageUtil.move(
						source,
						InventoryStorage.of(hopper, Direction.UP),
						iv -> true,
						1,
						null
				);
				cir.setReturnValue(moved == 1);
				return;
			}
		}
	}
}

