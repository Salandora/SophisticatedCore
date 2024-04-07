package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.extensions.client.gui.screens.inventory.SophisticatedAbstractContainerScreen;
import net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor.AbstractContainerScreenAccessor;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;

import java.util.function.Supplier;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements SophisticatedAbstractContainerScreen {
    @Unique
    private AbstractContainerScreen<?> getSelf() {
        return MixinHelper.cast(this);
    }

	@Unique
	private <T> T ifStorageScreenBase(Supplier<T> value, Supplier<T> elseValue) {
		return getSelf() instanceof StorageScreenBase ? value.get() : elseValue.get();
	}
	@Unique
	private void ifStorageScreenBase(Runnable value, Runnable elseValue) {
		if (getSelf() instanceof StorageScreenBase) {
			value.run();
		} else {
			elseValue.run();
		}
	}

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I"))
    private int sophisticatedcore$MenuSlotSize(NonNullList<Slot> instance) {
		return ifStorageScreenBase(() -> StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS, instance::size);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    private Object sophisticatedcore$MenuSlotGet(NonNullList<Slot> instance, int i) {
			return ifStorageScreenBase(() -> {
				StorageContainerMenuBase<?> menu = ((StorageScreenBase<? extends StorageContainerMenuBase<?>>) getSelf()).getMenu();
				return menu.getSlot(menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i);
			}, () -> instance.get(i));
    }

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.BEFORE))
	private void sophisticatedcore$resetHoveredSlot(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ifStorageScreenBase(() -> ((AbstractContainerScreenAccessor) this).setHoveredSlot(null), () -> {});
	}

	@Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hoveredSlot:Lnet/minecraft/world/inventory/Slot;", opcode = Opcodes.PUTFIELD, ordinal = 0))
	private void sophisticatedcore$patchHoveredSlot(AbstractContainerScreen<?> instance, Slot value) {
		ifStorageScreenBase(() -> {}, () -> ((AbstractContainerScreenAccessor) instance).setHoveredSlot(value));
	}
}
