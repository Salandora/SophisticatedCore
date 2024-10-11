package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransferRecipeMessage implements FabricPacket {
	public static final PacketType<TransferRecipeMessage> TYPE = PacketType.create(new ResourceLocation(SophisticatedCore.MOD_ID, "transfer_recipe"), TransferRecipeMessage::new);
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
		this(buffer.readResourceLocation(), buffer.readMap(FriendlyByteBuf::readInt, FriendlyByteBuf::readInt),
				buffer.readCollection(ArrayList::new, FriendlyByteBuf::readInt), buffer.readCollection(ArrayList::new, FriendlyByteBuf::readInt),
				buffer.readBoolean());
	}

	public void handle(ServerPlayer player, PacketSender responseSender) {
		CraftingContainerRecipeTransferHandlerServer.setItems(player, this.recipeId, this.matchingItems, this.craftingSlotIndexes, this.inventorySlotIndexes, this.maxTransfer);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeResourceLocation(recipeId);
		buffer.writeMap(matchingItems, FriendlyByteBuf::writeInt, FriendlyByteBuf::writeInt);
		buffer.writeCollection(craftingSlotIndexes, FriendlyByteBuf::writeInt);
		buffer.writeCollection(inventorySlotIndexes, FriendlyByteBuf::writeInt);
		buffer.writeBoolean(maxTransfer);
	}

	@Override
	public PacketType<?> getType() {
		return TYPE;
	}
}
