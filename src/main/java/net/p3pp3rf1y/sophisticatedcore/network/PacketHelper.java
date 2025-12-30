package net.p3pp3rf1y.sophisticatedcore.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PacketHelper {
	private PacketHelper() {}

	public static ItemStack readItemStack(FriendlyByteBuf packetBuffer) {
		if (!packetBuffer.readBoolean()) {
			return ItemStack.EMPTY;
		} else {
			int i = packetBuffer.readVarInt();
			int j = packetBuffer.readInt();
			ItemStack itemstack = new ItemStack(Item.byId(i), j);
			itemstack.setTag(packetBuffer.readNbt());
			return itemstack;
		}
	}

	public static void writeItemStack(ItemStack stack, FriendlyByteBuf packetBuffer) {
		if (stack.isEmpty()) {
			packetBuffer.writeBoolean(false);
		} else {
			packetBuffer.writeBoolean(true);
			Item item = stack.getItem();
			packetBuffer.writeVarInt(Item.getId(item));
			packetBuffer.writeInt(stack.getCount());
			CompoundTag compoundnbt = null;
			if (item.canBeDepleted() || item.shouldOverrideMultiplayerNbt()) {
				compoundnbt = stack.getTag();
			}

			packetBuffer.writeNbt(compoundnbt);
		}
	}
}
