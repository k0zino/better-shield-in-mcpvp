package com.shieldtweak;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 游戏内设置界面。
 *
 * 开关和音效预设点击即保存；颜色与数值进入编辑页，应用后保存。
 *
 * 设置列表使用原版按钮；编辑页使用原版文本框和滑块。
 *
 * 编辑页支持颜色预览、RGB 滑块与精确数值输入。
 */
public class ShieldTweakConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int BUTTON_WIDTH = 240;
    /** 标题距顶距离与底部按钮距下边缘距离 */
    private static final int TOP_MARGIN = 12;
    private static final int BOTTOM_MARGIN = 10;

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
            addDrawableChild(ButtonWidget.builder(Text.literal(label(option)), b -> select(option, b))
                    .dimensions(x, y, BUTTON_WIDTH, ROW_HEIGHT).build());
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
        return option.name() + "：" + option.displayValue()
                + (option instanceof NumOption || option instanceof ColorOption ? "（编辑）" : "");
    }

    private void select(Option option, ButtonWidget button) {
        if (option instanceof NumOption number) {
            client.setScreen(ShieldTweakValueScreen.number(this, number.name(), number.get().getAsDouble(),
                    number.suffix(), number.rule(), number.set()));
            return;
        }
        if (option instanceof ColorOption color) {
            client.setScreen(ShieldTweakValueScreen.color(this, color.name(), color.get().getAsInt(),
                    color.defaultValue(), color.set()));
            return;
        }
        if (option instanceof BoolOption bool) bool.set().accept(!bool.get().getAsBoolean());
        if (option instanceof StrOption string) string.cycle();
        ShieldTweakConfig.get().save();
        button.setMessage(Text.literal(label(option)));
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    /** 每次 init 都重新构建，lambda 因此总是引用当前的配置实例 */
    private static List<Option> buildOptions() {
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        ShieldTweakConfig defaults = new ShieldTweakConfig();
        List<Option> list = new ArrayList<>();

        list.add(new BoolOption("低盾（下移第一人称盾牌）",
                () -> cfg.lowShieldEnabled, v -> cfg.lowShieldEnabled = v));
        list.add(new NumOption("低盾下移量", " 格", decimal(0.05, defaults.lowShieldOffset),
                () -> cfg.lowShieldOffset, v -> cfg.lowShieldOffset = v));

        list.add(new BoolOption("盾牌染色",
                () -> cfg.tintEnabled, v -> cfg.tintEnabled = v));
        list.add(new BoolOption("染色：自己",
                () -> cfg.tintSelf, v -> cfg.tintSelf = v));
        list.add(new BoolOption("染色：其他玩家",
                () -> cfg.tintOthers, v -> cfg.tintOthers = v));
        list.add(new BoolOption("推断其他玩家破盾（染红）",
                () -> cfg.observedDisableEnabled, v -> cfg.observedDisableEnabled = v));
        list.add(new NumOption("他人禁用时长", " tick", ticks(20, defaults.observedDisableTicks),
                () -> cfg.observedDisableTicks, v -> cfg.observedDisableTicks = (int) Math.round(v)));
        list.add(new NumOption("破盾归属判定半径", " 格", decimal(0.25, defaults.observedDisableRadius),
                () -> cfg.observedDisableRadius, v -> cfg.observedDisableRadius = v));

        list.add(new ColorOption("颜色：未举起", defaults.colorIdle,
                () -> cfg.colorIdle, v -> cfg.colorIdle = v));
        list.add(new ColorOption("颜色：举起前摇", defaults.colorRaising,
                () -> cfg.colorRaising, v -> cfg.colorRaising = v));
        list.add(new ColorOption("颜色：举盾已生效", defaults.colorRaised,
                () -> cfg.colorRaised, v -> cfg.colorRaised = v));
        list.add(new ColorOption("颜色：被禁用", defaults.colorDisabled,
                () -> cfg.colorDisabled, v -> cfg.colorDisabled = v));

        list.add(new BoolOption("恢复提示音",
                () -> cfg.recoverSoundEnabled, v -> cfg.recoverSoundEnabled = v));
        list.add(new StrOption("提示音 第一声", SOUND_PRESETS,
                () -> cfg.recoverSoundId, v -> cfg.recoverSoundId = v));
        list.add(new NumOption("提示音1 音量", "", sound(0, 1, defaults.recoverSoundVolume),
                () -> cfg.recoverSoundVolume, v -> cfg.recoverSoundVolume = v.floatValue()));
        list.add(new NumOption("提示音1 音高", "", sound(0.5, 2, defaults.recoverSoundPitch),
                () -> cfg.recoverSoundPitch, v -> cfg.recoverSoundPitch = v.floatValue()));
        list.add(new StrOption("提示音 第二声", SOUND_PRESETS,
                () -> cfg.recoverSoundId2, v -> cfg.recoverSoundId2 = v));
        list.add(new NumOption("提示音2 音量", "", sound(0, 1, defaults.recoverSoundVolume2),
                () -> cfg.recoverSoundVolume2, v -> cfg.recoverSoundVolume2 = v.floatValue()));
        list.add(new NumOption("提示音2 音高", "", sound(0.5, 2, defaults.recoverSoundPitch2),
                () -> cfg.recoverSoundPitch2, v -> cfg.recoverSoundPitch2 = v.floatValue()));
        list.add(new NumOption("第二声延迟", " tick", ticks(1, defaults.recoverSoundDelayTicks),
                () -> cfg.recoverSoundDelayTicks, v -> cfg.recoverSoundDelayTicks = (int) Math.round(v)));

        return list;
    }

    // ---- 选项模型 ----

    private static ConfigEditorValues.NumberRule decimal(double step, double defaultValue) {
        return new ConfigEditorValues.NumberRule(ConfigEditorValues.NumberKind.DECIMAL, 0, Double.MAX_VALUE, step, defaultValue);
    }

    private static ConfigEditorValues.NumberRule ticks(double step, double defaultValue) {
        return new ConfigEditorValues.NumberRule(ConfigEditorValues.NumberKind.INTEGER, 0, Integer.MAX_VALUE, step, defaultValue);
    }

    private static ConfigEditorValues.NumberRule sound(double min, double max, double defaultValue) {
        return new ConfigEditorValues.NumberRule(ConfigEditorValues.NumberKind.FLOAT, min, max, 0.05, defaultValue);
    }

    private interface Option {
        String name();

        String displayValue();
    }

    private record BoolOption(String name, BooleanSupplier get, Consumer<Boolean> set) implements Option {
        @Override
        public String displayValue() {
            return get.getAsBoolean() ? "开" : "关";
        }
    }

    private record NumOption(String name, String suffix, ConfigEditorValues.NumberRule rule,
                             DoubleSupplier get, Consumer<Double> set) implements Option {
        @Override
        public String displayValue() {
            return rule.format(get.getAsDouble()) + suffix;
        }
    }

    private record ColorOption(String name, int defaultValue, IntSupplier get, IntConsumer set) implements Option {
        @Override
        public String displayValue() {
            return ConfigEditorValues.hex(get.getAsInt());
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

}
