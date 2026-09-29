package com.shieldtweak.client.mixin;

import com.shieldtweak.client.ShieldTintContext;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修改盾牌基础模型提交时的 tintedColor（绝对参数下标 5），实现按状态整体染色
 * （不影响旗帜图案与附魔光效）。
 * require = 0：在改动过此类的环境（如 Lunar）中匹配不到时静默跳过，而不是崩溃。
 */
@Mixin(ShieldSpecialRenderer.class)
public abstract class ShieldSpecialRendererMixin {
    @ModifyArg(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;Lnet/minecraft/client/resources/model/sprite/SpriteGetter;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
        ),
        index = 5,
        require = 0
    )
    private int shieldtweak$tintShield(final int tintedColor) {
        return ShieldTintContext.active() ? ShieldTintContext.argb() : tintedColor;
    }
}
