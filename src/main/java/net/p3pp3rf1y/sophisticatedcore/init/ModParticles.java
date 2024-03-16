package net.p3pp3rf1y.sophisticatedcore.init;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeNoteParticleData;

public class ModParticles {
	private ModParticles() {}

	public static final JukeboxUpgradeNoteParticleData JUKEBOX_NOTE = register("jukebox_note", new JukeboxUpgradeNoteParticleData());

	public static <T extends ParticleType<?>> T register(String id, T value) {
		return Registry.register(Registry.PARTICLE_TYPE, SophisticatedCore.getRL(id), value);
	}

	public static void registerParticles() {
	}
}
