package com.shieldtweak;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 游戏内设置界面。
 *
 * 每次点击都会立刻写入 {@link ShieldTweakConfig} 并存盘，因此改动即时生效，
 * 无需“保存”按钮，也不必重启游戏。
 *
 * 界面只用 ButtonWidget 构建、不自绘任何文本，标题同样用一个禁用按钮承载，
 * 以免依赖各版本差异较大的绘制 API（与 26.2 分支保持一致的结构）。
 *
 * 数值/颜色类选项在预设档位之间循环；需要精确值仍可直接编辑
 * config/shieldtweak.json。若当前值不在预设里，第一次点击会先吸附到最接近的档位。
 */
public class ShieldTweakConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int BUTTON_WIDTH = 240;
    /** 标题距顶距离与底部按钮距下边缘距离 */
    private static final int TOP_MARGIN = 12;
    private static final int BOTTOM_MARGIN = 10;

    /** 颜色预设，按点击顺序循环 */
    private static final double[] COLOR_PRESETS = {
            0xFFFFFF, 0x55FF55, 0xFF5555, 0xFFFF55, 0x55FFFF,
            0x5555FF, 0xFF55FF, 0xFFAA00, 0xAAAAAA, 0x000000
    };
    private static final String[] COLOR_NAMES = {
            "白（不染色）", "绿", "红", "黄", "青", "蓝", "品红", "橙", "灰", "黑"
    };

    private static final double[] OFFSET_PRESETS = {
            0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.5, 0.6, 0.75, 1.0
    };
    private static final double[] VOLUME_PRESETS = {
            0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0
    };
    private static final double[] PITCH_PRESETS = {
            0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0
    };
    private static final double[] DELAY_PRESETS = {0, 1, 2, 3, 4, 5, 6, 8, 10, 15, 20};
    private static final double[] DISABLE_TICKS_PRESETS = {0, 20, 40, 60, 80, 100, 120, 140, 200};
    private static final double[] RADIUS_PRESETS = {1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10, 16};

    /** 已在原版 SoundEvents 中核实存在的音效 */
    private static final String[] SOUND_PRESETS = {
            "minecraft:block.note_block.pling",
            "minecraft:block.note_block.bell",
            "minecraft:block.note_block.harp",
            "minecraft:block.note_block.chime",
            "minecraft:block.amethyst_block.chime",
            "minecraft:entity.experience_orb.pickup",
            "minecraft:entity.arrow.hit_player",
            "minecraft:block.anvil.land"
    };

    private final Screen parent;
    private int page;
    private int pageCount = 1;
    /** 每页行数，根据实际可用高度算出，窗口变化时随之调整 */
    private int rowsPerPage = 7;
    /** 翻页需要换掉整批控件，但不能在点击事件派发途中改 children 列表，因此延到 tick 里做 */
    private boolean needsRebuild;

    public ShieldTweakConfigScreen(final Screen parent) {
        super(Text.literal("Shield Tweak 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        final List<Option> options = buildOptions();
        // 按可用高度动态定行数：窗口小或 GUI 缩放大时自动减少，避免与底部按钮重叠
        int usable = this.height - TOP_MARGIN - (ROW_HEIGHT + 8) - (ROW_HEIGHT + BOTTOM_MARGIN);
        rowsPerPage = Math.max(1, usable / (ROW_HEIGHT + ROW_GAP));
        pageCount = Math.max(1, (options.size() + rowsPerPage - 1) / rowsPerPage);
        if (page < 0) {
            page = 0;
        }
        if (page >= pageCount) {
            page = pageCount - 1;
        }

        final int x = (this.width - BUTTON_WIDTH) / 2;
        int y = TOP_MARGIN;

        // 标题栏：用禁用按钮承载文字，避免自绘文本
        ButtonWidget title = ButtonWidget.builder(
                Text.literal(getTitle().getString() + "　第 " + (page + 1) + "/" + pageCount + " 页"),
                b -> {
                }).dimensions(x, y, BUTTON_WIDTH, ROW_HEIGHT).build();
        title.active = false;
        addDrawableChild(title);
        y += ROW_HEIGHT + 8;

        final int start = page * rowsPerPage;
        for (int i = 0; i < rowsPerPage && start + i < options.size(); i++) {
            final Option option = options.get(start + i);
            addDrawableChild(ButtonWidget.builder(Text.literal(label(option)), b -> {
                option.cycle();
                ShieldTweakConfig.get().save();
                // 只改按钮文字，不重建控件：避免在事件派发中改动 children
                b.setMessage(Text.literal(label(option)));
            }).dimensions(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
            y += ROW_HEIGHT + ROW_GAP;
        }

        final int footY = this.height - ROW_HEIGHT - BOTTOM_MARGIN;
        final int third = (BUTTON_WIDTH - 2 * 8) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("◀ 上一页"), b -> {
            page = (page - 1 + pageCount) % pageCount;
            needsRebuild = true;
        }).dimensions(x, footY, third, ROW_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("下一页 ▶"), b -> {
            page = (page + 1) % pageCount;
            needsRebuild = true;
        }).dimensions(x + third + 8, footY, third, ROW_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("完成"), b -> close())
                .dimensions(x + 2 * (third + 8), footY, third, ROW_HEIGHT).build());
    }

    @Override
    public void tick() {
        if (needsRebuild) {
            needsRebuild = false;
            clearAndInit();
        }
    }

    private static String label(final Option option) {
        return option.name() + "：" + option.displayValue();
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    /** 每次 init 都重新构建，lambda 因此总是引用当前的配置实例 */
    private static List<Option> buildOptions() {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        List<Option> list = new ArrayList<>();

        list.add(new BoolOption("低盾（下移第一人称盾牌）",
                () -> cfg.lowShieldEnabled, v -> cfg.lowShieldEnabled = v));
        list.add(new NumOption("低盾下移量", "", OFFSET_PRESETS,
                () -> cfg.lowShieldOffset, v -> cfg.lowShieldOffset = v));

        list.add(new BoolOption("盾牌染色",
                () -> cfg.tintEnabled, v -> cfg.tintEnabled = v));
        list.add(new BoolOption("染色：自己",
                () -> cfg.tintSelf, v -> cfg.tintSelf = v));
        list.add(new BoolOption("染色：其他玩家",
                () -> cfg.tintOthers, v -> cfg.tintOthers = v));
        list.add(new BoolOption("推断其他玩家破盾（染红）",
                () -> cfg.observedDisableEnabled, v -> cfg.observedDisableEnabled = v));
        list.add(new NumOption("他人禁用时长", " tick", DISABLE_TICKS_PRESETS,
                () -> cfg.observedDisableTicks, v -> cfg.observedDisableTicks = (int) Math.round(v)));
        list.add(new NumOption("破盾归属判定半径", " 格", RADIUS_PRESETS,
                () -> cfg.observedDisableRadius, v -> cfg.observedDisableRadius = v));

        list.add(new ColorOption("颜色：未举起",
                () -> cfg.colorIdle, v -> cfg.colorIdle = v));
        list.add(new ColorOption("颜色：举起前摇",
                () -> cfg.colorRaising, v -> cfg.colorRaising = v));
        list.add(new ColorOption("颜色：举盾已生效",
                () -> cfg.colorRaised, v -> cfg.colorRaised = v));
        list.add(new ColorOption("颜色：被禁用",
                () -> cfg.colorDisabled, v -> cfg.colorDisabled = v));

        list.add(new BoolOption("恢复提示音",
                () -> cfg.recoverSoundEnabled, v -> cfg.recoverSoundEnabled = v));
        list.add(new StrOption("提示音 第一声", SOUND_PRESETS,
                () -> cfg.recoverSoundId, v -> cfg.recoverSoundId = v));
        list.add(new NumOption("提示音1 音量", "", VOLUME_PRESETS,
                () -> cfg.recoverSoundVolume, v -> cfg.recoverSoundVolume = v.floatValue()));
        list.add(new NumOption("提示音1 音高", "", PITCH_PRESETS,
                () -> cfg.recoverSoundPitch, v -> cfg.recoverSoundPitch = v.floatValue()));
        list.add(new StrOption("提示音 第二声", SOUND_PRESETS,
                () -> cfg.recoverSoundId2, v -> cfg.recoverSoundId2 = v));
        list.add(new NumOption("提示音2 音量", "", VOLUME_PRESETS,
                () -> cfg.recoverSoundVolume2, v -> cfg.recoverSoundVolume2 = v.floatValue()));
        list.add(new NumOption("提示音2 音高", "", PITCH_PRESETS,
                () -> cfg.recoverSoundPitch2, v -> cfg.recoverSoundPitch2 = v.floatValue()));
        list.add(new NumOption("第二声延迟", " tick", DELAY_PRESETS,
                () -> cfg.recoverSoundDelayTicks, v -> cfg.recoverSoundDelayTicks = (int) Math.round(v)));

        return list;
    }

    // ---- 选项模型 ----

    private interface Option {
        String name();

        String displayValue();

        void cycle();
    }

    private record BoolOption(String name, BooleanSupplier get, Consumer<Boolean> set) implements Option {
        @Override
        public String displayValue() {
            return get.getAsBoolean() ? "开" : "关";
        }

        @Override
        public void cycle() {
            set.accept(!get.getAsBoolean());
        }
    }

    private record NumOption(String name, String suffix, double[] presets,
                             DoubleSupplier get, Consumer<Double> set) implements Option {
        @Override
        public String displayValue() {
            return format(get.getAsDouble()) + suffix;
        }

        @Override
        public void cycle() {
            set.accept(presets[nextIndex(presets, get.getAsDouble())]);
        }
    }

    private record ColorOption(String name, IntSupplier get, IntConsumer set) implements Option {
        @Override
        public String displayValue() {
            int rgb = get.getAsInt();
            return "#" + String.format("%06X", rgb & 0xFFFFFF) + "　" + colorName(rgb);
        }

        @Override
        public void cycle() {
            set.accept((int) COLOR_PRESETS[nextIndex(COLOR_PRESETS, get.getAsInt())]);
        }
    }

    private record StrOption(String name, String[] presets,
                             Supplier<String> get, Consumer<String> set) implements Option {
        @Override
        public String displayValue() {
            String value = get.get();
            int colon = value.indexOf(':');
            return colon >= 0 ? value.substring(colon + 1) : value;
        }

        @Override
        public void cycle() {
            String value = get.get();
            for (int i = 0; i < presets.length; i++) {
                if (presets[i].equals(value)) {
                    set.accept(presets[(i + 1) % presets.length]);
                    return;
                }
            }
            // 自定义值不在预设里：第一次点击吸附到首个预设，之后正常循环
            set.accept(presets[0]);
        }
    }

    // ---- 工具方法 ----

    /** 命中预设则前进一档；未命中则先吸附到最接近的档位，避免一次点击跳过用户当前的自定义值 */
    private static int nextIndex(final double[] presets, final double current) {
        int nearest = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < presets.length; i++) {
            double diff = Math.abs(presets[i] - current);
            if (diff < bestDiff) {
                bestDiff = diff;
                nearest = i;
            }
        }
        return bestDiff > 1e-6 ? nearest : (nearest + 1) % presets.length;
    }

    private static String colorName(final int rgb) {
        for (int i = 0; i < COLOR_PRESETS.length; i++) {
            if ((int) COLOR_PRESETS[i] == rgb) {
                return COLOR_NAMES[i];
            }
        }
        return "自定义";
    }

    private static String format(final double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e9) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
