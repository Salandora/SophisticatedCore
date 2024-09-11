package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.impl.attachment.AttachmentSerializingImpl;
import net.fabricmc.fabric.impl.attachment.AttachmentTargetImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.extensions.item.SophisticatedItemStack;

import java.util.IdentityHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements SophisticatedItemStack, AttachmentTargetImpl {
	@Shadow
	@Nullable
	private CompoundTag tag;

	@Shadow
	public abstract CompoundTag getOrCreateTag();

	@Unique
	@Nullable
	private IdentityHashMap<AttachmentType<?>, Object> fabric_dataAttachments = null;

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getAttached(AttachmentType<T> type) {
		return fabric_dataAttachments == null ? null : (T) fabric_dataAttachments.get(type);
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T setAttached(AttachmentType<T> type, @Nullable T value) {
		if (value == null) {
			if (fabric_dataAttachments == null) {
				return null;
			}

			T removed = (T) fabric_dataAttachments.remove(type);

			if (fabric_dataAttachments.isEmpty()) {
				fabric_dataAttachments = null;
			}

			return removed;
		} else {
			if (fabric_dataAttachments == null) {
				fabric_dataAttachments = new IdentityHashMap<>();
			}

			return (T) fabric_dataAttachments.put(type, value);
		}
	}

	@Override
	public boolean hasAttached(AttachmentType<?> type) {
		return fabric_dataAttachments != null && fabric_dataAttachments.containsKey(type);
	}

	@Override
	public void fabric_writeAttachmentsToNbt(CompoundTag nbt) {
		AttachmentSerializingImpl.serializeAttachmentData(nbt, fabric_dataAttachments);
	}

	@Override
	public void fabric_readAttachmentsFromNbt(CompoundTag nbt) {
		fabric_dataAttachments = AttachmentSerializingImpl.deserializeAttachmentData(nbt);
	}

	@Override
	public boolean fabric_hasPersistentAttachments() {
		return AttachmentSerializingImpl.hasPersistentAttachments(fabric_dataAttachments);
	}

	@Override
	public Map<AttachmentType<?>, ?> fabric_getAttachments() {
		return fabric_dataAttachments;
	}


	@Redirect(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getCompound(Ljava/lang/String;)Lnet/minecraft/nbt/CompoundTag;"))
	private CompoundTag sophisticatedCore$readAttachments(CompoundTag instance, String key) {
		CompoundTag tag = instance.getCompound(key);
		this.fabric_readAttachmentsFromNbt(tag);
		tag.remove(AttachmentTarget.NBT_ATTACHMENT_KEY);
		return tag;
	}

	@Inject(method = "save", at = @At(value = "HEAD"))
	private void sophisticatedcore$saveAttachments(CompoundTag compoundTag, CallbackInfoReturnable<CompoundTag> cir) {
		if (fabric_dataAttachments != null && !fabric_dataAttachments.isEmpty()) {
			this.fabric_writeAttachmentsToNbt(this.getOrCreateTag());
		}
	}

	@Inject(method = "copy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setPopTime(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
	private void sophisticatedcore$copyAttachments(CallbackInfoReturnable<ItemStack> cir, ItemStack itemStack) {
		AttachmentTargetImpl.transfer((AttachmentTarget) this, itemStack, false);
	}
}
