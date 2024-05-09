package net.p3pp3rf1y.sophisticatedcore.compat.litematica;

import fi.dy.masa.litematica.materials.MaterialListBase;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.p3pp3rf1y.sophisticatedcore.SophisticatedCore;
import net.p3pp3rf1y.sophisticatedcore.compat.ICompat;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;

public class LitematicaCompat implements ICompat {
	private static TaskCountReceivedContents task;
	public static final ItemApiLookup<Function<List<UUID>, SimplePacketBase>, UUID> REQUEST_CONTENTS_CAPABILITY = ItemApiLookup.get(SophisticatedCore.getRL("sophisticatedcore_requestcontents"), (Class<Function<List<UUID>, SimplePacketBase>>)(Class<?>) Function.class, UUID.class);

	@Override
	public void setup() {
		PacketHandler.registerC2SMessage(RequestContentsMessage.class, RequestContentsMessage::new);
		PacketHandler.registerS2CMessage(UpdateMaterialListMessage.class, UpdateMaterialListMessage::new);
	}

	public static void requestContents(MaterialListBase materialList) {
		task = new TaskCountReceivedContents(materialList);
		PacketHandler.sendToServer(new RequestContentsMessage());
	}

	@Nullable
	public static TaskCountReceivedContents getTask() {
		return task;
	}
}
