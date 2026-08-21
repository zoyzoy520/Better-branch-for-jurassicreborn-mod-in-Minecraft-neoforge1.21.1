# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目性质

NeoForge 1.21.1 **Mixin 附加 mod**:通过注入方式修改**闭源**的 Jurassic Reborn 1.3.44(包 `net.vit.jurassicreborn`,无源码,见 libs/ 下 jar)。不改原 mod jar。版本锁定:mods.toml 硬锁 JR `[1.3.44,1.3.45)`,mixins.json 设 `defaultRequire: 1`(注入点失效即崩溃,不静默失效)。

功能层(按版本):1.0.0 全物种驯服解锁 + 和平规则 → 1.1.0 驯服棒 + 机器速度/幸运 → 1.2.0 代谢恒定 → 1.3.0 骑乘系统。

## 构建与运行

```bash
JAVA_HOME="C:\Users\Mzdb\.jdks\ms-17.0.19" ./gradlew build    # 产物 build/libs/jrtameall-<version>.jar
JAVA_HOME="C:\Users\Mzdb\.jdks\ms-17.0.19" ./gradlew runClient  # 后台启动,mods 在 run/mods/
```

- 无测试代码(`test` task 为 NO-SOURCE);验证方式是 runClient + 游戏内实测
- gradle.properties 中本地代理(127.0.0.1:7892)已注释:NeoForge 制品全在 Gradle 缓存中,直接可达;只有需要拉取新 NeoForge 制品时才恢复代理配置
- 版本号要同步改两处:gradle.properties 的 `version` 和 mods.toml 的 `version`(漏改会导致 jar 文件名与内容版本不符)
- 每次功能变更 → build → runClient → 用户游戏内验证;提交按 ver-X.Y.Z 分支(如 ver-1.3.0),commit message 用简洁英文

## Mixin 关键约束(踩过的坑)

- **`@Shadow`/`@Inject` 只认目标类自身声明的方法**:继承来的方法(如 `getOrder` 在父类 DinosaurEntity)会 InvalidMixinException 崩溃。解法:直接 cast `((DinosaurEntity) (Object) this).getOrder()`
- **五个骑乘钩子用"声明式覆写"**:Mixin 无法 `@Inject` 继承的 vanilla 钩子(getControllingPassenger/getRiddenInput/getRiddenSpeed/tickRidden/getPassengerRidingPosition),改为在 mixin 类中直接声明同名方法,并入目标类形成覆写(均非 final)。这是骑乘激活的核心
- **`(Object) this instanceof X` 语法**:直接 `this instanceof X` 编译失败
- **javac 把子类体内的继承方法调用解析为 SUBCLASS owner**:`@ModifyExpressionValue` 的 `target` 必须写子类(如 `SwimmingDinosaurEntity;isCarcass()Z`),不能写基类 DinosaurEntity
- `defaultRequire: 1` 生效,注入点若随 JR/原版更新失效 → 崩溃,这是特性不是 bug
- 同一目标类可被多个 mixin 注入,但方法签名冲突的辅助方法要改名(`jr_tame_all$` 前缀)

## 骑乘系统架构(1.3.0)

**移动执行模型**:`travel` 只在骑手客户端执行(`isControlledByLocalInstance` = 本地玩家),服务端经 ServerboundMoveVehiclePacket 收位置。因此:移动逻辑放 travel/覆写钩子(客户端);服务端状态逻辑(唤醒、清目标、nav.stop、水下呼吸)放 `tickRidden`(双端都调用)。`LocalPlayer.input.jumping` 是客户端独有 → 引用前必须 `level().isClientSide &&` 短路,否则专用服务器链接类崩溃。

五个注册 mixin 分工:
- `DinosaurRidingMixin`(@Mixin DinosaurEntity):五个钩子覆写 + 上马门槛(mobInteract HEAD 注入,客户端宽松校验、服务端全量校验:主人/成年>75%/宽度≥猪/水生必须在水里)+ 死亡播种 fallDistance
- `DinosaurRidingTravelMixin`:travel 内 `canDinoSwim()` 骑乘时强制 true(陆地恐龙入水不走 JR 沉水分支)
- `DinosaurRidingStateMixin`:SIT/睡眠/战斗目标/immobile 守卫 + 内部嵌套 FlyingStateMixin(翼龙 isImmobile)
- `MarineRidingMixin`(@Mixin 四个目标,嵌套类):Swimming/Amphibian/Penguin 的 `isCarcass()` 骑乘时强制 true(跳过 JR 服务端阻尼下沉,落入原版水中 travel,俯仰生效);Crocodile 的 `isBaskingNow()` 强制 false
- `FlyingRidingMixin`:空中 travel 全接管(原版飞行分支不消费 input)+ SIT 禁飞行动画/禁起飞(startTakeOff HEAD 拦截,因为 JR 的 AIStartFlying goal 不检查 SIT)

**座位高度**:`getPassengerRidingPosition` 公式 = `max(bbHeight*0.6, 0.45) + 尺寸档位(1.5/3.0) + SeatOffsets 手工调校值`。SeatOffsets 是按物种手工调的表(每个条目有注释记录调整历史)。

## 未注册的参考文件(别误以为生效)

`jr_tame_all.mixins.json` 之外的 `mixin/` 文件**不生效**,保留作参考/实验:
- `WaterRidingTravelMixin`:废弃的水中加速方案(探测证明 LivingEntity.travel 不在被骑恐龙路径上),文件头有说明
- `DinosaurRendererRidingMixin` + `TabulaModelAccessor` + `DinosaurModelAccessor` + `RidingSeatCache`:渲染时测量模型真实顶部算座位的实验方案,被 SeatOffsets 手工调校取代
- 新增 mixin 文件必须手动注册进 mixins.json 才生效

## 调试闭源 JR

- JR 无源码:javap 反汇编(javap 用 JDK26:`/c/Users/Mzdb/.jdks/openjdk-26.0.1/bin/javap`),jar 解压到 /tmp/jrjar
- `System.out.println` 会进 gradle task 输出文件;崩溃看 `run/crash-reports/` 和 `run/logs/latest.log`
- 原版 1.21.1 源码/反编译在 `build/moddev/artifacts/neoforge-21.1.248-sources.jar`;原版资源 jar 在 Gradle 缓存 `~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar`

## 贴图工具链

原版贴图资源从 `minecraft_1.21.1_client.jar` 提取;无 PIL 环境,PNG 读写用标准库手写脚本(zlib+struct,支持 8bit RGBA 和 4bit palette+tRNS),工作目录 /tmp/stickbone。改贴图流程:先生成预览 PNG 到项目根目录 → 用户确认 → 才复制进 `src/main/resources/` 并构建。
