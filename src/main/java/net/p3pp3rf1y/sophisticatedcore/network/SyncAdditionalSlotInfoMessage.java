
package net.p3pp3rf1y.sophisticatedcore.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.p3pp3rf1y.sophisticatedcore.common.gui.IAdditionalSlotInfoMenu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SyncAdditionalSlotInfoMessage extends SimplePacketBase {
	private final Set<Integer> inaccessibleSlots;
	private final Map<Integer, Integer> slotLimitOverrides;
	private final Map<Integer, Item> slotFilterItems;
	public SyncAdditionalSlotInfoMessage(Set<Integer> inaccessibleSlots, Map<Integer, Integer> slotLimitOverrides, Map<Integer, Item> slotFilterItems) {
		this.inaccessibleSlots = inaccessibleSlots;
		this.slotLimitOverrides = slotLimitOverrides;
		this.slotFilterItems = slotFilterItems;
	}

	public SyncAdditionalSlotInfoMessage(FriendlyByteBuf buffer) {
		this(Arrays.stream(buffer.readVarIntArray()).boxed().collect(Collectors.toSet()), deserializeSlotLimitOverrides(buffer), deserializeSlotFilterItems(buffer));
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarIntArray(inaccessibleSlots.stream().mapToInt(i->i).toArray());
		serializeSlotLimitOverrides(buffer, slotLimitOverrides);
		serializeSlotFilterItems(buffer, slotFilterItems);
	}

	private static void serializeSlotFilterItems(FriendlyByteBuf buffer, Map<Integer, Item> slotFilterItems) {
		buffer.writeInt(slotFilterItems.size());

		slotFilterItems.forEach((slot, item) -> {
			buffer.writeInt(slot);
			buffer.writeInt(Item.getId(item));
		});
	}

	private static Map<Integer, Item> deserializeSlotFilterItems(FriendlyByteBuf buffer) {
		Map<Integer, Item> ret = new HashMap<>();
		int size = buffer.readInt();

		for (int i = 0; i < size; i++) {
			ret.put(buffer.readInt(), Item.byId(buffer.readInt()));
		}

		return ret;
	}

	private static Map<Integer, Integer> deserializeSlotLimitOverrides(FriendlyByteBuf buffer) {
		Map<Integer, Integer> ret = new HashMap<>();

		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			ret.put(buffer.readInt(), buffer.readInt());
		}

		return ret;
	}

	private static void serializeSlotLimitOverrides(FriendlyByteBuf buffer, Map<Integer, Integer> slotLimitOverrides) {
		buffer.writeInt(slotLimitOverrides.size());
		slotLimitOverrides.forEach((slot, limit) -> {
			buffer.writeInt(slot);
			buffer.writeInt(limit);
		});
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Player player = context.getClientPlayer();
			if (player == null || !(player.containerMenu instanceof IAdditionalSlotInfoMenu menu)) {
				return;
			}
			menu.updateAdditionalSlotInfo(inaccessibleSlots, slotLimitOverrides, slotFilterItems);
		});
		return true;
	}
}
