package net.p3pp3rf1y.sophisticatedcore.compat;

import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public class CompatRegistry {
	private static final Map<CompatInfo, List<Supplier<ICompat>>> compatFactories = new HashMap<>();
	private static final Map<CompatInfo, List<ICompat>> loadedCompats = new HashMap<>();


	public static void registerCompat(CompatInfo info, Supplier<ICompat> factory) {
		compatFactories.computeIfAbsent(info, k -> new ArrayList<>()).add(factory);
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

	public static void setupCompats() {
		loadedCompats.values().forEach(compats -> compats.forEach(ICompat::setup));
	}

	public static void initCompats() {
		compatFactories.forEach((compatInfo, factories) -> {
			if (compatInfo.isLoaded()) {
				factories.forEach(factory -> {
					try {
						loadedCompats.computeIfAbsent(compatInfo, k -> new ArrayList<>()).add(factory.get());
					} catch (Exception e) {
						SophisticatedCore.LOGGER.error("Error instantiating compatibility ", e);
					}
				});
			}
		});
		loadedCompats.values().forEach(compats -> compats.forEach(ICompat::init));
	}
}
