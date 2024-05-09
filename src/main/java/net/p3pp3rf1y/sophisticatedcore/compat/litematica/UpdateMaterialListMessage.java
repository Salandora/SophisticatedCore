package net.p3pp3rf1y.sophisticatedcore.compat.litematica;

import fi.dy.masa.litematica.scheduler.TaskScheduler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.network.SimplePacketBase;

public class UpdateMaterialListMessage extends SimplePacketBase {
	private final int requestedContents;

	public UpdateMaterialListMessage(int requestedContents) {
		this.requestedContents = requestedContents;
	}

	public UpdateMaterialListMessage(FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(this.requestedContents);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			Player player = context.getClientPlayer();
			if (player == null || LitematicaCompat.getTask() == null) {
				return;
			}

			LitematicaCompat.getTask().setRequested(this.requestedContents);
			TaskScheduler.getInstanceClient().scheduleTask(LitematicaCompat.getTask(), 20);
		});
		return true;
	}
}
