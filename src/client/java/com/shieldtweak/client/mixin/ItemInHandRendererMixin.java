package com.shieldtweak.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shieldtweak.client.ShieldStates;
import com.shieldtweak.client.ShieldTintContext;
import com.shieldtweak.client.ShieldTweakConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 第一人称：下调盾牌模型 + 设置染色上下文 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void shieldtweak$beforeRenderItem(
        final LivingEntity entity,
        final ItemStack stack,
        final ItemDisplayContext displayContext,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final CallbackInfo ci
    ) {
        if (!stack.is(Items.SHIELD)) {
            return;
        }
        ShieldTweakConfig cfg = ShieldTweakConfig.get();
        if (cfg.lowShieldEnabled) {
            poseStack.translate(0.0, -cfg.lowShieldOffset, 0.0);
        }
        ShieldStates.applyTint(entity, stack);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void shieldtweak$afterRenderItem(
        final LivingEntity entity,
        final ItemStack stack,
        final ItemDisplayContext displayContext,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final CallbackInfo ci
    ) {
        ShieldTintContext.clear();
    }
}
