package com.shieldtweak.client;

/**
 * 渲染线程上的临时染色上下文：在提交盾牌模型前设置颜色，
 * ShieldSpecialRendererMixin 读取后由调用方清除。
 */
public final class ShieldTintContext {
    private static int argb = -1;

    private ShieldTintContext() {
    }

    public static void set(final int rgb) {
        argb = 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static void clear() {
        argb = -1;
    }

    public static boolean active() {
        return argb != -1;
    }

    public static int argb() {
        return argb;
    }
}
