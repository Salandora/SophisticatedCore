package net.p3pp3rf1y.sophisticatedcore.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemStackKey {
	// TODO: Check
	// private static final Field ATTACHMENTS = ObfuscationReflectionHelper.findField(AttachmentHolder.class, "attachments");
	private final ItemStack stack;

	private static final Map<ItemStack, ItemStackKey> CACHE = new ConcurrentHashMap<>();

	public static ItemStackKey of(ItemStack stack) {
		return CACHE.computeIfAbsent(stack, ItemStackKey::new);
	}

	private boolean hashInitialized = false;
	private int hash;

	private ItemStackKey(ItemStack stack) {
		this.stack = stack.copy();
		this.stack.setCount(1);
	}

	public static void clearCacheOnTickEnd() {
		CACHE.clear();
	}

	public ItemStack getStack() {
		return stack;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ItemStackKey that = (ItemStackKey) o;
		return canItemStacksStack(stack, that.stack);
	}

	public static boolean canItemStacksStack(ItemStack a, ItemStack b) {
		if (a.isEmpty() || a.getItem() != b.getItem() || a.hasTag() != b.hasTag()) {
			return false;
		}

		//noinspection DataFlowIssue
		return (!a.hasTag() || a.getTag().equals(b.getTag())); // TODO: && a.areAttachmentsCompatible(b);
	}

	public boolean hashCodeNotEquals(ItemStack otherStack) {
		return hashCode() != getHashCode(otherStack);
	}

	@Override
	public int hashCode() {
		if (!hashInitialized) {
			hashInitialized = true;
			hash = getHashCode(stack);
		}
		return hash;
	}

	public static int getHashCode(ItemStack stack) {
		int hash = stack.getItem().hashCode();
		if (stack.hasTag()) {
			//noinspection ConstantConditions - hasTag call makes sure getTag doesn't return null
			hash = hash * 31 + stack.getTag().hashCode();
		}
		/*if (stack.hasAttachments()) {
			Map<AttachmentType<?>, Object> attachments = getAttachments(stack);
			if (attachments != null) {
				hash = hash * 31 + attachments.hashCode();
			}
		}*/
		return hash;
	}

	/*@Nullable
	private static Map<AttachmentType<?>, Object> getAttachments(ItemStack stack) {
		try {
			return (Map<AttachmentType<?>, Object>) ATTACHMENTS.get(stack);
		} catch (IllegalAccessException e) {
			SophisticatedCore.LOGGER.error("Error getting attachments of stack ", e);
			return null;
		}
	}*/

	public static int getHashCode(ItemVariant resource) {
		return getHashCode(resource.toStack());
	}

	public boolean matches(ItemVariant resource) {
		return hashCode() == getHashCode(resource);
	}

	public boolean matches(ItemStack stack) {
		return hashCode() == getHashCode(stack);
	}

	public ItemStack stack() {
		return stack;
	}

	@Override
	public String toString() {
		return "ItemStackKey[stack=" + stack + ']';
	}

}
