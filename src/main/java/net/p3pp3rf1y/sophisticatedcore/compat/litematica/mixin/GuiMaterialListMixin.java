package net.p3pp3rf1y.sophisticatedcore.compat.litematica.mixin;

import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaHelper;
import fi.dy.masa.litematica.gui.GuiMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.malilib.gui.GuiListBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMaterialList.class)
public abstract class GuiMaterialListMixin extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList> {
	protected GuiMaterialListMixin(int listX, int listY) {
		super(listX, listY);
	}

	@Inject(method = "<init>", at = @At("RETURN"), remap = false)
	private void sophisticatedCore$init(MaterialListBase materialList, CallbackInfo ci) {
		LitematicaHelper.requestContents(materialList);
	}
}
