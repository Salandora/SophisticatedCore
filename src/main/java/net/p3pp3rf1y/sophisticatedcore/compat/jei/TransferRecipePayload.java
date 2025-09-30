package net.p3pp3rf1y.sophisticatedcore.compat.jei;

import com.github.salandora.sophisticatedlibrary.network.handling.IPayloadContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record TransferRecipePayload(ResourceLocation recipeId, ResourceLocation recipeTypeId, Map<Integer, Integer> matchingItems,
									List<Integer> craftingSlotIndexes, List<Integer> inventorySlotIndexes,
									boolean maxTransfer) implements CustomPacketPayload {
	public static final Type<TransferRecipePayload> TYPE = new Type<>(SophisticatedCore.getRL("transfer_recipe"));
	public static final StreamCodec<ByteBuf, TransferRecipePayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC,
			TransferRecipePayload::recipeId,
			ResourceLocation.STREAM_CODEC,
			TransferRecipePayload::recipeTypeId,
			StreamCodecHelper.ofMap(ByteBufCodecs.INT, ByteBufCodecs.INT, HashMap::new),
			TransferRecipePayload::matchingItems,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
			TransferRecipePayload::craftingSlotIndexes,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
			TransferRecipePayload::inventorySlotIndexes,
			ByteBufCodecs.BOOL,
			TransferRecipePayload::maxTransfer,
			TransferRecipePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(TransferRecipePayload payload, IPayloadContext context) {
		RecipeType<?> recipeType = BuiltInRegistries.RECIPE_TYPE.get(payload.recipeTypeId);
		if (recipeType == null) {
			return;
		}

		CraftingContainerRecipeTransferHandlerServer.setItems(context.player(), payload.recipeId, recipeType, payload.matchingItems,
				payload.craftingSlotIndexes, payload.inventorySlotIndexes, payload.maxTransfer);
	}
}
