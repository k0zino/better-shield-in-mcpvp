package com.shieldtweak.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShieldTweakClient implements ClientModInitializer {
    public static final String MOD_ID = "shieldtweak";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final ShieldRecovery RECOVERY = new ShieldRecovery();
    private static Object lastWorld;
    /** 上次检测的玩家实例，用于识别重生/换世界导致的冷却表重置 */
    private static LocalPlayer lastPlayer;
    /** 打开设置界面的按键，默认 K，可在原版“控制”里改 */
    private static KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        ShieldTweakConfig.load();

        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "打开 Shield Tweak 设置", GLFW.GLFW_KEY_K, KeyMapping.Category.MISC));

        // 单独一个 tick 回调：上面的恢复检测在 player == null 时会提前 return，
        // 但设置界面在主菜单也应该能打开
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 按键只在没有界面打开时才会收到点击，所以 parent 传 null 即可：关闭后回到游戏
            if (configKey.consumeClick()) {
                client.setScreenAndShow(new ShieldTweakConfigScreen(null));
            }
        });

        // 检测自己的盾牌从禁用（冷却）恢复，播放提示音
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null) {
                // 不在世界中：复位所有状态，并清掉其他玩家的破盾观察记录
                RECOVERY.reset(false);
                lastWorld = null;
                lastPlayer = null;
                ShieldStates.clearObserved();
                return;
            }
            if (player != lastPlayer || client.level != lastWorld) {
                // 重生或换世界会换成新的玩家实例，其冷却表是空的，
                // 若沿用旧标志会把这次“清空”误判成禁用结束而白响一声
                lastPlayer = player;
                lastWorld = client.level;
                RECOVERY.reset(isShieldOnCooldown(player));
                ShieldStates.clearObserved();
            }
            if (client.isPaused()) {
                return;
            }
            ShieldStates.pruneExpired();
            ShieldTweakConfig cfg = ShieldTweakConfig.get();
            ShieldRecovery.Note note = RECOVERY.tick(isShieldOnCooldown(player), cfg.recoverSoundEnabled,
                    cfg.recoverSoundDelayTicks, !cfg.recoverSoundId2.isBlank());
            if (note == ShieldRecovery.Note.FIRST) {
                playNote(client, cfg.recoverSoundId, cfg.recoverSoundVolume, cfg.recoverSoundPitch);
            } else if (note == ShieldRecovery.Note.SECOND) {
                playNote(client, cfg.recoverSoundId2, cfg.recoverSoundVolume2, cfg.recoverSoundPitch2);
            }
        });
    }

    private static boolean isShieldOnCooldown(final LocalPlayer player) {
        return player.getCooldowns().isOnCooldown(new ItemStack(Items.SHIELD));
    }

    private static void playNote(final Minecraft client, final String soundId, final float volume, final float pitch) {
        if (soundId == null || soundId.isBlank()) {
            return;
        }
        SoundEvent sound = resolveSound(soundId);
        if (sound != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    /**
     * SOUND_EVENT 是带默认值的注册表：未知 ID 会返回默认音效而不是 null，
     * 所以必须先判存在，否则配置写错时会静默播成别的音。
     */
    private static SoundEvent resolveSound(final String soundId) {
        Identifier location;
        try {
            location = Identifier.parse(soundId);
        } catch (Exception e) {
            LOGGER.warn("非法的音效 ID: {}", soundId);
            return null;
        }
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(location)) {
            LOGGER.warn("未知的音效 ID: {}", soundId);
            return null;
        }
        return BuiltInRegistries.SOUND_EVENT.getValue(location);
    }
}
