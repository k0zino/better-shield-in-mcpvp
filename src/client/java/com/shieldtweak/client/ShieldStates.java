package com.shieldtweak.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;

/** 计算盾牌状态并设置染色上下文 */
public final class ShieldStates {
    /** 实体 id -> 观察到的破盾禁用结束时间 (gameTime)，用于其他玩家 */
    private static final Map<Integer, Long> OBSERVED_DISABLED_UNTIL = new HashMap<>();
    private static ClientLevel observedLevel;

    /**
     * 举盾前摇的兜底值（tick）。
     * 原版盾牌的 blocks_attacks 组件为 block_delay_seconds=0.25，即 5 tick，
     * 与 LivingEntity#getItemBlockingWith 的判定一致；优先读组件以兼容自定义盾牌。
     */
    private static final int DEFAULT_BLOCK_DELAY_TICKS = 5;

    private ShieldStates() {
    }

    /** 举盾需多少 tick 才真正开始格挡（未生效前不算举盾） */
    public static int blockDelayTicks(final ItemStack stack) {
        BlocksAttacks blocksAttacks = stack.get(DataComponents.BLOCKS_ATTACKS);
        return blocksAttacks != null ? blocksAttacks.blockDelayTicks() : DEFAULT_BLOCK_DELAY_TICKS;
    }

    /** 在客户端听到某玩家位置播放破盾音效时调用 */
    public static void markObservedDisabled(final int entityId) {
        pruneExpired();
        var player = Minecraft.getInstance().player;
        if (player != null && entityId == player.getId()) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            OBSERVED_DISABLED_UNTIL.put(entityId, level.getGameTime() + ShieldTweakConfig.get().observedDisableTicks);
        }
    }

    public static void clearObserved() {
        OBSERVED_DISABLED_UNTIL.clear();
        observedLevel = null;
    }

    /** 清除已过期的观察记录，避免 map 随玩家数量只增不减 */
    public static void pruneExpired() {
        var level = Minecraft.getInstance().level;
        if (level != observedLevel || !ShieldTweakConfig.get().observedDisableEnabled) {
            OBSERVED_DISABLED_UNTIL.clear();
            observedLevel = level;
        }
        if (level == null) {
            OBSERVED_DISABLED_UNTIL.clear();
            return;
        }
        long now = level.getGameTime();
        OBSERVED_DISABLED_UNTIL.values().removeIf(until -> until <= now);
    }

    /** 第一人称：直接从实体计算 */
    public static void applyTint(final LivingEntity entity, final ItemStack stack, final HumanoidArm arm) {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.tintEnabled || !cfg.tintSelf) {
            return;
        }
        if (isSelfDisabled(entity, stack)) {
            ShieldTintContext.set(cfg.colorDisabled);
        } else if (isUsingShield(entity, arm) && entity.isBlocking()) {
            ShieldTintContext.set(cfg.colorRaised);
        } else if (isUsingShield(entity, arm)) {
            applyRaisingOrIdle(cfg);
        } else if (cfg.colorIdle != 0xFFFFFF) {
            ShieldTintContext.set(cfg.colorIdle);
        }
    }

    /** 第三人称（所有玩家实体，含 F5 下的自己）：从渲染状态计算 */
    public static void applyTint(final AvatarRenderState avatar, final HumanoidArm arm, final ItemStack stack) {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.tintEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean self = mc.player != null && avatar.id == mc.player.getId();
        if (self ? !cfg.tintSelf : !cfg.tintOthers) {
            return;
        }
        // ticksUsingItem(arm) 已含手臂匹配判定，非使用手返回 0；它是带 partialTicks 插值的已使用 tick 数
        float ticksUsingItem = avatar.ticksUsingItem(arm);
        boolean usingThisArm = avatar.isUsingItem
                && (avatar.useItemHand == InteractionHand.MAIN_HAND) == (arm == avatar.mainArm);
        if (isDisabled(avatar.id, self, stack)) {
            ShieldTintContext.set(cfg.colorDisabled);
        } else if (usingThisArm && ticksUsingItem >= blockDelayTicks(stack)) {
            ShieldTintContext.set(cfg.colorRaised);
        } else if (usingThisArm) {
            applyRaisingOrIdle(cfg);
        } else if (cfg.colorIdle != 0xFFFFFF) {
            ShieldTintContext.set(cfg.colorIdle);
        }
    }

    /** 前摇中：默认不染色（等同未举起），配了 colorRaising 则用它 */
    private static void applyRaisingOrIdle(final ShieldTweakConfig cfg) {
        if (cfg.colorRaising != 0xFFFFFF) {
            ShieldTintContext.set(cfg.colorRaising);
        } else if (cfg.colorIdle != 0xFFFFFF) {
            ShieldTintContext.set(cfg.colorIdle);
        }
    }

    /** 只匹配当前渲染手；格挡是否已生效另由实体的 isBlocking() 判定。 */
    private static boolean isUsingShield(final LivingEntity entity, final HumanoidArm arm) {
        return entity.isUsingItem() && entity.getUseItem().is(Items.SHIELD)
                && (entity.getUsedItemHand() == InteractionHand.MAIN_HAND) == (arm == entity.getMainArm());
    }

    private static boolean isSelfDisabled(final LivingEntity entity, final ItemStack stack) {
        // 自己的冷却是由服务端冷却包精确同步的
        return entity instanceof Player player && player.getCooldowns().isOnCooldown(stack);
    }

    private static boolean isDisabled(final int entityId, final boolean self, final ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (self) {
            return mc.player != null && mc.player.getCooldowns().isOnCooldown(stack);
        }
        if (!ShieldTweakConfig.get().observedDisableEnabled) {
            // 推断已关闭：其他玩家不再染红（自己的红色走上面的冷却同步，不受影响）
            return false;
        }
        // 其他玩家：破盾时服务端会在其位置播放 item.shield.break，据此推断禁用窗口
        pruneExpired();
        Long until = OBSERVED_DISABLED_UNTIL.get(entityId);
        return until != null && mc.level != null && mc.level.getGameTime() < until;
    }
}
