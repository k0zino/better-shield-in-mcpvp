package com.shieldtweak.client;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 通过 Fabric Loader 加载实际目标类，验证 Mixin 的方法签名和注入点。 */
class MixinSmokeTest {
    @Test
    void allShieldMixinsApplyToVanillaClasses() {
        for (String name : new String[]{
                "net.minecraft.client.multiplayer.ClientPacketListener",
                "net.minecraft.client.renderer.ItemInHandRenderer",
                "net.minecraft.client.renderer.entity.layers.ItemInHandLayer",
                "net.minecraft.client.renderer.special.ShieldSpecialRenderer"
        }) {
            Class<?> target = assertDoesNotThrow(() -> Class.forName(name, false, getClass().getClassLoader()), name);
            assertTrue(Arrays.stream(target.getDeclaredMethods())
                    .anyMatch(method -> method.getName().contains("shieldtweak$")),
                    () -> "Shield Tweak mixin was not applied to " + name);
        }
    }
}
