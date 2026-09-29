package com.shieldtweak.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置文件: config/shieldtweak.json
 *
 * 颜色使用 RGB 十六进制整数 (0xRRGGBB)，可用 "#RRGGBB" 字符串或数字表示。
 */
public final class ShieldTweakConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shieldtweak.json");

    // ---- 低盾 ----
    /** 是否下调第一人称盾牌模型 */
    public boolean lowShieldEnabled = true;
    /** 模型下移量（方块单位，正值向下） */
    public double lowShieldOffset = 0.25;

    // ---- 盾牌染色 ----
    /** 是否启用染色 */
    public boolean tintEnabled = true;
    /** 未举起状态颜色，默认白色（不染色） */
    public int colorIdle = 0xFFFFFF;
    /** 举起前摇中（尚未真正格挡）颜色，默认白色（不染色，等同未举起） */
    public int colorRaising = 0xFFFFFF;
    /** 举起且已生效（格挡中）颜色，默认绿色 */
    public int colorRaised = 0x55FF55;
    /** 禁用（被斧破盾冷却中）状态颜色，默认红色 */
    public int colorDisabled = 0xFF5555;
    /** 是否给自己的（第一/第三人称）盾牌染色 */
    public boolean tintSelf = true;
    /** 是否给其他玩家的盾牌染色 */
    public boolean tintOthers = true;

    // ---- 恢复提示音 ----
    /** 自己的盾牌从禁用恢复时播放提示音 */
    public boolean recoverSoundEnabled = true;
    /** 提示音的音效事件 ID */
    public String recoverSoundId = "minecraft:block.note_block.pling";
    public float recoverSoundVolume = 0.6f;
    public float recoverSoundPitch = 1.5f;
    /**
     * 提示音的第二声，与第一声构成上行双音，用于和原版的破盾声（item.shield.break）明确区分。
     * 留空字符串则只播第一声。
     */
    public String recoverSoundId2 = "minecraft:block.note_block.pling";
    public float recoverSoundVolume2 = 0.6f;
    public float recoverSoundPitch2 = 2.0f;
    /** 第二声相对第一声的延迟（tick），小于等于 0 则不播第二声 */
    public int recoverSoundDelayTicks = 3;

    /**
     * 是否通过破盾音效推断其他玩家的禁用状态（染红）。
     * 客户端拿不到别人的冷却同步，只能“听到破盾音 + 猜是谁”，多人混战时可能标错或标不上；
     * 关闭后其他玩家不再染红，自己的红色不受影响（由服务端冷却包精确同步）。
     */
    public boolean observedDisableEnabled = true;
    /** 其他玩家被观察到破盾后视为禁用的时长（tick），与原版一致为 100 tick (5 秒) */
    public int observedDisableTicks = 100;
    /** 破盾音效归属判定的搜索半径（格），在此范围内挑选最可能的持盾玩家 */
    public double observedDisableRadius = 3.0;

    private static ShieldTweakConfig INSTANCE = new ShieldTweakConfig();

    public static ShieldTweakConfig get() {
        return INSTANCE;
    }

    public static void load() {
        ShieldTweakConfig cfg = new ShieldTweakConfig();
        if (Files.isRegularFile(FILE)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                cfg.lowShieldEnabled = getBool(json, "lowShieldEnabled", cfg.lowShieldEnabled);
                cfg.lowShieldOffset = getDouble(json, "lowShieldOffset", cfg.lowShieldOffset);
                cfg.tintEnabled = getBool(json, "tintEnabled", cfg.tintEnabled);
                cfg.colorIdle = getColor(json, "colorIdle", cfg.colorIdle);
                cfg.colorRaising = getColor(json, "colorRaising", cfg.colorRaising);
                cfg.colorRaised = getColor(json, "colorRaised", cfg.colorRaised);
                cfg.colorDisabled = getColor(json, "colorDisabled", cfg.colorDisabled);
                cfg.tintSelf = getBool(json, "tintSelf", cfg.tintSelf);
                cfg.tintOthers = getBool(json, "tintOthers", cfg.tintOthers);
                cfg.recoverSoundEnabled = getBool(json, "recoverSoundEnabled", cfg.recoverSoundEnabled);
                cfg.recoverSoundId = getString(json, "recoverSoundId", cfg.recoverSoundId);
                cfg.recoverSoundVolume = (float) getDouble(json, "recoverSoundVolume", cfg.recoverSoundVolume);
                cfg.recoverSoundPitch = (float) getDouble(json, "recoverSoundPitch", cfg.recoverSoundPitch);
                cfg.recoverSoundId2 = getString(json, "recoverSoundId2", cfg.recoverSoundId2);
                cfg.recoverSoundVolume2 = (float) getDouble(json, "recoverSoundVolume2", cfg.recoverSoundVolume2);
                cfg.recoverSoundPitch2 = (float) getDouble(json, "recoverSoundPitch2", cfg.recoverSoundPitch2);
                cfg.recoverSoundDelayTicks = (int) getDouble(json, "recoverSoundDelayTicks", cfg.recoverSoundDelayTicks);
                cfg.observedDisableEnabled = getBool(json, "observedDisableEnabled", cfg.observedDisableEnabled);
                cfg.observedDisableTicks = (int) getDouble(json, "observedDisableTicks", cfg.observedDisableTicks);
                cfg.observedDisableRadius = getDouble(json, "observedDisableRadius", cfg.observedDisableRadius);
            } catch (Exception e) {
                ShieldTweakClient.LOGGER.warn("读取配置失败，使用默认值", e);
            }
        }
        INSTANCE = cfg;
        cfg.save();
    }

    public void save() {
        JsonObject json = new JsonObject();
        json.addProperty("lowShieldEnabled", lowShieldEnabled);
        json.addProperty("lowShieldOffset", lowShieldOffset);
        json.addProperty("tintEnabled", tintEnabled);
        json.addProperty("colorIdle", hex(colorIdle));
        json.addProperty("colorRaising", hex(colorRaising));
        json.addProperty("colorRaised", hex(colorRaised));
        json.addProperty("colorDisabled", hex(colorDisabled));
        json.addProperty("tintSelf", tintSelf);
        json.addProperty("tintOthers", tintOthers);
        json.addProperty("recoverSoundEnabled", recoverSoundEnabled);
        json.addProperty("recoverSoundId", recoverSoundId);
        json.addProperty("recoverSoundVolume", recoverSoundVolume);
        json.addProperty("recoverSoundPitch", recoverSoundPitch);
        json.addProperty("recoverSoundId2", recoverSoundId2);
        json.addProperty("recoverSoundVolume2", recoverSoundVolume2);
        json.addProperty("recoverSoundPitch2", recoverSoundPitch2);
        json.addProperty("recoverSoundDelayTicks", recoverSoundDelayTicks);
        json.addProperty("observedDisableEnabled", observedDisableEnabled);
        json.addProperty("observedDisableTicks", observedDisableTicks);
        json.addProperty("observedDisableRadius", observedDisableRadius);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(json));
        } catch (IOException e) {
            ShieldTweakClient.LOGGER.warn("写入配置失败", e);
        }
    }

    private static String hex(int rgb) {
        return "#" + String.format("%06X", rgb & 0xFFFFFF);
    }

    private static boolean getBool(JsonObject json, String key, boolean def) {
        return json.has(key) ? json.get(key).getAsBoolean() : def;
    }

    private static double getDouble(JsonObject json, String key, double def) {
        return json.has(key) ? json.get(key).getAsDouble() : def;
    }

    private static String getString(JsonObject json, String key, String def) {
        return json.has(key) ? json.get(key).getAsString() : def;
    }

    /** 支持 "#RRGGBB"、"0xRRGGBB" 字符串或十进制数字 */
    private static int getColor(JsonObject json, String key, int def) {
        if (!json.has(key)) return def;
        var el = json.get(key);
        try {
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return el.getAsInt() & 0xFFFFFF;
            }
            String s = el.getAsString().trim();
            if (s.startsWith("#")) s = s.substring(1);
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (Exception e) {
            return def;
        }
    }
}
