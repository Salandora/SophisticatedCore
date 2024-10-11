package net.p3pp3rf1y.sophisticatedcore.compat.craftingtweaks;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.blay09.mods.craftingtweaks.api.CraftingTweaksAPI;

public class CraftingTweaksCompat implements ICompat {
	@Override
	public void setup() {
		CraftingTweaksAPI.registerCraftingGridProvider(new CraftingUpgradeTweakProvider());
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			CraftingUpgradeTweakUIPart.register();
		}
	}
}
