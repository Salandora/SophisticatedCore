package net.p3pp3rf1y.sophisticatedcore.compat.rei;

import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessorRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.impl.common.transfer.InputSlotCrafter;
import me.shedaniel.rei.impl.common.transfer.NewInputSlotCrafter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.List;

public class REIMoveItemsPayload implements CustomPacketPayload {
	public static final Type<REIMoveItemsPayload> TYPE = new Type<>(SophisticatedCore.getRL("rei_move_items"));
	public static final StreamCodec<RegistryFriendlyByteBuf, REIMoveItemsPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL,
			(p) -> p.shiftDown,
			ByteBufCodecs.COMPOUND_TAG,
			(p) -> p.tag,
			REIMoveItemsPayload::new);

	private final boolean shiftDown;
	private final CompoundTag tag;

	@Environment(EnvType.CLIENT)
	public REIMoveItemsPayload(TransferHandler.Context context, boolean shiftDown, List<InputIngredient<ItemStack>> inputs, Iterable<SlotAccessor> inputSlots, Iterable<SlotAccessor> inventorySlots) {
		this.shiftDown = shiftDown;
		this.tag = save(context, inputs, inputSlots, inventorySlots);
	}

	public REIMoveItemsPayload(boolean shiftDown, CompoundTag tag) {
		this.shiftDown = shiftDown;
		this.tag = tag;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handlePayload(REIMoveItemsPayload payload, ServerPlayNetworking.Context context) {
		ServerPlayer player = context.player();
		AbstractContainerMenu container = player.containerMenu;
		try {
			boolean shift = payload.shiftDown;
			try {
				CompoundTag nbt = payload.tag;
				int version = nbt.getInt("Version");
				if (version != 1) throw new IllegalStateException("Server and client REI protocol version mismatch! Server: 1, Client: " + version);
				List<InputIngredient<ItemStack>> inputs = readInputs(nbt.getList("Inputs", Tag.TAG_COMPOUND));
				List<SlotAccessor> input = readSlots(container, player, nbt.getList("InputSlots", Tag.TAG_COMPOUND));
				List<SlotAccessor> inventory = readSlots(container, player, nbt.getList("InventorySlots", Tag.TAG_COMPOUND));
				NewInputSlotCrafter<AbstractContainerMenu, Container> crafter = new NewInputSlotCrafter<>(container, input, inventory, inputs) {
					@Override
					protected void fillInputSlot(SlotAccessor slot, ItemStack toBeTakenStack) {
						SlotAccessor takenSlot = this.takeInventoryStack(toBeTakenStack);
						if (takenSlot != null) {
							ItemStack takenStack = takenSlot.getItemStack().copy();
							if (!takenStack.isEmpty()) {
								if (takenStack.getCount() > 1) {
									takenSlot.takeStack(1);
								} else {
									takenSlot.setItemStack(ItemStack.EMPTY);
								}

								takenStack.setCount(1);
								if (!slot.canPlace(takenStack)) {
									return;
								}

								if (slot.getItemStack().isEmpty()) {
									slot.setItemStack(takenStack);
								} else {
									ItemStack stack = slot.getItemStack();
									stack.grow(1);
									slot.setItemStack(stack);
								}
							}
						}
					}
				};
				crafter.fillInputSlots(player, shift);
			} catch (InputSlotCrafter.NotEnoughMaterialsException e) {
				if (!(container instanceof RecipeBookMenu)) {
					return;
				}
			} catch (IllegalStateException e) {
				player.sendSystemMessage(Component.translatable(e.getMessage()).withStyle(ChatFormatting.RED));
			} catch (Exception e) {
				player.sendSystemMessage(Component.translatable(e.getMessage()).withStyle(ChatFormatting.RED));
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Environment(EnvType.CLIENT)
	private static CompoundTag save(TransferHandler.Context context, List<InputIngredient<ItemStack>> inputs, Iterable<SlotAccessor> inputSlots, Iterable<SlotAccessor> inventorySlots) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Version", 1);
		tag.put("Inputs", saveInputs(inputs));
		tag.put("InventorySlots", saveSlots(context,inventorySlots));
		tag.put("InputSlots", saveSlots(context, inputSlots));
		return tag;
	}

	@Environment(EnvType.CLIENT)
	private static Tag saveSlots(TransferHandler.Context context, Iterable<SlotAccessor> slots) {
		ListTag tag = new ListTag();

		for (SlotAccessor slot : slots) {
			tag.add(SlotAccessorRegistry.getInstance().save(context.getMenu(), context.getMinecraft().player, slot));
		}

		return tag;
	}

	@Environment(EnvType.CLIENT)
	private static Tag saveInputs(List<InputIngredient<ItemStack>> inputs) {
		ListTag tag = new ListTag();

		for (InputIngredient<ItemStack> input : inputs) {
			CompoundTag innerTag = new CompoundTag();
			innerTag.put("Ingredient", EntryIngredients.ofItemStacks(input.get()).saveIngredient());
			innerTag.putInt("Index", input.getIndex());
			tag.add(innerTag);
		}

		return tag;
	}

	private static List<SlotAccessor> readSlots(AbstractContainerMenu menu, Player player, ListTag tag) {
		List<SlotAccessor> slots = new ArrayList<>();
		for (Tag t : tag) {
			slots.add(SlotAccessorRegistry.getInstance().read(menu, player, (CompoundTag) t));
		}
		return slots;
	}

	private static List<InputIngredient<ItemStack>> readInputs(ListTag tag) {
		List<InputIngredient<ItemStack>> inputs = new ArrayList<>();
		for (Tag t : tag) {
			CompoundTag compoundTag = (CompoundTag) t;
			InputIngredient<EntryStack<?>> stacks = InputIngredient.of(compoundTag.getInt("Index"), EntryIngredient.read(compoundTag.getList("Ingredient", Tag.TAG_COMPOUND)));
			inputs.add(InputIngredient.withType(stacks, VanillaEntryTypes.ITEM));
		}
		return inputs;
	}
}
