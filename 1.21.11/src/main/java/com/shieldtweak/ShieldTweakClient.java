package com.shieldtweak;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShieldTweakClient implements ClientModInitializer {
    public static final String MOD_ID = "shieldtweak";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean shieldWasOnCooldown;
    /** 上次检测的玩家实例，用于识别重生/换世界导致的冷却表重置 */
    private static ClientPlayerEntity lastPlayer;
    /** 恢复提示音第二声的倒计时（tick），<=0 表示无待播 */
    private static int secondNoteIn = -1;
    /** 打开设置界面的按键，默认 K，可在原版“控制”里改 */
    private static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        ShieldTweakConfig.load();

        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "打开 Shield Tweak 设置", GLFW.GLFW_KEY_K, KeyBinding.Category.MISC));

        // 单独一个 tick 回调：下面的恢复检测在 player == null 时会提前 return，
        // 但设置界面在主菜单也应该能打开
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 按键只在没有界面打开时才会收到点击，所以 parent 传 null 即可：关闭后回到游戏
            if (configKey.wasPressed()) {
                client.setScreen(new ShieldTweakConfigScreen(null));
            }
        });

        // 检测自己的盾牌从禁用（冷却）恢复，播放提示音
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null) {
                // 不在世界中：复位所有状态，并清掉其他玩家的破盾观察记录
                shieldWasOnCooldown = false;
                lastPlayer = null;
                secondNoteIn = -1;
                ShieldStates.clearObserved();
                return;
            }
            if (player != lastPlayer) {
                // 重生或换世界会换成新的玩家实例，其冷却表是空的，
                // 若沿用旧标志会把这次“清空”误判成禁用结束而白响一声
                lastPlayer = player;
                shieldWasOnCooldown = isShieldOnCooldown(player);
                secondNoteIn = -1;
            }
            ShieldStates.pruneExpired();

            if (secondNoteIn > 0 && --secondNoteIn == 0) {
                ShieldTweakConfig cfg = ShieldTweakConfig.get();
                playNote(client, cfg.recoverSoundId2, cfg.recoverSoundVolume2, cfg.recoverSoundPitch2);
            }

            boolean onCooldown = isShieldOnCooldown(player);
            if (shieldWasOnCooldown && !onCooldown) {
                playRecoverSound(client);
            }
            shieldWasOnCooldown = onCooldown;
        });
    }

    private static boolean isShieldOnCooldown(final ClientPlayerEntity player) {
        return player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.SHIELD));
    }

    private static void playRecoverSound(final MinecraftClient client) {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (!cfg.recoverSoundEnabled) {
            return;
        }
        playNote(client, cfg.recoverSoundId, cfg.recoverSoundVolume, cfg.recoverSoundPitch);
        // 第二声与第一声构成上行双音，和原版单次闷响的破盾声区分开
        secondNoteIn = cfg.recoverSoundDelayTicks > 0 && !cfg.recoverSoundId2.isBlank() ? cfg.recoverSoundDelayTicks : -1;
    }

    private static void playNote(final MinecraftClient client, final String soundId, final float volume, final float pitch) {
        SoundEvent sound = resolveSound(soundId);
        if (sound != null) {
            client.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
        }
    }

    /**
     * SOUND_EVENT 是带默认值的注册表：未知 ID 会返回默认音效而不是 null，
     * 所以必须先判存在，否则配置写错时会静默播成别的音。
     */
    private static SoundEvent resolveSound(final String soundId) {
        Identifier id;
        try {
            id = Identifier.of(soundId);
        } catch (Exception e) {
            LOGGER.warn("非法的音效 ID: {}", soundId);
            return null;
        }
        if (!Registries.SOUND_EVENT.containsId(id)) {
            LOGGER.warn("未知的音效 ID: {}", soundId);
            return null;
        }
        return Registries.SOUND_EVENT.get(id);
    }
}
