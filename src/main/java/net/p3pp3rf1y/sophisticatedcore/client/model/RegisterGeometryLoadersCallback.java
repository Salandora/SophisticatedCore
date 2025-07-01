package net.p3pp3rf1y.sophisticatedcore.client.model;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.p3pp3rf1y.sophisticatedcore.api.client.model.loading.IGeometryLoader;

import javax.annotation.Nullable;
import java.util.Map;

public class RegisterGeometryLoadersCallback {
	protected static final Map<ResourceLocation, IGeometryLoader<?>> LOADERS = Maps.newHashMap();

	public static void register(GeometryLoaderRegistry register) {
		register.registerLoader(LOADERS);
	}

	@Nullable
	public static IGeometryLoader<?> get(ResourceLocation loader) {
		return LOADERS.get(loader);
	}

	@FunctionalInterface
	public interface GeometryLoaderRegistry {
		void registerLoader(Map<ResourceLocation, IGeometryLoader<?>> loaders);
	}

	static {
	}
}
