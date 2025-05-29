package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.extensions.client.gui.screens.inventory.SophisticatedAbstractContainerScreen;
import net.p3pp3rf1y.sophisticatedcore.util.MixinHelper;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen implements SophisticatedAbstractContainerScreen {
	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	@Shadow protected abstract void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);

	@Shadow
	@Nullable
	public Slot hoveredSlot;

	@Shadow protected int leftPos;

	@Shadow protected int topPos;

	@Shadow protected int imageWidth;

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

	@Unique
	private <T> T ifSettingsScreen(Supplier<T> value, Supplier<T> elseValue) {
		return getSelf() instanceof SettingsScreen ? value.get() : elseValue.get();
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
		ifStorageScreenBase(() -> hoveredSlot = null, () -> {});
	}

	@Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hoveredSlot:Lnet/minecraft/world/inventory/Slot;", opcode = Opcodes.PUTFIELD, ordinal = 0))
	private void sophisticatedcore$patchHoveredSlot(AbstractContainerScreen<?> instance, Slot value) {
		ifStorageScreenBase(() -> {}, () -> hoveredSlot = value);
	}

	// Fix for Blur+ fix
	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private boolean sophisticatedCore$noSuperRender(Screen instance, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		boolean allowed;

		allowed = ifStorageScreenBase(() -> {
			renderBg(guiGraphics, partialTick, mouseX, mouseY);
			return false;
		}, () -> true);

		allowed &= ifSettingsScreen(() -> {
			renderBg(guiGraphics, partialTick, mouseX, mouseY);
			return false;
		}, () -> true);

		return allowed;
	}

	@Override
	public int sophisticatedCore_getXSize() {
		return imageWidth;
	}

	@Override
	public int sophisticatedCore_getGuiLeft() {
		return leftPos;
	}

	@Override
	public int sophisticatedCore_getGuiTop() {
		return topPos;
	}

	@Override
	public Slot sophisticatedCore_getSlotUnderMouse() {
		return hoveredSlot;
	}

	@Override
	public void sophisticatedCore_superRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.render(guiGraphics, mouseX, mouseY, partialTick);
	}
}
