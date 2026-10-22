package com.shieldtweak.mixin;

import com.shieldtweak.ShieldStates;
import com.shieldtweak.ShieldTintContext;
import com.shieldtweak.ShieldTweakConfig;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 第一人称：下调盾牌模型 + 设置染色上下文 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void shieldtweak$beforeRenderItem(
        final LivingEntity entity,
        final ItemStack stack,
        final ItemDisplayContext renderMode,
        final MatrixStack matrices,
        final OrderedRenderCommandQueue orderedRenderCommandQueue,
        final int light,
        final CallbackInfo ci
    ) {
        ShieldTintContext.clear();
        if (!stack.isOf(Items.SHIELD) || (renderMode != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                && renderMode != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
            return;
        }
        matrices.push();
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (cfg.lowShieldEnabled) {
            matrices.translate(0.0, -cfg.lowShieldOffset, 0.0);
        }
        ShieldStates.applyTint(entity, stack, renderMode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                ? Arm.RIGHT : Arm.LEFT);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void shieldtweak$afterRenderItem(
        final LivingEntity entity,
        final ItemStack stack,
        final ItemDisplayContext renderMode,
        final MatrixStack matrices,
        final OrderedRenderCommandQueue orderedRenderCommandQueue,
        final int light,
        final CallbackInfo ci
    ) {
        if (stack.isOf(Items.SHIELD) && (renderMode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || renderMode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
            matrices.pop();
        }
        ShieldTintContext.clear();
    }
}
