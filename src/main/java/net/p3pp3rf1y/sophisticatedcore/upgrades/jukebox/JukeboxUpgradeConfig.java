package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraftforge.common.ForgeConfigSpec;

public class JukeboxUpgradeConfig {
	public final ForgeConfigSpec.IntValue numberOfSlots;
	public final ForgeConfigSpec.IntValue slotsInRow;

	public JukeboxUpgradeConfig(ForgeConfigSpec.Builder builder, String upgradeName, String path, int defaultNumberOfSlots) {
		builder.comment(upgradeName + " Settings").push(path);
		numberOfSlots = builder.comment("Number of slots for discs in jukebox upgrade").defineInRange("numberOfSlots", defaultNumberOfSlots, 1, 16);
		slotsInRow = builder.comment("Number of lots displayed in a row").defineInRange("slotsInRow", 4, 1, 6);

		builder.pop();
	}
}
