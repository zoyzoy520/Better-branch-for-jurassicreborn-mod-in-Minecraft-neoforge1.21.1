# JR Tame All — Jurassic Reborn 全恐龙可驯服附加 mod

NeoForge 1.21.1 附加 mod（Mixin 实现）：解锁 [Jurassic Reborn](https://modrinth.com/mod/jurassic-reborn) 1.3.44 中**全部 ~109 个物种**的孵化印随（驯服）系统，并解锁全部物种的主人护卫。不改动原 mod jar，与原 mod 一起放入 mods 文件夹即生效。

## 工作原理

Jurassic Reborn 自带"孵化印随"驯服系统：孵化蛋右击孵化时（非潜行）`DinosaurEntity.setOwner(player)` 认主，之后可用空手右击打开指令 GUI（游荡/跟随/坐下）。唯一门槛是物种配置类 `Dinosaur.isImprintable()`，约 40 个物种（霸王龙、棘龙、南方巨兽龙等）在构造器中被设为 false。

本 mod 通过两个 Mixin（`@ModifyReturnValue`，来自 NeoForge 自带的 MixinExtras）把基类 `net.vit.jurassicreborn.common.entities.Dinosaurs.Dinosaur` 的两个 getter 返回值改为恒真：

- `isImprintable()` → true：全部物种孵化时可认主
- `shouldDefendOwner()` → true：全部物种注册 DefendOwnerAI / AssistOwnerAI（仅在有主人且下跟随指令时行动）

行为保留：潜行孵化仍不认主（原版设计）。

## 安装（游玩用）

将 `build/libs/jrtameall-1.0.0.jar` 与以下文件一起放入 mods 文件夹（Minecraft 1.21.1 + NeoForge ≥21.1.238）：

- `jurassicreborn-1.3.44.jar`（本体）
- `citadel-2.7.0-1.21.1.jar`（本体声明的依赖）
- `geckolib-neoforge-1.21.1-4.9.2.jar`（本体未声明但实际需要的依赖）
- `jei-1.21.1-neoforge-19.44.0.403.jar`（可选，本体以 `mandatory=false` 声明但在部分加载路径上仍会检查）

## 构建

```bash
# 需要：本机 JDK 17（启动 Gradle），Gradle 会自动下载 Temurin 21 作为工具链
# maven.neoforged.net 需走本地代理（gradle.properties 中已配置 127.0.0.1:7892）
JAVA_HOME="C:\Users\Mzdb\.jdks\ms-17.0.19" ./gradlew build
# 产物：build/libs/jrtameall-1.0.0.jar（约 3KB，只含本 mod 代码）
```

开发运行：`./gradlew runClient`（mods 放在 `run/mods/`）。

## 游戏内验证矩阵

1. **霸王龙（原不可驯服）**：/give 孵化蛋 → 非潜行右击孵化 → 聊天出现驯服消息 → 空手右击打开指令 GUI → 点"跟随"跟随玩家
2. **海洋物种**（如 mosasaurus）：同上
3. **负向**：潜行右击孵化 → 无驯服消息、无指令 GUI
4. **护卫**：驯服 + 跟随指令下被敌对生物攻击 → 恐龙反击
5. **回归**：原可驯服物种（如三角龙）行为不变

## 风险与备胎

- 若 JR 更新改动 getter：`defaultRequire: 1` 会直接报错（不静默失效）；mods.toml 已硬锁 JR `[1.3.44,1.3.45)`。
- 备胎方案：`src/main/java/net/example/jrtameall/mixin/DinosaurEntityMixin.java`（注释态），启用方式见文件头注释。
