package net.p3pp3rf1y.sophisticatedcore.compat.litematica;

import net.p3pp3rf1y.sophisticatedcore.compat.litematica.network.RequestContentsPayload;
import net.p3pp3rf1y.sophisticatedcore.network.PacketDistributor;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.scheduler.TaskScheduler;

public class LitematicaHelper {
	private static TaskCountReceivedContents task;

	public static void requestContents(MaterialListBase materialList) {
		task = new TaskCountReceivedContents(materialList);
		PacketDistributor.sendToServer(new RequestContentsPayload());
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
