package net.p3pp3rf1y.sophisticatedcore.client.init;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeNoteParticle;

import static net.p3pp3rf1y.sophisticatedcore.init.ModParticles.JUKEBOX_NOTE;

public class ModParticles {
	private ModParticles() {}

	public static void registerFactories() {
		ParticleFactoryRegistry.getInstance().register(JUKEBOX_NOTE, JukeboxUpgradeNoteParticle.Factory::new);
	}
}
