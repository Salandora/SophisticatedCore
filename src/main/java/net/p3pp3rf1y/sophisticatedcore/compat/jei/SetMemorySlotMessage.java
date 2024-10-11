package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;
import net.p3pp3rf1y.sophisticatedcore.settings.memory.MemorySettingsCategory;

public class SetMemorySlotMessage extends SimplePacketBase {
	private final ItemStack stack;
	private final int slotNumber;

	public SetMemorySlotMessage(ItemStack stack, int slotNumber) {
		this.stack = stack;
		this.slotNumber = slotNumber;
	}

	public SetMemorySlotMessage(FriendlyByteBuf buffer) {
		this(buffer.readItem(), buffer.readShort());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeItem(stack);
		buffer.writeShort(slotNumber);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null || !(sender.containerMenu instanceof SettingsContainerMenu<?> settingsContainerMenu)) {
				return;
			}
			IStorageWrapper storageWrapper = settingsContainerMenu.getStorageWrapper();
			storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).setFilter(slotNumber, stack);
			storageWrapper.getInventoryHandler().onSlotFilterChanged(slotNumber);
			settingsContainerMenu.sendAdditionalSlotInfo();
		});
		return true;
	}

}
