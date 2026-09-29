package com.shieldtweak;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;

/** 计算盾牌状态并设置染色上下文 */
public final class ShieldStates {
    /** 实体 id -> 观察到的破盾禁用结束时间 (world time)，用于其他玩家 */
    private static final Map<Integer, Long> OBSERVED_DISABLED_UNTIL = new HashMap<>();

    /**
     * 举盾前摇的兜底值（tick）。
     * 原版盾牌的 blocks_attacks 组件为 block_delay_seconds=0.25，即 5 tick，
     * 与 LivingEntity#getBlockingItem 的判定一致；优先读组件以兼容自定义盾牌。
     */
    private static final int DEFAULT_BLOCK_DELAY_TICKS = 5;

    private ShieldStates() {
    }

    /** 举盾需多少 tick 才真正开始格挡（未生效前不算举盾） */
    public static int getBlockDelayTicks(final ItemStack stack) {
        BlocksAttacksComponent blocksAttacks = stack.get(DataComponentTypes.BLOCKS_ATTACKS);
        return blocksAttacks != null ? blocksAttacks.getBlockDelayTicks() : DEFAULT_BLOCK_DELAY_TICKS;
    }

    /** 在客户端听到某玩家位置播放破盾音效时调用 */
    public static void markObservedDisabled(final int entityId) {
        var world = MinecraftClient.getInstance().world;
        if (world != null) {
            OBSERVED_DISABLED_UNTIL.put(entityId, world.getTime() + ShieldTweakConfig.get().observedDisableTicks);
        }
    }

    public static void clearObserved() {
        OBSERVED_DISABLED_UNTIL.clear();
    }

    /** 清除已过期的观察记录，避免 map 随玩家数量只增不减 */
    public static void pruneExpired() {
        var world = MinecraftClient.getInstance().world;
        if (world == null) {
            OBSERVED_DISABLED_UNTIL.clear();
            return;
        }
        long now = world.getTime();
        OBSERVED_DISABLED_UNTIL.values().removeIf(until -> until <= now);
    }

    /** 第一人称：直接从实体计算 */
    public static void applyTint(final LivingEntity entity, final ItemStack stack) {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.tintEnabled || !cfg.tintSelf) {
            return;
        }
        if (isSelfDisabled(entity, stack)) {
            ShieldTintContext.set(cfg.colorDisabled);
        } else if (isBlocking(entity)) {
            ShieldTintContext.set(cfg.colorRaised);
        } else if (isRaising(entity)) {
            applyRaisingOrIdle(cfg);
        } else if (cfg.colorIdle != 0xFFFFFF) {
            ShieldTintContext.set(cfg.colorIdle);
        }
    }

    /** 第三人称（自己 F5 与其他玩家）：从渲染状态计算 */
    public static void applyTint(final PlayerEntityRenderState playerState, final Arm arm, final ItemStack stack) {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.tintEnabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        boolean self = client.player != null && playerState.id == client.player.getId();
        if (self ? !cfg.tintSelf : !cfg.tintOthers) {
            return;
        }
        // getItemUseTime(arm) 已含手臂匹配判定，非使用手返回 0；它是带 tickProgress 插值的已使用 tick 数
        float itemUseTime = playerState.getItemUseTime(arm);
        if (isDisabled(playerState.id, self, stack)) {
            ShieldTintContext.set(cfg.colorDisabled);
        } else if (itemUseTime >= getBlockDelayTicks(stack)) {
            ShieldTintContext.set(cfg.colorRaised);
        } else if (itemUseTime > 0.0F) {
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

    /**
     * 举盾是否已真正生效。
     * LivingEntity#isBlocking 内部已按 blocks_attacks 的 block_delay_ticks 判定，
     * 前摇期间返回 false，因此不会提前变绿。
     */
    private static boolean isBlocking(final LivingEntity entity) {
        return entity.isBlocking() && entity.getActiveItem().isOf(Items.SHIELD);
    }

    /** 正在举盾但前摇未结束 */
    private static boolean isRaising(final LivingEntity entity) {
        return entity.isUsingItem() && entity.getActiveItem().isOf(Items.SHIELD);
    }

    private static boolean isSelfDisabled(final LivingEntity entity, final ItemStack stack) {
        // 自己的冷却是由服务端冷却包精确同步的
        return entity instanceof PlayerEntity player && player.getItemCooldownManager().isCoolingDown(stack);
    }

    private static boolean isDisabled(final int entityId, final boolean self, final ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (self && client.player != null && client.player.getItemCooldownManager().isCoolingDown(stack)) {
            return true;
        }
        if (!ShieldTweakConfig.get().observedDisableEnabled) {
            // 推断已关闭：其他玩家不再染红（自己的红色走上面的冷却同步，不受影响）
            return false;
        }
        // 其他玩家：破盾时服务端会在其位置播放 item.shield.break，据此推断禁用窗口
        Long until = OBSERVED_DISABLED_UNTIL.get(entityId);
        return until != null && client.world != null && client.world.getTime() < until;
    }
}
