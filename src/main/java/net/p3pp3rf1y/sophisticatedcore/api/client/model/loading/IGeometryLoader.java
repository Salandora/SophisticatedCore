package net.p3pp3rf1y.sophisticatedcore.api.client.model.loading;

import com.google.gson.JsonObject;

public interface IGeometryLoader<T extends IUnbakedGeometry> {
	T read(JsonObject modelContents);
}