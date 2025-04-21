package net.p3pp3rf1y.sophisticatedcore.compat.audioplayer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.CompatModIds;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;

import javax.annotation.Nullable;

public class AudioPlayerCompat implements ICompat {
	@Override
	public void setup() {
		VersionPredicate supportedVersionRange = fromSpec(">=1.20.1-1.10.2");
		if (supportedVersionRange == null) {
			return;
		}

		boolean isCorrectVersion = FabricLoader.getInstance().getModContainer(CompatModIds.AUDIOPLAYER).map(container -> supportedVersionRange.test(container.getMetadata().getVersion())).orElse(false);
		if (!isCorrectVersion) {
			SophisticatedCore.LOGGER.info("Incompatible AudioPlayer version found");
			return;
		}

		SophisticatedCore.LOGGER.info("AudioPlayer compatibility loaded");

		ServerTickEvents.END_WORLD_TICK.register(AudioPlayerSoundHandler::tick);

		ServerStorageSoundHandler.registerSoundHandler(new AudioPlayerSoundHandler());
	}

	@Nullable
	public static VersionPredicate fromSpec(String spec) {
		try {
			return VersionPredicate.parse(spec);
		}
		catch (VersionParsingException e) {
			return null;
		}
	}
}
