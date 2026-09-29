package com.shieldtweak.mixin;

import com.shieldtweak.ShieldStates;
import com.shieldtweak.ShieldTweakConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 破盾时服务端会在玩家位置广播 item.shield.break 音效，
 * 借此推断其他玩家的盾牌禁用状态（他们的冷却不会同步到本客户端）。
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void shieldtweak$onShieldBreakSound(final PlaySoundS2CPacket packet, final CallbackInfo ci) {
        if (packet.getSound() != SoundEvents.ITEM_SHIELD_BREAK) {
            return;
        }
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.observedDisableEnabled) {
            // 推断功能已关闭，不必扫描玩家
            return;
        }
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }
        double radiusSq = cfg.observedDisableRadius * cfg.observedDisableRadius;
        PlayerEntity nearest = null;
        double best = radiusSq;
        PlayerEntity nearestHoldingShield = null;
        double bestHoldingShield = radiusSq;
        for (PlayerEntity player : world.getPlayers()) {
            double distSq = player.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ());
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
        PlayerEntity target = nearestHoldingShield != null ? nearestHoldingShield : nearest;
        if (target != null) {
            ShieldStates.markObservedDisabled(target.getId());
        }
    }

    private static boolean holdsShield(final PlayerEntity player) {
        return player.getMainHandStack().isOf(Items.SHIELD) || player.getOffHandStack().isOf(Items.SHIELD);
    }
}
