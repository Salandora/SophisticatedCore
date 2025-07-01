package net.p3pp3rf1y.sophisticatedcore.client.model;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ModelState;

public class SimpleModelState implements ModelState {
	private final Transformation rotation;
	private final boolean uvLocked;

	public SimpleModelState(Transformation rotation, boolean uvLocked) {
		this.rotation = rotation;
		this.uvLocked = uvLocked;
	}

	@Override
	public Transformation getRotation() {
		return rotation;
	}

	@Override
	public boolean isUvLocked() {
		return uvLocked;
	}
}
