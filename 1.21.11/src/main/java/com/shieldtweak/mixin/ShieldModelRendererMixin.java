package com.shieldtweak.mixin;

import com.shieldtweak.ShieldTintContext;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修改盾牌基础模型提交时的 tintedColor，实现按状态整体染色（不影响旗帜图案与附魔光效）。
 * require = 0：在改动过此类的环境（如 Lunar）中匹配不到时静默跳过，而不是崩溃。
 */
@Mixin(ShieldModelRenderer.class)
public abstract class ShieldModelRendererMixin {
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"
        ),
        index = 8,
        require = 0
    )
    private int shieldtweak$tintShield(final int tintedColor) {
        return ShieldTintContext.active() ? ShieldTintContext.argb() : tintedColor;
    }
}
