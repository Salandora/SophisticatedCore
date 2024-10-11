package net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IUpgradeRenderer;
import net.p3pp3rf1y.sophisticatedcore.init.ModParticles;
import org.joml.Vector3f;

import java.util.function.UnaryOperator;

public class JukeboxUpgradeRenderer implements IUpgradeRenderer<JukeboxUpgradeRenderData> {
	@Override
	public void render(Level level, RandomSource rand, UnaryOperator<Vector3f> getPositionFromOffset, JukeboxUpgradeRenderData upgradeRenderData) {
		if (!upgradeRenderData.isPlaying() || rand.nextInt(2) != 0) {
			return;
		}

		float xOffset = level.random.nextFloat() * 0.6f - 0.3f;
		float yOffset = 0.5f + level.random.nextFloat() * 6.0f / 16.0f;
		float zOffset = level.random.nextFloat() * 0.6f - 0.1f;
		Vector3f randomAtTheBack = getPositionFromOffset.apply(new Vector3f(xOffset, yOffset, zOffset));

		level.addParticle(ModParticles.JUKEBOX_NOTE, randomAtTheBack.x(), randomAtTheBack.y(), randomAtTheBack.z(), rand.nextFloat(), 0.0D, 0.0D);
	}
}
