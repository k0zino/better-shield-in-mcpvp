package com.shieldtweak;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** 颜色与数值使用局部草稿；应用才写入，取消、Esc 与窗口缩放不会误改配置。 */
final class ShieldTweakValueScreen extends Screen {
    private static final int TEXT = 0xFFE0E0E0;
    private static final int ERROR = 0xFFFF7777;
    private static final int[] PRESETS = {
            0xFFFFFF, 0x55FF55, 0xFF5555, 0xFFFF55, 0x55FFFF,
            0x5555FF, 0xFF55FF, 0xFFAA00, 0xAAAAAA, 0x000000
    };
    private static final String[] PRESET_NAMES = {"白", "绿", "红", "黄", "青", "蓝", "品红", "橙", "灰", "黑"};
    private final Screen parent;
    private final ConfigEditorValues.Draft<Integer> color;
    private final ConfigEditorValues.Draft<Double> number;
    private final ConfigEditorValues.NumberRule rule;
    private final Consumer<Double> numberSetter;
    private final IntConsumer colorSetter;
    private final int defaultColor;
    private final String unit;
    private final ChannelSlider[] channels = new ChannelSlider[3];
    private TextFieldWidget input;
    private ButtonWidget apply;
    private ButtonWidget minus;
    private ButtonWidget plus;
    private int left;
    private int top;
    private int contentWidth;

    static Screen color(Screen parent, String name, int value, int defaultColor, IntConsumer setter) {
        return new ShieldTweakValueScreen(parent, name, value, defaultColor, setter, null, null, null, "");
    }

    static Screen number(Screen parent, String name, double value, String unit,
                         ConfigEditorValues.NumberRule rule, Consumer<Double> setter) {
        return new ShieldTweakValueScreen(parent, name, 0, 0, null, value, rule, setter, unit);
    }

    private ShieldTweakValueScreen(Screen parent, String name, int initialColor, int defaultColor,
                                   IntConsumer colorSetter, Double initialNumber,
                                   ConfigEditorValues.NumberRule rule, Consumer<Double> numberSetter, String unit) {
        super(Text.literal("编辑：" + name));
        this.parent = parent;
        this.defaultColor = defaultColor;
        this.colorSetter = colorSetter;
        this.numberSetter = numberSetter;
        this.rule = rule;
        this.unit = unit;
        color = colorSetter != null ? new ConfigEditorValues.Draft<>(ConfigEditorValues.hex(initialColor), ConfigEditorValues::color) : null;
        number = rule != null ? new ConfigEditorValues.Draft<>(rule.format(initialNumber), rule::parse) : null;
    }

    @Override
    protected void init() {
        contentWidth = Math.min(300, width - 24);
        left = (width - contentWidth) / 2;
        top = Math.max(6, (height - (color != null ? 228 : 154)) / 2);
        java.util.Arrays.fill(channels, null);
        input = new TextFieldWidget(textRenderer, left + (color != null ? 0 : 36), top + 34,
                contentWidth - (color != null ? 60 : 72), 20, getTitle());
        // 512 可容纳旧配置中的极大或极小有限 double，打开窗口不会截断原值。
        input.setMaxLength(color != null ? 32 : 512);
        input.setText(color != null ? color.text() : number.text());
        addDrawableChild(input);
        if (color != null) {
            for (int i = 0; i < channels.length; i++) {
                channels[i] = addDrawableChild(new ChannelSlider(left, top + 60 + i * 24, contentWidth, i));
            }
            int buttonWidth = (contentWidth - 16) / 5;
            for (int i = 0; i < PRESETS.length; i++) {
                final int selected = PRESETS[i];
                addDrawableChild(ButtonWidget.builder(Text.literal(PRESET_NAMES[i]), b -> input.setText(ConfigEditorValues.hex(selected)))
                        .dimensions(left + (i % 5) * (buttonWidth + 4), top + 134 + (i / 5) * 22, buttonWidth, 20).build());
            }
        } else {
            minus = addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> step(-1))
                    .dimensions(left, top + 34, 30, 20).build());
            plus = addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> step(1))
                    .dimensions(left + contentWidth - 30, top + 34, 30, 20).build());
        }
        int footer = top + (color != null ? 204 : 122);
        int third = (contentWidth - 12) / 3;
        addDrawableChild(ButtonWidget.builder(Text.literal("恢复默认"), b -> input.setText(color != null
                        ? ConfigEditorValues.hex(defaultColor) : rule.format(rule.defaultValue())))
                .dimensions(left, footer, third, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("取消"), b -> close())
                .dimensions(left + third + 6, footer, third, 20).build());
        apply = addDrawableChild(ButtonWidget.builder(Text.literal("应用"), b -> save())
                .dimensions(left + 2 * (third + 6), footer, third, 20).build());
        input.setChangedListener(this::edit);
        edit(input.getText());
        setInitialFocus(input);
    }

    private void edit(String text) {
        if (color != null) {
            color.edit(text);
            if (color.result().valid()) {
                for (ChannelSlider channel : channels) {
                    if (channel != null) channel.refresh();
                }
            }
        } else {
            number.edit(text);
        }
        boolean valid = valid();
        input.setEditableColor(valid ? TEXT : ERROR);
        apply.active = valid;
        if (number != null) {
            minus.active = valid && number.result().value() > rule.min();
            plus.active = valid && number.result().value() < rule.max();
        }
    }

    private boolean valid() {
        return color != null ? color.result().valid() : number.result().valid();
    }

    private void step(int direction) {
        input.setText(rule.step(number.text(), direction));
    }

    private void save() {
        boolean saved = color != null ? color.apply(colorSetter::accept) : number.apply(numberSetter);
        if (saved) {
            ShieldTweakConfig.get().save();
            close();
        }
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        if (input.isFocused() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            save();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, top + 4, TEXT);
        graphics.drawTextWithShadow(textRenderer, color != null ? "HEX（#RRGGBB）" : rule.range() + (unit.isBlank() ? "" : " · " + unit.trim()), left, top + 21, TEXT);
        if (color != null) {
            int swatchLeft = left + contentWidth - 52;
            graphics.fill(swatchLeft - 1, top + 33, left + contentWidth + 1, top + 55, TEXT);
            graphics.fill(swatchLeft, top + 34, left + contentWidth, top + 54, 0xFF000000 | color.preview());
        } else {
            graphics.drawTextWithShadow(textRenderer, "加减步长：" + rule.format(rule.step()) + unit, left, top + 64, TEXT);
            if (rule.kind() == ConfigEditorValues.NumberKind.INTEGER) {
                graphics.drawTextWithShadow(textRenderer, "20 tick = 1 秒", left, top + 80, TEXT);
            }
        }
        String message = valid() ? (color != null ? "预览颜色；白色表示不染色。应用后生效。" : "应用后保存；Esc 取消此次编辑。")
                : (color != null ? color.result().error() : number.result().error());
        int lineY = top + (color != null ? 181 : 98);
        for (var line : textRenderer.wrapLines(Text.literal(message), contentWidth)) {
            graphics.drawTextWithShadow(textRenderer, line, left, lineY, valid() ? TEXT : ERROR);
            lineY += 10;
        }
    }

    private final class ChannelSlider extends SliderWidget {
        private final int shift;
        private final String name;

        ChannelSlider(int x, int y, int width, int channel) {
            super(x, y, width, 20, Text.empty(), ((color.preview() >> (16 - channel * 8)) & 255) / 255.0);
            shift = 16 - channel * 8;
            name = new String[]{"红 R", "绿 G", "蓝 B"}[channel];
            updateMessage();
        }

        void refresh() {
            value = ((color.preview() >> shift) & 255) / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(name + "：" + (int) Math.round(value * 255)));
        }

        @Override
        protected void applyValue() {
            int rgb = (color.preview() & ~(255 << shift)) | ((int) Math.round(value * 255) << shift);
            input.setText(ConfigEditorValues.hex(rgb));
        }
    }
}
