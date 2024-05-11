package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransferRecipeMessage extends SimplePacketBase {
	private final Map<Integer, Integer> matchingItems;
	private final List<Integer> craftingSlotIndexes;
	private final List<Integer> inventorySlotIndexes;
	private final boolean maxTransfer;
	private final ResourceLocation recipeId;

	public TransferRecipeMessage(ResourceLocation recipeId, Map<Integer, Integer> matchingItems, List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes, boolean maxTransfer) {
		this.recipeId = recipeId;
		this.matchingItems = matchingItems;
		this.craftingSlotIndexes = craftingSlotIndexes;
		this.inventorySlotIndexes = inventorySlotIndexes;
		this.maxTransfer = maxTransfer;
	}

	public TransferRecipeMessage(FriendlyByteBuf buffer) {
		this(buffer.readResourceLocation(), readMap(buffer), readList(buffer), readList(buffer), buffer.readBoolean());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(this.recipeId);
		writeMap(buffer, this.matchingItems);
		writeList(buffer, this.craftingSlotIndexes);
		writeList(buffer, this.inventorySlotIndexes);
		buffer.writeBoolean(this.maxTransfer);
	}

	private static void writeMap(FriendlyByteBuf buffer, Map<Integer, Integer> map) {
		buffer.writeInt(map.size());
		map.forEach((key, value) -> {
			buffer.writeInt(key);
			buffer.writeInt(value);
		});
	}

	private static void writeList(FriendlyByteBuf buffer, List<Integer> list) {
		buffer.writeInt(list.size());
		list.forEach(buffer::writeInt);
	}

	private static Map<Integer, Integer> readMap(FriendlyByteBuf buffer) {
		Map<Integer, Integer> ret = new HashMap<>();
		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			ret.put(buffer.readInt(), buffer.readInt());
		}
		return ret;
	}

	private static List<Integer> readList(FriendlyByteBuf buffer) {
		List<Integer> ret = new ArrayList<>();
		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			ret.add(buffer.readInt());
		}
		return ret;
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer sender = context.getSender();
			if (sender == null) {
				return;
			}
			CraftingContainerRecipeTransferHandlerServer.setItems(sender, this.recipeId, this.matchingItems, this.craftingSlotIndexes, this.inventorySlotIndexes, this.maxTransfer);
		});
		return true;
	}

}
