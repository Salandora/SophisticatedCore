package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.p3pp3rf1y.porting_lib.transfer.items.ItemStackHandler;
import net.p3pp3rf1y.porting_lib.transfer.items.SlottedStackStorage;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class JukeboxUpgradeItem extends UpgradeItemBase<JukeboxUpgradeItem.Wrapper> {
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);

	public JukeboxUpgradeItem(CreativeModeTab itemGroup, IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
		super(itemGroup, upgradeTypeLimitConfig);
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, JukeboxUpgradeItem> implements ITickableUpgrade {
		private static final int KEEP_ALIVE_SEND_INTERVAL = 5;
		private final ItemStackHandler discInventory;
		private long lastKeepAliveSendTime = 0;
		private boolean isPlaying;

		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
			discInventory = new ItemStackHandler(1) {
				@Override
				protected void onContentsChanged(int slot) {
					super.onContentsChanged(slot);
					upgrade.addTagElement("discInventory", serializeNBT());
					save();
				}

				@Override
				public boolean isItemValid(int slot, ItemVariant resource) {
					return resource.getItem() instanceof RecordItem;
				}
			};
			NBTHelper.getCompound(upgrade, "discInventory").ifPresent(discInventory::deserializeNBT);
			isPlaying = NBTHelper.getBoolean(upgrade, "isPlaying").orElse(false);
		}

		public void setDisc(ItemStack disc) {
			discInventory.setStackInSlot(0, disc);
		}

		public ItemStack getDisc() {
			return discInventory.getStackInSlot(0);
		}

		public void play(Level world, BlockPos pos) {
			play(world, (serverWorld, storageUuid) ->
					ServerStorageSoundHandler.startPlayingDisc(serverWorld, pos, storageUuid, getDisc(), () -> setIsPlaying(false)));
		}

		public void play(LivingEntity entity) {
			play(entity.level, (world, storageUuid) ->
					ServerStorageSoundHandler.startPlayingDisc(world, entity.position(), storageUuid, entity.getId(), getDisc(), () -> setIsPlaying(false)));
		}

		private void play(Level world, BiConsumer<ServerLevel, UUID> play) {
			if (!(world instanceof ServerLevel) || getDisc().isEmpty()) {
				return;
			}
			storageWrapper.getContentsUuid().ifPresent(storageUuid -> play.accept((ServerLevel) world, storageUuid));
			setIsPlaying(true);
		}

		private void setIsPlaying(boolean playing) {
			isPlaying = playing;
			NBTHelper.setBoolean(upgrade, "isPlaying", playing);
			if (isPlaying) {
				storageWrapper.getRenderInfo().setUpgradeRenderData(JukeboxUpgradeRenderData.TYPE, new JukeboxUpgradeRenderData(true));
			} else {
				removeRenderData();
			}
			save();
		}

		private void removeRenderData() {
			storageWrapper.getRenderInfo().removeUpgradeRenderData(JukeboxUpgradeRenderData.TYPE);
		}

		public void stop(LivingEntity entity) {
			if (!(entity.level instanceof ServerLevel)) {
				return;
			}
			storageWrapper.getContentsUuid().ifPresent(storageUuid ->
					ServerStorageSoundHandler.stopPlayingDisc((ServerLevel) entity.level, entity.position(), storageUuid)
			);
			setIsPlaying(false);
		}

		public SlottedStackStorage getDiscInventory() {
			return discInventory;
		}

		@Override
		public void tick(@Nullable LivingEntity entity, Level world, BlockPos pos) {
			if (isPlaying && lastKeepAliveSendTime < world.getGameTime() - KEEP_ALIVE_SEND_INTERVAL) {
				storageWrapper.getContentsUuid().ifPresent(storageUuid ->
						ServerStorageSoundHandler.updateKeepAlive(storageUuid, world, entity != null ? entity.position() : Vec3.atCenterOf(pos), () -> setIsPlaying(false))
				);
				lastKeepAliveSendTime = world.getGameTime();
			}
		}

		public boolean isPlaying() {
			return isPlaying;
		}

		@Override
		public void onBeforeRemoved() {
			removeRenderData();
		}
	}
}
