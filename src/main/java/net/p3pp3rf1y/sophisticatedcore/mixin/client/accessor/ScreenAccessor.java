package net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccessor {
	@Accessor
	Font getFont();

	@Invoker
	GuiEventListener callAddRenderableWidget(GuiEventListener widget);
}
