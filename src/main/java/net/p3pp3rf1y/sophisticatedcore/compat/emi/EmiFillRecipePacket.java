package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import com.google.common.collect.Lists;
import dev.emi.emi.runtime.EmiLog;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import java.util.List;

public record EmiFillRecipePacket(int syncId, int action, List<Integer> slots, List<Integer> crafting, int output, List<ItemStack> stacks) implements CustomPacketPayload {
	public static final Type<EmiFillRecipePacket> TYPE = new Type<>(SophisticatedCore.getRL("emi_fill_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EmiFillRecipePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, EmiFillRecipePacket::syncId,
			ByteBufCodecs.INT, EmiFillRecipePacket::action,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()), EmiFillRecipePacket::slots,
			ByteBufCodecs.INT.apply(ByteBufCodecs.list()), EmiFillRecipePacket::crafting,
			ByteBufCodecs.INT, EmiFillRecipePacket::output,
			ItemStack.OPTIONAL_LIST_STREAM_CODEC, EmiFillRecipePacket::stacks,
			EmiFillRecipePacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(EmiFillRecipePacket payload, ServerPlayNetworking.Context context) {
		if (payload.slots == null || payload.crafting == null) {
			EmiLog.error("Client requested fill but passed input and crafting slot information was invalid, aborting");
			return;
		}

		ServerPlayer player = context.player();
		AbstractContainerMenu handler = player.containerMenu;
		if (handler == null || handler.containerId != payload.syncId || !(handler instanceof StorageContainerMenuBase<?> container)) {
			EmiLog.warn("Client requested fill but screen handler has changed, aborting");
			return;
		}

		List<Slot> slots = Lists.newArrayList();
		List<Slot> crafting = Lists.newArrayList();
		Slot output = null;
		for (int i : payload.slots) {
			if (i < 0 || i >= container.getTotalSlotsNumber()) {
				EmiLog.error("Client requested fill but passed input slots don't exist, aborting");
				return;
			}
			slots.add(container.getSlot(i));
		}

		for (int i : payload.crafting) {
			if (i >= 0 && i < container.getTotalSlotsNumber()) {
				crafting.add(container.getSlot(i));
			} else {
				crafting.add(null);
			}
		}
		if (payload.output != -1) {
			if (payload.output >= 0 && payload.output < container.getTotalSlotsNumber()) {
				output = container.getSlot(payload.output);
			}
		}

		if (crafting.size() >= payload.stacks.size()) {
			List<ItemStack> rubble = Lists.newArrayList();
			for (Slot s : crafting) {
				if (s != null && s.mayPickup(player) && !s.getItem().isEmpty()) {
					rubble.add(s.getItem().copy());
					s.setByPlayer(ItemStack.EMPTY);
				}
			}
			try {
				for (int i = 0; i < payload.stacks.size(); i++) {
					ItemStack stack = payload.stacks.get(i);
					if (stack.isEmpty()) {
						continue;
					}
					int gotten = grabMatching(player, slots, rubble, crafting, stack);
					if (gotten != stack.getCount()) {
						if (gotten > 0) {
							stack.setCount(gotten);
							player.getInventory().placeItemBackInInventory(stack);
						}
						return;
					} else {
						Slot s = crafting.get(i);
						if (s != null && s.mayPlace(stack) && stack.getCount() <= s.getMaxStackSize()) {
							s.setByPlayer(stack);
						} else {
							player.getInventory().placeItemBackInInventory(stack);
						}
					}
				}
				if (output != null) {
					if (payload.action == 1) {
						handler.clicked(output.getContainerSlot(), 0, ClickType.PICKUP, player);
					} else if (payload.action == 2) {
						handler.clicked(output.getContainerSlot(), 0, ClickType.QUICK_MOVE, player);
					}
				}
			} finally {
				for (ItemStack stack : rubble) {
					player.getInventory().placeItemBackInInventory(stack);
				}
			}
		}
    }

    private static int grabMatching(Player player, List<Slot> slots, List<ItemStack> rubble, List<Slot> crafting, ItemStack stack) {
        int amount = stack.getCount();
        int grabbed = 0;
        for (int i = 0; i < rubble.size(); i++) {
            if (grabbed >= amount) {
                return grabbed;
            }
            ItemStack r = rubble.get(i);
            if (ItemStack.isSameItemSameComponents(stack, r)) {
                int wanted = amount - grabbed;
                if (r.getCount() <= wanted) {
                    grabbed += r.getCount();
                    rubble.remove(i);
                    i--;
                } else {
                    grabbed = amount;
                    r.setCount(r.getCount() - wanted);
                }
            }
        }
        for (Slot s : slots) {
            if (grabbed >= amount) {
                return grabbed;
            }
            if (crafting.contains(s) || !s.mayPickup(player)) {
                continue;
            }
            ItemStack st = s.getItem();
            if (ItemStack.isSameItemSameComponents(stack, st)) {
                int wanted = amount - grabbed;
                if (st.getCount() <= wanted) {
                    grabbed += st.getCount();
                    s.setByPlayer(ItemStack.EMPTY);
                } else {
                    grabbed = amount;
                    st.setCount(st.getCount() - wanted);
					s.setByPlayer(st);
                }
            }
        }
        return grabbed;
    }
}
