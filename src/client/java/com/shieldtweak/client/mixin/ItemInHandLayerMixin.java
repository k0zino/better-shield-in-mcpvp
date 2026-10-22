package com.shieldtweak.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shieldtweak.client.ShieldStates;
import com.shieldtweak.client.ShieldTintContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 第三人称（自己 F5 与其他玩家）：为手持盾牌设置染色上下文 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void shieldtweak$beforeSubmitArm(
        final ArmedEntityRenderState state,
        final ItemStackRenderState item,
        final ItemStack itemStack,
        final HumanoidArm arm,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final CallbackInfo ci
    ) {
        ShieldTintContext.clear();
        if (itemStack.is(Items.SHIELD) && state instanceof AvatarRenderState avatar) {
            ShieldStates.applyTint(avatar, arm, itemStack);
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void shieldtweak$afterSubmitArm(
        final ArmedEntityRenderState state,
        final ItemStackRenderState item,
        final ItemStack itemStack,
        final HumanoidArm arm,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final CallbackInfo ci
    ) {
        ShieldTintContext.clear();
    }
}
