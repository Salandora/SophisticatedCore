package net.p3pp3rf1y.sophisticatedcore.init;

import io.github.fabricators_of_create.porting_lib.util.DeferredRegister;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeNoteParticleData;

import java.util.function.Supplier;

public class ModParticles {
	private ModParticles() {
	}

	private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, SophisticatedCore.MOD_ID);

	public static final Supplier<JukeboxUpgradeNoteParticleData> JUKEBOX_NOTE = PARTICLES.register("jukebox_note", JukeboxUpgradeNoteParticleData::new);

	public static void registerParticles() {
		PARTICLES.register();
	}

}
