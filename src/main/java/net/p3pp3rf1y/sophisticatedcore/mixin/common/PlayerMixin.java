package net.p3pp3rf1y.sophisticatedcore.mixin.common;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.extensions.entity.SophisticatedPlayer;

@Mixin(Player.class)
public abstract class PlayerMixin implements SophisticatedPlayer {
}
