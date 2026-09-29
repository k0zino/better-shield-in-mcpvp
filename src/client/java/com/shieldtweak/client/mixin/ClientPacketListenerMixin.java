package com.shieldtweak.client.mixin;

import com.shieldtweak.client.ShieldStates;
import com.shieldtweak.client.ShieldTweakConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 破盾时服务端会在玩家位置广播 item.shield.break 音效，
 * 借此推断其他玩家的盾牌禁用状态（他们的冷却不会同步到本客户端）。
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void shieldtweak$onShieldBreakSound(final ClientboundSoundPacket packet, final CallbackInfo ci) {
        if (packet.getSound().value() != SoundEvents.SHIELD_BREAK.value()) {
            return;
        }
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.observedDisableEnabled) {
            // 推断功能已关闭，不必扫描玩家
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        double radiusSq = cfg.observedDisableRadius * cfg.observedDisableRadius;
        Player nearest = null;
        double best = radiusSq;
        Player nearestHoldingShield = null;
        double bestHoldingShield = radiusSq;
        for (Player player : level.players()) {
            double distSq = player.distanceToSqr(packet.getX(), packet.getY(), packet.getZ());
            if (distSq >= radiusSq) {
                continue;
            }
            if (distSq < best) {
                best = distSq;
                nearest = player;
            }
            if (distSq < bestHoldingShield && holdsShield(player)) {
                bestHoldingShield = distSq;
                nearestHoldingShield = player;
            }
        }
        // 破盾的一定是当时手上拿着盾的人，优先按此归属，降低混战中标错人的概率
        Player target = nearestHoldingShield != null ? nearestHoldingShield : nearest;
        if (target != null) {
            ShieldStates.markObservedDisabled(target.getId());
        }
    }

    private static boolean holdsShield(final Player player) {
        return player.getMainHandItem().is(Items.SHIELD) || player.getOffhandItem().is(Items.SHIELD);
    }
}
