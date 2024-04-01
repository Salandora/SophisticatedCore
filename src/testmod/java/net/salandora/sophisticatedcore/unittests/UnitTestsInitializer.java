package net.salandora.sophisticatedcore.unittests;

import net.fabricmc.api.ModInitializer;

import org.slf4j.LoggerFactory;

public class UnitTestsInitializer implements ModInitializer {
	@Override
	public void onInitialize() {


		LoggerFactory.getLogger("sophisticatedcore testmod").info("SophisticatedCore unit tests successful.");
	}
}
