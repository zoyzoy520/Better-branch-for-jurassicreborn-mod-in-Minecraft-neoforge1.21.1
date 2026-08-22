# JR Tame All — Jurassic Reborn 恐龙更易驯养附加 mod

NeoForge 1.21.1 附加 mod（Mixin 实现）：让 [Jurassic Reborn](https://modrinth.com/mod/jurassic-reborn) 1.3.44 中的恐龙**更容易驯养、养护与乘骑**——解锁全部 ~109 个物种的孵化印随（驯服）系统与主人护卫，新增驯服棒、代谢恒定、动物和平规则与完整的恐龙乘骑系统。不改动原 mod jar，与原 mod 一起放入 mods 文件夹即生效。

> **版权声明**：本 mod 为**非官方**第三方附加 mod，与 Jurassic Reborn Team 无隶属关系。本仓库不包含、不重新分发 Jurassic Reborn 的任何代码或资产（其采用 All Rights Reserved 许可，版权归 Jurassic Reborn Team 所有）；Jurassic Reborn 本体需从官方渠道自行获取。详见 [NOTICE.md](NOTICE.md)。本 mod 自身代码按 [MIT](LICENSE) 许可发布。

## 功能总览（按版本）

### 1.0.0 — 全物种驯服解锁 + 和平规则

- **全物种孵化印随**：全部 ~109 个物种（霸王龙、棘龙、南方巨兽龙等约 40 个原版被禁物种）孵化时可认主训服；潜行孵化仍不认主（保留原版设计）。
- **全物种主人护卫**：注册 DefendOwnerAI / AssistOwnerAI（仅在有主人且下跟随指令时行动）。
- **和平规则**（`PeaceTargetMixin`，拦截恐龙所有攻击目标的唯一通道）：
  - 驯服的恐龙永不再攻击玩家（任意玩家）或其他驯服恐龙（任意主人）；
  - 翼龙（FlyingDinosaurEntity）永不互相攻击（野生或驯服）；
  - 海洋生物（`isMarineCreature()`）永不互相攻击（野生或驯服）。

### 1.1.0 — 驯服棒 + 机器与化石增强

- **驯服棒**：原版工具（TOOLS）创造栏中的新物品，右键任意野生恐龙强制认主（保持当前成长阶段，无合成配方）。
- **化石研磨机**：速度 ×5；软组织提取成功率 ×5（1/6 → 5/6）。
- **孵蛋器**：孵化速度 ×10。
- **胚胎机（胚胎机子宫/孕育机）**：孕育速度 ×10。
- **化石类提取必然成功**：化石蛋、石化木、植物化石的软组织/植物提取成功率 ×5（原 1/3、1/4、1/4 超过 100% 时钳位为必然成功）。

### 1.2.0 — 代谢恒定

- **驯服恐龙无需进食饮水**：能量与水分每 tick 拉满（关闭代谢消耗），配对等活力消耗也在下一 tick 即时补满。

### 1.3.0 — 乘骑系统

- **猪式骑乘**：成年（>75% 生长阶段）驯服恐龙手持胡萝卜钓竿右击上马，潜行下马。
- **陆地骑乘**：方向转向、自动爬升台阶。
- **海洋骑乘**：俯仰潜水/上浮（水生恐龙需在水中上马）。
- **飞行骑乘**：起飞跳跃、俯仰爬升/俯冲。
- **纯坐骑模式**：骑乘时恐龙不战斗、不睡觉、不坐下。
- **每物种座位偏移**：各体型恐龙身体重心手工调校，坐姿贴合。
- 驯服棒更新为 T 形贴图。

## 工作原理

Jurassic Reborn 自带"孵化印随"驯服系统：孵化蛋右击孵化时（非潜行）`DinosaurEntity.setOwner(player)` 认主，之后可用空手右击打开指令 GUI（游荡/跟随/坐下）。唯一门槛是物种配置类 `Dinosaur.isImprintable()`，约 40 个物种（霸王龙、棘龙、南方巨兽龙等）在构造器中被设为 false。

本 mod 通过两个 Mixin（`@ModifyReturnValue`，来自 NeoForge 自带的 MixinExtras）把基类 `net.vit.jurassicreborn.common.entities.Dinosaurs.Dinosaur` 的两个 getter 返回值改为恒真：

- `isImprintable()` → true：全部物种孵化时可认主
- `shouldDefendOwner()` → true：全部物种注册 DefendOwnerAI / AssistOwnerAI（仅在有主人且下跟随指令时行动）

其余功能（驯服棒、和平规则、代谢、乘骑）通过对应的独立 Mixin 与物品类实现，详见 `src/main/java/net/example/jrtameall/`。

## 安装（游玩用）

将以下文件全部放入 mods 文件夹（Minecraft 1.21.1 + NeoForge ≥21.1.238）：

- `jrtameall-1.3.0.jar`（本 mod，从 GitHub Releases 获取）
- `jurassicreborn-1.3.44.jar`（本体）
- `citadel-2.7.0-1.21.1.jar`（本体声明的依赖）
- `geckolib-neoforge-1.21.1-4.9.2.jar`（本体未声明但实际需要的依赖）
- `jei-1.21.1-neoforge-19.44.0.403.jar`（可选，本体以 `mandatory=false` 声明但在部分加载路径上仍会检查）

## 构建

```bash
# 需要：本机 JDK 17（启动 Gradle），Gradle 会自动下载 Temurin 21 作为工具链
# maven.neoforged.net 需走本地代理（gradle.properties 中已配置 127.0.0.1:7892）
JAVA_HOME="C:\Users\Mzdb\.jdks\ms-17.0.19" ./gradlew build
# 产物：build/libs/jrtameall-1.3.0.jar（约 40KB，只含本 mod 代码）
```

开发运行：`./gradlew runClient`（mods 放在 `run/mods/`）。

## 游戏内验证要点

1. **驯服解锁**：/give 孵化蛋（如霸王龙）→ 非潜行右击孵化 → 聊天出现驯服消息 → 空手右击打开指令 GUI
2. **驯服棒**：创造栏 Tools 页取用 → 右键野生恐龙 → 立即认主
3. **和平规则**：驯服恐龙不攻击玩家和被其他恐龙攻击时不还手（非生物仇恨目标）
4. **护卫**：驯服 + 跟随指令下被敌对生物攻击 → 恐龙反击
5. **代谢**：驯服恐龙饥饿值/水分条维持满值，无需喂食
6. **乘骑**：成年驯服恐龙 + 胡萝卜钓竿 → 陆地/海洋/飞行按物种测试
7. **负向**：潜行右击孵化 → 无驯服消息；未驯服恐龙无法上马
8. **回归**：原可驯服物种（如三角龙）行为不变

## 风险与备胎

- 若 JR 更新改动 getter：`defaultRequire: 1` 会直接报错（不静默失效）；mods.toml 已硬锁 JR `[1.3.44,1.3.45)`。
- 备胎方案：`src/main/java/net/example/jrtameall/mixin/DinosaurEntityMixin.java`（注释态），启用方式见文件头注释。
