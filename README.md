# 符箓 · FuLu

> A cultivation-themed (修仙) talisman & formation mod for **Minecraft 1.20.1 · Forge 47.x**
>
> 一个修仙向的符箓 / 阵法 / 妖丹模组，面向 **Minecraft 1.20.1 · Forge 47.x**

---

## 中文简介

用黄纸朱砂画符、布下阵法、猎取妖丹，再把妖丹塞进匣子里当"技能栏"用。

- **符纸与阵眼**：合成符纸，给阵眼装上不同的符 → 不同的阵法效果
- **14 种阵法**：内讧（让怪物互相打）、飞剑、反重力、替身、移祸、召唤、打滑、无明（致盲）、闭环、咫尺天涯、水墨、时缓、炼妖……
- **10 种妖丹**：僵尸 / 骷髅 / 苦力怕 / 蜘蛛 / 末影 / 烈焰 / 女巫 / 史莱姆 / 凋零骷髅 / 恶魂
  - 妖丹放进**妖丹匣**（6 格），按 `R` 打开轮盘选能力，按 `V` 选具体技能，按 `Z` 释放
  - 每颗妖丹有独立冷却，多个技能各算各的
- **炼妖阵**：在阵内击杀怪物掉对应妖丹
- **阵魔 boss**：带阶段、必杀技、处决弹反的超强敌人

### 按键

| 键 | 作用 |
|---|---|
| `R` | 选择生物能力（妖丹轮盘） |
| `V` | 选择当前妖丹的具体技能 |
| `Z` | 释放当前技能 |

### 妖丹一览

| 妖丹 | 技能 |
|---|---|
| 僵尸 | 绝仙瘴气：持续伤害 + 剥离增益 + 禁疗 |
| 骷髅 | 追踪箭：箭自动锁定视野内目标 |
| 苦力怕 | 自爆：不破坏地形 |
| 蜘蛛 | 盘丝天牢（法则级禁锢 + 湮灭）/ 蛛行（爬墙开关） |
| 末影 | 虚空放逐：与准星目标互换，低血怪直接被抹除并回血 |
| 烈焰 | 九幽冥火：慢速追踪火球；阴火状态每秒 3% 最大生命真伤 + 攻击反伤 |
| 女巫 | 诡计毒瓶：抛掷滞留毒瓶，每秒 4 次伤害 |
| 史莱姆 | 粘液沼泽：重力坍缩、跳起摔伤；你打泥沼里的怪必暴击 |
| 凋零骷髅 | 光翼连斩：2 秒三段扇形剑气，破甲 / 禁疗 / 30% 斩杀 / 第三斩破盾 |
| 恶魂 | 陨石天降（30 秒冷却）/ 恶魂飞行（开关） |

---

## English

A talisman-and-formation mod: craft talisman paper, slot talismans into formation eyes to build formations, and hunt essences dropped by mobs inside the *Refining Formation*. Essences go into an **Essence Box** (6 slots) that acts as your skill bar.

- **14 formations**: infighting, flying swords, anti-gravity, stand-in, blame shift, summon, slippery, blindness, loop, teleport, ink, time-slow, refining
- **10 essences**, each with its own cooldown
- **Keys**: `R` pick ability · `V` pick skill · `Z` cast

---

## Requirements

| Item | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x (tested on 47.2.20 / 47.4.x) |
| Java | 17 |
| GeckoLib | 4.x |

## Build

```bash
# Windows
gradlew.bat build
# Linux / macOS
./gradlew build
```

Output: `build/libs/fulu-1.0.0.jar`

> **Note:** the build depends on a local file
> `libs/geckolib-forge-1.20.1-4.4.9-deobf.jar` (declared in `build.gradle`).
> Keep it in place, or replace the dependency with the Maven coordinate
> `software.bernie.geckolib:geckolib-forge-1.20.1:4.4.9`.

## Install

1. Install Minecraft Forge 1.20.1
2. Put GeckoLib 4.x into `mods/`
3. Drop `fulu-1.0.0.jar` into `mods/`
4. Launch

## Project layout

```
src/main/java/com/lengyang/fulu/
  block/       formations, formation eyes, talisman kinds
  entity/      boss, minions, projectiles, visual-only effect entities
  refining/    essences, skills, cast pipeline, zone effects
  combat/      damage handling, essence passives, boss logic
  client/      screens (essence wheel, skill picker), overlays, renderers
  registry/    deferred registries
src/main/resources/
  assets/fulu/ models, textures, lang (zh_cn / en_us), animations
  data/fulu/   recipes, damage types, tags
docs/DEVLOG.md   full development log (Chinese, ~4000 lines)
```

## Development log

Every step, bug and verification is recorded in [`docs/DEVLOG.md`](docs/DEVLOG.md).

## License

MIT — see [`LICENSE`](LICENSE).
