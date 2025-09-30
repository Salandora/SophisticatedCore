package net.p3pp3rf1y.sophisticatedcore.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsScreen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
	@Shadow protected abstract void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY);

	@Shadow
	@Nullable
	public Slot hoveredSlot;

	@Shadow
	public abstract T getMenu();

	@Unique
	private boolean sophisticatedCore$isStorageScreen;
	@Unique
	private boolean sophisticatedCore$isSettingsScreen;

	protected AbstractContainerScreenMixin(Component title) {
		super(title);
	}

	@Inject(
			method = "render",
			at = @At("HEAD")
	)
	private void sophisticatedCore$renderHead(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		sophisticatedCore$isStorageScreen = (Object) this instanceof StorageScreenBase;
		sophisticatedCore$isSettingsScreen = (Object) this instanceof SettingsScreen;
	}

	@Inject(
			method = "render",
			at = @At("TAIL")
	)
	private void sophisticatedCore$renderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		sophisticatedCore$isStorageScreen = false;
		sophisticatedCore$isSettingsScreen = false;
	}

	@WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/core/NonNullList;size()I"
			)
	)
    private int sophisticatedCore$MenuSlotSize(NonNullList<Slot> instance, Operation<Integer> original) {
		if (sophisticatedCore$isStorageScreen) {
			return StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS;
		}
		return original.call(instance);
	}

    @WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"
			)
	)
    private Object sophisticatedCore$MenuSlotGet(NonNullList<Slot> instance, int i, Operation<Slot> original) {
		if (sophisticatedCore$isStorageScreen) {
			StorageContainerMenuBase<?> menu = (StorageContainerMenuBase<?>) getMenu();
			return menu.getSlot(menu.getInventorySlotsSize() - StorageContainerMenuBase.NUMBER_OF_PLAYER_SLOTS + i);
		}

		return original.call(instance, i);
    }

	@WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
			)
	)
	private void sophisticatedCore$wrapRenderCall(AbstractContainerScreen<?> instance, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
		// we run the original here and cancel it in its own mixin after all other mixins have been processed
		original.call(instance, guiGraphics, mouseX, mouseY, partialTick);
		if (sophisticatedCore$isStorageScreen || sophisticatedCore$isSettingsScreen) {
			renderBg(guiGraphics, partialTick, mouseX, mouseY);
		}
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableDepthTest()V",
					shift = At.Shift.AFTER
			)
	)
	private void sophisticatedCore$renderRenderables(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (sophisticatedCore$isStorageScreen || sophisticatedCore$isSettingsScreen) {
			hoveredSlot = null;

			for (Renderable widget : renderables) {
				widget.render(guiGraphics, mouseX, mouseY, partialTick);
			}
		}
	}

	@WrapWithCondition(
			method = "render",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;hoveredSlot:Lnet/minecraft/world/inventory/Slot;",
					opcode = Opcodes.PUTFIELD,
					ordinal = 0
			)
	)
	private boolean sophisticatedCore$bypassHoveredSlotReset(AbstractContainerScreen<T> instance, Slot newSlot) {
		return !sophisticatedCore$isStorageScreen;
	}
}
