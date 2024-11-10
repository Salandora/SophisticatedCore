package net.p3pp3rf1y.sophisticatedcore.init;

import net.p3pp3rf1y.sophisticatedcore.compat.CompatInfo;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatRegistry;
import net.p3pp3rf1y.sophisticatedcore.compat.audioplayer.AudioPlayerCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks.CraftingTweaksCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.emi.EmiCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.jei.JeiCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.litematica.LitematicaCompat;
import net.p3pp3rf1y.sophisticatedcore.compat.rei.REICompat;

import static net.p3pp3rf1y.sophisticatedcore.SophisticatedCore.MOD_ID;

public class ModCompat {
	private ModCompat() {
	}

	public static void register() {
		CompatRegistry registry = CompatRegistry.getRegistry(MOD_ID);
		registry.registerCompat(new CompatInfo(CompatModIds.EMI, null), () -> new EmiCompat());
		registry.registerCompat(new CompatInfo(CompatModIds.JEI, null), () -> new JeiCompat());
		registry.registerCompat(new CompatInfo(CompatModIds.REI, null), () -> new REICompat());
		registry.registerCompat(new CompatInfo(CompatModIds.CRAFTING_TWEAKS, null), () -> new CraftingTweaksCompat());
		registry.registerCompat(new CompatInfo(CompatModIds.LITEMATICA, null), () -> new LitematicaCompat());
		registry.registerCompat(new CompatInfo(CompatModIds.AUDIOPLAYER, null), () -> new AudioPlayerCompat());

		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.INVENTORY_SORTER, null), () -> modBus -> new InventorySorterCompat());
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.ITEM_BORDERS, null), () -> ItemBordersCompat::new);
		//CompatRegistry.registerCompat(new CompatInfo(CompatModIds.QUARK, null), QuarkCompat::new); //TODO readd quark compat
	}
}
