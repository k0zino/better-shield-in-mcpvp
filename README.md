# Better Shield in MCPvP

面向 PvP 的 Fabric **客户端** mod。它不改变任何游戏机制，只做一件事：**把盾牌的状态变成肉眼可见的信息**。

原版里，你无法一眼看出自己或对手的盾是不是真的在挡、是不是已经被斧头破掉了。这个 mod 用「模型位置」和「染色」把这两个状态直接画在屏幕上，并在破盾冷却结束的瞬间给你一个明确的声音提示。

---

## 功能

### 1. 低盾

下调第一人称的盾牌模型，避免举盾时挡住视野中央。下移量可调，设为 `0` 即关闭。

### 2. 状态染色

盾牌按当前状态染成不同颜色，**对自己和其他玩家都生效**：

| 状态 | 默认颜色 | 含义 |
| --- | --- | --- |
| 未举起 | 白（不染色） | 手上拿着盾但没在挡 |
| 举起前摇中 | 白（不染色） | 已经按下右键，但**还没开始格挡** |
| 举起且已生效 | 绿 `#55FF55` | 正在格挡，攻击会被挡下 |
| 禁用中 | 红 `#FF5555` | 被斧头破盾，冷却期间**无法格挡** |

**为什么要把「前摇」单独分出来？** 原版举盾不是瞬间生效的，按下右键后有 **5 tick（0.25 秒）前摇**，这段时间里盾不挡任何东西。如果只看「有没有举起来」就染绿，你会在最危险的瞬间误以为自己是安全的。本 mod 的判定直接取自实体的 `isBlocking()`，前摇期间保持未举起色，**真正开始格挡才变绿**。

### 3. 破盾恢复提示音

自己的盾从禁用状态恢复时，播放一个**上行双音**（默认 `pling` 音高 1.5 → 延迟 3 tick → 音高 2.0）。

**为什么要做双音？** 原版的破盾声 `item.shield.break` 是一声闷响。如果恢复提示也用单声、还用相近的音色，混战中你根本分不清「我刚被破盾」还是「我的盾好了」——这两个信息的应对方式完全相反。上行双音在听感上和破盾声有明确区别。两声的音效 ID、音量、音高、延迟都可独立配置，延迟设为 `0` 就退回单声。

---

## 支持版本

仓库里有**两套并行源码树**，各自对应一个 Minecraft 版本：

| 目录 | Minecraft | 映射 | Java | Fabric API | Mod Menu |
| --- | --- | --- | --- | --- | --- |
| `/`（根目录） | **26.2** | Mojang 官方映射 | 25 | `0.160.0+26.2` | `20.0.3` |
| `/1.21.11` | **1.21.11** | Yarn `1.21.11+build.6` | 21 | `0.141.6+1.21.11` | `17.0.1` |

两棵树功能完全一致，只是映射和版本 API 不同。26.2 的 GUI 层做过大重构（`GuiGraphics` → `GuiGraphicsExtractor`、`render()` → `extractRenderState()`、文本绘制被拆出独立管线），所以设置界面在两个版本里的实现细节有差异。

> **改代码请注意：** 任何功能改动都要在两棵树里对称同步，否则会造成版本行为不一致。

---

## 安装

1. 装好 [Fabric Loader](https://fabricmc.net/use/)
2. 下载对应版本的 **Fabric API**（必需）
3. 下载本 mod 的 jar，放进 `.minecraft/mods/`
4. （可选）装 **Mod Menu**，可以在 mod 列表里直接点「设置」打开配置界面

Mod Menu 是 `suggests` 而非 `depends`——没装也能正常用，只是少了那个入口。

---

## 用法

### 打开设置界面

| 方式 | 操作 |
| --- | --- |
| 按键 | 默认 **`K`**（可在 选项 → 控制 → 按键绑定 里改） |
| Mod Menu | mod 列表中找到本 mod，点「设置」 |

### 热切换

**所有改动点击即生效，并立刻写入配置文件。** 不需要重启游戏，也不需要点「保存」。

界面上每一行都是一个按钮，点一下切换到下一个预设值（颜色在 10 种预设间循环，数值在档位间循环）。窗口底部是「上一页 / 下一页 / 完成」。

### 配置文件

`config/shieldtweak.json`

界面改的内容会实时写进这个文件；反过来，**手动改文件需要重启游戏才生效**（配置只在客户端初始化时读一次）。

---

## 配置项

共 20 项。

### 低盾

| 键 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `lowShieldEnabled` | bool | `true` | 是否下调第一人称盾牌模型 |
| `lowShieldOffset` | double | `0.25` | 模型下移量（方块单位，正值向下） |

### 染色

| 键 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `tintEnabled` | bool | `true` | 染色总开关 |
| `tintSelf` | bool | `true` | 给自己的盾牌染色（第一/第三人称） |
| `tintOthers` | bool | `true` | 给其他玩家的盾牌染色 |
| `colorIdle` | color | `#FFFFFF` | 未举起 |
| `colorRaising` | color | `#FFFFFF` | 举起前摇中（尚未真正格挡） |
| `colorRaised` | color | `#55FF55` | 举起且已生效（格挡中） |
| `colorDisabled` | color | `#FF5555` | 禁用（破盾冷却中） |

颜色可用 `"#RRGGBB"`、`"0xRRGGBB"` 字符串或十进制整数表示。`#FFFFFF` 等于不染色。

配置中的 `null`、错误类型和非有限数值只会让对应项回退默认值，不会丢弃其他有效设置。音量限制在 `0–1`、音高限制在 `0.5–2`；下移量、延迟、推断时长和半径均不接受负值。

### 恢复提示音

| 键 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `recoverSoundEnabled` | bool | `true` | 自己的盾恢复时播放提示音 |
| `recoverSoundId` | string | `minecraft:block.note_block.pling` | 第一声音效 ID |
| `recoverSoundVolume` | float | `0.6` | 第一声音量 |
| `recoverSoundPitch` | float | `1.5` | 第一声音高 |
| `recoverSoundId2` | string | `minecraft:block.note_block.pling` | 第二声音效 ID（留空则只播一声） |
| `recoverSoundVolume2` | float | `0.6` | 第二声音量 |
| `recoverSoundPitch2` | float | `2.0` | 第二声音高 |
| `recoverSoundDelayTicks` | int | `3` | 第二声相对第一声的延迟（tick），`≤0` 则不播第二声 |

### 其他玩家禁用状态推断

| 键 | 类型 | 默认 | 说明 |
| --- | --- | --- | --- |
| `observedDisableEnabled` | bool | `true` | 是否通过破盾音效推断其他玩家的禁用状态 |
| `observedDisableTicks` | int | `100` | 推断出的禁用时长（tick），原版为 100（5 秒） |
| `observedDisableRadius` | double | `3.0` | 破盾音效归属判定的搜索半径（格） |

> **已知局限（重要）**
>
> 客户端**拿不到其他玩家的冷却同步数据**。要判断别人是不是被破盾了，本 mod 只能靠「听到 `item.shield.break` 音效 + 在半径内挑一个最可能的持盾玩家」来推断。
>
> 这意味着在多人混战、或音效来源玩家已离开渲染距离时，**可能标错人，也可能标不上**。
>
> - **自己的红色不受此影响**——走服务端的冷却同步包，完全准确。
> - **其他玩家的绿色不受此影响**——走渲染状态，完全准确。
> - 不想要这个不可靠的推断，把 `observedDisableEnabled` 设为 `false`，其他玩家就不再染红。
>
> 要做到对其他玩家完全准确，需要服务端也装 mod 或走自定义数据包同步。

---

## 从源码构建

需要对应版本的 JDK（26.2 分支要 JDK 25，1.21.11 分支要 JDK 21）。

```bash
# 构建 26.2 版本
./gradlew build
# 产物：build/libs/shieldtweak-mc26.2-1.0.1.jar

# 构建 1.21.11 版本
cd 1.21.11
./gradlew build
# 产物：1.21.11/build/libs/shieldtweak-mc1.21.11-1.0.1.jar
```

首次构建会下载 Minecraft、映射和依赖，需要联网且耗时较长。

`build` 会运行配置解析、恢复提示音的回归测试，以及四个 Mixin 目标类的实际加载和注入校验，也可以单独执行 `./gradlew test`。Release 中的两个 JAR 对应不同游戏版本，只安装与你的 Minecraft 版本匹配的文件；`-sources.jar` 是源码包，不用于游戏。

## 1.0.1 修复

- 修正设置页底部三个按钮的宽度，“完成”按钮保持在界面按钮区内。
- 破盾音效包在原版切回游戏主线程后才处理，避免跨线程访问玩家列表和观察记录。
- 第一人称按实际使用手判断盾牌状态，双持盾时不会同时将未使用的盾染绿；低盾变换在渲染后恢复。
- 第三人称正确处理零前摇的自定义盾牌，不会把未使用的盾判成正在格挡。
- 切换世界、重生或关闭推断时清除观察状态；自己的禁用状态始终以同步冷却为准。
- 修复 `0x` 颜色解析，并逐项校验配置中的空值、类型和数值范围。
- 关闭恢复音效、再次破盾或切换世界会取消待播的第二声；单人游戏暂停时不推进提示音倒计时。
- 固定 Loom 版本，并在构建产物名称中标明 Minecraft 版本，避免发布时文件重名。

开发环境启动客户端：

```bash
./gradlew runClient
```

---

## 实现方式

纯 Mixin，没有自绘模型或材质。四个 mixin，两套映射下各自的注入点：

| 功能 | 26.2（Mojmap） | 1.21.11（Yarn） |
| --- | --- | --- |
| 低盾·第一人称 | `ItemInHandRenderer.renderItem` | `HeldItemRenderer.renderItem` |
| 低盾·第三人称 | `ItemInHandLayer.submitArmWithItem` | `HeldItemFeatureRenderer.renderItem` |
| 染色 | `ShieldSpecialRenderer` + `@ModifyArg` | `ShieldModelRenderer` + `@ModifyArg` |
| 推断他人禁用 | `ClientPacketListener.handleSoundEvent` | `ClientPlayNetworkHandler.onPlaySound` |

其余部分不走 mixin：

| 功能 | 手段 |
| --- | --- |
| 状态判定 | 读取实体的 `BlocksAttacks` / `BlocksAttacksComponent` 组件与 `isBlocking()`（前摇期间它返回 false，这正是分色的依据） |
| 染色上下文 | 静态 `ShieldTintContext` 在渲染前写入当前状态，`@ModifyArg` 改写 `tintedColor` 参数（26.2 是第 5 个参数，1.21.11 是第 8 个） |
| 恢复提示音 | 客户端 tick 回调里检测冷却从「有」到「无」的跳变 |
| 设置界面 | 纯 `Button` 构成，零自绘——26.2 的 `GuiGraphicsExtractor` 没有 `drawString`，文本绘制被拆到了独立管线 |

---

## 许可

[CC0-1.0](LICENSE) —— 公有领域。随便用、随便改、随便打包进整合包，不需要署名。
