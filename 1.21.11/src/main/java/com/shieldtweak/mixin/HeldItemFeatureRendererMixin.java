package com.shieldtweak.mixin;

import com.shieldtweak.ShieldStates;
import com.shieldtweak.ShieldTintContext;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 第三人称（自己 F5 与其他玩家）：为手持盾牌设置染色上下文 */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void shieldtweak$beforeRenderItem(
        final ArmedEntityRenderState entityState,
        final ItemRenderState itemState,
        final ItemStack stack,
        final Arm arm,
        final MatrixStack matrices,
        final OrderedRenderCommandQueue queue,
        final int light,
        final CallbackInfo ci
    ) {
        ShieldTintContext.clear();
        if (stack.isOf(Items.SHIELD) && entityState instanceof PlayerEntityRenderState playerState) {
            ShieldStates.applyTint(playerState, arm, stack);
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void shieldtweak$afterRenderItem(
        final ArmedEntityRenderState entityState,
        final ItemRenderState itemState,
        final ItemStack stack,
        final Arm arm,
        final MatrixStack matrices,
        final OrderedRenderCommandQueue queue,
        final int light,
        final CallbackInfo ci
    ) {
        ShieldTintContext.clear();
    }
}
