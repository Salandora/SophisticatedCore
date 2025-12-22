package net.p3pp3rf1y.sophisticatedcore.compat.emi;

import com.github.salandora.sophisticatedlibrary.network.api.v0.NetworkEvent;
import com.google.common.collect.Lists;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class EmiFillRecipeC2SPacket {
    private final int syncId;
    private final int action;
    private final List<Integer> slots, crafting;
    private final int output;
    private final List<ItemStack> stacks;

    public EmiFillRecipeC2SPacket(AbstractContainerMenu handler, int action, List<Slot> slots, List<Slot> crafting, @Nullable Slot output, List<ItemStack> stacks) {
		this(
				handler.containerId,
				action,
				slots.stream().map(s -> s == null ? -1 : s.index).toList(),
				crafting.stream().map(s -> s == null ? -1 : s.index).toList(),
				output == null ? -1 : output.index,
				stacks
		);
	}

	private EmiFillRecipeC2SPacket(int syncId, int action, List<Integer> slots, List<Integer> crafting, int output, List<ItemStack> stacks) {
        this.syncId = syncId;
        this.action = action;
        this.slots = slots;
        this.crafting = crafting;
        this.output = output;
        this.stacks = stacks;
    }

    public static void encode(EmiFillRecipeC2SPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.syncId);
        buf.writeInt(msg.action);
        writeCompressedSlots(msg.slots, buf);
        buf.writeVarInt(msg.crafting.size());
        for (Integer s : msg.crafting) {
            buf.writeVarInt(s);
        }
        if (msg.output != -1) {
            buf.writeBoolean(true);
            buf.writeVarInt(msg.output);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeVarInt(msg.stacks.size());
        for (ItemStack stack : msg.stacks) {
            buf.writeItem(stack);
        }
    }

	public static EmiFillRecipeC2SPacket decode(FriendlyByteBuf buf) {
		return new EmiFillRecipeC2SPacket(
			buf.readInt(),
			buf.readInt(),
			parseCompressedSlots(buf),
			readSlots(buf),
			buf.readBoolean() ? buf.readVarInt() : -1,
			readStacks(buf)
		);
	}

	public static void onMessage(EmiFillRecipeC2SPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> handleMessage(context.getSender(), msg));
		context.setPacketHandled(true);
	}

    private static void handleMessage(@Nullable ServerPlayer sender, EmiFillRecipeC2SPacket msg) {
		if (sender == null) {
			return;
		}

		if (msg.slots == null || msg.crafting == null) {
			EmiLog.error("Client requested fill but passed input and crafting slot information was invalid, aborting");
			return;
		}

		AbstractContainerMenu handler = sender.containerMenu;
		if (handler == null || handler.containerId != msg.syncId || !(handler instanceof StorageContainerMenuBase<?> container)) {
			EmiLog.warn("Client requested fill but screen handler has changed, aborting");
			return;
		}

		List<Slot> slots = Lists.newArrayList();
		List<Slot> crafting = Lists.newArrayList();
		Slot output = null;
		for (int i : msg.slots) {
			if (i < 0 || i >= container.getTotalSlotsNumber()) {
				EmiLog.error("Client requested fill but passed input slots don't exist, aborting");
				return;
			}
			slots.add(container.getSlot(i));
		}

		for (int i : msg.crafting) {
			if (i >= 0 && i < container.getTotalSlotsNumber()) {
				crafting.add(container.getSlot(i));
			} else {
				crafting.add(null);
			}
		}
		if (msg.output != -1) {
			if (msg.output >= 0 && msg.output < container.getTotalSlotsNumber()) {
				output = container.getSlot(msg.output);
			}
		}

		if (crafting.size() >= msg.stacks.size()) {
			List<ItemStack> rubble = Lists.newArrayList();
			for (Slot s : crafting) {
				if (s != null && s.mayPickup(sender) && !s.getItem().isEmpty()) {
					rubble.add(s.getItem().copy());
					s.setByPlayer(ItemStack.EMPTY);
				}
			}
			try {
				for (int i = 0; i < msg.stacks.size(); i++) {
					ItemStack stack = msg.stacks.get(i);
					if (stack.isEmpty()) {
						continue;
					}
					int gotten = grabMatching(sender, slots, rubble, crafting, stack);
					if (gotten != stack.getCount()) {
						if (gotten > 0) {
							stack.setCount(gotten);
							sender.getInventory().placeItemBackInInventory(stack);
						}
						return;
					} else {
						Slot s = crafting.get(i);
						if (s != null && s.mayPlace(stack) && stack.getCount() <= s.getMaxStackSize()) {
							s.setByPlayer(stack);
						} else {
							sender.getInventory().placeItemBackInInventory(stack);
						}
					}
				}
				if (output != null) {
					if (msg.action == 1) {
						handler.clicked(output.index, 0, ClickType.PICKUP, sender);
					} else if (msg.action == 2) {
						handler.clicked(output.index, 0, ClickType.QUICK_MOVE, sender);
					}
				}
			} finally {
				for (ItemStack stack : rubble) {
					sender.getInventory().placeItemBackInInventory(stack);
				}
			}
		}
    }

    private static List<Integer> parseCompressedSlots(FriendlyByteBuf buf) {
        List<Integer> list = Lists.newArrayList();
        int amount = buf.readVarInt();
        for (int i = 0; i < amount; i++) {
            int low = buf.readVarInt();
            int high = buf.readVarInt();
            if (low < 0) {
                return null;
            }
            for (int j = low; j <= high; j++) {
                list.add(j);
            }
        }
        return list;
    }

    private static void writeCompressedSlots(List<Integer> list, FriendlyByteBuf buf) {
        List<Consumer<FriendlyByteBuf>> postWrite = Lists.newArrayList();
        int groups = 0;
        int i = 0;
        while (i < list.size()) {
            groups++;
            int start = i;
            int startValue = list.get(start);
            while (i < list.size() && i - start == list.get(i) - startValue) {
                i++;
            }
            int end = i - 1;
            postWrite.add(b -> {
                b.writeVarInt(startValue);
                b.writeVarInt(list.get(end));
            });
        }
        buf.writeVarInt(groups);
        for (Consumer<FriendlyByteBuf> consumer : postWrite) {
            consumer.accept(buf);
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
            if (ItemStack.isSameItemSameTags(stack, r)) {
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
            if (ItemStack.isSameItemSameTags(stack, st)) {
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

	private static List<Integer> readSlots(FriendlyByteBuf buf) {
		return readList(FriendlyByteBuf::readVarInt, buf);
	}

	private static List<ItemStack> readStacks(FriendlyByteBuf buf) {
		return readList(FriendlyByteBuf::readItem, buf);
	}

	private static <T> List<T> readList(Function<FriendlyByteBuf, T> readCommand, FriendlyByteBuf buf) {
		List<T> list = Lists.newArrayList();
		int size = buf.readVarInt();
		for (int i = 0; i < size; i++) {
			list.add(readCommand.apply(buf));
		}
		return list;
	}
}
