package com.shieldtweak.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu 入口：在 mod 列表里点“设置”打开配置界面。
 *
 * 这个 entrypoint 只在 Mod Menu 存在时才会被 Fabric Loader 加载，
 * 所以 Mod Menu 未安装时本类根本不会被触碰，不需要额外的存在性判断。
 */
public class ShieldTweakModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ShieldTweakConfigScreen::new;
    }
}
