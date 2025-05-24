package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.extensions.entity.SophisticatedPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public abstract class PlayerMixin implements SophisticatedPlayer {
}