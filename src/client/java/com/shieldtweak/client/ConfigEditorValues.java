package com.shieldtweak.client;

import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;

/** 编辑草稿与校验，不依赖游戏窗口，也不会在输入途中改写配置。 */
final class ConfigEditorValues {
    private ConfigEditorValues() { }

    record Result<T>(T value, String error) {
        boolean valid() { return value != null; }
    }

    static Result<Integer> color(String text) {
        String value = text.trim().replaceFirst("^(#|0[xX])", "");
        if (!value.matches("[0-9a-fA-F]{6}")) {
            return new Result<>(null, "请输入 6 位十六进制颜色，例如 #55FF55");
        }
        return new Result<>(Integer.parseInt(value, 16), "");
    }

    static String hex(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    enum NumberKind { DECIMAL, FLOAT, INTEGER }

    record NumberRule(NumberKind kind, double min, double max, double step, double defaultValue) {
        Result<Double> parse(String text) {
            final BigDecimal decimal;
            try {
                decimal = new BigDecimal(text.trim());
            } catch (NumberFormatException e) {
                return new Result<>(null, "请输入有效数字");
            }
            double value = decimal.doubleValue();
            if (!Double.isFinite(value)) {
                return new Result<>(null, "数值过大，请输入有限数字");
            }
            if (kind == NumberKind.INTEGER && decimal.stripTrailingZeros().scale() > 0) {
                return new Result<>(null, "tick 数必须是整数");
            }
            if (decimal.compareTo(BigDecimal.valueOf(min)) < 0
                    || decimal.compareTo(BigDecimal.valueOf(max)) > 0) {
                return new Result<>(null, range());
            }
            return new Result<>(kind == NumberKind.FLOAT ? (double) (float) value : value, "");
        }

        String format(double value) {
            String canonical = kind == NumberKind.FLOAT ? Float.toString((float) value) : Double.toString(value);
            return new BigDecimal(canonical).stripTrailingZeros().toPlainString();
        }

        String range() {
            if (max == Double.MAX_VALUE) return "范围：大于或等于 " + format(min);
            return "范围：" + format(min) + " ～ " + format(max)
                    + (kind == NumberKind.INTEGER ? "（整数）" : "");
        }

        String step(String text, int direction) {
            Result<Double> parsed = parse(text);
            if (!parsed.valid()) return text;
            BigDecimal value = new BigDecimal(format(parsed.value()))
                    .add(BigDecimal.valueOf(step).multiply(BigDecimal.valueOf(direction)))
                    .max(BigDecimal.valueOf(min)).min(BigDecimal.valueOf(max));
            return format(value.doubleValue());
        }
    }

    static final class Draft<T> {
        private final Function<String, Result<T>> parser;
        private String text;
        private Result<T> result;
        private T preview;

        Draft(String initial, Function<String, Result<T>> parser) {
            this.parser = parser;
            edit(initial);
            if (!result.valid()) throw new IllegalArgumentException("Invalid initial value: " + initial);
        }

        void edit(String text) {
            this.text = text;
            result = parser.apply(text);
            if (result.valid()) preview = result.value();
        }

        String text() { return text; }
        Result<T> result() { return result; }
        T preview() { return preview; }

        boolean apply(Consumer<T> setter) {
            if (!result.valid()) return false;
            setter.accept(result.value());
            return true;
        }
    }
}
