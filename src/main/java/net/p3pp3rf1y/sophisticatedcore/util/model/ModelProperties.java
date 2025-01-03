package net.p3pp3rf1y.sophisticatedcore.util.model;

import net.p3pp3rf1y.sophisticatedcore.renderdata.RenderInfo;

import java.util.List;

public class ModelProperties {
	public static final ModelProperty<String> WOOD_NAME = new ModelProperty<>();
	public static final ModelProperty<Boolean> IS_PACKED = new ModelProperty<>();
	public static final ModelProperty<Boolean> SHOWS_LOCK = new ModelProperty<>();
	public static final ModelProperty<Boolean> SHOWS_TIER = new ModelProperty<>();
	public static final ModelProperty<Boolean> HAS_MAIN_COLOR = new ModelProperty<>();
	public static final ModelProperty<Boolean> HAS_ACCENT_COLOR = new ModelProperty<>();
	public static final ModelProperty<List<RenderInfo.DisplayItem>> DISPLAY_ITEMS = new ModelProperty<>();
	public static final ModelProperty<List<Integer>> INACCESSIBLE_SLOTS = new ModelProperty<>();
}
