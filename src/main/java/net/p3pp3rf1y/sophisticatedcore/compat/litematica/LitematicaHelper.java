package net.p3pp3rf1y.sophisticatedcore.compat.litematica;

import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.scheduler.TaskScheduler;

import net.p3pp3rf1y.sophisticatedcore.compat.litematica.network.RequestContentsPacket;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHelper;

public class LitematicaHelper {
	private static TaskCountReceivedContents task;

	public static void requestContents(MaterialListBase materialList) {
		task = new TaskCountReceivedContents(materialList);
		PacketHelper.sendToServer(new RequestContentsPacket());
	}

	public static void setRequested(int requested) {
		task.setRequested(requested);
		TaskScheduler.getInstanceClient().scheduleTask(task, 20);
	}

	public static void incrementReceived(int count) {
		if (task == null) {
			return;
		}
		task.incrementReceived(count);
	}
}
