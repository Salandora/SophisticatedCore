package net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.EditBox;

@Mixin(EditBox.class)
public interface EditBoxAccessor {
	@Accessor
	boolean getIsEditable();
}
