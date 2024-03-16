package net.p3pp3rf1y.sophisticatedcore.compat.emi;

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
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class EmiFillRecipeC2SPacket extends SimplePacketBase {
    private final int syncId;
    private final int action;
    private final List<Integer> slots, crafting;
    private final int output;
    private final List<ItemStack> stacks;

    public EmiFillRecipeC2SPacket(AbstractContainerMenu handler, int action, List<Slot> slots, List<Slot> crafting, @Nullable Slot output, List<ItemStack> stacks) {
        this.syncId = handler.containerId;
        this.action = action;
        this.slots = slots.stream().map(s -> s == null ? -1 : s.index).toList();
        this.crafting = crafting.stream().map(s -> s == null ? -1 : s.index).toList();
        this.output = output == null ? -1 : output.index;
        this.stacks = stacks;
    }

    public EmiFillRecipeC2SPacket(FriendlyByteBuf buf) {
        syncId = buf.readInt();
        action = buf.readByte();
        slots = parseCompressedSlots(buf);
        crafting = Lists.newArrayList();
        int craftingSize = buf.readVarInt();
        for (int i = 0; i < craftingSize; i++) {
            int s = buf.readVarInt();
            crafting.add(s);
        }
        if (buf.readBoolean()) {
            output = buf.readVarInt();
        } else {
            output = -1;
        }
        int size = buf.readVarInt();
        stacks = Lists.newArrayList();
        for (int i = 0; i < size; i++) {
            stacks.add(buf.readItem());
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(syncId);
        buf.writeByte(action);
        writeCompressedSlots(slots, buf);
        buf.writeVarInt(crafting.size());
        for (Integer s : crafting) {
            buf.writeVarInt(s);
        }
        if (output != -1) {
            buf.writeBoolean(true);
            buf.writeVarInt(output);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeVarInt(stacks.size());
        for (ItemStack stack : stacks) {
            buf.writeItem(stack);
        }
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            if (slots == null || crafting == null) {
                EmiLog.error("Client requested fill but passed input and crafting slot information was invalid, aborting");
                return;
            }

            AbstractContainerMenu handler = sender.containerMenu;
            if (handler == null || handler.containerId != syncId || !(handler instanceof StorageContainerMenuBase<?> container)) {
                EmiLog.warn("Client requested fill but screen handler has changed, aborting");
                return;
            }

            List<Slot> slots = Lists.newArrayList();
            List<Slot> crafting = Lists.newArrayList();
            Slot output = null;
            for (int i : this.slots) {
                if (i < 0 || i >= container.getTotalSlotsNumber()) {
                    EmiLog.error("Client requested fill but passed input slots don't exist, aborting");
                    return;
                }
                slots.add(container.getSlot(i));
            }

            for (int i : this.crafting) {
                if (i >= 0 && i < container.getTotalSlotsNumber()) {
                    crafting.add(container.getSlot(i));
                } else {
                    crafting.add(null);
                }
            }
            if (this.output != -1) {
                if (this.output >= 0 && this.output < container.getTotalSlotsNumber()) {
                    output = container.getSlot(this.output);
                }
            }

            if (crafting.size() >= stacks.size()) {
                List<ItemStack> rubble = Lists.newArrayList();
				for (Slot s : crafting) {
					if (s != null && s.mayPickup(sender) && !s.getItem().isEmpty()) {
						rubble.add(s.getItem().copy());
						s.set(ItemStack.EMPTY);
					}
				}
                try {
                    for (int i = 0; i < stacks.size(); i++) {
                        ItemStack stack = stacks.get(i);
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
                                s.set(stack);
                            } else {
                                sender.getInventory().placeItemBackInInventory(stack);
                            }
                        }
                    }
                    if (output != null) {
                        if (action == 1) {
                            handler.clicked(output.getContainerSlot(), 0, ClickType.PICKUP, sender);
                        } else if (action == 2) {
                            handler.clicked(output.getContainerSlot(), 0, ClickType.QUICK_MOVE, sender);
                        }
                    }
                } finally {
                    for (ItemStack stack : rubble) {
                        sender.getInventory().placeItemBackInInventory(stack);
                    }
                }
            }
        });
        return true;
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
                    s.set(ItemStack.EMPTY);
                } else {
                    grabbed = amount;
                    st.setCount(st.getCount() - wanted);
                }
            }
        }
        return grabbed;
    }
}
