package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import me.shedaniel.rei.api.common.plugins.REIServerPlugin;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessorRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import org.jetbrains.annotations.Nullable;

public class REIServerCompat implements REIServerPlugin {
	@Override
	public void registerSlotAccessors(SlotAccessorRegistry registry) {
		registry.register(ResourceLocation.fromNamespaceAndPath(SophisticatedCore.MOD_ID, "storage"),
				slotAccessor -> slotAccessor instanceof SophisticatedSlotAccessor,
				new SlotAccessorRegistry.Serializer() {
					@Override
					public SlotAccessor read(AbstractContainerMenu menu, Player player, CompoundTag tag) {
						int slot = tag.getInt("Slot");
						return new SophisticatedSlotAccessor(menu.getSlot(slot));
					}

					@Override
					@Nullable
					public CompoundTag save(AbstractContainerMenu menu, Player player, SlotAccessor accessor) {
						if (!(accessor instanceof SophisticatedSlotAccessor sophisticatedSlotAccessor)) {
							throw new IllegalArgumentException("Cannot save non-sophisticated slot accessor!");
						}
						CompoundTag tag = new CompoundTag();
						tag.putInt("Slot", sophisticatedSlotAccessor.getSlot().index);
						return tag;
					}
				});
	}
}
