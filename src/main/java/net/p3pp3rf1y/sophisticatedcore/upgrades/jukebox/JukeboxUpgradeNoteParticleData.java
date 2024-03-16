package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;

public class JukeboxUpgradeNoteParticleData extends ParticleType<JukeboxUpgradeNoteParticleData> implements ParticleOptions {
	public JukeboxUpgradeNoteParticleData() {
		super(false, DESERIALIZER);
	}

	@Override
	public JukeboxUpgradeNoteParticleData getType() {
		return ModParticles.JUKEBOX_NOTE;
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf pBuffer) {
		//noop
	}

	@Override
	public String writeToString() {
		//noinspection ConstantConditions
		return Registry.PARTICLE_TYPE.getKey(ModParticles.JUKEBOX_NOTE).toString();
	}

	@SuppressWarnings("deprecation")
	public static final Deserializer<JukeboxUpgradeNoteParticleData> DESERIALIZER = new Deserializer<>() {
		@Override
		public JukeboxUpgradeNoteParticleData fromCommand(ParticleType<JukeboxUpgradeNoteParticleData> pParticleType, StringReader pReader) {
			return (JukeboxUpgradeNoteParticleData) pParticleType;
		}

		@Override
		public JukeboxUpgradeNoteParticleData fromNetwork(ParticleType<JukeboxUpgradeNoteParticleData> pParticleType, FriendlyByteBuf pBuffer) {
			return (JukeboxUpgradeNoteParticleData) pParticleType;
		}
	};

	private final Codec<JukeboxUpgradeNoteParticleData> codec = Codec.unit(this::getType);

	@Override
	public Codec<JukeboxUpgradeNoteParticleData> codec() {
		return codec;
	}
}
