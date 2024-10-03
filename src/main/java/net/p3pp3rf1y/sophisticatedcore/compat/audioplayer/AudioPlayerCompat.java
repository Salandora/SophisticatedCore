package net.p3pp3rf1y.sophisticatedcore.compat.audioplayer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.ServerStorageSoundHandler;

public class AudioPlayerCompat implements ICompat {
	@Override
	public void setup() {
		ServerTickEvents.END_WORLD_TICK.register(AudioPlayerSoundHandler::tick);

		ServerStorageSoundHandler.registerSoundHandler(new AudioPlayerSoundHandler());
	}
}
