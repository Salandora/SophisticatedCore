package net.p3pp3rf1y.sophisticatedcore.mixin.client.accessor;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface EditBoxAccessor {
	@Accessor
	boolean getIsEditable();
}
