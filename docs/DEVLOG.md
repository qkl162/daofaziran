# 符箓 · FuLu

> **Forge 1.20.1** · 独立小模组 · 产物 `build/libs/fulu-1.0.0.jar`
>
> - 第一步 ✅ **游戏里出现了"符纸"这个物品**（代号 `talisman_paper`，中文名"符纸"）
> - 第二步 ✅ **游戏里出现了"阵眼"这个方块**（代号 `formation_eye`，中文名"阵眼"）
> - 第三步 ✅ **阵眼有了自己的运行逻辑**（方块实体每刻执行一次，已在无头服务端里跑通）
> - 第四步 ✅ **阵眼每 5 秒放一次灵气粒子**（自定义粒子 `spirit_mote` + 倒计时冷却，已实跑核对）
> - 第五步 ✅ **阵法开关**：默认关，右键开 / 关；关着时整座阵一件活都不干
> - 第六步 ✅ **扫描 11×11×11 内的敌对怪物**并打印"找到 X 只怪物"（已用真僵尸跑过四阶段对照）
> - 第七步 ✅ **范围改成"上方无限、下方 10 格"**，结果改成发给周围 16 格玩家的**动作栏**
> - 第八步 ✅ **范围内的怪物互相攻击**（少于 2 只不动手；实测真掉血、真打死了一只）
> - 第九步 ✅ **改成"每一只都分一个目标"**（全场大乱斗；实测每轮分配数 == 怪物数）
> - 第十步·第一部分 ✅ **"有没有符"开关 + 真的存进 NBT**（实测过了区块卸载/重载，还从区域文件里读出了那个值）
> - 第十步·第二部分 ✅ **第二套外观**（有符 / 没符两张贴图两个模型，由方块状态切换）
> - 第十步·第三部分 ✅ **右键装符 / 取符**（连带把"乱神符"这个物品造了出来）
> - 第十步·第四部分 ✅ **每刻逻辑切成两段**（没符只扫描播报，有符才让怪物互相打）
> - 第十步·第五部分 ✅ **去掉冷却**（每刻直接往下走；实测第二段从"16 秒 8 行"变成"544 行"）
> - 第十步·第六部分 ✅ **把快慢拆开**（扫描/内讧每刻，播报/粒子回到 5 秒；两边都实测过）
> - 第十步·第七部分 ✅ **有符就自动开启**（符成了唯一的开关，第五步的手动开关整套删掉）
> - 第十步·第八部分 ✅ **状态永久保存**（两个服务器进程、同一个世界实测：NBT 与外观都回来了）
> - 第十步·第九部分 ✅ **内讧负面效果**（纯标记，结构上不掉血；挂/清都实测过）
> - 第十步·第十部分 ✅ **传递攻击效果**（尸壳把饥饿传给被打的怪）+ 里面无限 / 出圈 20 秒
> - 第十步·第十一部分 ✅ **内讧粒子**（原版"愤怒的村民"，只发给 16 格内的玩家，每只 1~2 颗）
> - 第十一步·第一部分 ✅ **范围改成 ±10**（水平各 10 格；边界 10 计入 / 11 不计，实测过）
> - 第十一步·第二部分 ✅ **右键显示边界**（木棍右键 → 一圈粒子，只给本人看，3 秒消失）
> - 第十一步·第三部分 ✅ **右键显示阵法信息**（空手没符 → 状态 / 半径 / 怪物数量，复用已扫到的数字）
> - 第十二步 ✅ **阵法图鉴**（物品 + 配方 + 三阶段解锁；？？？→ 名字 → 详情，实测过）
> - 第十三步 ✅ **图鉴界面 GUI**（列表/状态图标/详情/搜索/关闭 + 阵眼界面；逻辑实测，画面待你验）
> - 第四部分 ✅ **乱神阵的图鉴内容**（已见闻 3 行 / 已精通 6 行，逐字核对）
> - 第五部分 ✅ **朱砂 / 阵基 + 图鉴配方**（方块能放能挖、掉落表实测；差点漏掉 mineable 标签）
> - 第六部分 ✅ **图鉴物品说明 + 解锁进度**（存玩家 NBT + 计分板镜像；"退出重进不丢"两进程实测）
> - 第七部分 ✅ **界面 / 内容逐条对照**（示意图用语言文件真实字串生成；355 项自检全过）
> - 第八部分 ✅ **修两个实测 bug**（界面读不到解锁进度 / 左上角物品提示；截图反馈驱动）
> - 第九部分 ✅ **飞剑符 + 阵眼装两种符**（TalismanKind 三值 + 存档迁移；对照实验实测）
> - 第十部分 ✅ **飞剑阵的效果**（每秒一道飞行飞剑、5 点伤害、音效；实测 5.0/发）
> - 第十一部分 ✅ **飞剑粒子 + 拖尾剑光**（新粒子类型 sword_glow；每格一颗、命中炸 8 颗）
> - 第十二部分 ✅ **命中爆发 + 两个粒子的行为**（sword_burst；按方向转光带、0.4 格间距、命中炸一圈 10 颗）
> - 第十三部分 ✅ **飞剑实体**（`fulu:flying_sword`；自己追目标、打 5 点、打完消失）+ 飞剑符用你的图重做
> - 第十四部分 ✅ **发射逻辑**（每秒 10 把齐射；按怪物数量分配 10/5+5/3+3+4/…；出生点散开）
> - 第十五部分 ✅ **飞行行为 / 拖尾 / 音效**（锁定目标、0.5 格/刻、撞方块消失、每刻 1~2 颗拖尾、发射声只播一次）
> - 第十六部分 ✅ **拿掉拖尾剑光**（sword_glow 连类带贴图全删）+ **修掉"十把剑只掉 5 点血"**（无敌帧）
> - 第十七部分 ✅ **换成玄剑 bbmodel**（自动转换 + 重映射贴图）+ **每把剑独立** + **符文从剑尾绕到剑锋**
> - 第十八部分 ✅ **骨骼 + 四段动画**（剑身父骨骼；idle/fly/launch/hit；朝向仍在渲染器里）
> - 第十九部分 ✅ **修"有些怪物无法识别"**（`Monster` 类 → `Enemy` 接口；史莱姆/岩浆怪/恶魂/幻翼/疣猪兽；驯服的不打）
> - 第二十部分 ✅ **反重力阵**（反重力符 + 第四种状态 + 浮空效果/上限/粒子/音效 + 图鉴三条齐全）
> - 第二十一部分 ✅ **反重力改成"倒立"**（效果只当标记 + 渲染事件翻 180° + 不能走但**能攻击** + 音效改成整个阵法一声）
> - 第二十二部分 ✅ **修"创造标签页没有物品"**（反重力符漏加 + 物品只在自己页签、原版页签里一个都没有）
> - 第二十三部分 ✅ **修"反重力符装不进阵眼"**（`of()` 手写 if 链漏了新符 → 改成只问 `item()`）+ 阵眼反重力外观做明显
> - 第二十四部分 ✅ **五条清单逐条核对**（都已实现；补上"能存能读"的落盘实测 + 修掉自检里同形状的两处手写清单）
> - 第二十五部分 ✅ **修"没有升空"**（位置每刻走的距离 = 设的速度值；RISE 改回字面 0.02、HOVER=0）+ **负面效果 + 阵法内无限时长**
> - 第二十六部分 ✅ **倒立改成显式注册**（注解扫描会静默失败 → 改成 `EVENT_BUS.register`）+ 翻转时写日志，让"跑没跑"可验证
> - 第二十七部分 ✅ **修掉倒立的真正原因**：平移的**符号写反**（身体被推到头顶上方 `[-2h,-h]`）→ 改成绕身体中心转，与通用做法逐点相同
> - 第二十八部分 ✅ **再检查一遍**：按 **Forge 补丁文件**核对钩子位置与原版变换链，确认当前变换「倒立且原地」；并给出**一条命令**把"回调没跑"和"标记没到"分开
> - 第二十九部分 ✅ **画面还是没倒立**：先排除"这只怪在名单外"（幻翼/恶魂/蜜蜂/蝙蝠/BOSS 本就不翻）+ 回调现在会**在怪物头顶放一粒紫色粒子**自证
> - 第三十部分 ✅ **找到真因**：原版 `scale(-1,-1,1)` **就等于**绕 Z 转 180°，我用旋转去翻被它抵消成**恒等**（可证明的空操作）→ 改成**反射** `scale(1,-1,1)`，并加断言禁止再改回 `mulPose`
> - 第三十一部分 ✅ **补上 Gradle wrapper + IDEA 打开步骤**（IDEA 满屏红是因为原来没有 wrapper、Gradle 没同步，不是代码错）
> - 第三十二部分 ✅ **乱神符配方**（符纸 + 朱砂 + **新物品"兽血"** + 腐肉 → 乱神符 ×1）；附：原来乱神符**没有配方**、符纸也**没有配方**，且 `Loaded N recipes` 那一行**不是**证据
> - 第三十三部分 ✅ **动物死亡留下"血泊"实体 + 玻璃瓶右键换兽血**；新增 `-BloodPoolTest`（牛留一滩、僵尸不留）；附两个测试坑：`limit=2` 不等于"至少两个"、改 `.ps1` 必须保住 CRLF
> - 第三十四部分 ✅ **血泊改为只算玩家击杀**（用 `getKillCredit()`，1.20.1 没有 `getKiller()`）+ **三条配方**：阵眼（唯一有序）/ 朱砂改煤炭 / 符纸新增
> - 第三十五部分 ✅ **三种符改成有序配方**（乱神符 / 飞剑符 / 浮空符三个九宫格，替换掉原来的无序版）；顺手修掉自检里 32 行被写坏的中文标签
> - 第三十六部分 ✅ **替身符**（有序配方 + 小草人贴图）+ **抗性提升效果**（I–VIII 减伤表、VII/VIII 带 80% 反弹、30 秒、盾图标）；次数盾留了接缝没有自己编规则
> - 第三十七部分 ✅ **替身阵（三～十）**：阵眼第五态 + 玩家数据（含网络同步）+ 替死触发 + 次数盾 + 全屏碎裂图案 + 血色动作栏；减伤表改成每级 +20%（V 起即免疫）
> - 第三十八部分 ✅ **替身阵（十一～十四）**：绿色次数盾血条（5 格，照原版血量条定位）+ 粒子/音效 + 图鉴第四条；登录时补同步
> - 第三十九部分 ✅ **绿条改成顶替原版血条**（同位置同尺寸）+ **VIII 期间不消耗次数盾** + 碎裂图案重做（256 贴图、按屏幕高度、连续缓动）；视频打不开（缺 H.264 解码器）已说明
> - 第四十部分 ✅ **绿心**：原版血条**不动**、上方**另加一条**，形状用**原版心形**改成绿色（自己烘的 27×9 贴图 —— `setShaderColor` 是乘法，染不出绿）
> - 第四十一部分 ✅ **"绿心不显示"加了诊断日志**（`[standin-sync]` / `[standin-hud]`，连 x/y 一起打）；最可能的原因是**替死还没触发过**（点数只在替死后才有）
> - 第四十二部分 ✅ **修掉真 bug：编译后的类里中文丢了**（`????` 是我用 ASCII 写补丁脚本造成的，10 行/5 文件；已按编译产物核对）+ 绿心加**心跳日志**等日志定位
> - 第四十三部分 ✅ **部署到「阵法师」实例**（`tools/deploy.ps1`：自动备份到实例根目录 + 比对 SHA-256 + 游戏在跑时拒绝）；以后构建完都走它
> - 第四十四部分 ✅ **绿心 5→10 颗并对齐原版血条**（每颗 2 点）+ **修真 bug：阵眼消失后无限时长的负面效果不回收**（`setRemoved` 里清理）
> - 第四十五部分 ✅ **第五个阵法：嫁祸阵**（嫁祸符 + 阵眼第六态 + 伤害转移 50% + 紫色魂线 + 音效 + 图鉴第六条）；BOSS 免疫、标记 3 秒自过期
> - 第四十六部分 ✅ **金色半球屏障**（半径 10 上半球 + 网格线 + 亮底圈 + 顶端亮点 + 1 秒生长/收缩 + 靠近变亮/在内呼吸）；纯客户端、64 格内、10 刻重扫
> - 第四十七部分 ✅ **修启动崩溃：split package**（我把类放进 `net.minecraft.client.renderer` → JPMS 禁止两模块导出同包）；改为直接设 GL 状态、不用自定义 RenderType；并记下"专用服务端冒烟测不出客户端崩溃"
> - 第四十八部分 ✅ **屏障扩展（一半）**：流动光点 / 呼吸 / 阵纹环绕 / 五分色 / 进入波纹 / 叠加加深 / 碎裂粒子+音效；服务端加了怪减速、玩家加速夜视回血、进阵警报、碎裂冲击波。**6/7/14/26/32/33 逐条说明了为什么没做**
![符纸：网上的参考照片 / 16×16 成品（放大 8 倍）/ 放进物品栏的样子](tools/out/talisman_paper_preview.png)

![阵眼：16×16 贴图 / 放进世界里的样子（顶面最亮，两侧面依次压暗）](tools/out/formation_eye_cube.png)

![灵气粒子：8×8 贴图（放大 16 倍）/ 阵眼往外飘灵气的样子](tools/out/spirit_mote_preview.png)

---

## 第一步：符纸（物品）

| 教程里的说法 | 这一步实际做的事 | 文件 |
|---|---|---|
| ① 新建一个文件，专门用来登记物品 | 建物品登记表，声明命名空间 `fulu` | [`registry/ModItems.java`](src/main/java/com/lengyang/fulu/registry/ModItems.java) |
| ② 登记一个物品，代号 `talisman_paper` | `ITEMS.register("talisman_paper", ...)` | 同上 |
| ③ 打开模组主文件，加载登记文件 | 在主类构造方法里 `ITEMS.register(modBus)` | [`FuLu.java`](src/main/java/com/lengyang/fulu/FuLu.java) |
| ④ 语言文件写上中文名"符纸" | `"item.fulu.talisman_paper": "符纸"` | [`assets/fulu/lang/zh_cn.json`](src/main/resources/assets/fulu/lang/zh_cn.json) |
| ⑤ 物品模型文件 | `parent = item/generated`，指向贴图 | [`models/item/talisman_paper.json`](src/main/resources/assets/fulu/models/item/talisman_paper.json) |
| ⑥ 16×16 贴图 `talisman_paper.png` | 按网上搜到的符纸照片手绘的像素画 | [`textures/item/talisman_paper.png`](src/main/resources/assets/fulu/textures/item/talisman_paper.png) |

---

## 第二步：阵眼（方块）

| 教程里的说法 | 这一步实际做的事 | 文件 |
|---|---|---|
| ① 新建一个文件，专门用来登记方块 | 建方块登记表 `BLOCKS` | [`registry/ModBlocks.java`](src/main/java/com/lengyang/fulu/registry/ModBlocks.java) |
| ② 登记一个方块，代号 `formation_eye` | `BLOCKS.register("formation_eye", ...)` | 同上 |
| ③ 同时登记一个方块物品 | 另开 `BLOCK_ITEMS` 表登记 `BlockItem` | 同上 |
| ④ 打开模组主文件，加载方块登记文件 | `ModBlocks.register(modBus)` 挂两张表 | [`FuLu.java`](src/main/java/com/lengyang/fulu/FuLu.java) |
| ⑤ 语言文件写上中文名"阵眼" | `"block.fulu.formation_eye": "阵眼"` | [`assets/fulu/lang/zh_cn.json`](src/main/resources/assets/fulu/lang/zh_cn.json) |
| ⑥ 方块状态 / 方块模型 / 物品模型 | 三份 json，见下面第二节 | [`blockstates/`](src/main/resources/assets/fulu/blockstates/formation_eye.json) · [`models/block/`](src/main/resources/assets/fulu/models/block/formation_eye.json) · [`models/item/`](src/main/resources/assets/fulu/models/item/formation_eye.json) |
| ⑦ 16×16 贴图 `formation_eye.png` | 放 **`textures/block/`**（不是 item！） | [`textures/block/formation_eye.png`](src/main/resources/assets/fulu/textures/block/formation_eye.png) |

### 教程外补的两件事（不做的话方块是"半成品"）

- **掉落表** [`data/fulu/loot_tables/blocks/formation_eye.json`](src/main/resources/data/fulu/loot_tables/blocks/formation_eye.json)。
  1.20.1 里方块**没有掉落表就等于挖了什么都不掉**，而且不报错。
- **镐子可挖标签** [`data/minecraft/tags/blocks/mineable/pickaxe.json`](src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json)。
  方块属性里写了 `requiresCorrectToolForDrops()`，就必须同时贴上这个标签，
  否则它变成"徒手也能挖、但挖下来永远不掉"的怪东西。

还有一件小事：教程里说"同时登记一个对应的方块物品"，但**没说要把它加进创造模式物品栏**。
不加的话方块只能靠 `/give` 拿出来，所以 [`ModCreativeTabs.java`](src/main/java/com/lengyang/fulu/registry/ModCreativeTabs.java)
里也 accept 了它。

---

## 第三步：阵眼的运行逻辑（方块实体）

| 教程里的说法 | 这一步实际做的事 | 文件 |
|---|---|---|
| ① 新建一个文件，专门放运行逻辑类 | 新建方块实体类 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| ② 继承"方块实体"基类 | `extends BlockEntity` | 同上 |
| ③ 写一个构造函数 | `FormationEyeBlockEntity(BlockPos, BlockState)` | 同上 |
| ④ 加一个每刻执行的方法，先打印一句话 | `static void tick(...)` 里 `LOGGER.info(...)` | 同上 |
| ⑤ 新建登记文件，把方块实体类型绑到阵眼上 | `BlockEntityType.Builder.of(..., ModBlocks.FORMATION_EYE.get())` | [`registry/ModBlockEntities.java`](src/main/java/com/lengyang/fulu/registry/ModBlockEntities.java) |
| ⑥ 改阵眼方块文件，让它创建运行逻辑类并每刻调用 | `FormationEyeBlock extends BaseEntityBlock`，覆盖 `newBlockEntity` / `getTicker` | [`block/FormationEyeBlock.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlock.java) |
| ⑦ 主文件加载方块实体登记文件 | `ModBlockEntities.register(modBus)` | [`FuLu.java`](src/main/java/com/lengyang/fulu/FuLu.java) |

方块实体这一步**不需要任何 json**：贴图、模型、语言全在方块那边，所以这一整步只动 java。

### ⚠️ 一个会让阵眼"凭空消失"的坑

`BaseEntityBlock` 的 `getRenderShape` 默认返回 **`INVISIBLE`** —— 原版那些继承它的方块
（箱子、附魔台）都配了一个"方块实体渲染器"自己把方块画出来。我们没有渲染器，
用的是普通方块模型，所以**不覆盖这个方法的话**：阵眼放下去以后看不见，
但碰撞箱还在、还能挖、还在每刻打日志，**而且不报任何错**。
[`FormationEyeBlock#getRenderShape`](src/main/java/com/lengyang/fulu/block/FormationEyeBlock.java)
里那三行就是为它准备的。

### 另外三个和直觉不一样的地方

1. **构造函数只能有两个参数** `(BlockPos, BlockState)`。`BlockEntityType.Builder.of()`
   要的是一个函数式的 `BlockEntitySupplier`，签名不对编译就过不去。
2. **`tick` 是 static 的，而且它不是覆盖父类方法** —— `BlockEntity` 本身根本没有
   `tick`。是我们把它交给 `getTicker`，游戏才会每刻拿它来调用。
3. **方块实体不会自动存档**。冷却计时器 `cooldown` 只活在内存里，区块卸载再加载就从 0 开始；
   要存下来得重写 `saveAdditional` / `load`。

### 日志是每一刻一行（照教程原文）

`LOG_INTERVAL_TICKS = 1`，也就是**每一刻打印一句**。实测 20 行/秒、时间戳间隔约 50 ms
（正好 20 tick/s），序号每行 +1：

```text
阵眼在运行 (0, -60, 0) 第 1 刻
阵眼在运行 (0, -60, 0) 第 2 刻
阵眼在运行 (0, -60, 0) 第 3 刻
```

代价是日志涨得很快（约 1200 行/分钟）。只想确认"它在跑"就把 `LOG_INTERVAL_TICKS`
改成 `20`（每秒一行）；调试完把 `DEBUG_TICK_LOG` 置成 `false`，这段就彻底安静了。

---

## 第四步：阵眼每 5 秒放一次灵气粒子（用冷却控制节奏）

> ⚠️ **这一节讲的做法已经在第十步第五部分被删掉了**：冷却字段、判断、重置全部移除，
> 现在每刻都干活。这一节保留下来，是因为它是"为什么要在意节奏"的完整来历
> （每秒 20 次做同一件事会怎样），值得看一遍再决定要不要重新加。

| 要做的事 | 落点 | 文件 |
|---|---|---|
| 登记粒子类型（服务端那一半） | `DeferredRegister<ParticleType<?>>` + `new SimpleParticleType(false)` | [`registry/ModParticles.java`](src/main/java/com/lengyang/fulu/registry/ModParticles.java) |
| "看到 id 之后怎么画"（客户端那一半） | 继承 `TextureSheetParticle` + 内嵌 `Provider` | [`client/particle/SpiritMoteParticle.java`](src/main/java/com/lengyang/fulu/client/particle/SpiritMoteParticle.java) |
| 把 id 和"怎么画"接起来 | `RegisterParticleProvidersEvent` | [`client/ModClientSetup.java`](src/main/java/com/lengyang/fulu/client/ModClientSetup.java) |
| 粒子贴图（8×8）+ 声明文件 | 白灰光点，颜色在代码里乘 | [`textures/particle/spirit_mote.png`](src/main/resources/assets/fulu/textures/particle/spirit_mote.png) · [`particles/spirit_mote.json`](src/main/resources/assets/fulu/particles/spirit_mote.json) |
| 每 5 秒放一把 | 冷却倒计时：`cooldown > 0` 就减 1 并 return，到 0 才干活 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| 主文件加载粒子登记文件 | `ModParticles.register(modBus)` | [`FuLu.java`](src/main/java/com/lengyang/fulu/FuLu.java) |

### 粒子为什么必须写两半

```text
服务端：ModParticles 里登记一个 id（fulu:spirit_mote）
          ↓ sendParticles(...) 把这个 id + 坐标广播给附近玩家
客户端：ModClientSetup 把 id 接到 SpiritMoteParticle.Provider
          ↓ 玩家屏幕上才真的出现一个金色光点
```

**只写服务端那一半**：日志一切正常、别人也能"收到"粒子包，但屏幕上什么都没有，
而且不报错。**只写客户端那一半**：代码根本不会被调用（没有 id 能触发它）。

客户端那一半还必须用 `@Mod.EventBusSubscriber(..., value = Dist.CLIENT)` 圈起来：
专用服务端没有贴图、没有渲染管线，加载到 `SpiritMoteParticle` 会直接
`NoClassDefFoundError`。这次的无头服务端能正常启动，就是这条边界生效的证据。

### 三件"粒子看起来很廉价"的常见成因，这里都躲开了

- **突然出现、突然消失** → `tick()` 里按生命比例做淡入淡出（前 25% 淡入、后 40% 淡出）；
- **大小一模一样** → `quadSize` 每一颗都加随机（0.10 ~ 0.16）；
- **往下掉**（灵气该往上飘） → `gravity = -0.012F`。

贴图本身是**白灰色**的，金色是代码里 `setColor(0.98, 0.84, 0.46)` 乘上去的 ——
以后想换成青色灵气，改一个常量就行，不用重画贴图。

### 冷却：把"每刻"压成"每 5 秒"

`tick()` 是**每刻**被调用的（20 次/秒）。不加约束的话，一秒就是 20 条日志 +
20 把粒子 —— 日志被冲没，客户端的粒子预算也会被一个方块吃光。所以加一个倒计时：

```java
private int cooldown = 0;                    // 0 = 随时可以做

public static void tick(...) {
    if (eye.cooldown > 0) {                  // ① 还在冷却：减 1，直接结束
        eye.cooldown--;
        return;                              //    后面的打印和粒子一起被挡住
    }
    // ② 冷却到底：干活
    LOGGER.info("阵眼在运行 {}", pos.toShortString());
    server.sendParticles(SPIRIT_MOTE, ...);
    eye.cooldown = COOLDOWN_TICKS;           // ③ 100 刻 = 5 秒后再来
}
```

三个容易写错的地方：

- **`return` 的位置决定了冷却管住什么。** 写在打印之后、粒子之前，就变成
  "日志每 5 秒一行，粒子还是每刻刷"。所以减 1 之后立刻 return，
  打印和粒子都在它后面。
- **是"干完活拉满"而不是"每刻减到 0 再重置"**：拉满 100 之后，接下来 100 刻
  走完冷却，第 101 刻才触发 —— 间隔正好 100 刻 = 5.000 秒。
- **冷却不存档。** 它只活在内存里，区块卸载再加载会从 0 开始；
  对计时器来说最坏是重载后立刻多放一次灵气，可以接受。要精确的话得
  `saveAdditional` / `load`。

> 顺带一提：一开始这里用的是 `ticks % 40 == 0`（取模）。取模也能达到同样效果，
> 但它有个隐患 —— 只要 `ticks` 因为任何原因被重置或跳变，节奏就乱了；
> 而且"还有多久触发"没法读出来。倒计时写法天然支持"冷却还剩 N 刻"这种显示。

---

## 第五步：阵法开关（右键开 / 关）

> ⚠️ **这一节的功能已经在第十步第七部分删掉了**：现在"符就是开关"——
> 插上乱神符自动生效，取下就停。那一节保留下来，是因为它是"开关状态该放哪、
> 为什么不能放在 Block 里、为什么不能只在客户端改"的完整来历，
> 这些道理在别的功能上照样用得上。

| 你说的 | 代码里对应什么 |
|---|---|
| ① 打开阵眼大脑那个文件 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| ② 加一个开关变量，默认关 | `private boolean active = false;` |
| ③ 每刻方法里先判断开关，关着直接结束 | `if (!eye.active) return;` —— tick() 的**第一条指令** |
| ④ 打开阵眼方块的文件 | [`block/FormationEyeBlock.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlock.java) |
| ⑤ 加右键交互，把开关反过来 | `use(...)` → `eye.toggleActive()` |
| ⑥ 右键一下开始每 5 秒打印，再右键停止 | 开关在最外层，冷却在里面 |
| ⑦ 挖掉再放一个，默认是关的 | 方块实体是**新建的实例**，字段回到默认 `false` |

### 开关写在冷却外面，是有讲究的

```java
if (!eye.active) return;                    // ① 整座阵法要不要工作
if (eye.cooldown > 0) { eye.cooldown--; return; }   // ② 这一轮该不该干活
// ③ 干活：打印 + 粒子，然后把冷却拉满
```

两层 `return` 的顺序决定了两件事各管多大范围：**开关管"整座阵法"，冷却管"节奏"**。
如果把开关塞到冷却里面（先减冷却再判断开关），就会变成"关着的时候冷却还在偷偷走"，
再打开时立刻触发一次 —— 看起来像是"关了没用"。

### 三个"多人游戏里会翻车"的细节

1. **潜行时让开**（`player.isSecondaryUseActive()`）。不判断的话，玩家拿着方块想往阵眼上贴，
   会变成"开阵法"，方块永远放不下去。
2. **只在服务端改状态**。单人游戏里客户端和服务端各有一份方块实体 —— 两边都翻一次
   就等于没翻。所以客户端只负责摆手臂（`InteractionResult.SUCCESS`），
   真正的切换在服务端（`CONSUME`），这就是 `InteractionResult.sidedSuccess(...)` 的用法。
3. **给玩家一行反馈**（`displayClientMessage`，显示在物品栏上方）。
   开关本身没有外观变化，不提示的话玩家只能去翻日志才知道点没点上。
   文案走语言文件：`msg.fulu.formation_eye.on` / `.off`。

### ⚠️ 开关不存档

`active` 只活在内存里。也就是说：

- **挖掉再放一个** → 新实例 → 默认关 ✅（正是第 ⑦ 条要的）；
- **走远、区块卸载、再回来** → 方块实体被重新构造 → **也回到"关"**。
  多人服务器上这会显得很怪："我明明开了，怎么回来就停了。"

要让它记住状态，就得重写 `saveAdditional` / `load`（各三行）。
现在 `toggleActive()` 里的 `setChanged()` 已经先标好"我变了"，等加存档时不用回头补。

---

## 第六步：扫描周围 11×11×11 里的怪物

| 你说的 | 代码里对应什么 |
|---|---|
| ① 打开阵眼大脑那个文件 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| ② 在每刻方法里找到打印那句话的位置 | 就是 `LOGGER.info("阵眼在运行 ...")` 那一行 |
| ③ 把打印删掉，换成扫描怪物 | 换成 `countMonsters(...)` + `LOGGER.info("找到 {} 只怪物", n)` |
| ④ 前后左右各 5 格、上下各 5 格 | `new AABB(pos).inflate(5)` → 11×11×11 |
| ⑤ 只找敌对怪物 | `level.getEntitiesOfClass(Monster.class, area)` |
| ⑥ 打印"找到 X 只怪物" | `LOGGER.info("找到 {} 只怪物", monsters)`，但**只在变多时**才打印 |
| ⑦ 0 只 / 3 只 / 引远变少 | 实测：0 → 1 → 2 → 3 → 5（涨），掉下来时**一行都不打**，再涨又报 2 |

### 11×11×11 是怎么算出来的

```java
new AABB(pos)            // 这一格自己的盒子，边长 1
    .inflate(5)          // 六个面各往外撑 5 格
    // => x 从 pos.x-5 到 pos.x+5（含）= 11 格；y、z 同理
```

`inflate` 是"往六个方向各撑开 n 格"，所以传 5 得到的就是 11 —— 不需要写 `11`，
也不需要手动减半。写 `inflate(11)` 会变成 23×23×23，这是最容易错的一处。

### 为什么用 `Monster` 而不是"所有生物"

`Monster` 是**敌对怪物**的公共父类：僵尸、骷髅、苦力怕、蜘蛛、女巫、末影人全在里面。
牛、羊、猪、村民、玩家、盔甲架都不是 Monster，所以不会被数进去 —— 这一点是用
"往范围内放一头牛，数量不变"验出来的（见下面的 EDGE 阶段）。

它**不包含**史莱姆和恶魂（这两个只实现了 `Enemy` 接口，不继承 Monster）。
想把它们也算上，就把筛选条件换成：

```java
level.getEntities((Entity) null, area, e -> e instanceof Enemy)   // 所有敌对生物
```

### 这个扫描贵不贵

每 5 秒一次、范围 11³，一次实体查询的开销可以忽略。真正要小心的是
**别把它放到冷却外面**：`tick` 一秒跑 20 次，几十个阵眼就是每秒上千次查询。
现在它待在两个 `return` 后面（开关 + 冷却），所以是 5 秒一次。

### 打印规则：只在"变多"的时候说话

扫描仍然每 5 秒做一次（不做就发现不了"变多了"），但**打印是有条件的**：

```java
int monsters = countMonsters(level, pos);
if (monsters > eye.lastMonsterCount) {      // 比上一次多，才开口
    FuLu.LOGGER.info("找到 {} 只怪物", monsters);
}
eye.lastMonsterCount = monsters;            // 基准总是跟着最新结果走
```

规则的四条边：

- **持平不打印** —— 一直 0 只就一行都不刷，这就是"5 秒内没增加就不再显示"；
- **变少也不打印** —— 怪物被清光了不会专门报一声；
- **再变多会重新开口** —— 哪怕中途掉回 0，只要又涨起来就再报一次
  （基准是"上一次扫描值"而不是"上一次打印值"，否则掉下去之后再涨回来会被吞掉）；
- **第一次一定报** —— `lastMonsterCount` 初值是 `-1`，所以第一次扫描无论是 0 只还是 5 只都会显示一次。

> 还有一条在别处：**每次右键开启阵法也一定报一次**。`toggleActive()` 里把基准重置回 `-1`，
> 否则玩家刚打开阵法时，若数量和上次关闭前持平，就会一行都看不到、像是没生效。

### 顺便：原来那句日志被换掉了

"阵眼在运行"没有了，取而代之的是扫描结果。所以第五步"右键一下开始每 5 秒打印一次"
现在的表现是：**右键一下，先报一次当前数量，之后只在数量变多时才再冒一行**。

---

## 第七步：范围改成"上方无限、下方 10 格"，结果改成动作栏

| 你说的 | 代码里对应什么 |
|---|---|
| ① 打开阵眼大脑那个文件 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| ② 找到扫描怪物那段逻辑 | `countMonsters(level, pos)` |
| ③ 水平还是前后左右各 5 格 | AABB 的 x / z 仍然是 `pos ± 5`（和以前一样） |
| ④ 垂直：下方 10 格 → 世界最高处 | AABB 的 y 取 `getMinBuildHeight()` ~ `getMaxBuildHeight()`，再用 `floorY = pos.y - 10` 卡掉太深的 |
| ⑤ 删掉控制台打印 | 那句 `找到 {} 只怪物` 从代码里删了（现在连字节码里都搜不到这三个字） |
| ⑥ 发给周围 16 格内的玩家 | `announceMonsterCount(...)`：`distanceToSqr(...) <= 16²` 的 `ServerPlayer` |
| ⑦ 显示在动作栏 | `player.displayClientMessage(message, true)` —— **`true` 就是动作栏** |
| ⑧ 只在数量变化时发一次 | `if (monsters != eye.lastMonsterCount)` |

### 垂直方向为什么用"脚底高度"，而不是"碰撞箱有没有挨着"

这是这一步唯一容易写错的地方。僵尸高 1.95 格：

```text
   阵眼 y = 100
        │  ← 下方 10 格的分界线在 y = 90
   僵尸站在 y = 89（下方 11 格）：
        脚底 89   ← 在外面
        头顶 90.95 ← 却探进分界线里了！
```

如果按"碰撞箱是否与范围盒相交"来判定（第 6 步水平方向就是那么做的），
这只僵尸**会被算进去** —— 和"下方超过 10 格不算"矛盾。所以垂直方向显式卡脚底：

```java
int floorY = pos.getY() - SCAN_DEPTH_BELOW;      // 阵眼下方 10 格
if (monster.getY() >= floorY) { count++; }        // 脚底不低于这条线才算
```

水平方向仍然是碰撞箱相交（靠实体宽度小于 1 格，站在 x+5 算、x+7 不算）。
两个方向判定方式不同这件事，注释里写清楚了，免得以后有人以为写歪了。

### 动作栏 vs 聊天框，差别就在一个 `true`

```java
player.displayClientMessage(message, true);    // true  = 动作栏（物品栏上方，几秒后消失）
player.displayClientMessage(message, false);   // false = 聊天框（会被刷满）
```

文案走语言文件 `msg.fulu.formation_eye.monsters`（中文"附近怪物：%s 只"），
而不是把中文写死在代码里 —— 写死的话英文客户端也会看到中文，改文案还得重新编译。

### "16 格内"用的是距离，不是盒子

`player.distanceToSqr(center) <= 16 * 16` 是**球**；如果写成 `new AABB(pos).inflate(16)`，
四个角上离阵眼 27 格的玩家也会收到。后者更符合"和扫描同一套写法"，但不符合"16 格内"。

### 控制台还是会写一行日志？

不会。**出厂 `DEBUG_SCAN_LOG = false`** —— 正常玩法里结果只进动作栏，日志干干净净。
这个常量是留给开发/自动化测试的：无头服务端没有玩家、收不到动作栏，
把它临时改成 `true` 才能在日志里看到每次播报的数字（下面的验证就是这么做的）。

---

## 第八步：让扫描到的怪物互相攻击

| 你说的 | 代码里对应什么 |
|---|---|
| ① 打开阵眼大脑那个文件 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| ② 找到扫描怪物那段逻辑 | `findMonsters(level, pos)`（这一步顺手把"返回数量"改成了"返回列表"） |
| ③ 扫描完之后判断：少于 2 只就什么都不做 | `if (count >= 2) { makeThemFight(level, monsters); }` |
| ④ 2 只及以上：**每一只**都分一个目标 | `makeThemFight(...)` 里的 `for` 循环，每只抽一次 |
| ⑤ 目标不能是它自己 | 对第 i 只：从 `0..size-2` 抽，`>= i` 就往后挪一格 |
| ⑥ BOSS 类先不管 | 这一版**没加任何过滤**，所有 `Monster` 都参与（下面有说明） |

### "让怪物去打另一只"到底怎么实现

```java
for (int i = 0; i < size; i++) {
    int targetIndex = level.random.nextInt(size - 1);
    if (targetIndex >= i) targetIndex++;        // 跳过它自己
    monsters.get(i).setTarget(monsters.get(targetIndex));
}
```

`setTarget` 会覆盖这只怪原本的目标（僵尸平时只想打玩家和村民），
接下来的**寻路、靠近、挥击全是原版 AI 自己做的** ——
`MeleeAttackGoal` 只要看到 `getTarget()` 不为空就会冲上去打。

> 这里经历过一次改版：最早是"随机挑一只去打另一只"。那样其实**只有两只在打**
> —— 被挑中的那只冲过去，它的目标用原版 `HurtByTargetGoal` 还手，
> 剩下的怪在旁边发呆。改成"每只都分一个目标"之后，全场才会真的乱起来。

### "目标不能是自己"：一次就中，不用循环重抽

```java
int attackerIndex = random.nextInt(size);            // 挑攻击者
int targetIndex = random.nextInt(size - 1);          // 在"少一个位置"的范围里挑
if (targetIndex >= attackerIndex) targetIndex++;     // 撞上了就往后挪一格
```

这样每个"别人"被选中的概率都一样，而且**永远不会等于攻击者**，
不需要 `while (target == attacker) 重抽` 那种写法（那种写法在只剩 2 只时会反复撞自己）。

### ⚠️ 一个把测试坑了两次的细节：`NoAI`

要让怪物能打人，它必须**开着 AI**；但开着 AI 的怪物会自己乱走。
第一次验证就是栽在这里：两只僵尸溜出 ±5 的扫描范围，数量掉到 0，谁也没打谁。
最后是把测试区用一个 7×7、两层高的石墙围起来才测出来。

另外 `NoAI:1b` 的怪物**连目标都会被清掉**（`Mob.serverAiStep` 里做的），
所以"给 NoAI 的怪设目标"是永远不会生效的 —— 调试时很容易被这一点骗到。

> 关于第 ⑥ 条：`Monster` 这个父类已经包含**凋灵**（它继承 Monster），
> 但**不包含**末影龙（那只继承 Mob），也**不包含**史莱姆、恶魂。
> 所以现在"凋灵会被阵眼挑去打架"是成立的行为。以后要加 BOSS 免疫，
> 就是在 `findMonsters` 里加一条过滤（比如排除 `Boss` 接口的实体），一处就够。

---

## 第十步（第一部分）：给阵眼加"有没有符"，并且真的存进存档

| 你说的 | 代码里对应什么 |
|---|---|
| ① 打开阵眼大脑那个文件 | [`block/FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) |
| ② 加一个只有两种值的开关，默认"没符" | `private boolean hasTalisman = false;` |
| ③ 找到保存数据的方法，把它存进去 | 覆盖 `saveAdditional(CompoundTag)` → `tag.putBoolean("HasTalisman", ...)` |
| ④ 找到读取数据的方法，把它读出来 | 覆盖 `load(CompoundTag)` → `hasTalisman = tag.getBoolean("HasTalisman")` |

### 存档两个方法的三条规矩

```java
@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);                       // ① 必须先调父类
    tag.putBoolean(NBT_HAS_TALISMAN, hasTalisman);   // ② 只写需要跨存档保留的
}

@Override
public void load(CompoundTag tag) {
    super.load(tag);                                 // ① 同样先调父类
    hasTalisman = tag.getBoolean(NBT_HAS_TALISMAN);  // ③ 老存档没这个键时返回 false
}
```

- **不调 `super`**：父类要写 `id` / `x` / `y` / `z`，漏了它，下次读档时游戏认不出这是什么方块实体。
- **键名抽成常量**：`"HasTalisman"` 在两处各写一遍，一旦写岔（哪怕只是大小写），
  表现是"存进去了、读出来永远是默认值"，**而且不报任何错**。
- **读的时候不用先判 `contains`**：`getBoolean` 在键不存在时返回 `false`，
  正好等于"没符"的默认值 —— 所以老存档升级上来不会出问题。

> 注意范围：这一步**只存"有没有符"**。`active`（阵法开没开）和 `cooldown`（冷却）
> 仍然只在内存里，所以走远、区块卸载再回来，阵法会回到"关"。
> 想一起存下来，就是各自在 `saveAdditional` / `load` 里再加一行。

---

## 第十步（第二部分）：第二套外观（有符 / 没符）

![两套外观：旧贴图 / 有符贴图 / 两种在游戏里的样子](tools/out/formation_eye_two_looks.png)

| 你说的 | 做成了什么 |
|---|---|
| ① 复制现在的贴图，改个名 | [`textures/block/formation_eye_talisman.png`](src/main/resources/assets/fulu/textures/block/formation_eye_talisman.png) |
| ② 只在正中间画一个符箓，其他一模一样 | 由 [`tools/gen_formation_eye_talisman.py`](tools/gen_formation_eye_talisman.py) 生成；**贴图区域外 0 个像素变化** |
| ③ 复制方块模型，改个名 | [`models/block/formation_eye_talisman.json`](src/main/resources/assets/fulu/models/block/formation_eye_talisman.json) |
| ④ 新模型里把贴图换成新贴图 | `"all": "fulu:block/formation_eye_talisman"` |
| ⑤ 方块状态：没符用旧模型，有符用新模型 | [`blockstates/formation_eye.json`](src/main/resources/assets/fulu/blockstates/formation_eye.json) |

```json
{
  "variants": {
    "has_talisman=false": { "model": "fulu:block/formation_eye" },
    "has_talisman=true":  { "model": "fulu:block/formation_eye_talisman" }
  }
}
```

### 中间那个符是怎么画的

没有另起一套画风：把模组自己的**符纸贴图**（6×14）用 NEAREST 缩到 4×10，
贴到方块贴图的正中间（x=6..9、y=3..12）。所以方块上的纸、墨、印章和物品栏里的
符纸是同一套美术。

> 脚本会顺手断言"贴图区域外有没有像素被动过"，结果是 **0** —— 这正是你要求的
> "其他部分和原来一模一样"，而且它是被检查过的，不是靠肉眼。

### ⚠️ 必须先加一个"方块状态属性"

`blockstates/*.json` **只能按方块状态分模型** —— 它看不见方块实体里的数据。
第一部分的那个开关在方块实体里，所以光有它换不了外观。

于是这一步给方块加了：

```java
public static final BooleanProperty HAS_TALISMAN = BooleanProperty.create("has_talisman");
// 构造里 registerDefaultState(...setValue(HAS_TALISMAN, false))
// 并且 createBlockStateDefinition 里 builder.add(HAS_TALISMAN)
```

漏了 `createBlockStateDefinition` 那一句，`setblock ... [has_talisman=true]` 会**直接报错**。

### 现在同一个事实存在两个地方

| 存在哪 | 作用 | 谁负责存 |
|---|---|---|
| 方块实体的 `hasTalisman` 字段 | 逻辑 + 能用 `/data get` 查 | `saveAdditional` / `load`（第一部分写的） |
| 方块的 `has_talisman` 状态 | **客户端靠它换模型** | 区块自己存档 |

所以 `setTalisman()` 现在**两处一起改**（`syncBlockState()` 里调 `level.setBlock`）。
只改字段：逻辑对、外观不变；只改状态：外观变了、`/data get` 查不到。
两个都改，才是"看起来变了、查起来也对"。

> 说实话这是**同一份数据存了两遍**。严格讲只留方块状态就够（它自带存档），
> 第一部分那套 NBT 就是多余的；但那是你要的接口，我保留了，并且把两处的同步
> 收在一个方法里，避免它们各说各话。要精简的话说一声。

### 验证：能验的与验不了的

```text
  a new eye defaults to has_talisman=false       OK  DEFAULT-OFF-OK x1
  the state can be switched to true              OK  SWITCH-ON-OK x1
  switching the state keeps the block entity     OK  BE-KEPT-OK x1 (nbt flag still readable)
 verdict    : PASS - has_talisman exists, defaults to false, and can be switched
```

- **能验**：属性存在、默认是 `false`、能切成 `true`（说明它真的在状态定义里）、
  以及 `setblock` 换状态**不会重建方块实体**（NBT 里那个开关还在）。
- **验不了**：客户端到底有没有画出另一套模型 —— 那是客户端渲染。
  文件本身由 `verify_jar.py` 串起来查（blockstate 两种变体 → 两个模型 → 两张贴图），
  样子就用上面那张预览图 + 你自己进游戏看。
- 手动试外观（不想等第三部分的话）：
  ```mcfunction
  setblock <阵眼坐标> fulu:formation_eye[has_talisman=true]    # 换成"有符"的样子
  setblock <阵眼坐标> fulu:formation_eye[has_talisman=false]   # 换回去
  ```
  （同一个方块只改状态，不会重建方块实体，所以第一部分存的那个开关不会丢。）

---

## 第十步（第三部分）：右键装符 / 取符

![符纸与乱神符](tools/out/chaos_talisman_next_to_paper.png)

### 先说一件事：乱神符这个物品原本不存在

代码里只有**符纸**（`talisman_paper`，空白材料）。既然这一步要"拿着乱神符右键"，
就先把 **`chaos_talisman`（乱神符）** 造了出来：

| 你要的三件事 | 对应 |
|---|---|
| 新物品 | [`registry/ModItems.java`](src/main/java/com/lengyang/fulu/registry/ModItems.java) 里的 `CHAOS_TALISMAN` |
| 贴图 | [`textures/item/chaos_talisman.png`](src/main/resources/assets/fulu/textures/item/chaos_talisman.png)（[`tools/gen_chaos_talisman.py`](tools/gen_chaos_talisman.py) 生成） |
| 模型 / 中文名 / 创造页签 | `models/item/chaos_talisman.json`、`lang/*.json`、`ModCreativeTabs` |

它是符纸的"成品版"：同样是窄纸条，但纸是**冷紫色**、墨是**紫朱砂**，
中间画的是**阵眼那只眼睛**——一眼能看出它是要装进阵眼的。

> 如果你其实想让**符纸**来装（而不是新物品），改一行就行：把 `use()` 里
> `ModItems.CHAOS_TALISMAN` 换成 `ModItems.TALISMAN_PAPER`。

### 右键逻辑（`FormationEyeBlock.use`）

| 情况 | 结果 |
|---|---|
| 普通右键 + 拿乱神符 + 阵眼**没符** | 扣掉一个乱神符 → 装上 → 音效 + 提示 → 结束 |
| 普通右键 + **空手** + 阵眼**有符** | 还你一个乱神符 → 取下 → 音效 + 提示 → 结束 |
| 普通右键 + 其他任何情况 | **什么都不做**（返回 `PASS`，把手让给别人） |
| **潜行**右键 | 开 / 关阵法（第五步的老功能） |

```java
// ① 装符
if (!hasTalisman && held.is(ModItems.CHAOS_TALISMAN.get())) {
    if (!level.isClientSide()) {
        if (!player.getAbilities().instabuild) held.shrink(1);   // 创造模式不扣
        eye.setTalisman(true);                                   // ← 同时改 NBT 和外观
        player.displayClientMessage(Component.translatable("msg.fulu.formation_eye.talisman_on"), false);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.4F);
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
}
```

### ⚠️ 开关挪到"潜行右键"了

你说"其他情况：什么都不做"，而**空手 + 没符**正属于"其他情况"。
如果普通右键还兼着开关阵法，那空手点一下就会把阵法打开 —— 与你的要求冲突。
所以两个功能抢同一个按键时，把旧的那个挪到潜行键上（Minecraft 的惯例：潜行 = 第二功能）：

- **普通右键** = 装符 / 取符
- **潜行右键** = 开 / 关阵法

### 外观为什么能立刻变

`eye.setTalisman(true)` 里干了两件事：改 NBT 字段 + `level.setBlock(..., 3)` 改方块状态。
**后者才是让客户端立刻换模型的那个**（`3` = 通知客户端 + 更新邻居）。
这正是第二部分把两处同步收进一个方法的原因 —— 这一步只管调它，不用操心两边会不会走岔。

### 验证：能验的与验不了的

```text
  a new eye defaults to has_talisman=false       OK  DEFAULT-OFF-OK x1
  the state can be switched to true              OK  SWITCH-ON-OK x1
  switching the state keeps the block entity     OK  BE-KEPT-OK x1 (nbt flag still readable)
  the chaos talisman item really exists          OK  ITEM-OK x1 (summoned, matched by nbt)
 verdict    : PASS
```

"乱神符真的存在"是这么验的：召唤一个带着 `fulu:chaos_talisman` 的掉落物，
再用 NBT 选择器按 id 匹配回来 —— **如果这个 id 没注册，物品会变成空气，就匹配不上**。

> **验不了的一件事**：右键本身。无头服务端上**没有玩家**，而右键是玩家动作。
> 这一下会调用的东西（方块状态切换、物品是否注册、提示文案）都单独验过了；
> 但"手里拿的是什么→走哪条分支"这套接线，只能你自己进游戏点一下。

### 自己试

```mcfunction
give @s fulu:chaos_talisman          # 拿一张乱神符
```
1. 放着阵眼，手拿乱神符右键 → 外观变成"有符"，聊天框出现"阵眼已装上乱神符"，符少一个。
2. 空手右键 → 外观变回"没符"，收到一张乱神符。
3. **潜行**右键 → 阵法的开 / 关还在。
4. 手里拿着别的东西右键 → 什么都不发生。

---

## 第十步（第四部分）：把每刻的逻辑切成"两段"

`tick` 现在长这样（两道提前返回的门之后，就是两段）：

```java
public static void tick(Level level, BlockPos pos, BlockState state, FormationEyeBlockEntity eye) {
    if (!eye.active) return;                 // 阵法开关（第五步）
    if (eye.cooldown > 0) { eye.cooldown--; return; }   // 冷却（第四步）

    // ========== 第一段：不管有没有符，都做 ==========
    List<Monster> monsters = findMonsters(level, pos);
    int count = monsters.size();
    if (count != eye.lastMonsterCount) {     // 只在数量变化时播一次
        announceMonsterCount(level, pos, count);
    }
    eye.lastMonsterCount = count;

    // ========== 第二段：只有插了符，才做 ==========
    if (eye.hasTalisman()) {
        makeThemFight(level, monsters);      // 少于 2 只时它自己跳过
    }

    // 灵气粒子（第四步）+ 冷却拉满
}
```

**符在这套逻辑里是什么**：它不是"点亮一个特效"，而是**第二段的总闸**。
没插符时，阵眼就是个"看得到怪物、但不挑事"的摆设；插上乱神符才开始让它们自相残杀。
这正好解释了第三部分为什么要费劲做一个乱神符物品出来。

### 两道门 + 两段，是四种不同的"什么都不做"

| 状态 | 行为 |
|---|---|
| 阵法关（`active = false`） | 什么都不做，连扫描都不做 |
| 阵法开、到点了、**没符** | 扫描 + 播报，但**不挑事** |
| 阵法开、到点了、**有符** | 扫描 + 播报 + 让怪物互相打 |

> 原本还有一档"冷却中"（每 5 秒才干活一次）—— 见第五部分，那一档已经删掉了。

### 验证：没符时必须"只扫描、不挑事"

```text
  no talisman  : scan x1, fight x0, untouched zombies x3, hurt zombies x0
  with talisman: scan x1, fight x8, untouched zombies x0, hurt zombies x2

  segment 1 runs without a talisman              OK  the scan spoke 1x
  segment 2 is skipped without a talisman        OK  0 fight rounds, 0 hurt (3 zombies stayed at 20 HP)
  segment 2 runs once the talisman is fitted     OK  fight x8, hurt x2
 verdict    : PASS - scanning always, fighting only with the talisman
```

这次是在**同一局里中途插符**：先放三只僵尸等 14 秒（扫描在说话、三只都还是满血），
再用 `data merge block ... {HasTalisman:1b}` 插上符，再等 16 秒 —— 立刻开打，两只掉血。

> **这次改动连累了一个旧测试**：`-FightTest` 原本指望"没插符也会打起来"，
> 加了第二段的闸门后它会直接失败。已经改成"放好阵眼就顺手插上符"，
> 这样 ALONE 阶段验的才是真的"**只有一只怪、而且已经插了符**，仍然不打"，
> 而不是因为"反正没符，谁都不会打"而蒙过去。重跑：四项全过。

---

## 第十步（第五部分）：去掉冷却

删掉的东西（`FormationEyeBlockEntity.java`）：

```java
private int cooldown = 0;                              // 字段
private static final int COOLDOWN_TICKS = 100;         // 常量
if (eye.cooldown > 0) { eye.cooldown--; return; }      // tick 里的判断
eye.cooldown = COOLDOWN_TICKS;                         // tick 末尾的重置
public int getCooldown() { return cooldown; }          // 没人用的取值方法
```

现在 `tick` 只剩**一个提前返回**（阵法开关），其余逻辑每刻都走。

### 怎么验的："每刻"是可以量出来的

同一个测试、同一个 16 秒窗口，前后对比：

| | 那 16 秒里第二段跑了多少行 |
|---|---|
| **有** 5 秒冷却（改之前） | 8 行 |
| **没有**冷却（改之后） | **544～664 行**（约每秒 34～42 次） |

`20 刻/秒 × 3 只怪 ≈ 60`，实测落在这个量级，说明确实是每刻都在跑，
而不是"看起来去掉冷却了、其实还每 5 秒一次"。

```text
  no talisman  : scan x1, fight x0, untouched x3, hurt x0, casualties 0
  with talisman: scan x2, fight x544, untouched x0, hurt x1, casualties 1

  segment 1 runs without a talisman              OK  the scan spoke 1x
  segment 2 is skipped without a talisman        OK  0 fight, 0 hurt, 0 casualties
  segment 2 runs once the talisman is fitted     OK  fight x544, hurt x1, casualties 1
  segment 2 runs every tick (no cooldown)        OK  544 lines in 16 s (~34/s)
 verdict    : PASS
```

> 顺手修了一个**太弱的断言**：原来"打起来了"要求"至少一只掉血"，
> 但**被打死的怪不再是掉血的怪**。有一轮三只里死了两只，标记却只显示 `hurt x1`
> —— 极端情况下"全军覆没"反而会被判成"没人打架"。现在测试同时统计**减员**
> （窗口内怪物数量下降），"掉血 **或** 减员"都算战斗生效。

### ⚠️ 去掉冷却的三个代价（都是真实存在的，不是理论）

| 代价 | 具体表现 | 现在的状态 |
|---|---|---|
| 实体查询变勤 | 每秒 20 次扫描，而不是每 5 秒 1 次 | 保留（就是要它快） |
| 粒子变成 20 倍 | 每秒 120 颗灵气 | **已修**：第六部分装回 5 秒节拍 |
| 目标每刻重抽 | 每秒 20 次改目标，寻路被反复重置 | 保留（实测打架没有变弱） |

实测里第三点**没有**让打架停下来（16 秒内死 1～2 只）。如果你觉得怪物抖得难受，
正确的改法是给它单独加计数器，**不要**把全局冷却加回来。

### 第四步那一节怎么办

第四步讲的是"用冷却控制节奏"，那个全局冷却现在没了 ——
但"播报 + 粒子 5 秒一次"这个做法在第六部分又装回来了（只管这两件事）。
**那一节保留着**，因为它是"为什么要在意节奏"的完整来历。

---

## 第十步（第六部分）：把"快"和"慢"拆开

第五部分把冷却整个删掉之后，粒子和播报都变成每刻一次了 —— 这次把它们单独装回 5 秒的节拍，
但**不碰扫描和打架**：

```java
private static final int DISPLAY_INTERVAL_TICKS = 100;   // 5 秒
private int displayTimer = 0;

public static void tick(...) {
    if (!eye.active) return;

    // ========== 每刻都做 ==========
    List<Monster> monsters = findMonsters(level, pos);    // 扫描
    if (eye.hasTalisman()) {
        makeThemFight(level, monsters);                   // 内讧
    }

    // ========== 每 5 秒才做一次：播报 + 粒子 ==========
    if (eye.displayTimer > 0) {
        eye.displayTimer--;
        return;
    }
    eye.displayTimer = DISPLAY_INTERVAL_TICKS;
    if (count != eye.lastMonsterCount) {                  // 只在数字变了才播
        announceMonsterCount(level, pos, count);
        eye.lastMonsterCount = count;
    }
    // 粒子……
}
```

| 做什么 | 节奏 | 为什么 |
|---|---|---|
| 扫描 | **每刻** | 怪物来了要立刻发现 |
| 内讧（分目标） | **每刻** | 换目标要跟得上战斗 |
| 播报"附近怪物：X 只" | **5 秒** | 给人看的；每秒刷 20 次只会烦人 |
| 灵气粒子 | **5 秒** | 给人看的；每秒 120 颗会变成一条光柱，还多 20 倍网络包 |

> 关键区别：**这不是把冷却加回来**。原来的冷却是"整轮等 5 秒"——扫描和打架一起被拖慢；
> 现在这个 `displayTimer` 只管"人看得见的两件事"。

**`lastMonsterCount` 的含义变了一点**：它现在只在**真的播报出去**时才更新，
所以它代表"玩家上一次被告知的数字"。效果：任何变化都会在 5 秒内汇报给玩家，
但 5 秒里变 20 次也只说一次。

### 验证：两个方向都要量

**① 播报确实慢下来了**（`-DisplayTest`：让怪物数量**每秒涨一只**）

```text
  in the window the count grew by 1 every second; the eye announced 5 time(s), last number 22
  the announce walks the 5 s beat                OK  5 announces (~20 without a beat)
  the scan still sees every arrival              OK  last announced number was 22
```

一边每秒变一次、一共变了二十多次，播报只发了 **5 次** ✓（没有节拍的话会发二十多次）。
"最后播出去的数字是 22" 又说明**扫描一只都没漏**。

**② 打架仍然每刻**（`-TalismanTest`，同一个测试在第五部分是 8 行/544 行）

```text
  with talisman: scan x1, fight x591, untouched x0, hurt x1, casualties 0
  segment 2 runs every tick (no cooldown)        OK  591 lines in 16 s (~37/s)
```

591 行 ≈ 20 刻/秒 × 3 只怪 —— 节流**没有**波及到它。

---

## 第十步（第七部分）：有符就自动开启（顺便把手动开关删了）

`tick` 里那个"有没有符"的判断现在是**唯一**的条件，而且它上面再没有别的闸门：

```java
public static void tick(...) {
    // ========== 每刻都做 ==========
    List<Monster> monsters = findMonsters(level, pos);   // 扫描
    int count = monsters.size();

    // 内讧：**只看有没有符**，不看别的开关
    if (eye.hasTalisman()) {
        makeThemFight(level, monsters);                  // 插上符的那一刻就生效
    }

    // ========== 每 5 秒：播报 + 粒子 ==========
    ...
}
```

没符时：只扫描 + 报数，不触发内讧 ✓
有符时：立刻开始内讧 ✓（不需要任何额外操作）

### ⚠️ 这一句话删掉了第五步那个手动开关，我必须说清楚

你说的是"**不需要再判断别的开关**"。`tick` 里除了"有没有符"就只剩一个开关：
第五步做的那个 `active`（右键开 / 关阵法）。所以它已经**不再是逻辑的一部分**了。

我把它**整套删掉**了，而不是留着当摆设：

| 删掉的东西 | 为什么 |
|---|---|
| `private boolean active` | `tick` 不再读它 |
| `toggleActive()` | 没有人调了 |
| 潜行右键的开关分支 | 留着的话，"点一下提示阵法已开启、其实什么都没发生"就是**假反馈** |
| `msg.fulu.formation_eye.on` / `.off` 两条文案 | 没人用了 |

**但"潜行 = 让位"仍然保留**：蹲着右键返回 `PASS`，这样你蹲着拿方块还是能往阵眼上贴，
不会被当成"取符"。

> 想要手动开关回来的话，是一处小改（把 `active` 字段 + `tick` 开头的
> `if (!eye.active) return;` 加回来，右键那个分支也加回来）。说一声就行 ——
> 我没有留半吊子代码，是为了避免"开关看起来还在、其实已经无效"。

### 验证：全程没碰任何开关，只靠"插符"这一个动作

```text
  no talisman  : scan x1, fight x0, untouched x3, hurt x0, casualties 0
  with talisman: scan x1, fight x679, untouched x0, hurt x1, casualties 0

  segment 1 runs without a talisman              OK  the scan spoke 1x while no talisman was fitted
  segment 2 is skipped without a talisman        OK  0 fight rounds, 0 hurt, 0 casualties
  segment 2 runs once the talisman is fitted     OK  fight x679, hurt x1
  segment 2 runs every tick (no cooldown)        OK  679 lines in 16 s (~42/s)
 verdict    : PASS
```

测试流程里**从头到尾没有开过任何开关**：放下阵眼 → 刷三只僵尸（只报数、不打）→
`data merge block ... {HasTalisman:1b}` 插上符 → **立刻开打**（679 行分配，有怪掉血）。

> 这个测试现在是"更强"的版本了：以前它要先把 `active` 写死成 true 才能跑，
> 现在什么都不用改——**符本身就是那个开关**，测试正好验的就是这一点。

---

## 第十步（第八部分）：状态永久保存（逐条确认）

这一步基本没有新代码 —— 是**把前七部分做过的事逐条确认一遍**，
并且用一次真实的"关掉游戏再打开"来证明它。

### 你要确认的六条，逐条对着代码看

| # | 你说的 | 在哪 | 结论 |
|---|---|---|---|
| 1 | 保存数据的方法里存了"有没有符" | [`FormationEyeBlockEntity.java`](src/main/java/com/lengyang/fulu/block/FormationEyeBlockEntity.java) `saveAdditional()` | ✅ `tag.putBoolean("HasTalisman", hasTalisman)` |
| 2 | 读取数据的方法里读了这个状态 | 同文件 `load()` | ✅ `hasTalisman = tag.getBoolean("HasTalisman")` |
| 3 | 不是临时变量、不是 static，而是存在方块实体自己的 NBT 里 | `private boolean hasTalisman` | ✅ 实例字段（不是 static），随 NBT 走 |
| 4 | 阵眼被加载时会自动从 NBT 恢复 | `load()` 由游戏在加载区块时调用 | ✅ 实测（见下面两阶段） |
| 5 | 每刻方法在服务端一直跑，不依赖附近有没有玩家 | `tick()` | ✅ 实测：整场**没有一个玩家连进来** |

### 第 3 条为什么是"实例字段"而不是 static

```java
private boolean hasTalisman = false;    // 每一个阵眼各一份
// 而不是
private static boolean hasTalisman;     // ✗ 全世界所有阵眼共用一份
```

方块实体是**每放一个就 new 一份**，所以实例字段天然是"这一格自己的状态"。
写成 static 的话，你在 A 地插了符，B 地的阵眼也会跟着"有符"——
而且存档时无从下手（static 不属于任何一格）。

### 第 4、5 条：用"真的关掉再打开"来验（`-RestartTest`）

源码里 `saveAdditional` / `load` 都写着，也可能接错线。唯一诚实的验法是**关掉游戏再打开**：

```powershell
# 阶段 1：擦除世界、放阵眼、插上符，然后强制区块写盘，关掉服务器
tools\verify_tick.ps1 -RestartTest -Phase 1 -Seconds 34
# 阶段 2：全新进程、同一个世界（不擦除），只读回来，什么都不碰
tools\verify_tick.ps1 -RestartTest -Phase 2 -KeepWorld -Seconds 30
```

**阶段 1（一次都没碰过开关，只做了"插符"）**

```text
  the talisman was fitted and stored             OK  SET-OK x1
  it is on the disk before the game is closed    OK  1 hit(s) in chunk 0,0, value byte 1
 verdict    : PASS - fitted and written to disk
```

第二条是拿 [`tools/check_region.py`](tools/check_region.py) 直接解压区域文件看到的 ——
不是在游戏里问，而是**在磁盘上找那几个字节**。

**阶段 2（全新的服务器进程，什么都没碰）**

```text
  the nbt flag came back after a restart         OK  NBT-SURVIVED x1
  the blockstate came back too (the look)        OK  LOOK-SURVIVED x1
  it ran with NO player connected                OK  NO-PLAYERS confirmed in game
  and it works right away, nothing switched on   OK  FIGHTING x0, ALL-DEAD x1 (16 s)
  the new server process really ran              OK  6 heartbeats
 verdict    : PASS - restored from disk and working
```

- **NBT 回来了** ✓（`execute if data block 0 -60 0 {HasTalisman:1b}`）
- **方块状态也回来了** ✓ —— 这条容易漏：外观是方块状态驱动的，
  状态跟着区块一起存档，所以重进世界**连贴图都还是"有符"那套**，不用重插
- **全程没有玩家** ✓（`execute unless entity @a` 在游戏里确认的）
- **而且立刻就能用** ✓ —— 刷了三只僵尸，16 秒内**全被打死了**（所以是 `ALL-DEAD`，
  一个"受伤但活着"的都没有）。没有点任何开关，没有重新插符。

> 这就是你这一部分要解决的那个问题：**"每次进世界、切维度都要重新开"** —— 已经不会了。
> 切维度也没问题：每个维度的区块各存各的方块实体 NBT，`load()` 照常被调用。

### ⚠️ 第 5 条要说清楚一个边界：不依赖玩家 ≠ 区块永远加载

`tick()` 里**没有任何和玩家相关的判断** ✓ —— 这是"不依赖玩家"的正确含义，
上面那次测试也在零玩家的服务器上跑通了。

但是：**区块本身还需要是加载状态**。Minecraft 的规则是"附近没有玩家时区块会被卸载"，
卸载了方块实体就不再 tick（这个坑在第四步就踩过：方块"睡着了"）。
所以要让阵法在无人区一直运转，得让那一格区块保持加载（`forceload`、区块加载器，
或别的模组帮忙）。**这是我们代码之外的原版机制**，不是缺陷，但必须知道。

### 顺带发现的一件事：什么在什么时候被存盘

写这个测试时踩到了，值得记：

| 数据 | 什么时候写进磁盘 |
|---|---|
| 区块（方块、方块实体 NBT、方块状态） | **区块卸载时**就会写 |
| 计分板、level.dat 这类世界数据 | 只在**完整存档**时写（自动存档 / 正常关服） |

我的测试是硬杀进程（`taskkill`），所以阶段 1 建的计分板目标在阶段 2 **不存在**——
第一版测试因此报了两个假 FAIL。现在 NBT 检查改用不需要计分板的
`execute if data block`，阶段 2 也自己建需要的东西。
**"到底什么被持久化了"这件事，不能靠猜。**

---

## 第十步（第九部分）：内讧负面效果（一个纯标记）

![内讧的图标](tools/out/infighting_icon_x8.png)

### 新加的东西

| 你说的 | 做成了什么 |
|---|---|
| 新建一个文件专门做这个效果 | [`effect/InfightingEffect.java`](src/main/java/com/lengyang/fulu/effect/InfightingEffect.java)（+ 登记表 [`registry/ModMobEffects.java`](src/main/java/com/lengyang/fulu/registry/ModMobEffects.java)） |
| 能挂在怪物身上 | 它就是 `MobEffect`，`monster.addEffect(...)` 挂上 |
| 图标随便找一个 | [`textures/mob_effect/infighting.png`](src/main/resources/assets/fulu/textures/mob_effect/infighting.png)，18×18，两个箭头对撞 + 中间红点（[`tools/gen_infighting_icon.py`](tools/gen_infighting_icon.py) 画的） |
| 颜色红或紫 | 分类 `HARMFUL`（状态栏**红框**）+ 染色值紫色 `0x8B2FC9` —— 红紫都在 |
| 本身不掉血，只是标记 | 见下：**结构上就不可能掉血** |
| 每刻给范围内的怪挂上 | `markInfighting(monsters)` |
| 给范围外、之前挂过的去掉 | `clearInfightingOutside(level, pos)` |

### "不掉血"是**结构**保证的，不是调参调出来的

`MobEffect` 只有两个会被游戏**周期性调用**的钩子：

```java
applyEffectTick(...)        // 中毒、生命恢复就是在这里扣血/回血的
isDurationEffectTick(...)   // 决定"要不要调上面那个"
```

这个效果**一个都没覆盖** —— 所以游戏来调用时什么都不会发生。
`verify_jar.py` 直接把这条写成了断言：

```text
  [ok]   没有覆盖 applyEffectTick —— 所以它每刻什么也不做，不掉血
  [ok]   没有覆盖 isDurationEffectTick
```

也就是说，哪天有人顺手加了个 `applyEffectTick` 让它扣血，**自检会立刻变红**。

### 挂标记的两个细节

```java
// ① 范围内：只在"剩余不到 1 秒"时才续期
if (current == null || current.getDuration() <= MARK_REFRESH_BELOW) {
    monster.addEffect(new MobEffectInstance(INFIGHTING.get(), 40 /* 2 秒 */, 0, false, false, true));
}
```

- **为什么不每刻都挂**：效果一样（持续时间会被刷新），但每刻都会产生一次
  "同步到客户端"的动作 —— 20 只怪 × 20 刻 = 每秒 400 个包，图标还会一直闪。
  只在快过期时续一次，一秒最多一次。
- **为什么持续时间是有限的 2 秒**：万一阵眼被挖掉、区块被卸载、怪跑得没影了，
  标记会**自然过期**，不会在世界里留下一堆永远擦不掉的"内讧"怪。
  这是兜底；正常情况下走的是下面那条"立刻清掉"。

```java
// ② 范围外：主动清掉，而且查询范围放宽 16 格
AABB wide = formationBox(level, pos).inflate(MARK_CLEAR_MARGIN);
for (Monster monster : level.getEntitiesOfClass(Monster.class, wide)) {
    if (monster.hasEffect(INFIGHTING.get()) && !isInsideFormation(level, pos, monster)) {
        monster.removeEffect(INFIGHTING.get());
    }
}
```

放宽 16 格是为了让"刚走出去"的怪**立刻**掉图标，而不是等 2 秒；
更远的怪不查也没关系（它们会自然过期）。
另外 `formationBox()` / `isInsideFormation()` 是**只定义一次**的 ——
挂和清必须用同一个"在不在范围内"的判断，否则改半径时漏一处，
表现就是边界上的怪图标闪来闪去。

### 验证（`-EffectTest`，出厂版本就能跑）

```text
  the effect is registered                       OK  effect give fulu:infighting worked
  a monster inside the formation is marked       OK  MARKED-INSIDE x1
  stepping out removes it AT ONCE                OK  CLEARED-NEAR x1 (checked 0.5 s later)
  walking back in marks it again                 OK  MARKED-AGAIN x1
  running far away lets it expire by itself      OK  CLEARED-FAR x1, still marked x0
  and that marker really is fulu:infighting      OK  matched by forge:id inside/outside
 verdict    : PASS
```

**"AT ONCE" 那条是这次最关键的设计**：把怪传送出阵法后，**只等 0.5 秒**就去检查 ——
而效果的持续时间是 2 秒，所以"到时间自己过期"根本来不及解释它为什么没了，
**只可能是被主动清掉的**。这就是把"立刻清除"和"自然过期"分开验的办法。

### 两个只有踩过才知道的 1.20.1 + Forge 细节

**① `execute if data entity` 只接受一个实体。** 写成 `@e[tag=t]` 会让**整个函数加载失败**，
而函数加载失败的表现是"后面每一行都静默不执行" —— 看起来就像"模组什么都没做"。
必须写 `@e[tag=t,limit=1]`。

**② 模组效果的 `Id` 字节是个占位 0，身份在字符串键 `forge:id` 里。**
实测：`ActiveEffects[0].Amplifier` 读得出来（3），但 `ActiveEffects[0].Id` 读出来是 0 ——
因为 Forge 对**非原版**效果不用那个字节，而是另存 `forge:id: "fulu:infighting"`。
所以：

```mcfunction
# ✗ 永远匹配不到（Id 是 0）
@e[nbt={ActiveEffects:[{Id:"fulu:infighting"}]}]

# ✓ 能匹配到
@e[nbt={ActiveEffects:[{"forge:id":"fulu:infighting"}]}]
```

测试里主判定用 `Duration`（有持续时间 = 身上挂着效果；这个测试里只有阵眼会发效果），
上面那个 `forge:id` 选择器再确认一次"确实是我们这个效果，不是别的"。

---

## 第十步（第十部分）：内讧怪物传递攻击效果

### 新文件：[`combat/InfightingCombatHandler.java`](src/main/java/com/lengyang/fulu/combat/InfightingCombatHandler.java)

```java
@Mod.EventBusSubscriber(modid = FuLu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InfightingCombatHandler {
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) { ... }
}
```

判定按你给的顺序一条条走：

| # | 判断 | 结果 |
|---|---|---|
| 1 | 加害者不是怪物 | 放过（原版逻辑） |
| 2 | 加害者身上没有"内讧" | 放过（原版逻辑） |
| 3 | 受害者是玩家 | **伤害清零 + 取消事件**（内讧怪打不动玩家） |
| 4 | 受害者不是怪物 | 放过（村民、动物、盔甲架） |
| 5 | 这只怪本来就没有攻击附带效果 | 放过（僵尸、骷髅、苦力怕……） |
| 6 | 其他 | 把它的攻击附带效果**复制一份**加到受害者身上 |

### ⚠️ 为什么是一张写死的表

原版把"打玩家附带什么效果"**写死在每个怪物类的 `doHurtTarget` 里**
（`Husk` 里 new 一个饥饿、`CaveSpider` 里 new 一个中毒……），
**没有任何接口可以问一只怪"你打人会附带什么"**。所以只能照抄：

| 怪物 | 附带 | 时长（照原版） |
|---|---|---|
| 尸壳 Husk | 饥饿 | 140 刻（7 秒） |
| 洞穴蜘蛛 CaveSpider | 中毒 | 普通 7 秒 / 困难 15 秒（原版按难度给，简单不给） |
| 凋灵骷髅 WitherSkeleton | 凋零 | 200 刻（10 秒） |
| 流浪者 Stray | 缓慢 | 600 刻（30 秒） |

加新怪物时，照着它的 `doHurtTarget` 抄进这张表就行。

### "内讧不会通过打架传播"是**结构**保证的

这个类里只有一处 `addEffect`，加的是**攻击附带效果**（表里查出来的），
**从来不是"攻击者身上的效果"**。自检里也钉死了这一点：
`verify_jar.py` 会数编译后类里 `m_7292_`（= `addEffect`）出现的次数，多于一处就报错。

而且实际上"没被标记的怪物被打"这种情况根本不会出现：
阵法只标记范围内的怪，而只有被标记的怪才会被分配目标 →
**能打人的一定有标记，挨打的也都在同一个范围内**。

### 时长规则也改了（这一部分要求）

| 情况 | 内讧的剩余时间 |
|---|---|
| 站在阵法里 | **无限**（`-1`） |
| 走出范围 | 变成 **20 秒**倒计时（400 刻） |

关键实现：**只转换一次**。已经在倒计时的那一份不会每刻被重新拉满，
否则它在阵法边上晃多久，这个 20 秒就永远走不完。

> **一个已知的洞（故意的，写在这里）**：被**瞬移**到 16 格以外的怪，
> 那一次不会立刻被改成 20 秒（它还带着"无限"），因为查找范围只有外扩 16 格。
> 正常**走出去**不会碰到这个洞 —— 它出圈的途中就会被改成倒计时。
> 要把这个洞也堵上，就得在方块实体里记一份"我标记过谁"的名单。

### 验证

```text
-CombatTest
  inside the formation the marker is INFINITE        OK  duration read back as -1
  a marked husk passes hunger to the zombie it hits  OK  found vanilla hunger (id 17)
  stepping out turns it into a 20 s countdown        OK  duration within 1..400
 verdict    : PASS

-EffectTest（已按新规则重写）
  the effect is registered / marked / 20 s / again / counts down / forge:id  OK×7
 verdict    : PASS
```

**没有验到的**：第 3 条（打玩家不掉血）。无头服务端**没有玩家**，
所以那条只有代码和断言在兜着，需要你自己进游戏点一下。

### 自己试

```mcfunction
/summon minecraft:husk ~ ~ ~          # 站在阵法里
/summon minecraft:zombie ~2 ~ ~       # 也站在阵法里
```
插上乱神符后两只打起来，僵尸身上会出现**饥饿**图标（尸壳传过来的）。
然后你自己走进阵法 → 尸壳会挥你但**不掉血**；走出阵法 20 秒后图标消失。

---

## 第十步（第十一部分）：内讧粒子（看得出阵法在生效）

在挂标记的下一行加一句：

```java
markInfighting(monsters);
// 挂上标记之后，顺手让这只怪身上冒粒子
emitInfightingParticles(level, pos, monsters);
```

### 四个刻意的选择

| 你说的 | 怎么做的 |
|---|---|
| 粒子类型用原版现成的 | `ParticleTypes.ANGRY_VILLAGER`（原版"愤怒的村民"头顶那个小云）——不用自己画贴图，语义也正好 |
| 只发给阵眼周围 16 格内的玩家 | **不能用广播版** `sendParticles` ——它的距离上限是 **32** 格，比要的宽一倍。所以先挑出"看得见的玩家"，再一个个发 |
| 每只每刻 1~2 个 | `1 + random(2)`（1 或 2），每只自己抽，不会整齐划一地一起冒 |
| 位置在头顶偏上 | `y + getBbHeight() + 0.25` |
| 只给内讧的怪冒 | 用的就是**上面那份列表**（= 范围内刚被标记的怪），范围外的怪连遍历都进不去 |

> 想换成红/紫烟也很简单：把 `ParticleTypes.ANGRY_VILLAGER` 换成
> `new DustParticleOptions(new Vector3f(0.55F, 0.18F, 0.78F), 1.0F)` 就是紫色尘粒
> （颜色用 RGB 0~1 给）。这一行注释就写在调用处上方。

### 验证：让阵法里 3 只、阵法外 1 只

```text
  particles are emitted every tick               OK  80 ticks in a 4 s window (20/s)
  only the monsters inside the formation count   OK  every tick said marked=3, never 4
  1-2 particles per monster, no more             OK  3 monsters -> 5 particles per tick
  nobody was close enough, so nothing was sent   OK  watchers=0 on all 80 ticks
 verdict    : PASS
```

- **"只有阵法内的怪"是量出来的**：阵法里 3 只、阵法外还停着 1 只（给了 NoAI 防止它走进来），
  80 个刻里每一行都报 `marked=3` ——**一次都没数到 4**。
- **"每刻都有"**：4 秒窗口里 80 行 = 每秒 20 次 ✓。
- **"1~2 个"**：3 只怪最少 3 颗、最多 6 颗，日志里是 5 颗 ✓（每只自己抽，所以是区间内的值）。

### ⚠️ 这一条我验不到：粒子长什么样

服务端只负责"发给谁、在哪儿、发几颗"，**画出来是客户端的事**，而这个测试环境没有客户端。
而且测试时**没有玩家在线**，所以那 80 行报的是"打算发什么"（`watchers=0` = 一个包都没真发出去）。

- **16 格这条**我用结构断言补上了：`verify_jar.py` 检查编译后的类调用的是
  `m_8624_`（= `sendParticles(ServerPlayer, ...)` 单玩家版），**不是** `m_8767_`（32 格广播版）。
- **看起来对不对**得你自己进游戏：

```mcfunction
/setblock ~ ~ ~ fulu:formation_eye     # 放阵眼
# 拿乱神符右键装上                                # 装符
/summon minecraft:zombie ~2 ~ ~        # 刷几只僵尸（多刷几只）
```
站到阵眼旁边 16 格内 → 每只被标记的僵尸头顶会持续冒灰色小云；
走到 20 格外 → 一个粒子都看不到（同时动作栏也不再更新）。

---

## 第十一步（第一部分）：范围改成 ±10

一行改动，其余全部自动跟着变：

```java
private static final int SCAN_RADIUS = 10;   // 原来是 5
// SCAN_DEPTH_BELOW = 10   不变（下方最多 10 格）
// 上方仍然无限
// ANNOUNCE_RADIUS  = 16   不变 —— 见下
```

**为什么只改一行就够**：范围只在 `formationBox()` 里定义了一次，
扫描、数数、挂内讧、清理标记、冒粒子全都读它。这就是当初把它抽出来的回报。

### 关于"显示范围"：查过了，**不需要**跟着改

你说"检查'附近怪物：X 只'的显示范围，如果需要，也改成 10 格"。查完的结论是**不用改**：

| 数字 | 管什么 | 要不要跟着变 |
|---|---|---|
| `SCAN_RADIUS = 10` | **数哪些怪**（阵法范围） | 已改 ✓ |
| `ANNOUNCE_RADIUS = 16` | **谁收得到播报**（发消息的距离） | **保持不变** ✓ |

理由是几何：阵法现在是 10×10 的方阵，**角上离阵眼 √(10²+10²) ≈ 14.1 格**。
把播报半径砍到 10，站在自家阵法角上的玩家反而收不到播报了 —— 那是帮倒忙。
16 格刚好把整个阵脚都罩住。

### 验证：把边界两个方向都钉住

```text
  ZERO     expect 0       saw [0]  OK
  THREE    expect 3       saw [3]  OK
  DOWN10   expect 4       saw [4]  OK   <- 正下方 10 格：计入
  DOWN11   expect silent  saw []   OK   <- 下方 11 格：不计
  HIGH     expect 5       saw [5]  OK   <- 上方 100 格：计入（上方无限）
  X10      expect 6       saw [6]  OK   <- 侧面正好 10 格：计入（新）
  X11      expect silent  saw []   OK   <- 侧面 11 格：不计（新）
  CLEAR    expect 0       saw [0]  OK
 lines total: 6  (expect 6)
 verdict    : PASS
```

"expect silent" 那些阶段的允许清单是**空的** —— 只要有一只圈外的怪被数进去，
那一阶段就会冒出一个数字，测试立刻红。

### 改半径**连累**了三个测试（都是测试的问题，不是模组的问题）

1. **旧阶段在 x=7 放一只怪、期望静默** —— 现在 ±10，7 在范围内 ✗。
   改成"正好 10（必须计入）"和"11（必须静默）"，和已有的 DOWN10/DOWN11 对称。
2. **战斗/效果测试**用 x=10 当"走出阵法" —— 现在 10 在阵内 ✗，改到 x=13
   （仍在 +16 清理边界内，那正是"变成 20 秒"需要的距离）。
3. **`-MobTest` 从第十步第八部分起就已经悄悄坏掉了**：它的分析里用了局部变量 `$phase`，
   而脚本有个 `-Phase` **参数**带 `[ValidateSet('1','2')]`；PowerShell 变量名**不区分大小写**，
   于是给局部变量赋值时撞上了参数的校验器，整个测试直接中止。
   改名成 `$scanPhase` 修好。（这个 bug 藏了两步，因为那之后我一直没跑 `-MobTest`。）

> 另外：第十一部分加的粒子让 tick 每刻多写一行 `[mark]` 日志，
> 而范围分析把那些也当成"播报"数进去了 —— 报出"745 行 / 期望 6 行"的假失败。
> 现在只数 `[scan] monsters=` 开头的行。

---

## 第十一步（第二部分）：右键显示范围边界

拿**木棍**右键阵眼 → 沿着阵法最外圈，每格冒一个粒子，只给右键的那个人看，3 秒后自己消失。

```java
// FormationEyeBlock：先用最简单的东西试，以后换成专属工具（阵尺/罗盘）只改这一行
private static final Item OUTLINE_TOOL = Items.STICK;

if (held.is(OUTLINE_TOOL)) {
    if (!level.isClientSide()) {
        if (!eye.requestOutline(player)) {           // false = 还在连点间隔里
            player.displayClientMessage(Component.translatable(
                    "msg.fulu.formation_eye.outline_wait"), true);
        }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
}
```

### 为什么要让方块实体去记这件事

一次右键只是**一瞬间**，可边界要在屏幕上留几秒。粒子自己活不到一秒，
所以真正的做法是：**右键时只登记一个请求**（`requestOutline`），
由每刻的 tick 在 60 刻（3 秒）里**每 10 刻补画一轮**（6 轮）。

- "持续几秒后自然消失"就是这么来的 —— 不用手动清除，粒子自己老死。
- 只有 `requestOutline` 记得"我还欠谁一次显示"（`outlineViewer`），
  所以**只有右键的那位玩家**收得到粒子（用的是单玩家版 `sendParticles`，
  不是会在 32 格内广播的那版）。
- 连点保护：20 刻内只接受第一次，后面几次给一行提示"边界刚显示过，稍等一下再看"。

### 画在哪：贴合 ±10 的最外圈

```text
  [outline] points=80 r=10 y=100.1 sent=0     ← 每 0.5 秒一轮，共 6 轮
```

`80 = (2×10+1)×2 + (2×10−1)×2`：四条边上每格一个点，四个角只算一次。
高度取阵眼自己那一层 +0.1（贴着地面）。**只画了"地面一圈"**，
四条竖边和上下两个面还没画 —— 按你说的"先画简单的地面一圈"。

### 验证

```text
  the boundary is redrawn over and over          OK  30 bursts
  one dot per block, 80 of them                  OK  every burst said points=80
  the ring sits on the +-10 boundary             OK  r=10 every burst
  the ring is drawn just above the eye's layer   OK  y=100.1 for an eye at y=100
  nothing was actually sent (no player)          OK  sent=0 on all 30 bursts
 verdict    : PASS
```

**验不到的**：画面。无头服务端没有玩家可右键，所以"发给谁"这件事是**结构断言**兜的
（自检要求调用单玩家版 `m_8624_`），几何/节奏/目标玩家判定都在同一条代码路径上。

> ⚠️ 这一部分我写坏过两处，都得记：
> 1. **调试开关每刻把倒计时重置成 60**，递减后永远停在 59，`59 % 10 ≠ 0` ——
>    **一个点都画不出来**。"每 10 刻补一轮"的前提是计数器真的在往下走。
> 2. **用 shell 改 .java 又踩了两次**：一次 `Set-Content -NoNewline` 把所有换行吃了，
>    一次 `Set-Content -Encoding UTF8` 写了 BOM（javac 直接报"非法字符"）。
>    这两条**在第一步就写在 README 里了** —— 说明"记下来的坑"如果自己不遵守，
>    照样会再踩一次。改 .java 只用编辑器。

### 自己试

```mcfunction
/give @s minecraft:stick
```
拿木棍右键阵眼 → 脚下出现一圈白色光点（只有你看得见），3 秒后消失；
连点第二下会提示"边界刚显示过"。换专属工具时只改 `OUTLINE_TOOL` 那一行。

---

## 第十一步（第三部分）：右键显示阵法信息

空手右键那个分支现在分两条路：

```java
if (held.isEmpty()) {
    if (!level.isClientSide()) {
        if (hasTalisman) {
            ... 取符（和以前一样） ...
        } else {
            showFormationInfo(player, eye);      // ← 新
        }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
}
```

发到**聊天栏**（不是动作栏）——一次三条，动作栏放不下，而且几秒就淡了：

```text
阵法状态：关闭
范围半径：10 格
当前怪物数量：3 只
```

| 信息 | 从哪来 |
|---|---|
| 阵法状态 | `eye.hasTalisman()`（现在插了符就等于开启） |
| 范围半径 | `FormationEyeBlockEntity.getScanRadius()` —— 范围的唯一来源 |
| 当前怪物数量 | `eye.getMonsterCount()` —— **每刻扫描时顺手存下来的那个数** |

### "复用已经扫描到的数字"是怎么保证的

两条独立的保证：

1. **每刻都在更新**：tick 里扫描完立刻 `eye.currentMonsterCount = count;`，
   位置在**任何提前 return 之前** —— 所以右键读到的永远是最新一轮的结果，不会读到旧值。
2. **根本没法重扫**：`findMonsters` 是方块实体里的 `private static`，方块类碰不到它；
   自检里还钉了一条**否定断言** —— `FormationEyeBlock.class` 里**不允许出现**
   `findMonsters` 这个名字。以后有人图省事想在这儿再扫一遍，测试会立刻红。

### 验证

```text
  particles are emitted every tick                  OK  80 ticks in a 4 s window
  only the monsters inside the formation count      OK  marked=3 every tick, never 4
  1-2 particles per monster, no more                OK  3 monsters -> 5 per tick
  nobody was close enough, so nothing was sent      OK  watchers=0 on all 80 ticks
  the stored monster count is refreshed every tick   OK  count=3 on all 80 ticks
 verdict    : PASS
```

最后一条才是这一步的重点：**80 个刻里 `count` 一直是 3**，说明信息面板要读的那个数字
每刻都被重写、而且和扫描结果一致。

**验不到的**：那三行聊天。它需要玩家右键，而无头服务端没有玩家 ——
接线由自检的断言兜着（`showFormationInfo` 存在、读的是 `getMonsterCount`/`getScanRadius`、
五个文案 key 真的被引用），数字的正确性由上面那条覆盖。

### 自己试

```mcfunction
# 空手右键一个没插符的阵眼
```
聊天栏出现三行（状态=关闭 / 半径=10 格 / 数量=X 只）；
拿乱神符右键装上后再空手右键，第一行会变成"开启"；
再空手右键一次就是把符取回来。

---

## 第十二步：阵法图鉴

![阵法图鉴](tools/out/formation_codex_x8.png)

### 新加了什么

| 你说的 | 做成了什么 |
|---|---|
| 新建物品"阵法图鉴" | `formation_codex`，[`item/FormationCodexItem.java`](src/main/java/com/lengyang/fulu/item/FormationCodexItem.java)（本模组第一个**有自己类**的物品） |
| 写合成配方 | [`data/fulu/recipes/formation_codex.json`](src/main/resources/data/fulu/recipes/formation_codex.json)：书 + 红石粉 + 符纸 |
| 右键打开图鉴 | 右键 → 把列表发到聊天栏 |
| 第一次放置阵眼 → 解锁"乱神阵" | 覆盖 `setPlacedBy` → 记一位解锁标志 |
| 第一次装符激活 → 解锁详细信息 | 装符分支里 → 再记一位 |
| 右键显示已解锁列表 | 没解锁的显示 **？？？** |
| 点某个阵法看详情 | 列表那行带**原版点击事件**，点一下执行 `/fulu codex show infighting` |
| 详情：名称/范围/效果/材料 | 五行文本（标题/名称/范围/效果/材料） |
| 以后每个新阵法加一条 | 只改 [`FormationCodex.ALL`](src/main/java/com/lengyang/fulu/codex/FormationCodex.java) 里的一行 + 几条 lang |

### ⚠️ 两个我自己拿的主意，都要说清楚

**① 朱砂还没有这个物品** —— 你说的是"比如书 + 朱砂 + 符纸"，所以我先用
**红石粉**顶上（配方里是 `minecraft:redstone`）。模组里现在没有朱砂；
真要加一个朱砂物品（贴图 + 注册），说一声，配方里换成 `fulu:cinnabar` 就行。

**② "点某个阵法"用的是聊天栏点击事件，不是自绘 GUI 界面。**
原版聊天消息可以带点击事件，所以列表里那行 "▸ 乱神阵 [点击查看]" 点一下就执行
`/fulu codex show infighting`，详情立刻发到聊天栏。这样不用写客户端界面，
而且同一个命令在聊天框里也能直接敲。**如果你要的是那种翻页式的图鉴界面
（Screen + 菜单），那是另一块客户端渲染的活**，得单独做。

### 解锁状态存在计分板里（这是能验证的关键）

```text
  计分板目标 fuluCodex，每个玩家一条分数，按位记：
    第 0 位（1）= 见过：放下过阵眼      -> 条目从"？？？"变成真名
    第 1 位（2）= 用过：装符激活过阵法  -> 详细信息（5 行）才打开
```

为什么用计分板而不是玩家 NBT：它本来就是"每个玩家一份、随存档保存"的东西，
而且**可以用命令读写** —— 于是"没玩家的自动化测试"也能验这套解锁逻辑
（测试直接用 `scoreboard players set Tester fuluCodex 1/3` 摆出三个阶段）。

### 验证：三个阶段 + 一个错误 id

```text
  a fresh world shows one ??? and no names       OK  bits=0 named=0 locked=1
  placing an eye reveals the name only           OK  named=1; detail 3 lines, unlocked=false
  activating unlocks the full detail             OK  unlocked=true, 5 lines
  an unknown formation id is reported            OK  1 line
 verdict    : PASS
```

正好对应你给的测试步骤：**新世界只有 ？？？ → 放阵眼后出现"乱神阵" → 装符后显示详细**。

- 5 行 = 标题 / 名称 / 范围 / 效果 / 材料
- 3 行 = 标题 / 名称 /"（详细信息：装符激活阵法后解锁）"
- 1 行 = "没有这个阵法：xxx"

**验不到的**：两个解锁**触发点**本身（放方块、右键装符都需要玩家）。
测试验的是它们写下的那个状态 —— 用命令写同样的位，图鉴的渲染和真实玩家完全一样。
触发点由自检兜着：`setPlacedBy` 被覆盖 ✓、`FormationCodex.unlock` 被调用 ✓
（javap 能看到 **两处**调用点）。

### 两个 1.20.1 的 API 意外（都是编译器抓出来的）

1. **`ScoreHolder` 还不存在**（1.20.3 才有）—— 这里的计分板 API 吃的是**玩家名字符串**。
2. **`Score` 是 `getScore()` / `setScore()`**，不是 `get()` / `set()`。

### 还有一个测试自己的 bug

图鉴分析用**位置捕获组**读 `$Matches`，而 `$Matches[0]` 是**整行**——于是每组都错位一位，
一行完全正常的日志被读成失败（`unlocked=5 lines=`）。改成**具名捕获组**就好了。

### 自己试

```mcfunction
/give @s fulu:formation_codex      # 或者按配方合成：书 + 红石粉 + 符纸
```
1. 新世界的图鉴：只有 `？？？`。
2. 放下一个阵眼，再右键图鉴：出现 `▸ 乱神阵 [点击查看]`，点它 → 详情那里还写着"装符激活后解锁"。
3. 拿乱神符右键阵眼装上，再点一次 → 范围 / 效果 / 材料 三行都出来了。

---

## 第十三步：图鉴界面（GUI）

![布局草图](tools/out/codex_layout_sketch.png)

> ⚠️ 上面这张是**布局草图**，不是截图 —— 我这边没有 Minecraft 客户端（没显示器、没 GPU），
> 画面本身是我唯一验不了的东西。它是照着同一套数字（面板 320×200、行高 20、左栏 130）
> 用 Python 画的，方便你先看布局。

### 三块新东西

| 文件 | 是什么 |
|---|---|
| [`menu/CodexMenu.java`](src/main/java/com/lengyang/fulu/menu/CodexMenu.java) | 菜单（**服务端那一半**）：两种模式 + 实时数据 |
| [`registry/ModMenus.java`](src/main/java/com/lengyang/fulu/registry/ModMenus.java) | 菜单类型登记（`fulu:codex`） |
| [`client/screen/CodexScreen.java`](src/main/java/com/lengyang/fulu/client/screen/CodexScreen.java) | 界面（**纯客户端**）：列表 / 详情 / 搜索 / 关闭 |

MC 的界面是**两端合作**的：服务端开一个 `AbstractContainerMenu`，客户端照着同一个 id
开一个 `Screen`。所以 `ModClientSetup` 里多了一行 `MenuScreens.register(...)` ——
**这一行就是最后一环**，漏了它的表现是"右键之后什么都没发生"。

### 你要的每一条

| 你要的 | 怎么做的 |
|---|---|
| 左侧列表：名字 + 状态图标 | 手工画行；图标用该阵法的物品图标（乱神符） |
| 未解锁：？？？ + 灰色问号 | 灰色画一个 `?` |
| 已见闻：名字 + 半亮图标 | 画物品图标，上面盖一层半透明黑（"半亮"） |
| 已精通：名字 + 全亮图标 | 画物品图标，不盖 |
| 右侧详情（三档内容） | `FormationCodex.detailLines(stage, entry)` |
| 底部搜索框 | 原版 `EditBox`，过滤 `FormationCodex.ALL` |
| 关闭按钮 | 原版 `Button`（ESC 照旧能用） |
| 顺便做阵眼 GUI | **拿图鉴右键阵眼** → 打开阵眼界面（状态/半径/怪物数量，数字是活的） |

### 为什么把"界面逻辑"抽成纯函数

`stage()` / `matches()` / `detailLines()` 三个方法**不碰渲染、不碰网络**，只吃数据出数据。
这样"界面该显示什么"可以脱离客户端单独测（`-CodexTest` 测的就是它们），
剩下必须人眼看的只有"画出来的样子"。

### 验证

```text
  no datapack function failed to load            OK
  a fresh world shows one ??? and no names       OK  bits=0 named=0 locked=1
  placing an eye reveals the name only           OK  named=1; detail 3 lines
  activating unlocks the full detail             OK  unlocked=true, 5 lines
  an unknown formation id is reported            OK  1 line
  the GUI's stage model: SEEN then MASTERED      OK  bits=1 -> SEEN, bits=3 -> MASTERED
  the search box filters (same rule as screen)   OK  infight -> 1 hit, zzzz -> 0
 verdict    : PASS
```

**专用服务端检查（加 GUI 最大的风险）**：出厂 jar 起一个无头服务端**正常**，冒烟测试照旧通过。
这就是"公共类里不许出现 `net/minecraft/client`"那条自检断言的实测版本 ——
客户端类被公共代码碰到，正是"单机能玩、上服务器就崩"的经典死法。

**验不到的（要你的客户端）**：所有看得见的东西 —— 控件位置、"半亮"看起来够不够半亮、
详情长句的折行、阵眼界面数字是不是真的在动。

### 这一步我又错了两次，都是被测试抓出来的

1. 给 `fulu codex find` **多写了一个参数** → 整个 datapack 函数**加载失败**，
   后面两个阶段什么都没产生，四条检查全红。原因在加上
   "没有函数加载失败"这条检查之前是看不见的 —— 现在它是这个测试的**第一条**。
2. 同一个分析在按阶段分桶时**把 `find` 行丢掉了** → 一次完全正常的搜索被报成"没有命中行"。
   测试会**两头骗人**：产品坏了它可能过，产品没问题它也可能红。

> 另外，这一步我**第三次**用 shell 改 .java 文件、又写出 BOM 被 javac 拒绝。
> 规则早就写在 README 第一步里了 —— 改 .java 只用编辑器。

### 自己试

```mcfunction
/give @s fulu:formation_codex
```
1. **右键空气** → 图鉴界面：左边一列（没解锁是灰色 `?` + ？？？），右边"尚未解锁"，底部搜索框 + 关闭按钮。
2. 放下阵眼后再开：条目的 `?` 变成**半亮的乱神符图标**，名字出来，右边有名字/范围/大致效果。
3. 装符激活后再开：图标**全亮**，右边多出"需要材料 / 合成方式"两行。
4. **拿图鉴右键阵眼** → 阵眼界面：状态、半径、当前怪物数量（盯着看，数字会跟着变）。

---

## 第四部分：乱神阵的图鉴内容（已见闻 / 已精通）

内容按你给的原文写，**一个字没改**：

```text
已见闻（3 行）                        已精通（6 行）
名称：乱神阵                          名称：乱神阵
范围：半径 10 格                      范围：半径 10 格，下方 10 格，上方无限
效果：让范围内怪物互相攻击            效果：范围内怪物互相攻击，内讧时会把自身攻击负面效果传给其他怪物
                                     材料：阵基 ×4、阵眼 ×1、乱神符 ×2
                                     激活方式：手持乱神符右键阵眼
                                     关闭方式：空手右键阵眼取回乱神符
```

### 做法：标签 + 值，两档共用一份

每行都是"标签：值"：

| 标签（通用文案，只需翻译一次） | 值（每个阵法自己的文案） |
|---|---|
| `codex.fulu.detail.name` = `名称：%s` | `codex.fulu.infighting.name` = `乱神阵` |
| `codex.fulu.detail.range` = `范围：%s` | `…range_short` / `…range`（两档不同） |
| `codex.fulu.detail.effect` = `效果：%s` | `…effect_short` / `…effect` |
| `codex.fulu.detail.material` = `材料：%s` | `…material` |
| `codex.fulu.detail.activate` = `激活方式：%s` | `…activate` |
| `codex.fulu.detail.deactivate` = `关闭方式：%s` | `…deactivate` |

"范围"和"效果"各有**两条**文案（`.range_short` 给已见闻，`.range` 给已精通）——
这就是同一句话在两档里长度不同的实现方式。

**聊天栏和 GUI 共用 `FormationCodex.detailLines(...)`**，所以不可能出现
"界面上六个字段、聊天里只打出三个"这种两边不一致。

### 验证：逐字核对

自检里现在**直接存着你给的字符串**作为期望值 —— 错一个字、改一个标点都会红：

```text
  [ok]   已见闻是 3 行：名称：乱神阵 / 范围：半径 10 格 / 效果：让范围内怪物互相攻击
  [ok]   已精通是 6 行：名称：乱神阵 / 范围：半径 10 格，下方 10 格，上方无限 / 效果：… / 材料：阵基 ×4、阵眼 ×1、乱神符 ×2 / 激活方式：手持乱神符右键阵眼 / 关闭方式：空手右键阵眼取回乱神符
```

行为侧（无头服务端跑一遍，数行数）：

```text
  placing an eye reveals the seen-level detail   OK  4 lines（标题 + 3 行正文）
  activating unlocks the full detail             OK  7 lines（标题 + 6 行正文）
 verdict    : PASS
```

### ⚠️ 两件必须告诉你的事

1. **"合成方式"这行没了** —— 第十三步你要的已精通里有它，但这一次你给的清单里没有，
   所以我按新清单办（不留旧字段）。**要加回来说一声**，它会是第 7 行正文。
2. **`阵基` 这个物品现在还不存在** —— 图鉴里写着"材料：阵基 ×4"，
   但模组里没有阵基这个方块/物品可以拿。我**照你的原文写**，没有自己造一个方块出来。
   **要不要接着做阵基？**（它是阵法占地用的底座，听起来是下一步）

### 关于刚才的帕秋莉适配：已按要求撤掉

删除清单（只删了为它而加的东西）：

```text
  src/main/java/com/lengyang/fulu/compat/PatchouliCompat.java     删
  src/main/resources/data/fulu/patchouli_books/                  删（整本书）
  src/main/resources/data/fulu/advancements/                     删（只为 Patchouli 的条目上锁而加）
  run/mods/                                                      删（下载本来就失败了）
```

图鉴物品回到单一入口：右键 → 我们自己的界面。

> 顺便交代一句：那个 jar **压根没下载成功** —— 这个沙箱里 `curl` 和 .NET 都拿不到 TLS 凭证
> （`SEC_E_NO_CREDENTIALS`）。所以就算你保留那套适配，我也**没有条件真机验证它**，
> 只能做结构检查。现在撤掉了，这个遗憾也就不存在了。

---

## 第五部分：补物品（朱砂 / 阵基）+ 图鉴配方

### 朱砂

| 项目 | 内容 |
|---|---|
| 注册名 | `fulu:cinnabar` |
| 中文名 | 朱砂 |
| 贴图 | [`textures/item/cinnabar.png`](src/main/resources/assets/fulu/textures/item/cinnabar.png)（红色矿粉） |
| 配方 | 红石 + 火药 → 朱砂（无序合成） |
| 生成脚本 | [`tools/gen_cinnabar_and_base.py`](tools/gen_cinnabar_and_base.py) |

### 阵基

| 项目 | 内容 |
|---|---|
| 注册名 | `fulu:formation_base`（**方块 + 方块物品**两个都登记） |
| 中文名 | 阵基 |
| 贴图 | [`textures/block/formation_base.png`](src/main/resources/assets/fulu/textures/block/formation_base.png)（**石砖底 + 朱砂线条符文**） |
| 方块状态 | [`blockstates/formation_base.json`](src/main/resources/assets/fulu/blockstates/formation_base.json)（**只有一个变体**，因为它没有属性） |
| 模型 | 方块 `cube_all` + 物品模型指向方块模型（手里和地上一个样） |
| 掉落表 | [`loot_tables/blocks/formation_base.json`](src/main/resources/data/fulu/loot_tables/blocks/formation_base.json) |
| 配方 | 石砖 + 铁粒 + 朱砂 → **4 个**阵基 |
| 语言 | [`block.fulu.formation_base`](src/main/resources/assets/fulu/lang/zh_cn.json) = 阵基 |

阵基是**普通方块**：没有自己的类、没有方块实体、没有 tick、没有方块状态属性。
（自检里有一条否定断言钉着："`ModBlockEntities` 里不许出现 `formation_base`" ——
它不需要每刻逻辑，多一个方块实体只是白白的开销。）

### 贴图是怎么画的：石砖底 + 朱砂线条

[`tools/gen_cinnabar_and_base.py`](tools/gen_cinnabar_and_base.py) 里阵基的贴图是**算出来的**，不是手摆的：

1. **石砖底**：`base_pixel(x, y)` —— 每 4 行一条勾缝，竖缝每 8 列一条、
   并且**隔层错开 4 列**（这就是砖墙的错缝）；每块砖再按坐标取一个固定的小扰动，
   免得一片死平。
2. **朱砂线条**：`base_rune(x, y)` —— 只画**一像素宽**的线：
   到中心的距离四舍五入等于 6 的像素组成圆环，
   `|dx| + |dy| == 3` 组成内层菱形，再加中心 2×2 的点。

> 第一版我把中间画成了**实心菱形**，结果看着像一坨红，不像"刻在石头上的符文" ✗。
> 改成纯线条之后就对了 —— 这也是把它写成函数而不是硬编码像素图的好处：
> 想调粗细只改一个数字。

### ⚠️ 一个差点出海的 bug

阵基设了 `requiresCorrectToolForDrops`（要用对工具才掉），但我**忘了把它加进
`data/minecraft/tags/blocks/mineable/pickaxe.json`**。这种组合的结果是：
方块能放、能看见、看着一切正常，**挖掉什么都不掉** —— 玩家会以为"这方块坏了"。

发现它的方式很土：看到标签文件早就为阵眼存在，就顺手对了一眼。现在自检里
有一条断言要求**每个方块都在那个标签里**。

> 注意：下面的掉落测试是**直接调掉落表**的，所以它**抓不到**这个 bug ——
> 标签是另一道闸门。两条检查都得有。

### 图鉴配方改成：书 + 朱砂 + 符纸

从"红石粉 + 符纸 + 书"改成你写的三样，**无序合成**。
自检里加了一条**否定断言**：配方里**不许再出现红石粉**（代用品要撤干净）。

### 验证

```text
=========== 方块测试（不需要玩家）===========
  the formation base can stand in the world      OK  setblock + 读回来
  breaking it drops the block itself             OK  掉落表产出了 fulu:formation_base
  the cinnabar item id resolves                  OK  summon item fulu:cinnabar 成功
  it can be taken away again                     OK  之后读回来是空气
  no recipe failed to load                       OK  3 个配方都没报错
 verdict    : PASS

=========== 图鉴回归（你给的测试步骤）===========
  a fresh world shows one ??? and no names       OK  bits=0 named=0 locked=1
  placing an eye reveals the seen-level detail   OK  4 行
  activating unlocks the full detail             OK  7 行
  the GUI stage model: SEEN then MASTERED        OK
  the search box filters                         OK
  all wording matched exactly                    OK
 verdict    : PASS
```

"能挖掉"是怎么在无头服务端验的：`/loot spawn <pos> mine <pos>` 会在那个位置
**真的跑一遍这块方块的掉落表**，然后再用命令检查地上出现的那个物品实体。
掉落表缺失或指错东西的表现就是"地上什么都没有 / 是别的物品"。

打包自检 **346 项全过**。

### 你自己验一下

```mcfunction
/give @s fulu:cinnabar              # 或者：红石 + 火药 合成
/give @s fulu:formation_base        # 或者：石砖 + 铁粒 + 朱砂 → 4 个
/give @s fulu:formation_codex       # 或者：书 + 朱砂 + 符纸
```
1. 三种东西都能拿到、能看到图标。
2. 阵基能放下去、**挖掉会掉回自己**（这条最值得亲自试，就是上面那个坑）。
3. 合成台里按配方都能合出东西。
4. 图鉴三档：新世界只有 ？？？ → 放阵眼变"已见闻" → 装符变"已精通"。

---

## 第六部分：图鉴物品 + 解锁进度（补缺 + 实测持久化）

这两件事前面几步已经做了大半，这次补上**真正缺的**，并把"退出重进不丢"**实测**出来。

### 一、图鉴物品

已有：物品 `fulu:formation_codex`、配方 **书 + 朱砂 + 符纸（无序）**、模型贴图、语言里的名字。

**本次补 1：物品的"说明"** —— 之前只有名字，鼠标停上去什么都没有。现在有两行：

```text
记录你见过的阵法，以及它们怎么布。
右键打开图鉴；拿它对阵眼看阵法状态。
```

由 [`FormationCodexItem.appendHoverText`](src/main/java/com/lengyang/fulu/item/FormationCodexItem.java) 输出，
两行都是语言 key（中英各一份）。

**本次补 2：贴图重画成"一本带符文的书"** —— 棕色皮封面 + 朱砂菱形符文 + 右侧书口。

> 第一版我画的是**圆环**符文，可 16px 下圆环就是一个方框 ✗，看着像相框不像符文。
> 改成**菱形轮廓 + 中心点**就对了 —— 顺便和阵基贴图的内菱形凑成同一套视觉语言。
> 贴图是 [`tools/gen_formation_codex.py`](tools/gen_formation_codex.py) 算出来的，改一个数字就能调。

### 二、解锁进度

已有：三个状态（未解锁 / 已见闻 / 已精通）、两个解锁点（放阵眼 → 已见闻；装符激活 → 已精通）、
以及渲染三档内容的那套纯逻辑。

**本次改的是存储。** 你说"**数据存在玩家身上**"，所以正式存档现在写进**玩家自己的 NBT**：

```java
player.getPersistentData().putInt("FuLuCodex", bits);
// -> 落进存档的 playerdata/<uuid>.dat，跟着那个玩家走
```

计分板 `fuluCodex` 保留为**镜像**，理由是它有两个玩家 NBT 给不了的好处：
按**名字**查（玩家不在线也能查）、以及无头测试唯一能读写这个状态的地方。
读取规则 `bitsForName(level, name)`：

```text
在线的玩家 -> 读他身上的 NBT（正式数据）
不在线的   -> 退回计分板镜像
```

还带了一次**迁移**：老存档里只有计分板、没有 NBT 时，先把镜像补进 NBT，进度不会丢。

### 三、"退出重进不丢"——实测

两个**完全独立**的服务端进程，同一个世界：

```text
  [阶段 1]
    phase 1 unlocked the formation                 OK  Tester = 3（已见闻 + 已精通）
    the world was written to disk on request       OK  rcon save-all flush 已确认
  [阶段 2：全新进程]
    the unlock progress came back after a restart  OK  读到 Tester = 3
  verdict: PASS / PASS
```

**过程中的两个发现，都得记下来：**

1. 我原先的停止方式是 `taskkill /F`（**硬杀**）。硬杀**不触发**服务端的 shutdown hook，
   世界根本没存盘 —— 这种失败是**测试环境不真实**，不是模组丢数据。
2. 想改成"优雅关闭"也不行：`taskkill` 不带 `/F` 对**无窗口进程没有作用**，它拒绝关闭。

最后写了一个最小 **RCON 客户端** [`tools/rcon.py`](tools/rcon.py)，让服务端执行
`save-all flush` —— 这正是正常退出时做的存档动作。自检现在会**先确认 `Saved the game` 再下结论**，
避免把"没存盘"误报成"数据丢了"。

**落盘证据**（数据文件到底是哪一个）：

```text
  run\verifyworld\data\scoreboard.dat   241 字节（解压后）
      含 'fuluCodex' : True
      含 'Tester'    : True
```

### 四、回归

图鉴三档渲染在改动后重跑：新世界 ？？？ → 放阵眼 4 行 → 装符 7 行 ✓，
搜索/状态图标判定 ✓，逐字核对 ✓。打包自检 **355 项全过**。

### 还得你客户端确认的

- 物品说明那两行、以及新贴图长什么样（画出来的东西我看不到）。
- 两个解锁**触发点**：放方块、右键装符都要真玩家。状态和渲染我验了，
  "谁在什么时候改状态"只能靠代码结构（自检钉着 `setPlacedBy` 被覆盖 + `unlock` 被两处调用）。
- 玩家 NBT 那份写入本身（无头环境产生不了 `playerdata/<uuid>.dat`）——
  但用的就是 Minecraft 自己的玩家存档机制，和第十步阵眼 NBT 是同一套办法（那个实测过）。

---

## 第七部分：图鉴界面 / 乱神阵内容 —— 逐条对照

这两部分在第十三步和第四部分就做完了。这次做的是**逐条对照你的规格**，
并把界面在三个状态下的样子画成**"文字精确的示意图"**：

![三个状态](tools/out/codex_screen_states.png)

> ⚠️ 这是**示意图，不是截图**（我这边没有客户端）。但**文字不是编的**：
> 生成脚本 [`tools/gen_codex_screen_sketch.py`](tools/gen_codex_screen_sketch.py)
> 直接读 [`zh_cn.json`](src/main/resources/assets/fulu/lang/zh_cn.json)，
> 用的 key 和顺序与 `FormationCodex.detailLines()` 完全一致。
> 所以"界面上会写什么字"是准的，"看起来什么样"是猜的。

### 三、界面规格对照

| 你的要求 | 实现在哪 | 实测 |
|---|---|---|
| 右键图鉴打开界面 | `FormationCodexItem.use` → `NetworkHooks.openScreen` | 自检钉住：菜单类型已注册、公共类不引用客户端类 |
| 列表：未解锁 = ？？？ + 灰色问号 | `codex.fulu.locked` + `drawStatusIcon` 画灰 `?` | 无头测试：`bits=0 → locked=1` |
| 列表：已见闻 = 名字 + **半亮**图标 | 物品图标上盖一层半透明黑 | `bits=1 → SEEN` |
| 列表：已精通 = 名字 + **全亮**图标 | 图标不盖 | `bits=3 → MASTERED` |
| 右侧：未解锁 = 只有"尚未解锁" | `detailLines` LOCKED 分支，1 行 | ✓ |
| 右侧：已见闻 = 名字 / 范围 / 大致效果 | SEEN 分支，3 行 | 详情 4 行（含标题）✓ |
| 右侧：已精通 = 名字 / 范围 / 详细效果 / 材料 / 激活方式 / **关闭方式** | MASTERED 分支，6 行 | 详情 7 行（含标题）✓ |
| 底部搜索框 | 原版 `EditBox`，过滤 `FormationCodex.ALL` | 无头测试：`infight → 1`，`zzzz → 0` |
| 关闭按钮 | 原版 `Button`（ESC 照旧能用） | 自检钉住 `Button` 在界面类里 |

### 四、乱神阵内容对照（逐字）

```text
已见闻                                    已精通
名称：乱神阵                              名称：乱神阵
范围：半径 10 格                          范围：半径 10 格，下方 10 格，上方无限
效果：让范围内怪物互相攻击                效果：范围内怪物互相攻击，内讧时会把自身攻击负面效果传给其他怪物
                                         材料：阵基 ×4、阵眼 ×1、乱神符 ×2
                                         激活方式：手持乱神符右键阵眼
                                         关闭方式：空手右键阵眼取回乱神符
```

自检里存着这些字符串本身，**错一个字都会红**：

```text
  [ok]   已见闻是 3 行：名称：乱神阵 / 范围：半径 10 格 / 效果：让范围内怪物互相攻击
  [ok]   已精通是 6 行：名称：乱神阵 / 范围：半径 10 格，下方 10 格，上方无限 / 效果：… / 材料：阵基 ×4、阵眼 ×1、乱神符 ×2 / 激活方式：手持乱神符右键阵眼 / 关闭方式：空手右键阵眼取回乱神符
```

> **一处我保留了旧写法**：这次的"已精通"清单里没有写"名称"，
> 但你在第四部分明确要求过要有名字，而且界面上第一行就是名字，所以**保留**了。
> 如果你确实要去掉，说一声。

### 还是那件事：画面我看不到

界面**写什么字**、**三档怎么切换**、**搜索怎么筛**都验过了；
**画出来的样子**（控件位置、半亮够不够半亮、长句折行）要你进游戏看一眼。

---

## 第八部分：修两个**实测**出来的 bug（截图反馈）

你的截图给了两条硬证据，都是真 bug：

### bug 1：界面读不到解锁进度（显示 ？？？）

**现象**：界面写着"？？？/尚未解锁"，但动作栏明明显示"阵眼已装上乱神符" ——
说明**装符逻辑跑了、状态也记下了**，只有**界面读不到**。

**根因**：界面原来去读**客户端**那份计分板：

```java
minecraft.player.getScoreboard().getObjective("fuluCodex") ... getScore()
```

而原版只会把**"显示中的目标"**（侧边栏/列表里的）的分数同步给客户端 ——
`fuluCodex` 是隐藏目标，**客户端一个字节都收不到** ✗，于是永远是 0、永远 LOCKED。
（服务端侧一直是好的：`/fulu codex` 那条路走的是服务端数据。）

**修法**：不让界面自己去翻计分板，改成**开界面时把解锁位交给菜单**：

```text
FormationCodexItem:  int bits = FormationCodex.unlocked(serverPlayer);
                     new CodexMenu(id, inventory, bits)
CodexMenu:           解锁位放进 ContainerData 的 DATA_UNLOCKS 格
CodexScreen:         menu.getData(DATA_UNLOCKS)
```

`ContainerData` 由菜单**每刻同步一次**，是"服务端状态送到客户端界面"的标准做法，
也彻底不依赖那个隐藏目标会不会被同步。

### bug 2：左上角悬停弹出"煤炭"

**根因**：`CodexMenu` 里原来走了一遍 `addPlayerInventory(...)`，
36 个 `Slot` 的坐标**全是 (0,0)** —— 叠在屏幕左上角，鼠标划过去就弹物品提示。
图鉴是**只读面板**，本来就不该带背包格；不加也不会掉东西
（这些格子只是玩家自己背包的视图，不是独立容器）。现在整个删掉。

顺带把绘制顺序理顺：以前我自己调 `renderBackground`、父类又画一次；
现在统一成 **先 `super.render`、再画我们自己的面板和文字**，不会再互相盖。

### 防复发（自检 +5 条，共 361 项全过）

```text
  [ok]  开界面时把解锁位从服务端取出来（FormationCodex.unlocked）交给菜单
  [ok]  界面不碰计分板（隐藏目标不会同步给客户端 —— 这是踩过的坑）
  [ok]  解锁位是从菜单的同步数据里读的（getData）
  [ok]  解锁位也在同步数据里（DATA_UNLOCKS）
  [ok]  菜单里没有物品格
```

### 我验不了的那一环

链路的两端（服务端取值 / 界面取用）都能在字节码里钉住；
中间那段"菜单数据同步到客户端"是**原版的网络机制** —— 无头环境没有客户端，我没法实测。
所以这次修复的**最后一环要你确认**：

```text
  1. 新世界放一个阵眼（→ 已见闻）
  2. 拿乱神符右键阵眼装上（→ 已精通）
  3. 右键图鉴：左边出现"乱神阵" + 图标，右边六行正文
  4. 左上角不该再有任何悬停提示
  5. 退出重进，进度还在
```

> ⚠️ 另外记一笔：`static final int` 会被**内联**，字节码里根本没有常量名 ——
> 这次想断言"界面读的是 DATA_UNLOCKS 那一格"又撞上了（**同一个坑第三次**）。
> 现在改成"菜单那边确实把解锁位放在 DATA_UNLOCKS 上" + "界面确实调了 getData"，
> 两条合起来说明接线正确。

---

## 第九部分：飞剑符 + 阵眼装两种符

![三种外观](tools/out/flying_sword_three_eyes.png)

### 一、飞剑符

| 项目 | 内容 |
|---|---|
| 注册名 | `fulu:flying_sword_talisman`（飞剑符） |
| 配方 | 符纸 + 朱砂 + 铁锭 + 羽毛（**无序**合成） |
| 贴图 | [`textures/item/flying_sword_talisman.png`](src/main/resources/assets/fulu/textures/item/flying_sword_talisman.png)（符纸上画一把小剑） |
| 生成脚本 | [`tools/gen_flying_sword_talisman.py`](tools/gen_flying_sword_talisman.py) |

> **关于"上网搜图照着做"**：我搜了，也定位到了具体参考
> （[16x16 剑 sprite sheet](https://www.summerengine.com/pt/asset-store/16x16-eight-direction-sword-sprite-sheet-46bc27e7)、
> [16-bit wooden sword](https://dinopixel.com/16-bit-wooden-sword-pixel-art-59656)、
> 以及一个真实仓库里的 [custom_sword.png 直链](https://raw.githubusercontent.com/jgarciaf04/poc-addon-minecraft/HEAD/resource_pack/textures/items/custom_sword.png)）。
> 但这个环境**看不了网图**：视觉工具读不了 URL（报图片解析错误），命令行下载被沙箱挡住
> （`schannel: SEC_E_NO_CREDENTIALS`）。**所以剑是我按 16x16 惯例 + 本模组配色画的，不是临摹的。**
> 你把参考图存进工作区（比如 `fulu/ref/sword.png`），我就能用视觉工具看图照着改。

### 二、阵眼："有没有符" → "装的是哪种符"

新增 [`TalismanKind`](src/main/java/com/lengyang/fulu/block/TalismanKind.java)：
`EMPTY / CHAOS / SWORD`（实现 `StringRepresentable`，方块状态里写的是小写名）。

| 以前 | 现在 |
|---|---|
| `BooleanProperty has_talisman`（两套模型） | `EnumProperty talisman`（**三套**模型：empty / chaos / sword） |
| NBT `HasTalisman:1b` | NBT `Talisman:"chaos"`（种类名，以后加符不用改存档格式） |
| 装符：只有乱神符能装 | 手里是哪种就装哪种；**已经有符时不让装**并提示 |
| 取符：还乱神符 | 还**原来那种**（乱神符还乱神符、飞剑符还飞剑符） |
| 每刻逻辑：有符就打架 | **只认乱神符** —— 飞剑符现在只是能装上去，还没有效果 |

> ⚠️ **带迁移**：老存档里 `HasTalisman:1b` 会被读成"装着乱神符"，
> 升级上来符不会凭空消失。

### 验证：一个对照实验（只换符，别的全一样）

```text
  with the chaos talisman they fight             OK  damage or deaths seen
  with the flying sword talisman they do NOT     OK  no damage, no deaths
  and they were still there to be counted        OK  SWORD-ALIVE seen
 verdict    : PASS
```

同一个 7×7 围栏、同样两只僵尸、同样 14 秒 ✓。
最后一条很重要："飞剑符下面没打架"**必须不是"怪根本不在"造成的** —— 所以还检查了它们仍然活着、能被数到 ✓。

方块状态三值也测了（新建的阵眼默认 `talisman=empty` ✓、能切成 `chaos` ✓、切状态不丢方块实体 ✓），
新 NBT 也真的落到了区块文件里（直接读 `.mca`，1 hit in chunk 0,0 ✓）。

打包自检 **403 项全过**。

> 顺手交代：为了读字符串 NBT，把 `check_region.py` 扩展成也能读字符串值 ——
> 而它自己也有个 bug：期望值写死 `int(...)`，传 `"chaos"` 直接崩。
> 好在 harness 把它报成 **"reader broken"**，而不是误报"模组丢数据" ✓。

### 自己试

```mcfunction
/give @s fulu:flying_sword_talisman     # 或 符纸 + 朱砂 + 铁锭 + 羽毛
```
1. 拿飞剑符右键阵眼 → 装上，阵眼外观变成"剑"那套，动作栏提示"阵眼已装上飞剑符"。
2. 再拿乱神符右键 → **不让装**，提示"阵眼里已经有符了…"。
3. 空手右键 → 取回**飞剑符**（不是乱神符），阵眼变回普通外观。
4. 装上飞剑符时，阵内的怪**不会**互相打（它还没有效果）；装上乱神符才会。

---

## 第十部分：飞剑阵的效果

`tick()` 现在第一件事是**看符号分派**：

```java
switch (eye.talismanKind()) {
    case CHAOS -> { 原来的内讧逻辑（挑目标 + 标记 + 粒子 + 清理） }
    case SWORD -> { 每 1 秒射一剑；让在飞的那一剑往前走 }
    case EMPTY -> { 什么都不做，只扫描 + 每 5 秒报数量 }
}
```

### 飞剑逻辑

| 你的要求 | 实现 |
|---|---|
| 扫描周围 10 格内的怪物 | 复用 `findMonsters`（阵法那个盒子：水平 ±10、下方 10、上方无限） |
| 每隔 1 秒 | `level.getGameTime() % 20 == 0`（用游戏时间取模，区块卸载再加载也不会错拍） |
| 选一只最近的 | 按到**阵眼**的距离取最小 |
| 在阵眼和怪物之间发射一道飞剑粒子 | 不是瞬间命中：**真的飞过去** —— 记下目标 id，用 10 刻插值飞行，路径上撒本模组的灵气粒子（目标在走，剑也跟着拐） |
| 造成 5 点伤害 | 命中时 5 点，用 `magic` 伤害源 —— **穿盔甲**，所以"5 点"一定就是 5 点 |
| 播放音效 | 发射 = `PLAYER_ATTACK_SWEEP`（剑挥砍），命中 = `ANVIL_LAND`（铁砧） |

> ⚠️ **一处行为改动**：内讧**标记**（和它的粒子）以前是"不管有没有符都挂"，
> 现在**只在装乱神符时挂** —— 因为你说"空的时候只扫描和显示数量"，
> 而且那个标记本来就是乱神阵的东西。换符/取符时会把旧的标记清掉。

### 验证：只放一只僵尸

内讧至少要两只怪，所以"一只僵尸掉血"只可能是飞剑打的：

```text
  no datapack function failed to load            OK
  the flying sword hits it                       OK  血量掉了（采到 5 次伤害）
  each hit takes exactly 5                       OK  每次采样都是 20/15/10/5 或更低
  it does not put the infighting mark on them    OK
 verdict    : PASS

  调试日志（临时开 DEBUG_SCAN_LOG）：
    [sword] launched at entity 1 (dist 4.03)
    [sword] hit entity 1 for 5.0 (health now 15.0)
    [sword] launched at entity 1 (dist 4.03)
    [sword] hit entity 1 for 5.0 (health now 10.0)
```

两种符的对比也跟着更新了（乱神符：打架 + 挂标记 ✓；飞剑符：被剑打、**不挂标记** ✓）。

> 插曲：第一次跑对比时两只僵尸被**打死了** ✗ —— 每秒 5 点，14 秒够杀两只 ✗。
> 这不是 bug，恰恰说明飞剑很有效 ✓；改成每 0.5 秒采样一次就对了 ✓。

### 又一个我自己踩的坑（同一个，第二次）

第一次跑飞剑测试时报"僵尸没掉血" ✗，看起来像飞剑没生效 ✗。
实际上飞剑**工作得很好** ✓（调试日志里 4 次命中、每发 5.0 都在 ✓）——
坏的是我那个采样函数：NBT 键 `forge:id` **没加引号**，整个函数**加载失败** ✗，
后面所有行都没跑 ✓。加上"没有函数加载失败"这条检查之后一眼就看出来了 ✓。
（第一次是 `fulu codex find` 多写了一个参数 —— 现在这是每个新测试的**第一条**检查。）

### 两处要说的

1. 原版音效常量打包后只剩 SRG 名（`f_12317_` / `f_11668_`），
   自检里用 javap **按方法分开**确认过是哪一个，不是猜的。
2. **"周围 10 格"用的是阵法那个盒子**（上方无限）。如果你要的是**球形** 10 格
   （上方也只算 10），说一声，改一行。

### 自己试

```mcfunction
/give @s fulu:flying_sword_talisman
```
装到阵眼上，往附近放几只僵尸：每秒会有一道灵气飞剑从阵眼射向**最近**的那只，
命中扣 5 点血，发射有剑挥砍声、命中是铁砧声 ✓。

---

## 第十一部分：飞剑的粒子 + 拖尾剑光（新粒子类型）

### 第一部分：拖尾剑光

| 项目 | 内容 |
|---|---|
| 贴图 | [`textures/particle/sword_glow.png`](src/main/resources/assets/fulu/textures/particle/sword_glow.png) —— 一条细长光带，**左边亮（青白）、右边透明（淡蓝渐变）** |
| 生成脚本 | [`tools/gen_sword_glow.py`](tools/gen_sword_glow.py)（算出来的，改颜色/长度只改两个常量） |
| 注册 | `ModParticles.SWORD_GLOW` = `fulu:sword_glow`（`SimpleParticleType`） |
| 粒子类 | [`client/particle/SwordGlowParticle.java`](src/main/java/com/lengyang/fulu/client/particle/SwordGlowParticle.java) —— 继承原版 `TextureSheetParticle` |
| 提供者 | `SwordGlowParticle.Provider`（内部类），在 [`ModClientSetup`](src/main/java/com/lengyang/fulu/client/ModClientSetup.java) 里 `registerSpriteSet` 注册 |
| 粒子 JSON | [`assets/fulu/particles/sword_glow.json`](src/main/resources/assets/fulu/particles/sword_glow.json) → `["fulu:sword_glow"]` |

和灵气粒子的区别（同一个基类，数值不同）：**不下落、不被方块挡、生命很短（0.15~0.35 秒）、只淡出不淡入** ——
拖尾靠"路径上一串粒子"叠出来，而不是靠单颗粒子活得久。

用 **16×16** 而不是灵气那个 8×8：8×8 画不出"细长"的光带（自检里的尺寸检查也跟着放宽了）。

### 四、飞剑的粒子

| 你的要求 | 实现 |
|---|---|
| 从阵眼**顶部**发射 | 起点改成 `pos.getY() + 1.0`（方块顶面），不是方块中心 |
| 路径上每格放一个粒子 | 每刻把"上一刻位置 → 当前位置"这一小段按 **1.0 格**切开，每个切点放一颗剑光 —— 整条路径正好每格一颗 |
| 命中时在怪物身上再放几个 | 以怪物身体中心为球心，**8 颗**剑光随机散在一小圈里 |

### 验证

```text
=== 飞剑测试（多了第 6 条检查）===
  no datapack function failed to load            OK
  the flying sword hits it                       OK  血量掉了
  each hit takes exactly 5                       OK
  it does not put the infighting mark on them    OK
  the debug log shows the hits landing           OK  4 次命中
  on a hit it bursts particles on the monster    OK  4 burst 行；第一行：
      [sword] hit burst: 8 particles on entity 1
 verdict    : PASS

=== 灵气粒子回归（乱神符那一路不能被改坏）===
  particles are emitted every tick               OK  4 秒 80 刻
  only the monsters inside the formation count   OK  marked=3，从未 4
  1-2 particles per monster, no more             OK
 verdict    : PASS
```

打包自检 **428 项全过**（剑光的贴图 / JSON / 注册 / 粒子类 / provider 都钉住了）。

### 验不到的（要你的客户端）

- 粒子长什么样、光带够不够长、颜色对不对。日志记的是"**该发几颗**"——
  无头环境没有玩家，粒子按设计不发。
- 一个已知的简化：剑光是**永远面向玩家**的四边形（原版粒子的默认行为），
  不是"顺着飞行方向"的光带。要真正顺着飞得自己写渲染（BER 或自定义 `ParticleRenderType`），
  说一声我可以做。

### ⚠️ 我在这一步犯的一个错

我用 `Set-Content -Encoding ASCII` 重写了**自己的测试脚本** `tools/verify_tick.ps1`，
把它里面的中文（**401 个字符**，主要是历史注释和个别文案）全变成了问号 ✗。

- 影响范围：**只有这个测试脚本**。模组代码、README、成品 jar 都没事 ✓
- 脚本的**运行输出**本来就是我写的英文，所以测试照常能跑、结论不受影响 ✓
- 这**正是我这一路一直在记的那个坑**，结果我自己踩了一次 ✗
- 要恢复那些注释的话，我可以按之前的记录重写（工作量大但纯机械）

---

## 第十二部分：命中爆发粒子 + 两个粒子的行为

### 第二部分：命中爆发（小星星）

| 项目 | 内容 |
|---|---|
| 贴图 | [`textures/particle/sword_burst.png`](src/main/resources/assets/fulu/textures/particle/sword_burst.png)（**8×8**：中间亮、边缘透明，白 → 金，一颗小星星） |
| 生成脚本 | [`tools/gen_sword_burst.py`](tools/gen_sword_burst.py) |
| 注册 | `ModParticles.SWORD_BURST` = `fulu:sword_burst` |
| 粒子类 | [`client/particle/SwordBurstParticle.java`](src/main/java/com/lengyang/fulu/client/particle/SwordBurstParticle.java)，继承原版 `TextureSheetParticle` |
| 提供者 | `SwordBurstParticle.Provider`，在 [`ModClientSetup`](src/main/java/com/lengyang/fulu/client/ModClientSetup.java) 里注册 |
| 粒子 JSON | [`assets/fulu/particles/sword_burst.json`](src/main/resources/assets/fulu/particles/sword_burst.json) → `["fulu:sword_burst"]` |

用 **8×8**（而剑光是 16×16）：它得在细长光带旁边读起来是"**小**火花"，而且一次要出十几颗。

### 第三部分：拖尾剑光的行为

| 你的要求 | 实现 |
|---|---|
| 从阵眼顶部生成、飞向目标 | 起点 `pos.y + 1.0`，位置由服务端每刻算 |
| **根据飞行方向拉伸贴图** | 把飞行方向投影到**相机屏幕平面**（右/上轴）求出屏幕角度，写进粒子的 `roll`，四边形就跟着转 —— 见 [SwordGlowParticle.render](src/main/java/com/lengyang/fulu/client/particle/SwordGlowParticle.java) |
| 存在时间短，比如 10 刻 | `LIFE_TICKS = 10` |
| 逐渐变小、变透明 | 每刻 `quadSize *= 0.92`，alpha 按平方曲线衰减 |
| 每格 2~3 个粒子 | 间距 **0.4 格**（= 每格 2~3 颗），连起来就是一条剑光 |

> **方向是怎么传过去的**：原版粒子包只有位置和速度可用，所以**用速度传方向** ✗ ——
> 但粒子自己**不动**（速度抄一份到 `dirX/dirY/dirZ`，运动置零），
> 否则光带会一边飞一边糊掉 ✓。

### 第四部分：命中爆发的行为

| 你的要求 | 实现 |
|---|---|
| 命中时在怪物位置生成一圈 | 以怪物为中心、半径 0.45 的圆上**均匀排一圈**，带一点上下抖动 |
| 一次 8~12 个，向四周扩散 | **10 个**，每个再给一点向外的随机初速 |
| 速度先快后慢 | 在**粒子类**里做：阻力 0.78（服务端只管"在哪儿、多快出发"） |
| 存在 10~15 刻 | `MIN_LIFE = 10` / `MAX_LIFE = 15` |
| 白 → 金，和拖尾的青白对比 | 颜色**随生命从白插值到金**（`START_R…END_R`） |

### 第五部分：接进阵法逻辑

拖尾用 `SWORD_GLOW` ✓、命中用 `SWORD_BURST` ✓、伤害仍 5 点 ✓、仍每秒一次 ✓
（按符分派的那段 `switch` 没动 ✓）。

### 验证

```text
=== 飞剑 + 爆发 ===
  no datapack function failed to load            OK
  the flying sword hits it                       OK  血量掉了
  each hit takes exactly 5                       OK
  it does not put the infighting mark on them    OK
  the debug log shows the hits landing           OK  4 次命中
  on a hit it bursts particles on the monster    OK  4 burst 行；第一行：
      [sword] hit burst: 10 particles on entity 1
 verdict    : PASS

=== 灵气粒子回归 ===   PASS（4 秒 80 刻、只标记阵内 3 只）
```

打包自检 **443 项全过**（三个粒子的贴图 / JSON / 注册 / 客户端类 / provider 全部钉住）。

### 验不到的（要你的客户端）

- 画面本身。日志记的是"**该发几颗**"—— 无头环境没有玩家，粒子按设计不发。
- **方向对齐是"屏幕内的旋转"**，不是真正的 3D 对齐：正常角度看就是一条顺着飞的
  光带，但正对着飞行方向看会偏侧。真 3D 对齐要自己写渲染（BER 或自定义渲染类型）。
- 光带**没有做各向异性拉伸**：细长的形状是烤进 16×16 贴图里的；
  要让四边形本身被拉长，同样得自己写渲染。

---

## 第十三部分：飞剑实体 + 飞剑符贴图重做

### 第一部分：做飞剑实体

飞剑以前只是"服务端每刻算位置 + 撒粒子" —— 粒子在飞，但**没有东西在飞**。
现在它是一个**实体**：

| 文件 | 作用 |
|---|---|
| [`entity/FlyingSwordEntity.java`](src/main/java/com/lengyang/fulu/entity/FlyingSwordEntity.java) | 会飞、会追、会砍，打完自己消失 |
| [`registry/ModEntities.java`](src/main/java/com/lengyang/fulu/registry/ModEntities.java) | 登记实体类型 `fulu:flying_sword`（`MobCategory.MISC`） |
| [`client/model/FlyingSwordModel.java`](src/main/java/com/lengyang/fulu/client/model/FlyingSwordModel.java) | 外形：剑身 + 护手 + 柄（**照钻石剑的形制**，换成青玉/黄铜/深柄） |
| [`client/render/FlyingSwordRenderer.java`](src/main/java/com/lengyang/fulu/client/render/FlyingSwordRenderer.java) | 渲染器，把剑身对准飞行方向 |
| [`registry/ModModelLayers.java`](src/main/java/com/lengyang/fulu/registry/ModModelLayers.java) | 烘焙好的模型层名字 |
| `textures/entity/flying_sword.png` | 32×32 模型贴图 |

**三个属性**（就是你要的飞行速度 / 伤害值 / 目标怪物）：

| 属性 | 值 |
|---|---|
| 飞行速度 | `SPEED = 0.85` 格/刻（约 17 格/秒，比玩家跑得快） |
| 伤害值 | `DAMAGE = 5.0F`（由阵法传进来；用 `magic` 伤害源，保证正好 5） |
| 目标怪物 | 按 **UUID** 记着（区块卸载再加载也能找回来） |

**行为**：每刻朝目标走一步并转向它 ✓；撞到就结算伤害 + 爆发粒子 + 音效，然后**自己消失** ✓；
**目标死了或不见了，自己也消失** ✓。没目标时会自己找 12 格内最近的怪 ✓
（所以 `/summon` 出来的剑也会自己去打 —— 这条也是它能被自动化测试的原因）。

### 接进阵法逻辑

方块实体不再自己算飞行了：它每秒挑一只最近的怪，`addFreshEntity` 放出一把瞄准它的飞剑实体。
原来在方块实体里的 `advanceSword` / 拖尾 / 命中爆发**全部搬进了实体**，死代码已删除
（922 行，从 1063 行）。

### 飞剑符贴图重做（用你上传的图）

`ref/talisman_source.png` 是你上传的图，[`tools/gen_flying_sword_assets.py`](tools/gen_flying_sword_assets.py) 做了三件事：

1. **扣掉黑底**：暗色像素变透明；
2. **去掉水印**：右下角那些"亮但不偏黄"的像素是 AI 水印 —— 黑底一扣，它会**浮在半空**，所以一并去掉；
3. 裁到纸的边界 → 缩成 10×14 当**符纸**，中间画上新的青玉飞剑；
4. 阵眼的第三种外观（`formation_eye_sword.png`）用同一套图重做。

### 验证

```text
=== 飞剑实体测试（直接 summon，不经过阵法、没有玩家）===
  no datapack function failed to load            OK
  the entity can be summoned                     OK
  it flew over and hit the monster on its own    OK
  the hit takes exactly 5                        OK
  it removed itself after hitting                OK
 verdict    : PASS

=== 阵法那条路（飞剑符）===
  the flying sword hits it                       OK  血量掉了
  each hit takes exactly 5                       OK
  it does not put the infighting mark on them    OK
  the formation launches a flying sword entity   OK  世界里真的出现了 fulu:flying_sword
 verdict    : PASS
```

打包自检 **461 项全过**（含"实体类和实体登记表都不能引用客户端类" —— 专用服务端要能加载）；
出厂冒烟通过（专用服务端仍能启动）。

### 要你的客户端才能确认的

- **剑在天上长什么样**：模型比例、剑尖朝向对不对、新符纸好不好看。
  我能验证贴图打进去了、模型有三个部件，验证不了它好看。
- 剑身就是三个方块的组合（一眼能看懂、渲染也便宜）。要更讲究的造型
  （收锋、缠绳、剑穗）说一声。

### 这一步的插曲（照例说清楚）

- 删死代码用了一个小 Python 脚本按行范围删，**带断言检查要删的是什么**、删完查 BOM。
  前三次都被断言拦下来了 —— 这正是断言的作用。
- 中途我又用 `Set-Content -Encoding ASCII` 写了一个带中文的辅助脚本，
  结果中文锚点被写坏、脚本报"找不到" ✗。改成**纯 ASCII 锚点**才对。
- `ref/talisman_source.png`（3.4 MB）还留在工作区，因为生成脚本要读它 —— 不想留可以删。

---

## 第十四部分：飞剑符的发射逻辑（每秒 10 把齐射）

方块实体里的单发改成**齐射**：`launchSword` → `launchVolley`。

```text
每 20 刻（每秒）：
  扫描范围内所有怪物
  数量为 0 → 直接返回，不发射
  按到阵眼的距离排序，最多留 MAX_TARGETS = 10 只
  base  = 10 / 数量          （SWORDS_PER_VOLLEY / 数量）
  extra = 10 % 数量          → 分给**排在后面**的那几只
  全部发射，然后整个齐射只播**一次**剑挥砍音效
```

**结果就是你要的那张表：**

| 怪物数量 | 每只分到的剑 |
|---|---|
| 1 只 | 10 |
| 2 只 | 5 + 5 |
| 3 只 | **3 + 3 + 4** |
| 5 只 | 2 + 2 + 2 + 2 + 2 |
| 10 只 | 每只 1 把 |
| 超过 10 只 | 只打**最近的 10 只**，每只 1 把 |

> ⚠️ **一处需要你确认的取舍**：你写的"简单算法"是"第 i 把剑打第 i % 数量 只怪" ——
> 对 3 只怪，那会得到 **4 + 3 + 3**（因为第 4 把剑（i=3）回到第一只）。
> 但你的例子写的是"前两只各 3 把，**最后一只** 4 把"。
> 我实现的是**你的例子**（余数给后面的怪），循环方式完全一样，只是余数落在末尾。
> 想要字面版（4+3+3）说一声，改一行。

### 出生点：10 把不重叠

从阵眼**顶部**（`pos.y + 1.0`）射出，水平 **±0.35** 格、竖直 ±0.15 格随机散开 ✓；
瞄准时再加一点抖动（0.06），不然 10 把会沿着**同一条直线**飞 ✓。

### 验证

```text
=== 齐射测试 ===
  no datapack function failed to load            OK
  with no monsters it launches nothing           OK  NO-VOLLEY-EMPTY，从未 WRONG
  a volley is ten swords                         OK  世界里同时存在 10 把
  three monsters split 3/3/4 (near ones 15, far one 20) OK
  it does not put the infighting mark on them    OK
  the formation launches a flying sword entity   OK
 verdict    : PASS

=== 回归 ===
  飞剑实体测试                                    PASS（单把仍然正好 5 点）
  出厂冒烟                                        PASS
```

**3/3/4 是怎么被"证出来"的**：三只僵尸放在 2、3、4 格处 ——
最近的**两只各吃 3 把 = 15 点**，剩 5 血活着；**最远那只吃 4 把 = 20 点**，死。
测试同时看到了"有只怪剩 5 血"和"只剩 2 只"两个特征 ✓ —— 4/3/3 不会长这样 ✓。

打包自检 **468 项全过**。

### 两件你应该知道的事

1. **粒子量翻十倍**：每把剑沿路径每 0.4 格撒一颗，一次齐射就是**每秒几百颗粒子**。
   看着应该很壮观；如果嫌重，可调的是实体里的拖尾间距和 `SWORDS_PER_VOLLEY`。
2. **每秒 50 点伤害**集中在一只怪身上（10 × 5）—— 这是你的规格；
   一只 20 血的僵尸会在**第一轮齐射**就死。

---

## 第十五部分：飞行行为、拖尾、音效

### 第三部分：飞剑的飞行行为

| 要求 | 实现 |
|---|---|
| 生成后**锁定**目标怪物 | 阵法发射的剑记下目标的 UUID，**永不改追**；目标死了它就消失 |
| 每刻朝目标移动，速度约 0.5 格/刻 | `SPEED = 0.5`（原来是 0.85） |
| 剑身朝向飞行方向 | 模型竖着建，渲染器按 yaw/pitch 转完再把它放倒 |
| 命中：伤害 | 5 点（`magic` 伤害源，保证正好 5） |
| 命中：剑砍声 | `PLAYER_ATTACK_STRONG`（SRG `f_12316_`） |
| 命中：一圈金色爆发粒子 | `sword_burst` 粒子排成一圈 |
| 命中：飞剑消失 | `discard()` |
| **超过 5 秒没命中，自动消失** | `MAX_LIFE = 100` 刻 |
| **撞到方块，消失** | 每刻检查下一格是不是实心方块（见下） |

> **只有 `/summon` 出来、出生时就没目标的剑**才会自己找最近的怪 ——
> 阵法发射的剑是**锁定**的。这条留着的价值是：飞剑实体能被自动化测试直接验证。

**撞方块的判定**是"下一格 + 上一格都是实心方块"才算撞上 ——
不依赖原版碰撞（剑的碰撞箱很小、飞得直，直接看方块更清楚，
也不会因为擦着墙飞被弹开）。**实测**：在剑和僵尸之间砌一道石墙，
20 刻后剑没了（`SWORD-BLOCKED-GONE`，从未 `SWORD-WENT-THROUGH`），
墙后的僵尸**一点血都没掉**（`WALL-ZOMBIE-UNHURT`）✓。

### 第四部分：保留拖尾

每刻在**剑的位置**撒 **1~2 颗**拖尾剑光粒子（`TRAIL_PER_TICK = 2`，
取你说的上限 —— 看着更连贯）✓。拖尾用 `sword_glow` ✓、命中用 `sword_burst` ✓。

> 这一步顺带把上一版"路径上每 0.4 格撒一颗"换掉了 ——
> 现在一次齐射的粒子量比上一版**少很多** ✓（正好补上上一版我提醒过的粒子量问题）。

### 第五部分：音效

| 时机 | 音效 | 次数 |
|---|---|---|
| 发射 | `ARROW_SHOOT`（"咻"） | **一次齐射只播一次**（在方块实体里播） |
| 命中 | `PLAYER_ATTACK_STRONG`（剑砍声） | 每次命中 |

> ⚠️ "不要叠加太吵"我按**发射声**处理了 ✓；10 把剑同时命中时仍可能响好几下命中声 ✗。
> 要连命中声也节流（比如每个目标每轮只响一次）说一声。

### 验证

```text
=== 飞剑实体（含撞墙）===
  no datapack function failed to load            OK
  the entity can be summoned                     OK
  it flew over and hit the monster on its own    OK
  the hit takes exactly 5                        OK
  it removed itself after hitting                OK
  a block in the way stops it                    OK  剑在墙上没了，墙后的僵尸毫发无伤
 verdict    : PASS

=== 齐射回归 ===   PASS（无怪不发 / 一次 10 把 / 3 只怪 3+3+4）
```

打包自检 **475 项全过**，出厂冒烟通过。

### 一件我验不了的

**5 秒上限**是个安全网：搜索范围 12 格、速度 0.5 格/刻 → 24 刻就能飞到，
而且它追的东西跑不过它 —— 所以我在无头环境里**造不出**触发它的场景 ✗。
它实现了、也被自检断言钉住 ✓，但我不会说"测试证明了它" ✗。

---

## 第十六部分：拿掉拖尾剑光 + 每把剑各打 5 点

### 一、拖尾剑光（sword_glow）整个移除

按你的要求，**拖尾那套全部删掉**：

| 删掉的东西 | 说明 |
|---|---|
| `ModParticles.SWORD_GLOW` | 粒子类型登记 |
| `client/particle/SwordGlowParticle.java` | 粒子类 |
| `ModClientSetup` 里的 provider 注册 | 客户端那一半 |
| `assets/fulu/particles/sword_glow.json` | 粒子定义 |
| `assets/fulu/textures/particle/sword_glow.png` | 贴图 |
| 飞剑实体里的 `emitTrail` | 每刻撒拖尾那次调用 |

**保留**的是**命中爆发**（`sword_burst`，金色小星星）和阵眼自己的灵气粒子 ——
你说的是"剑光"（拖尾那条光带），这两个不是它。要连命中爆发也去掉说一声。

自检里加了**"确实不在包里"**的断言（粒子类 / JSON / 贴图 / 登记全部查空），
免得哪天又被加回来 ✓。生成脚本 `tools/gen_sword_glow.py` 留在原处但标注了
**不要运行**（一跑就会把贴图写回资源目录）。

### 二、每把剑各打 5 点（这是个真 bug）

你说得对，我原来的实现有 bug ✗：代码是"每把剑打 5 点"，但**原版生物受伤后有 10 刻无敌帧**
（`invulnerableTime`）—— 一轮齐射的十把剑几乎同刻到达，于是**只有第一把的 5 点生效**，
其余全被吃掉 ✗。玩家看到的就是"十把剑只掉了 5 点血" ✗。

修复：命中前把**目标的无敌帧清零**（只清这个目标、只在这一刻）：

```java
if (target instanceof LivingEntity living) {
    living.invulnerableTime = 0;   // 别让无敌帧吃掉这一把的伤害
}
target.hurt(server.damageSources().magic(), this.damage);
```

飞剑命中后本来就消失，所以不存在"一把剑反复刷伤害"的问题 ✓。

### 怎么证明的

不是看代码，是**实测**：召一只 **100 血**僵尸，挨一轮齐射（10 把 × 5 点）——

```text
  each sword deals its own 5 (ten swords = 50 off a 100 hp monster) OK
      the 100 hp monster went to exactly 50 after one volley
```

100 → **正好 50** ✓✓。修复前这个检查会看到 **95** ✗（只有第一把生效）——
所以测试里专门留了一个 `BIG-ONE-HIT-ONLY` 标记来抓这个 bug 复发 ✓。

完整齐射测试（7 项全过）：

```text
  no datapack function failed to load            OK
  with no monsters it launches nothing           OK
  a volley is ten swords                         OK
  three monsters split 3/3/4                     OK
  it does not put the infighting mark on them    OK
  each sword deals its own 5                     OK  100 血怪一轮后正好 50
  the formation launches a flying sword entity   OK
 verdict    : PASS
```

打包自检 **465 项全过**，飞剑实体回归 PASS，出厂冒烟 PASS。

---

## 第十七部分：换成玄剑模型（bbmodel）+ 每把剑独立 + 符文环绕

### 一、把你的 bbmodel 变成 Java 模型

`ref/mohei_xuanjian.bbmodel` 是 **Blockbench / 基岩格式**：25 个方块
（柄 / 护手 / 三段剑身 / 六段剑尖）、一张 256×256 图集、而且用的是**逐面 UV**
（`box_uv: false`）。

Java 实体模型**只支持盒式展开**，所以 [`tools/bbmodel_to_java.py`](tools/bbmodel_to_java.py)
负责跨过两处格式差异：

1. **逐面 UV → 盒式展开**：每个方块的六个面从图集里裁出来，缩放后放进盒式展开的六个槽位
   （图集是模型分辨率的 **6 倍**，所以是缩小 6 倍放进去的）；
2. **y 轴反向**：基岩模型站在 y=0、y 向上；Java 模型 y 向下 —— 所有 y 取负。

产出：[`FlyingSwordModel.java`](src/main/java/com/lengyang/fulu/client/model/FlyingSwordModel.java)
（脚本生成，25 个方块）+ `textures/entity/flying_sword.png`（128×128）。
**25 × 6 = 150 个面全部映射成功**，一个都不缺。

> 盒式展开的槽位顺序（DOWN、UP、WEST、NORTH、EAST、SOUTH）**不是凭记忆写的** ——
> 我把原版 `ModelPart$Cube` 的字节码反汇编出来，六个 `Polygon` 的构造顺序正是这个，
> 和文档里的布局一致。

### 二、每把飞剑都是独立的（这处是真 bug）

`ModelPart` 是**烘焙一次、所有实体共用**的，所以往它身上写"这一把剑的状态"一定会串：
旧模型在 `setupAnim` 里写了 `this.root.yRot = spin` ✗ —— 一轮齐射十把剑会**共用同一个自转** ✗。

现在：`setupAnim` 是**空的** ✓，朝向/缩放全部施加在渲染器的 `PoseStack` 上 ✓
（`PoseStack` 天然是每实体一份 ✓）。自检里加了断言：**模型类里不许出现
`.yRot` / `.xRot` / `.zRot`**，免得哪天又写回去 ✓。

模型长 48 单位 = **3 格**，渲染时缩到一半（1.5 格），并沿剑轴挪 1.5 格 ——
让**剑的中段**落在实体位置上，而不是整把剑悬在实体前面 3 格。
两个数各是一行：渲染器的 `MODEL_SCALE`、实体的 `BLADE_LENGTH`。

### 三、符文环绕

新粒子 `rune_mote`（贴图是 [`tools/gen_rune_texture.py`](tools/gen_rune_texture.py)
画的符形），几何在实体里，纯三角函数 —— 用**剑自己的朝向当轴**，所以螺旋永远贴着剑身：

```text
axis    = 实体的朝向向量（剑锋指的方向）
side/up = 由 axis 叉出来的两个垂直方向
for i in 0..7:
    t     = (i/8 + phase) % 1          // 0 = 剑尾，1 = 剑锋
    along = (t - 0.5) * BLADE_LENGTH   // 以实体位置为中心
    angle = t * 1.5 圈 + phase          // 螺旋，外加整体旋转
    pos   = 中心 + axis*along + side*cos(angle)*0.42 + up*sin(angle)*0.42
```

`phase` 跟着游戏时间走（**40 刻转一圈**）—— 所以符文**一边转圈、一边从剑尾流向剑锋** ✓
正是你要的"从剑尾到剑锋的环绕，一直转动" ✓。

### 验证

```text
打包自检 489 项全过，其中：
  [ok] 模型里有 bbmodel 的方块 seg_low_eL / seg_tip_sp / tip_6 / guard_bar / handle / pommel
  [ok] 模型类里不改 .yRot（姿态只能由渲染器用 PoseStack 施加）
  [ok] 渲染器把姿态施加在 PoseStack 上（每把剑各自独立）
  [ok] 有 emitRunes（符文环绕）/ RUNE_COUNT / RUNE_SPIRALS / RUNE_RADIUS / RUNE_PERIOD_TICKS
  [ok] 用实体朝向当剑轴（算符文位置）
  [ok] rune_mote 贴图 + 粒子定义 + 客户端类都在

飞剑实体测试 PASS ；齐射测试 PASS ；出厂冒烟 PASS
```

### 要你的客户端才能确认的

- **好不好看**：转换是几何层面的、数据层面验过了，但我看不到渲染 ✗。
  唯一可能还偏的是**某个面被转了 180°**（图集面朝向的约定）—— 真看到的话，
  在 [`tools/bbmodel_to_java.py`](tools/bbmodel_to_java.py) 里改一行翻转就行。
- **符文开销**：每把剑每刻 8 颗，一轮齐射 10 把就是 80 颗/刻 ✗。
  嫌重就调 `RUNE_COUNT` / `RUNE_PERIOD_TICKS`。

---

## 第十八部分：骨骼与四段动画（idle / fly / launch / hit）

### 一、分骨骼

按你的规范（**剑身是父骨骼**，转它整把剑跟着转）：

```text
blade  剑身（21 块：三段剑身 + 六段剑尖）—— 父骨骼
 ├─ guard  护手（2 块）
 └─ hilt   剑柄（2 块：柄 + 末端）
```

> ⚠️ bbmodel 里 `hilt` 和 `blade` 是 **root 下的兄弟**，转剑身护手不会跟着动 ✗ ——
> 所以分组是在 [`tools/bbmodel_to_java.py`](tools/bbmodel_to_java.py) 里**按你的规范重做**的 ✓。
> 几何生成在 [`FlyingSwordShape.java`](src/main/java/com/lengyang/fulu/client/model/FlyingSwordShape.java)，
> 动画手写在 [`FlyingSwordModel.java`](src/main/java/com/lengyang/fulu/client/model/FlyingSwordModel.java) —— 重跑转换脚本不会冲掉动画 ✓。

### 二、基准姿势

模型本身是**竖着**建的（剑尖在 -Y，长轴 = 模型的 Y）✓，渲染器把它放平成"水平朝前" ✓。
所以"绕长轴旋转"就是绕模型 Y 转 ✓，不用额外记账 ✓，而且和朝向互不干扰 ✓。

### 三~六、四段动画

| 动画 | 时长 | 内容 |
|---|---|---|
| **idle** | 2 秒循环 | 剑身绕长轴 0→360°；整把剑上下浮动 **±0.05 格**（0.5 秒在上、1.5 秒在下，用 sin）；剑尖摆 **1.5°**（频率是自转的两倍，像"甩"而不是"翻"） |
| **fly** | 1 秒循环 | 剑身 0→360°（比 idle 快一倍，像在钻）；**不浮动**（飞行要利落）；护手/剑柄**反向转 15%** 增加层次 |
| **launch** | 0.5 秒单次 | 绕长轴 0→720°（0.25 秒）→1080°（0.5 秒）；整把剑 **0.5→1.2→1.0 倍** |
| **hit** | 0.3 秒单次 | 沿剑轴前后震两下，逐刻表 `{0, +0.10, -0.07, +0.05, -0.03, 0}` 格（+=朝剑锋）；剑身 **1.3 倍**再缩回 |

**伤害是立刻结算的** ✓，然后剑花 6 刻把 hit 演完再消失 ✓（有个 `dying` 标记：这 6 刻里它不动、也不会再打一次 ✓）。

### 七、切换

```text
出生 -> launch（实体的同步状态一开始就是它）
      10 刻后服务端自动切 fly，之后一直循环
命中 -> hit，动画播完 -> 消失
没目标可追 -> idle（现在的行为是这种剑直接消失，所以 idle 是留给"停在某处"的剑的）
```

状态通过网络同步（`DATA_ANIM`）✓；hit 的进度也同步（`DATA_HIT_AGE`，只有那 6 刻 ✓）✓。
launch/idle/fly 的相位直接用实体自己的 `tickCount` —— 两边都是出生时从 0 开始数 ✓。

### 八、朝向

在**渲染器**里做 ✓：用"这一小段的位移方向"（`pos - xo`）算 yaw/pitch ✓，这一小段没动就沿用实体记着的朝向 ✓。
动画**从不碰朝向** ✓。

### 九、两个坑（都是静默失败那种）

1. **`EntityRenderer` 永远不会替你调用 `setupAnim`** ✗ —— 只有 `LivingEntityRenderer` 会 ✓。
   所以动画会编译通过、打包进去、然后**一行都不生效，还不报错** ✗，模型永远停在初始姿态 ✗。
   现在渲染器**自己调** ✓，自检里也钉住了这一条 ✓。
2. `ModelPart` 是**烘焙一次、所有剑共用**的 ✓ —— 所以 `setupAnim` 开头先 `reset()` ✓，之后写的全是**绝对值** ✓。
   用 `+=` 会让十把剑抢同一份姿态 ✗。

### 验证

```text
打包自检 513 项全过，其中：
  [ok] 有 blade / guard / hilt 骨骼
  [ok] 动画写在 setupAnim 里（每实体每帧一次）
  [ok] 动画里有 animIdle / animFly / animLaunch / animHit
  [ok] 动画会写骨骼的旋转（自转 / 摆动）
  [ok] 动画会写骨骼的缩放（launch / hit 的伸缩）
  [ok] 渲染器**自己调用**了 setupAnim（否则动画不会生效）
  [ok] 动画状态同步给客户端（否则客户端不知道该播哪段）

飞剑实体测试 PASS（launch -> fly -> hit -> 消失 这条链仍然通）
齐射测试 PASS ；出厂冒烟 PASS
```

### 要你的客户端才能确认的

**顺不顺**。每个数字都是你要的那个、接线也验过了 ✓，但"像一把利落的剑、而不是在跳舞"
只有真渲染出来才能判断 ✗。哪个环节觉得太快，`FLY_TICKS` / `LAUNCH_TICKS` 和几个旋转量
都是 `FlyingSwordModel` 里的单个常量 ✓。

---

## 第十九部分：修 "阵眼有些怪物无法识别"

### 原因

扫描用的是 **`Monster` 类**：

```java
level.getEntitiesOfClass(Monster.class, area)      // ← 漏怪
```

但原版对"敌对生物"的正式定义是 **`Enemy` 接口**，而这几种怪**只实现 `Enemy`、不继承 `Monster`**：

> **史莱姆、岩浆怪、恶魂、幻翼、疣猪兽**

所以阵眼根本看不见它们 —— 它们压根不在那个列表里。

> 我自己在那个方法上写的注释**早就提到了这个可能** ✗，但只点了"史莱姆和恶魂"两句，
> 而且归类成"如果你也想算上它们的话" ✗ —— 所以一直没修 ✗。注释本身也不全：
> 疣猪兽和幻翼是一样的情况 ✗。这是"注释里写下的隐患没人当回事"的典型 ✗。

### 修法

1. 改用 **`Enemy` 接口**筛（通过 `Mob` 拿，因为实现它的是 `Mob`）；
2. 但**已驯服**的动物绝不打 —— 驯服的狼也是 `Enemy` ✗，把玩家的狗打了比原来的 bug 更糟 ✗；
3. 规则**只写一处**：[`FormationTarget.isTarget()`](src/main/java/com/lengyang/fulu/combat/FormationTarget.java) ✓。
   阵眼扫描、判断"在不在阵法内"、飞剑自己找目标，**三处都调它** ✓
   —— 这样就不会出现以后"阵眼看得见、飞剑却不打"这种对不上的毛病 ✓。

方块实体里 `Monster` → `Mob`：行为上没有任何一处真的需要 `Monster` ✓
（`setTarget` 和 `hurt` 分别在 `Mob` / `Entity` 上 ✓）。

### 回归测试（`-EnemyScanTest`）

四只怪摆在一起，看谁会被打上乱神符的标记：

```text
  no datapack function failed to load                  OK
  slime (Enemy but not Monster) is recognised          OK  SLIME-MARKED，从未 MISSED
  hoglin (Enemy but not Monster) is recognised         OK  HOGLIN-MARKED，从未 MISSED
  a tamed wolf is left alone                           OK  WOLF-UNMARKED，从未 MARKED
  a villager is left alone                             OK  VILLAGER-UNMARKED，从未 MARKED
 verdict    : PASS
```

剩下的回归：飞剑实体 PASS ✓ / 齐射 PASS ✓ / 出厂冒烟 PASS ✓ / 打包自检 **519 项全过** ✓。

### 如果你遇到的那只怪不在这五种里

那原因就不是类，**下一个嫌疑是范围** ✓。阵法那个盒子是刻意的：
水平 10 格、**下方 10 格**、上方不限 ✓，而且竖直判定用的是**怪物的脚底**
（僵尸站在阵眼下方 11 格，头只低 9.05 格，照样不算 ✓）。

**告诉我具体是哪只怪、大概在哪个位置**，我去查那条线 ——
我修的是**能证明的那个原因**，不是我猜的那个 ✓。

---

## 第二十部分：第三个阵法 —— 反重力阵

### 一、反重力符

| 项目 | 内容 |
|---|---|
| 物品 | `fulu:anti_gravity_talisman`（反重力符） |
| 配方 | 符纸 + 朱砂 + 羽毛 + **潜影壳**（无序合成） |
| 贴图 | [`tools/gen_antigravity_assets.py`](tools/gen_antigravity_assets.py) 在**同一张符纸**上画一个淡紫向上箭头 |

用同一张符纸（你上传的那张）是为了三个阵法看起来是一套 ✓。

### 二、阵眼的第四种状态

`TalismanKind` 从三种变成**四种**：`EMPTY` / `CHAOS` / `SWORD` / **`ANTIGRAVITY`** ✓。

**装符/取符的代码一行没改** ✓ —— 它本来就是"读玩家手里拿的是哪种符"✓、有符就拒绝 ✓、
空手就还回去 ✓。所以"手拿反重力符右键 → 装上"只要**枚举多一个常量 + 多一个物品**就自动生效 ✓。

方块状态文件现在是**四种模型** ✓，反重力的阵眼外观是把同一个箭头图案贴进阵眼贴图里 ✓
（和飞剑符那套做法完全一样 ✓）。

### 三、反重力阵的效果

每刻仍然先判断装的是哪种符 ✓，`ANTIGRAVITY` 走新的那一支：扫描同一个阵法盒子 ✓，
给范围内每个敌人挂上**真正的状态效果** `fulu:antigravity`（3 秒，每刻刷新 ✓）。

**效果自己每刻在干活**（这就是它为什么是"真效果"而不是一个标记 ✓）：

| 每刻做的事 | 说明 |
|---|---|
| 往上推 | 到"天花板"之前一直抬，到顶就悬着 |
| 停寻路 + 清攻击目标 | `navigation.stop()` + `setTarget(null)` = 不能寻路、不能近战 |
| 脚底出粒子 | 用新的 `anti_gravity` 粒子 |
| 偶尔一声飘浮声 | 轻微的音效 |

"天花板"（阵眼 y + 5）是**阵眼写进怪物持久数据**的 ✓ —— 效果本身不需要知道阵眼在哪 ✓。

**飞行怪物**不是靠列类名排除的 ✓，而是问引擎："没有重力 / 是 FlyingMob、FlyingAnimal、蝙蝠" ✓
—— 模组加进来的飞行怪也自动排除 ✓。**BOSS**（末影龙、凋灵）也排除了 ✓
（你说免疫"后面再加" ✓，一行就能加上，顺手先排除免得龙被抬走 ✓）。

### 四、高度与限制

| 要求 | 实现 |
|---|---|
| 每刻上升一点点（0.02 格/刻） | ⚠️ 见下面的说明 |
| 浮空上限 5 格，到顶不再上升 | 阵眼 y + 5，到顶就停在那个高度 |
| 不能攻击玩家、不能寻路 | 清目标 + 停寻路 |
| 离开范围 3 秒内消失然后掉下来 | 效果只在范围内刷新，离开后自然到期 |
| 掉落伤害按正常计算 | **从不清 `fallDistance`** —— 摔死真的算阵法的功劳 |

> ⚠️ **一处不是字面照做的**：`0.02` **做不到** ✗ —— 原版生物每刻被重力拉掉约 **0.08**，
> 把速度设成 0.02 的结果是**它还在往下沉** ✗（我一开始就是这么写的 ✓，测试里僵尸
> **一动不动** ✗）。现在常量是 **0.10**，抵掉重力后**净上升约 0.02 格/刻** ✓ —— 你要的慢速 ✓。

> 还有一个真错误值得记：第一版我用 `setDeltaMovement` 推 ✗ —— 原版每刻在 `travel()` 里
> 会把这个速度**覆盖掉** ✗，所以僵尸 120 次采样全在地面 ✗。改成 `move()` 直接挪位置才对 ✓
> （和飞剑实体用的是同一招 ✓）。**是测试抓出来的** ✓。

### 五、六：粒子与音效

- **粒子**：新粒子 `anti_gravity`（淡紫 ✓，自己往上飘 ✓）—— 只在怪物**真的离开地面**时才出 ✓。
- **音效**：阵法在抬东西时，每 5 秒一声**潜影贝打开**（低沉的启动声 ✓）；
  另外每个被抬的怪每 3 秒一声轻微的飘浮声。
  > 怪多的时候"每个怪一声"会有点密 ✗ —— 嫌吵就说一声，改成整个阵法一声 ✓。

### 七、图鉴

加了**反重力阵**一条 ✓（名称 / 范围 / 效果 / 材料 / 激活 / 关闭，中英各一份 ✓）。

> 顺手发现：**飞剑阵从来就没进过图鉴** ✗ —— 那是我前面几步留下的缺口 ✗，这次一起补上了 ✓。
> 另外装符时的解锁从"只有乱神符"改成"装哪种符就解锁哪一条" ✓。

### 验证

```text
  no datapack function failed to load                  OK
  the monster is lifted off the ground                 OK  从 -60 升到 -58 以上
  it stops at the 5 block ceiling                      OK  到 -55，从未超过 -53
  it carries the anti-gravity effect                   OK  fulu:antigravity 挂在它身上
 verdict    : PASS
```

打包自检 **554 项全过**（含第四种符状态、第四个方块状态、效果登记与 18×18 图标、
配方的四种材料、图鉴三条齐全）✓；怪物识别 PASS ✓；齐射 PASS ✓；出厂冒烟 PASS ✓。

---

## 第二十一部分：反重力改成"倒立"（效果只当标记）

> 这一版**改掉了上一版的行为** ✓：上一版是"抬起来"（每刻上升 0.02、5 格天花板、悬停），
> 现在按新要求改成**倒立** ✓。抬高那一套（天花板 / 上升 / 悬停 / 那几个常量）
> **是删掉的** ✓，不是关掉的 ✗。

### 一、效果是一个纯标记

`AntiGravityEffect` 现在**不碰位置、不碰速度、不碰攻击目标** ✓，也不掉血、不改任何属性 ✓ ——
它唯一还做的事是**粒子** ✓。持续 3 秒、每刻刷新 ✓，所以离开阵法最多 3 秒就没了 ✓，
而且因为从来没改过什么，也就**不需要"恢复"** ✓（速度、寻路、跳跃都是原版每刻自己算的 ✓）。

图标按你说的改成**倒过来的箭头**（向下 ✓），和"倒立"对上 ✓。

### 二、让怪物倒过来（只影响渲染）

新事件 [`AntiGravityRenderHandler`](fulu/src/main/java/com/lengyang/fulu/combat/AntiGravityRenderHandler.java)（**纯客户端** ✓，监听 `RenderLivingEvent.Pre` ✓）：

```java
pose.translate(0, -entity.getBbHeight(), 0);
pose.mulPose(Axis.ZP.rotationDegrees(180));   // 上下翻转
```

**翻转的数学**：模型的**原点在脚底**、y 向上为负 ✓ —— 直接绕 Z 转 180° 会把身体从
`[-h, 0]` 映射到 `[0, h]`（跑到地面以下 ✗）✓，所以先向上平移 h 再转 ✓，
身体正好回到 `[-h, 0]`：**原地倒过来** ✓。

**碰撞箱、位置、AI 一律不动** ✓ —— 只有客户端画出来的姿态变了 ✓。
这也是为什么必须用事件而不是改模型：原版有几十种怪物模型 ✗，逐个改不可能 ✗；
而且服务端**根本不需要知道**"倒立"这件事 ✓（它只知道那个标记 ✓）。

### 三、不能走，但能攻击

新事件 [`AntiGravityTickHandler`](fulu/src/main/java/com/lengyang/fulu/combat/AntiGravityTickHandler.java)
（监听 `LivingEvent.LivingTickEvent` ✓）：水平速度清零 ✓、停寻路 ✓、关跳跃 ✓。

> **重点是它"不做什么"**：它**不调用 `setTarget(null)`** ✓。
> 上一版清了目标 ✗ —— 那会把"不能走但能攻击"变成"**完全不能攻击**" ✗
> （怪物彻底忘了玩家 ✓，玩家站在它面前也不会挨打 ✗）。
> 保留目标之后，怪物的攻击目标照常每刻检查距离 ✓：**够得着就照打** ✓，
> 够不着也走不过来（寻路被停了 ✓），远程怪照样放箭 ✓ —— 正好是你要的
> "远处不能走过来，但能远程攻击" ✓。

飞行怪 / BOSS 三处共用同一个判定 [`FormationTarget.canInvert`](fulu/src/main/java/com/lengyang/fulu/combat/FormationTarget.java) ✓
（阵眼挂标记、渲染翻转、每刻按住 ✓ —— 一个标准 ✓，不会出现"翻了但不按住"✓）。

### 四、五：粒子与音效

- **粒子**不变 ✓：淡紫、从脚底往上飘、只在有标记时出 ✓。
- **音效**：**每个怪一声的飘浮声删掉了** ✓（那是你上一轮说的"改成整个阵法一声"✓）。
  现在只剩：
  - **整个阵法**每 5 秒一声低沉的启动声（潜影贝打开 ✓）；
  - 怪物**刚被罩住**那一下，一声轻微的"翻转"音效（翻书页 ✓）—— 不是每刻响 ✓。

### 六、图鉴

反重力阵的文案改成：**身体倒立、无法行走但能攻击、持续 3 秒、对飞行怪物无效** ✓（中英各一份 ✓）。

### 验证

```text
  no datapack function failed to load                  OK
  the monster stays on the ground (flip is render-only) OK  它的 Y 从未超过 -58
  it cannot walk away                                  OK  X 600 刻都待在 [2,4]
  the marker is refreshed every tick                   OK  在范围内从未掉过标记
  it carries the anti-gravity effect                   OK  fulu:antigravity
 verdict    : PASS
```

> "**不能走**"那条是最有说服力的 ✓：僵尸**保留 AI**、可以自由游荡 ✓，
> 但 600 刻（30 秒）里它的 X **从未离开 [2, 4]** ✓（出生点是 3 ✓）。没有按住的话它早走远了 ✓。

打包自检 **557 项全过** ✓；怪物识别 PASS ✓；齐射 PASS ✓；飞剑实体 PASS ✓；出厂冒烟 PASS ✓。

### 要你的客户端才能确认的

**倒立看起来对不对** ✓。我能证明"它没动、没升起来、带着标记" ✓；
但"翻得够不够convincing、高个子和矮个子怪物的位置对不对"只能渲染出来看 ✓。
位置用的是怪物**自己的碰撞箱高度** ✓，所以理论上两种体型都合适 ✓；
真觉得偏高偏低，改渲染事件里那一行 translate 就行 ✓。

---

## 第二十二部分：修"创造标签页没有物品"

### 查出来的两个真问题

模组**本来就有**自己的页签（"符箓" ✓，也**确实注册了** ✓），里面列了八个物品中的**七个** ✓。但：

1. **反重力符从来没加进去** ✗ —— 我上一步加了物品、忘了页签 ✗。
   所以想去创造模式拿新符的人，翻到那一页是**空的（对他来说）** ✗。
   **这最可能就是你遇到的情况** ✓。
2. **物品只在自己那个页签里** ✗ —— 原版那几页（材料 / 工具 / 建筑方块）里
   **一个都没有** ✗。而玩家翻创造模式时先看的几乎总是那几页 ✓，
   不会想到去看最后一页多出来的那个陌生页签 ✓ —— 于是很容易得出
   "这个模组在创造模式里没有物品" ✓。

### 修法

| 位置 | 内容 |
|---|---|
| 本模组页签 | 八个**全列**：符纸 / 朱砂 / 乱神符 / 飞剑符 / **反重力符** / 阵法图鉴 / 阵基 / 阵眼 |
| 原版「材料」页 | 符纸、朱砂 |
| 原版「工具与实用物品」页 | 三种符、阵法图鉴 |
| 原版「建筑方块」页 | 阵基、阵眼 |

第二条靠订阅 `BuildCreativeModeTabContentsEvent` 实现 ✓（Forge 1.20.1 的标准做法 ✓）。

### 验证

打包自检 **574 项全过** ✓，其中新增一段专门钉这件事：

```text
[ok] 登记的是创造模式物品栏（DeferredRegister）
[ok] 自己的页签里有 符纸 / 朱砂 / 乱神符 / 飞剑符 / 反重力符 / 阵法图鉴 / 阵基 / 阵眼   ← 八个全查
[ok] 订阅了 BuildCreativeModeTabContentsEvent（把物品也加进原版页签）
[ok] 材料进原版『材料』页（SRG f_256968_）
[ok] 符和图鉴进原版『工具与实用物品』页（SRG f_256869_）
[ok] 阵眼阵基进原版『建筑方块』页（SRG f_256788_）
[ok] 页签标题 中文 / 英文 文案在
```

> **"八个全查"这一条，就是能抓住反重力符遗漏的那个检查** ✓ ——
> 以前没有它，所以漏了也没人发现 ✓。

出厂冒烟 PASS ✓（新的页签事件处理器在**专用服务端**上不会炸 ✓）；反重力测试 PASS ✓。

### 如果你那边还是空的

那就不是这个原因 ✓。按顺序查两件事：

1. **"符箓"这个页签在不在**？如果**根本没有这个页签** ✗，说明那个客户端**没加载模组** ✓ ——
   去游戏日志里搜 `fulu` 看启动时有没有它 ✓，并且确认 `mods/` 里放的是**新的** jar ✓
   （里面躺着一个旧副本，是"我刚修的东西怎么还是坏的"最经典的原因 ✓）。
2. **页签在、但仍然是空的** ✓ —— 告诉我，我接着查 ✓。
   不过要注意：页签内容是由**服务端**生成后发给客户端的 ✓，
   所以**客户端/服务端版本不一致**（比如单机存档里是旧版 ✓）表现就是这样的 ✓。

---

## 第二十三部分：修"反重力符装不进阵眼" + 装上后的阵眼外观

### 原因

`TalismanKind.of(stack)`（判断"手里拿的是哪种符"）是一串**手写的 if** ✗：

```java
if (stack.is(CHAOS_TALISMAN))        return CHAOS;
if (stack.is(FLYING_SWORD_TALISMAN)) return SWORD;
return EMPTY;                                  // ← 反重力符掉到这里
```

所以拿着反重力符被当成**"没拿符"** ✗ —— 阵眼走的是"这不是符"那条分支 ✗：
装不上去 ✓，而且因为压根没装上 ✓，**图鉴也不会解锁** ✗（你大概也一起遇到了 ✓）。

### 为什么会漏（这才是重点）

"有哪几种符"这件事原来写在**两个地方** ✗：

| 位置 | 写法 | 加新符时会怎样 |
|---|---|---|
| `item()` | `switch` 表达式 | **编译器**会报"漏了一个分支" ✓ → 我改了 ✓ |
| `of()` | 手写 if 链 | 编译器**什么都不说** ✗ → 我忘了 ✗ |

**同一个事实写了两份，而只有一份被编译器看着** ✓ —— 这就是整个 bug ✓。

### 修法：只有一个来源

`of()` 不再列举种类 ✓，而是反过来问 `item()` ✓：

```java
for (TalismanKind kind : values()) {
    Item item = kind.item();
    if (item != null && stack.is(item)) return kind;
}
return EMPTY;
```

现在"哪种符对应哪个物品"只有**一处** ✓，而且是**编译器会检查的那一处** ✓ ——
以后再加第四种、第五种符，**不可能再对不上** ✓。

### 同一类 bug 的第二处

取符的提示语原来是个三元 ✗：`inside == CHAOS ? 取下乱神符 : 取下飞剑符` ✗ ——
有了第三种符之后 ✓，**取下反重力符会说成"取下飞剑符"** ✗。
形状完全一样 ✓：三个分支的地方写了"二选一" ✓。现在改成穷尽的 `switch` ✓，
反重力符有自己的一对文案 ✓。

### 装上之后的阵眼外观

这一版外观其实**早就有了** ✓（方块状态指的就是它 ✓），只是你**从来没装上过、所以没见过** ✓✗。
顺手也做明显了：符纸外面加一圈**淡紫光晕环** + 四个角的光点 ✓，
四种外观现在一眼能分清 ✓：

![四种外观对比](tools/out/eye_four_looks.png)

### 验证

打包自检 **580 项全过** ✓，新增一段钉这件事：

```text
[ok] TalismanKind 认得 乱神符 的物品
[ok] TalismanKind 认得 飞剑符 的物品
[ok] TalismanKind 认得 反重力符 的物品
[ok] 装符读的是手里的符种类
[ok] 装上/取下反重力符各有自己的文案（不是复用飞剑符那句）
```

反重力 PASS ✓；怪物识别 PASS ✓；齐射 PASS ✓；出厂冒烟 PASS ✓。

### 我验不了的那一步

**右键阵眼需要玩家** ✓，而无头测试里没有玩家 ✗。所以这次修的是**结构** ✓
（只剩一个来源 ✓、而且那个类是编译器盯着的那个 ✓），不是靠重放那次点击 ✓。
真正的确认是你拿符右键一下：应该看到预览里那圈**淡紫光晕** ✓，并且图鉴解锁反重力阵 ✓。

---

## 第二十四部分：五条清单逐条核对

这一轮**没有新功能** ✓ —— 你列的五条在之前几轮里都已经实现了 ✓（第二十、二十一部分 ✓，
加上第二十三部分修掉的 `of()` ✓，那一条正是让第 2 点能工作起来的关键 ✓）。
所以这一轮做的是**核对** ✓，并且补上清单里**唯一还没真正验证过**的一环 ✓：
「**确认新状态能存能读**」✓。

### 逐条

| 你的清单 | 代码里的位置 | 状态 |
|---|---|---|
| 一 认识反重力符 | `FormationEyeBlockEntity.talisman`，类型 `TalismanKind`，**四值** | ✅ 已有 |
| 一 存档能存能读 | `saveAdditional` 写种类**名字** / `readAdditionalSaveData` → `byName` | ✅ **本轮实测** |
| 二 能装反重力符 | `use()` → `of(held)` → 有符则拒绝并提示先取出 → 扣一个 + `setTalisman` + 提示 + 音效 | ✅ 已有（上一轮修好） |
| 三 装上后的外观 | 第四张贴图 + 模型 + 方块状态第四条 | ✅ 已有 |
| 四 按符分派 | 每刻 `switch`：乱神 / 飞剑 / 反重力 / 空 | ✅ 已有 |
| 五 取符 | 空手右键 → 还回 `inside.item()`（哪种符就还哪种）+ 状态置空 | ✅ 已有 |

### 「能存能读」怎么证的

这个测试以前只验了**运行时行为** ✓，没验过**写盘** ✓。这次：

```text
[stop]  rcon: Saving the game ... | Saved the game      ← 优雅停服，真的落盘
verdict : PASS - marked, frozen in place, flip is render-only

region file : run/verifyworld/region/r.0.0.mca
looking for : Talisman
  found in chunk 0,0 -> as string: 'antigravity'
RESULT: OK (expected 'antigravity')                     ← 直接读区域文件，不经过游戏
```

**第四种状态确实能存下来** ✓。读的那一侧用的是另外两种符**同一套**加载代码 ✓，
而且 `byName` 是**遍历枚举** ✓（不是手写清单 ✓）—— 结构上不可能漏掉某种符 ✓，
和上一轮那个 bug 的根因正好相反 ✓。

### 顺便抓到我自检里的两处同类问题

1. **枚举的断言只列了 `EMPTY / CHAOS / SWORD`** ✗ —— 从来没查过 `ANTIGRAVITY` ✗。
   所以上次"反重力符装不进去"时，**自检也一声不吭** ✗。
   这和 bug 本身是**同一个形状**：一份手写清单 ✓ —— 只不过这次在测试里 ✓。
2. 一处标签还写着"空 / 乱神符 / 飞剑符" ✗。

两处都修了 ✓，现在**四种状态全查** ✓、大小写两种序列化名也一起查 ✓。

打包自检 **582 项全过** ✓；反重力 PASS ✓（含上面那次落盘核对 ✓）；怪物识别 PASS ✓；
齐射 PASS ✓；出厂冒烟 PASS ✓。

---

## 第二十五部分：修"没有升空" + 反重力改成负面效果、阵法内无限时长

### 一、为什么没升空（这次的坑很有意思）

上一轮我把"抬升"从效果挪到了**每刻事件**里 ✓，但沿用了一个**旧数值**：`RISE = 0.10` ✓，
注释还写着"重力吃掉 0.08，所以净升 0.02" ✗ —— **放在这个位置这句话是错的** ✗，
日志直接把它证伪了 ✓：

```text
[antigravity-tick] y=-58.20 ceiling=-55.00 vy=0.100
[antigravity-tick] y=-56.20 ceiling=-55.00 vy=0.100     ← 20 刻正好升 2.00 格
[antigravity-tick] y=-39.96 ceiling=-55.00 vy=0.080     ← 设 0.08 就升 0.08/刻
```

规律很清楚 ✓：**位置每刻走的距离 = 我设的速度值** ✓。原因是原版 `travel()` 的顺序是
**先按当前速度移动、再把重力扣在速度上** ✓，而这个事件在 travel **之前** ✓、
且每刻都把速度重写一遍 ✓ —— 所以**重力根本参与不了位置更新** ✓。

于是正确值就是**字面值** ✓：

| 常量 | 值 | 含义 |
|---|---|---|
| `RISE` | **0.02** | 每刻上升 0.02 格 —— 就是你原话里的数 ✓ |
| `HOVER` | **0.0** | 到顶每刻移动 0 = 悬在那里 |

改完日志是：

```text
[antigravity-tick] y=-55.00 ceiling=-55.00 vy=0.000
[antigravity-tick] y=-55.00 ceiling=-55.00 vy=0.000     ← 稳稳定在天花板
```

那句误导人的注释也**照着实测重写**了 ✓ —— 它已经把我带偏两次 ✗。

### 二、为什么没有倒立

**这一条我复现不了** ✓，所以要说清楚我能证明到哪一步 ✓：

- 标记**确实挂上了** ✓（测试里看得到 `fulu:antigravity` ✓）；
- 渲染事件**在包里、逻辑也读过了** ✓（`RenderLivingEvent.Pre` → 上移一个身高 → 绕 Z 转 180° ✓）；
- 但**倒立是客户端的渲染姿态** ✗ —— 无头环境里没有"画面"这个东西可观察 ✓，
  所以"它是不是倒着的"**没法做成一条检查** ✗。

按可能性排：**你那个客户端用的 jar 比改动旧** ✓（倒立是两轮前加的 ✓）；
或者客户端渲染路径上还有别的问题 ✓。
**拆分这两者最快的办法**：看怪物现在**会不会升空** ✓ ——
会升空说明标记已经到客户端了 ✓，那问题就纯在渲染那边 ✓，我再加一条客户端调试输出 ✓。

### 三、负面效果 + 阵法内无限时长

| 要求 | 实现 |
|---|---|
| 反重力是**负面效果** | 效果分类改成 `MobEffectCategory.HARMFUL` ✓ |
| **阵法内时间无限** | 挂的是 `duration = -1`（原版意义上的"无限" ✓，客户端显示 ∞ 而不是倒计时 ✓） |
| 离开阵法 3 秒内消失、然后掉下来 | `releaseAntiGravity` 把它换成 **60 刻（3 秒）** 的实例 ✓ |
| 掉落伤害按正常计算 | 抬升那边**从不清** `fallDistance` ✓ —— 摔死真算阵法的功劳 ✓ |

> 效果类里**依然一行每刻逻辑都没有** ✓，这是刻意的 ✓：
> `duration = -1` 的原版效果**不保证每刻被 tick** ✓，
> 所以"每刻必须发生的事"（抬升、按住移动）全在每刻事件里 ✓，效果只负责"存在" ✓。

### 验证

```text
  no datapack function failed to load                  OK
  the monster is lifted into the air                   OK  从 -60 升到 -58 以上
  it stops at the 5 block ceiling                      OK  到 -55，从未超过 -53
  inside the formation the effect is infinite          OK  Duration 全程是 -1
  it cannot walk away                                  OK  X 600 刻都在 [2,4]
  the marker never lapses while it is inside           OK
  it carries the anti-gravity effect                   OK
 verdict    : PASS
```

打包自检 **588 项全过** ✓（含负面分类 ✓、RISE/HOVER ✓、无限时长 ✓、离场倒计时 ✓、
以及"效果类里没有任何每刻逻辑" ✓）；怪物识别 ✓ / 齐射 ✓ / 飞剑实体 ✓ / 出厂冒烟 ✓ 全绿 ✓。

---

## 第二十六部分：倒立能不能做？能 —— 但有一环是"静默失败"

**先直接回答：能 ✓，而且代码早就写好了 ✓。** 卡住的不是可行性 ✓，是**其中一环会静默失败** ✓，
而我在无头环境里看不到渲染 ✗，所以一直没能把它揪出来 ✓。

### 倒立这条链子

| 环节 | 在哪 | 能否验证 |
|---|---|---|
| 1 阵眼挂标记 | 服务端 | ✅ 测试证明 |
| 2 标记同步到客户端 | 同步效果 | ✅ 测试证明 |
| 3 **客户端的渲染回调被调用** | 客户端 | ❌ **就是这一环** |
| 4 回调变换姿态 | 纯数学 | 读过代码 |

第 3 环原来用的是 `@Mod.EventBusSubscriber` **注解扫描** ✗ —— 这个机制**静默失败** ✗：
没扫描到就什么都不发生 ✓，不报错、不留日志 ✓，唯一的现象就是"模型没倒" ✓
—— 正是你看到的现象 ✓。

### 修法：改成显式注册

`AntiGravityRenderHandler` **去掉注解** ✗，改由 [`ModClientSetup`](src/main/java/com/lengyang/fulu/client/ModClientSetup.java)
（它本身就是 `Dist.CLIENT` ✓、本来就有 `FMLClientSetupEvent` 处理器 ✓）调用：

```java
AntiGravityRenderHandler.register();          // → MinecraftForge.EVENT_BUS.register(...)
```

这条路只有两种结果：**注册上了** ✓ 或者**编译不过** ✗ —— 没有"静默什么都不做" ✓。

> ⚠️ 注解是**删掉**的 ✓，不是和显式注册并存 ✗ —— 两边都注册会让回调跑**两遍** ✓，
> **翻两次等于没翻** ✗。自检里有一条断言专门盯这件事 ✓（这种很容易以后又被加回来 ✓）。

### 它现在会自己报告

回调真正翻转时会写一行日志 ✓：

```text
[antigravity-render] flipped entity 1234 (h=1.95)
```

（每个实体每秒最多一条 ✓，**默认开着** ✓ —— 这样你不用重新编译就能拿到答案 ✓。）

**这行日志的含义是穷尽的**：

| 日志现象 | 结论 |
|---|---|
| 有这行 + 怪物确实倒着 | 完成 ✓，我把日志去掉 ✓ |
| 有这行 + 怪物看起来正常 | 回调在跑、**变换写错了** ✗（就那一行姿态数学 ✓，我改 ✓） |
| **完全没有这行** | 回调仍然没被调用 ✗ —— 那就指向本模组代码之外 ✗（jar 不是我构建的那个 ✓，或者别的模组动了 `RenderLivingEvent` ✓） |

### 我仍然做不到的那件事

**看渲染** ✗。关于倒立我说的每一句，要么是"代码在那儿" ✓，要么是"数据是对的" ✓ ——
**从来没有**"我看着它翻了" ✗。所以这一轮的重点是**让客户端自己说话** ✓，
而不是我再猜一遍 ✓ —— 前两次回答我猜了（"大概是旧的 jar" ✓），那是不够的 ✗。

### 验证

打包自检 **592 项全过** ✓，其中四条新的：

```text
[ok] 监听怪物渲染事件
[ok] 有 register()：显式挂到游戏事件总线
[ok] **没有**加注解（否则会和显式注册叠加，翻两次等于没翻）
[ok] 翻转时写一条日志（让'跑没跑'可验证）
[ok] 客户端启动时调用 AntiGravityRenderHandler.register()
```

出厂冒烟 PASS ✓（纯客户端类依然不炸专用服务端 ✓）。

---

## 第二十七部分：倒立还是不生效 —— 是**符号写反**了

上一轮我把"没倒立"归到注册方式上 ✓（那条确实脆 ✓），但**真正在坏事的是那两行姿态数学** ✗。

### 当时写的是

```java
pose.translate(0.0D, -entity.getBbHeight(), 0.0D);   // ← 负号
pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
```

`PoseStack` 的合成是 **`M = T(A) · R`** ✓ —— 也就是**先转、后平移** ✓。
身体在这个事件能拿到的那一层是「脚 y=0，头 y=h」✓，于是：

- **确实翻了** ✓（头到脚下面 ✓）；
- 但被推到了 **`[-2h, -h]`** ✗ —— **比自己头顶还高一个多身高** ✗，**整个出了碰撞箱** ✗。

屏幕上看起来就是"这只怪好好地站着" ✗ —— 正是你报的现象 ✓。

### 修法：绕**身体中心**转

```java
float half = entity.getBbHeight() * 0.5F;
pose.translate(0.0D, half, 0.0D);
pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
pose.translate(0.0D, -half, 0.0D);
```

### 这次不是"推理完就交"：把三种写法**逐点算了一遍**

身体的关键点是「脚 y=0、腰 y=h/2、头 y=h」✓；一次正确的翻转必须**既倒立、又原地**（跨度仍是 `[0, h]`）✓：

```text
v1（上一版 jar 里的）  foot y=-1.95  head y=-3.90  span=[-3.90,-1.95]  倒立=是  原地=否
标准做法 T(+h) 再转    foot y=+1.95  head y=+0.00  span=[+0.00,+1.95]  倒立=是  原地=是
我改后的（绕中心）    foot y=+1.95  head y=+0.00  span=[+0.00,+1.95]  倒立=是  原地=是
```

两点值得看 ✓：

1. **v1 这一行"倒立=是、原地=否"** ✓ —— 正好对应你看到的现象 ✓，而它就是在你手上那个 jar 里跑的 ✓；
2. **「绕中心」和业界通用做法逐点完全相同** ✓✓ —— 我上一版是同一个形状 ✓，**就差一个负号** ✗。

> 所以这次的修法**不是又一次猜** ✓：它和成熟做法对得上每一个数字 ✓。
> 上一轮的**显式注册也保留** ✓（注解扫描那条确实脆 ✓），但真正坏事的确实是符号 ✗。

### 还没变的那件事

**我看不到渲染** ✗，所以这仍然是"几何层面验证" ✓，不是"我看着它翻了" ✗。
诊断日志还在、还默认开着 ✓：

```text
[antigravity-render] flipped entity 1234 (h=1.95)
```

- **有这行 + 怪物确实倒着** → 成了 ✓，告诉我，我把日志去掉 ✓；
- **有这行、看着还是正常** → 那还是姿态数学 ✗，我会把上面那张表重新逐点核 ✓。

打包自检 **592 项全过** ✓；出厂冒烟 PASS ✓。

---

## 第二十八部分：再检查一遍 —— 这次查**真实代码**

### 权威依据：Forge 的补丁文件

不靠记忆 ✓ —— `forge-1.20.1-47.2.20-sources.jar` 里带着补丁文件
`patches/net/minecraft/client/renderer/entity/LivingEntityRenderer.java.patch` ✓，
它明确写着钩子插在哪：

```java
+ if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre<>(...))) return;
  ...
  p_115311_.m_252880_(0.0F, -1.501F, 0.0F);      // translate(0, -1.501, 0)
```

**`Pre` 在所有变换之前触发** ✓，之后原版依次做 `scale(-1,-1,1)` → `translate(0,-1.501,0)` ✓。

> 附带一条：gradle 缓存里那个 `joined-*-srg.jar` 是**打补丁之前**的原版类 ✗ ——
> 拿它搜 `RenderLivingEvent` 会**什么都搜不到** ✓，我一度以为出大事了 ✓，
> 其实只是问错了文件 ✓。

### 那条链子在屏幕上摆出什么

模型空间里人形怪的头在 y=−8 单位、脚在 +24 ✓（y 向下为正，16 单位一格 ✓）。
跑一遍原版自己的链子 ✓：

```text
头 y = +2.001      脚 y = +0.001      → 身体跨度 [0.001, 2.001]
```

**所以身体占世界坐标 [0, 2.0]** ✓，要"原地翻"就得绕 **1.0** 附近转 ✓。
代码用的是 `身高/2 = 0.975` ✓ —— **差 2.5 厘米** ✓：

```text
支点 0.975（代码用的） → 脚 +1.949  头 -0.051  跨度 [-0.051, 1.949]  倒立=是  原地=是
支点 1.000（模型中点） → 脚 +1.999  头 -0.001  跨度 [-0.001, 1.999]  倒立=是  原地=是
```

**这两行都是"倒立且原地"** ✓ → 现在这版**按真实代码核对是对的** ✓。

### 那么如果还是没倒

那就不是变换的问题 ✗，而是**那个回调在这台客户端上到底跑没跑** ✗ ——
这正好是日志要回答的问题 ✓。一条命令就能把两半切开 ✓：

```mcfunction
/effect give @e[type=minecraft:zombie,distance=..6] fulu:antigravity 30 0
```

（**直接挂标记，完全绕开阵法** ✓。）

| 现象 | 结论 |
|---|---|
| 倒立了 + 日志里有 `[antigravity-render]` | 回调正常 ✓ → 问题在**阵法那个标记没到这台客户端** ✗ |
| 没倒立 + **日志里什么都没有** | 回调在那台客户端上**根本没被调用** ✗ |

打包自检 **592 项全过** ✓；出厂冒烟 PASS ✓。
变换的正确性依据是：(a) 与通用做法逐点一致 ✓，(b) 按 Forge 补丁里那条链子逐点算过 ✓。

---

## 第二十九部分：画面还是没倒立 —— 先排除"这只怪根本不该被翻"

### 最可能的原因：**排除名单**

`FormationTarget.canInvert` 是**故意**拒绝这几类的 ✓（反重力阵本来就不该影响它们 ✓）：

| 条件 | 覆盖的怪 |
|---|---|
| `isNoGravity()` | 任何自身就飘着的怪 |
| `instanceof FlyingMob` | **幻翼、恶魂** |
| `instanceof FlyingAnimal` | **蜜蜂、鹦鹉、悦灵** |
| `instanceof Bat` | **蝙蝠** |
| `EnderDragon` / `WitherBoss` | 末影龙、凋灵 |

**如果你测试用的怪是上面任意一种，那"没翻"就是设计如此** ✓，不是 bug ✓。
排除它不花什么成本 ✓：在阵法里放一只**僵尸**（或者骷髅、苦力怕、蜘蛛 —— 任何地面上的怪 ✓）。

### 其次：回调现在会在**画面里**自证

日志要翻文件、还容易看漏 ✗ —— 所以渲染回调现在会给**每一只被它翻转的怪**头顶放一颗粒子 ✓
（每秒最多一粒 ✓）。这样"回调到底跑没跑"就变成**肉眼可见**的 ✓：

| 现象 | 结论 |
|---|---|
| 有紫色粒子 + 怪倒立 | 正常 ✓ |
| 有紫色粒子 + 怪**没**倒立 | 回调在跑、**变换错了** ✗（我用三种方式验过 ✓，真这样会让我很意外 ✓） |
| **完全没有粒子** | 回调在那台客户端上**没被调用** ✗ → 问题根本不在变换上 ✗ |

### 关于变换，已经验过的是这些

1. 与业界通用做法**逐点一致** ✓；
2. 按 **Forge 补丁文件**里那条真实链子（`scale(-1,-1,1)` → `translate(0,-1.501,0)`，且事件在这之前）逐点算过 ✓；
3. 算出来是「脚 +1.949 / 头 −0.051」（身体原本占 `[0, 2.0]`）—— **倒立、且在自己碰撞箱内** ✓。

**没验过的、也没法从这里验的**：这些到底有没有出现在画面上 ✗ —— 这正是那颗粒子的用途 ✓。

### 我唯一想请你做的

**用僵尸试**（站在阵法里 ✓），然后告诉我两件事：

1. **怪物看起来是倒着的吗？**
2. **它头顶大约每秒有一颗粒子吗？**

这两个答案就能把它彻底定死 ✓。

### 验证

打包自检 **592 项全过** ✓；反重力 PASS ✓；怪物识别 PASS ✓；齐射 PASS ✓；
出厂冒烟 PASS ✓（纯客户端类依然不炸专用服务端 ✓）。

---

## 第三十部分：找到了 —— 那个变换是一个**可证明的空操作**

### 你的观测就是答案

「**有粒子**」+「**画面正常**」同时成立，意味着两件事 ✓：

1. 回调**确实在跑** ✓（粒子就是回调自己放的 ✓）；
2. 变换**没有任何可见效果** ✓。

"一个可证明什么都不做的变换"是个很具体的 bug ✓ —— 就是下面这个 ✓。

### 原版那句 `scale(-1,-1,1)` **就是**绕 Z 转 180°

矩阵验证（跑过真实数字 ✓）：

```text
is the renderer's own scale(-1,-1,1) exactly Rz180 ?  -> True
```

原版渲染器对**每个生物**都会做这句 ✓；而 jar 里那一版**也**是绕 Z 转 180° ✗ ——
**同一个矩阵** ✓。两个相同的变换叠起来 = **恒等** ✓：

```text
mine: rotate Z 180  composed with the renderer's flip  ->  identity（什么都没改）
```

**这就是"粒子出现了、模型纹丝不动"的原因** ✓ —— 不是渲染玄学 ✓，是算术 ✓。

### 其它旋转同样不行，而且是同一个原因

绕 X 转 ✗ → 合成出来是**绕 Y 转**（原地转身 ✓）：

```text
mine: rotate X 180  composed  ->  diag(-1,+1,-1) = Ry180（只是转了个身）
```

原版那句的**行列式是 +1**（旋转 ✓），我也用旋转 ✗ —— 而"上下颠倒"在数学上是**镜像**
（**行列式 −1** ✓）。**+1 相乘永远得不到 −1** ✓ —— 所以**任何旋转都做不了这件事** ✓，
这就是我连着错三次的原因 ✓。

### 修法：一次**反射**

```java
pose.translate(0.0D, half, 0.0D);
pose.scale(1.0F, -1.0F, 1.0F);      // 反射：行列式 -1
pose.translate(0.0D, -half, 0.0D);
```

反射**不可能**被原版那个行列式 +1 的翻转抵消 ✓ → 脚朝上、头朝下 ✓，
而且身体留在自己的碰撞箱里 ✓（`y → 身高 − y` ✓）。

### 诚实的代价

奇数个负缩放 = **镜像** ✓ → 模型除了上下颠倒，**左右也会翻** ✓。
对称的怪（僵尸、骷髅、苦力怕）看不出来 ✓；**单手拿东西的怪**，物品会换边 ✓。
这不是实现问题 ✓ —— "上下颠倒"在数学上就是镜像 ✓，镜像必然有这个后果 ✓。
原版实体用 `entityCutoutNoCull`（不剔除背面 ✓），所以镜像后照常可见 ✓。

### 加了两道防线，防止它悄悄回来

前三版都错在同一个盲区里 ✗：**我看不见模型** ✓，所以"变换没生效"和"回调没被调用"
在我这里长得一模一样 ✗。现在有两件事挡住它：

- 回调会**放一颗粒子** ✓ → "跑没跑"在画面里直接可见 ✓；
- 自检新增断言：**必须用 `scale`、不许出现 `mulPose`** ✓ ——
  以后谁把它改回旋转 ✓，**打包自检会直接报错** ✓，而不是又一次"屏幕上什么都没发生" ✓。

### 验证

打包自检 **595 项全过** ✓，其中：

```text
[ok] 用的是 scale（镜像）
[ok] **没有**用 mulPose —— 旋转会被原版那句 scale(-1,-1,1) 抵消掉
[ok] 翻转时放一颗粒子（画面上就能看出回调跑没跑）
```

出厂冒烟 PASS ✓。

---

## 第三十一部分：用 IDEA 打开（IDEA 报错不是代码错）

### 先回答：不是用 IDEA 做的

这个项目全程是**命令行 Gradle**（`tools\build.ps1` → 直接调 `tools\.toolchain\gradle-8.12.1`
的 launcher ✓）+ 自己的校验脚本（Python / PowerShell ✓）。所以仓库里**从来没有** `.idea` ✓。

### IDEA 为什么满屏红

**因为缺少 Gradle wrapper** ✗ —— 仓库里原来没有 `gradlew` / `gradle\` 目录 ✓，
而 Minecraft / Forge 的依赖是 **ForgeGradle 同步时生成**的 ✓：
IDEA 没法同步 → `net.minecraft.*`、`net.minecraftforge.*` **一个都解析不了** ✗ → 满屏红 ✓。

**代码本身没错** ✓ —— 只是 IDE 还没拿到那份依赖 ✓。

### 已经补上了 wrapper

```text
gradlew                                （8700 字节）
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties    → Gradle 8.12.1
```

### IDEA 里的四步（关键是第 2 步，能省 1.2GB）

1. **File → Open**，选 **`fulu` 这个目录**（有 `build.gradle` 的那一层 ✓）→
   要选 **"Open as Project"**，不是 "Open as File" ✓；
2. **Settings → Build, Execution, Deployment → Build Tools → Gradle**：
   - **Gradle user home** → `fulu\tools\.gradle-home` ✓
     —— 这里已经有 **1.22 GB** 的 Forge / Minecraft 缓存 ✓，**直接复用，不用重新下载**；
   - **Use Gradle from** → *Specified location* → `fulu\tools\.toolchain\gradle-8.12.1` ✓
     —— 这样也不会去下 wrapper 那一份 130MB ✓；
   - **Gradle JVM** → `D:\软件\我的世界\Java21` ✓（就是 `tools\java-home.txt` 里那个 ✓，
     这个组合是**实测能构建**的 ✓）；
3. **Project Structure → SDK**：17（有的话）或 21，**Language level 17** ✓
   （`build.gradle` 里写的是 `JavaLanguageVersion.of(17)` ✓）；
4. 点 Gradle 面板的 **Reload**（大象图标 ✓），等同步跑完 ✓ —— 红色应该全部消失 ✓。

### 两个可能让你困惑的地方

| 现象 | 说明 |
|---|---|
| IDEA 提示 "Gradle daemon disabled" | `gradle.properties` 里有 `org.gradle.daemon=false` ✓ —— 那是**故意**的（无头构建不留残留进程 ✓），无害 ✓ |
| 同步报 "invalid JAVA_HOME" / 路径相关错 | 这台机器的 JDK 路径带中文 ✓ —— 这正是 `tools\build.ps1` 不用 `gradlew.bat` 的原因 ✓（批处理处理不了中文路径 ✓）；照第 2 步把 Gradle JVM 指到那个路径即可 ✓ |

### 不想折腾 IDEA 的话

直接构建就行 ✓（IDEA 不需要参与 ✓）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\build.ps1 -Task build
# 产物 build\libs\fulu-1.0.0.jar
```

**如果 IDEA 还是报错**，把**红色的具体文字**发我 ✓（比如 `Cannot resolve symbol 'minecraft'`
✓ 那是同步问题 ✓；如果是报我自己写的某个类/方法 ✓，那才可能是真的代码问题 ✓）。

---

## 第三十二部分：乱神符配方（符纸 + 朱砂 + **兽血** + 腐肉）

### 两件必须先说的事

**1. 「兽血」原本不存在。** 模组里当时只有：符纸 / 朱砂 / 乱神符 / 飞剑符 / 反重力符 /
图鉴 / 阵基 / 阵眼 ✓ —— 你配方里点名的这个材料**得先做出来** ✓。做好了 ✓，
内部代号 `beast_blood`（兽 = beast ✓）：物品 + 贴图 + 模型 + 中英文名 + 创造页签 ✓。

> ⚠️ **它现在没有获取途径** ✗ —— 你没说兽血从哪来 ✓，我**不自己编**（比如"杀牛掉落" ✓），
> 所以目前只能在创造模式页签里拿到 ✓。要怎么获得，等你定 ✓。

**2. 乱神符原本根本没有配方。** 之前只有 反重力符 / 飞剑符 / 朱砂 / 阵基 / 图鉴 五个配方文件 ✓，
乱神符一直是"只能在创造模式拿" ✓。顺手还发现：**符纸也没有配方** ✗ ——
所以这条链子在生存里**还走不通**（配方的基础材料自己做不出来 ✓）。
这超出你这次要的范围 ✓，我没动它 ✓，但要用之前得有 ✓。

### 实现的内容（照字面）

`data/fulu/recipes/chaos_talisman.json` ✓，**无序合成** ✓（你写的是 `+` ✓，不是格子阵 ✓）：

```text
fulu:talisman_paper     ×1     符纸
fulu:cinnabar           ×1     朱砂
fulu:beast_blood        ×1     兽血（新物品）
minecraft:rotten_flesh  ×1     腐肉
        → fulu:chaos_talisman ×1
```

其它配方**一个都没动** ✓。

兽血的样子（16×16 ✓，生成脚本 `tools/gen_beast_blood.py` ✓）：

![兽血](tools/out/beast_blood_preview.png)

### 「配方到底加载了没有」——这一步反而最有意思

服务器日志里有一行 `Loaded N recipes` ✓，显示 **7** ✓。但我去翻了两天前的旧日志 ✓：
**那时只有 5 个配方文件，也是 7** ✗；加了配方还是 7 ✗；故意放一个坏文件进去，**仍然是 7** ✗。
→ **这个数字在这个测试世界里不是"模组配方数"** ✓，**不能当证据** ✓。
（幸好查了 ✓ —— 差一点就拿它当"验证通过"报给你 ✓。）

**真正能当证据的做法**：往包里丢一个**语法坏掉**的配方 ✓，看服务器会不会点名抱怨 ✓：

```text
[Worker-Main-17/ERROR] [SimpleJsonResourceReloadListener]: Couldn't parse data file fulu:_probe_b...
com.google.gson.JsonParseException: MalformedJsonException ... at line 1 column 91
```

这说明**我们的 `data/fulu/recipes/` 确实被读了** ✓。探针删掉后重跑一遍 ✓ ——
`chaos_talisman.json` **不在**报错名单里 ✓，也就是说它**被加载且解析干净** ✓。

> 第一次探针我用的是"引用了不存在的物品" ✗ —— **一条错误都没有** ✓
> （原版对配方里的未知物品很宽松 ✓）。**一个会通过的坏探针什么都证明不了** ✓，
> 所以才值得换一个重做 ✓。

### 验证

打包自检 **608 项全过** ✓，其中新增十二条，例如：

```text
[ok] assets/fulu/textures/item/beast_blood.png（兽血）已打包
[ok] 兽血的中文名是 '兽血'
[ok] data/fulu/recipes/chaos_talisman.json（乱神符配方）已打包 —— 这一步之前**根本没有这个文件**
[ok] 乱神符配方用到了 符纸 / 朱砂 / 兽血 / 腐肉
[ok] 乱神符配方产出 乱神符 ×1
[ok] 乱神符配方正好四个材料
[ok] 自己的页签里有 兽血
```

出厂冒烟 PASS ✓；反重力回归 PASS ✓。

### 只有你能确认的那一步

专用服务器**没有玩家** ✗，所以这里没法真的往合成格里放东西 ✓。
配方作为数据是**已加载且正确**的 ✓；真的点着合出来一个乱神符，要你试 ✓。

---

## 第三十三部分：动物留下的血泊 + 玻璃瓶换兽血

![血泊贴图](tools/out/blood_pool_preview.png)

### 做了什么

| 东西 | 说明 |
|---|---|
| `fulu:blood_pool` 实体 | **不是生物** ✓：没有 AI、不占刷怪上限、不会自己走掉；有重力，动物死在半空也会落到地上；**不挡路**（`canBeCollidedWith=false`）但**能被右键射线打到**（`isPickable=true`）—— 这是两个不同的问题，混了就会"能点但走不过去" |
| `BloodPoolSpawner` | 监听 `LivingDeathEvent` ✓，死的是 `Animal` 就在原地留一滩 |
| `BloodPoolRenderer` | **一张贴地的四边形** ✓，不是模型 —— 一滩水不需要厚度，16×16 贴图下也看不出厚度 |
| 贴图 | `textures/entity/blood_pool.png` ✓，生成脚本 `tools/gen_beast_blood.py` 同款的字符画/程序化做法（`tools/gen_blood_pool.py` ✓，固定种子可复现 ✓） |

**玻璃瓶右键** ✓：扣掉瓶子 ✓ → 给 1 瓶兽血 ✓ → 播放接液体音效 ✓ → **血泊消失**（血被接走了 ✓）。

### 我做了选择、需要你确认的六件事

1. **任何死法都会留** ✓，不只是玩家击杀 ✗ —— 你说的是"击败" ✓，字面更像"玩家打死" ✓；
   但专用服务器**没有玩家** ✗，按"玩家击杀"写我就**完全没法验证** ✓。
   要改成只认玩家击杀，一行的事 ✓。
2. **驯服的动物也算** ✓（狗、猫也是 `Animal` ✓）→ 打死自家狗也会留一滩 ✓（同一个取舍 ✓）。
3. **非动物不留** ✓（僵尸、村民、鱿鱼 ✓）—— 下面有实测 ✓。
4. **一滩只能接一瓶** ✓，接完血泊消失 ✓；想接多瓶说一声 ✓。
5. **血泊不会自动消失** ✗ —— 它是普通实体，没有寿命 ✓ → 杀得多会攒一地 ✓。要加寿命很容易 ✓。
6. **我加了接液体的音效** ✓（`BOTTLE_FILL` ✓）—— 这是我加的 ✓。

### 新增测试模式 `-BloodPoolTest`

召唤一头牛杀掉 ✓、再召唤一只僵尸杀掉 ✓，用**计分板**准确数血泊 ✓：

```text
  no datapack function failed to load            OK
  no pool while the animal is alive              OK  nothing had died yet
  killing an animal leaves exactly one pool      OK  count was 1 after the cow died
  a non-animal leaves nothing                    OK  the zombie died and count stayed 1
 verdict    : PASS - animals leave a pool, non-animals do not
```

### 写这个测试时踩的两个坑（都值得记）

1. **`@e[...,limit=2]` 不等于"至少两个"** ✗ —— `limit` 只是**选择器最多返回几个** ✓，
   配 `if entity` 在**只有一个**时也会成功 ✓ → 测试报了一个**不存在的失败** ✗。
   改成**计分板计数**才准 ✓（`matches 1` / `matches 2..` ✓）。
2. **文本模式改这个 `.ps1` 会把 CRLF 变成 LF** ✗ —— 这个文件是 **UTF-8 无 BOM + 大量中文注释** ✓，
   PowerShell 5.1 会按 ANSI 读 ✗ → 注释变乱码 ✗ → **乱码把大括号吃掉** ✗ →
   一行改动引出 **43 个语法错误** ✗。**必须二进制读写**（或写完转回 CRLF ✓）。

### 验证

打包自检 **621 项全过** ✓。注意新增的断言块**没有**去查玻璃瓶/`discard`/`isPickable`/渲染类型 ✗ ——
它们是**原版名字** ✓，正式 jar 里全被混淆成 `f_xxxxx_`/`m_xxxxx_` ✓，查字面名必然失败 ✓
（这个坑在本项目里踩过好几次 ✓）。所以「拿瓶子换血」这一半靠**行为测试 + 你自己点** ✓。

回归：血泊 PASS ✓；反重力 PASS ✓；怪物识别 PASS ✓；飞剑实体 PASS ✓；出厂冒烟 PASS ✓；无 java 残留 ✓。

### 只有你能确认的

专用服务器没有玩家 ✗，所以这里没法真的拿瓶子点 ✓。
已证明的是「动物留一滩、非动物不留」✓；**右键玻璃瓶换到兽血、血泊消失**要你试 ✓。

---

## 第三十四部分：血泊改成"只算玩家击杀" + 三条配方

### 一、"要" —— 血泊只算玩家击杀

判断改用 `getKillCredit()` ✓：

| 死法 | 留血泊？ |
|---|---|
| 玩家近战 | ✅ |
| 玩家射箭 / 三叉戟 | ✅（算成射手 ✓） |
| 岩浆、摔死、别的怪咬死 | ❌ |
| `/kill` | ❌（没有击杀者 ✓） |

> **注意**：1.20.1 **没有** `getKiller()` 这个方法 ✗ —— 我第一次就是这么写的 ✓ 编译不过 ✓，
> 翻 `client_mappings.txt` 才确认：字段叫 `lastHurtByPlayer` ✓，拿"击杀者"的方法是 **`getKillCredit()`** ✓。

**代价要说清楚** ✓：专用服务器没有玩家 ✗ → **"玩家击杀 → 出现血泊"这条正方向没法无头验证** ✗。
所以测试只证明了**反方向** ✓：

```text
  no pool while the animal is alive                 OK
  an animal killed without a player leaves nothing  OK  the cow was /kill-ed, no pool appeared
  a non-animal leaves nothing                       OK
 verdict    : PASS - nothing appears without a player kill
```

正方向要你手上试 ✓。

### 二、三条配方

**阵眼（唯一有序 ✓）**：

```text
   阵基  铁粒  阵基        B I B
   铁粒  朱砂  铁粒   =    I C I      → fulu:formation_eye ×1
   阵基  铁粒  阵基        B I B
```

**朱砂（无序）**：红石 + 煤炭 ✓ —— 这是**替换**了原来的"红石 + 火药" ✗（旧配方已删除 ✓）。

**符纸（无序）**：纸 + 羊毛 + 纸 ✓。

> 阵眼和符纸**之前根本没有配方** ✓（上一轮我提过：乱神符那条链子在生存里走不通 ✓，
> 因为基础材料合不出来 ✓）。现在能合了 ✓。

**两处需要你确认**：

1. 我用的是**白色羊毛**（`minecraft:white_wool` ✓）—— 你说的是"羊毛" ✓，
   如果要**任何颜色**都行，改成标签一行的事 ✓。
2. 乱神符配方**没动** ✓：符纸 + 朱砂 + 兽血 + 腐肉 ✓。

### 三、验证

打包自检 **634 项全过** ✓（新增 13 条 ✓）：

```text
[ok] 阵眼配方已打包（之前根本没有）
[ok] 阵眼是**有序**合成（用户说只有它是）
[ok] 图案是 阵基铁粒阵基 / 铁粒朱砂铁粒 / 阵基铁粒阵基
[ok] 图案里的 B = 阵基 / I = 铁粒 / C = 朱砂
[ok] 产出 阵眼 ×1
[ok] 符纸配方已打包（之前根本没有）
[ok] 符纸是无序合成 / 用**两张**纸 / 白色羊毛 / 产出 符纸
[ok] 配方用到了煤炭
[ok] 配方**不再**用火药（这一步改的）
```

**配方真的被加载** ✓：出厂冒烟日志里**没有** `Couldn't parse` / `Parsing error` ✓
（这正是能抓出"材料 id 写错"的那条检查 ✓，之前故意放坏配方验证过它会点名 ✓）。

血泊 PASS ✓；反重力 PASS ✓；出厂冒烟 PASS ✓；无 java 残留 ✓。
（中途有一次全量回归因为等待被中断而丢了 ✗ —— 所以关键的三个是**重跑**的 ✓，不是假设通过 ✓。）

### 四、只有你能确认的

1. 拿武器（或弓箭）打死一头牛 → 死亡处应出现血泊 ✓
2. 玻璃瓶右键血泊 → 得到兽血、血泊消失 ✓
3. 合成台里：九宫格合阵眼 ✓、红石+煤炭合朱砂 ✓、纸+羊毛+纸合符纸 ✓

---

## 第三十五部分：三种符改成**有序**配方

### 三个九宫格（照字面）

```text
乱神符                          飞剑符                        浮空符（反重力符）
空   符纸  空                   空   符纸  空                 空    符纸   空
朱砂 发酵蛛眼 幻翼膜             兽血 铁剑  羽毛               潜影壳 兽血   羽毛
空   兽血  空                   空   朱砂  空                 空    朱砂   空
```

实现成 `crafting_shaped` ✓：

| 文件 | 图案 |
|---|---|
| `chaos_talisman.json` | `[" P ", "CFM", " B "]` |
| `flying_sword_talisman.json` | `[" P ", "BSA", " C "]` |
| `anti_gravity_talisman.json` | `[" P ", "HBA", " C "]` |

字母表 ✓：`P` 符纸 / `C` 朱砂 / `B` 兽血 / `F` 发酵蛛眼 / `M` 幻翼膜 / `S` 铁剑 / `A` 羽毛 / `H` 潜影壳 ✓

### 这一步**替换**掉的东西（要回来就说）

三条原来都是**无序**的 ✓，材料也不一样 ✗：

| 配方 | 原来的材料 | 现在 |
|---|---|---|
| 乱神符 | 符纸 + 朱砂 + 兽血 + **腐肉** | 腐肉 ✗ 去掉，换成发酵蛛眼 + 幻翼膜 |
| 飞剑符 | 符纸 + 朱砂 + **铁锭** + 羽毛 | 铁锭 ✗ → **铁剑**；+ 兽血 |
| 反重力符 | 符纸 + 朱砂 + 羽毛 + 潜影壳 | **+ 兽血** |

旧的无序文件是**覆盖**掉的 ✓，不是并列保留 ✓。

### 一处需要确认

你写的是「**浮空符**」✓，而模组里的物品叫「**反重力符**」（`anti_gravity_talisman` ✓）——
同一个东西 ✓，我把配方给了它 ✓。**如果浮空符是要一个全新的物品**，说一声我加 ✓
（现在模组里没有这个名字的物品 ✓）。

### 验证

打包自检 **645 项全过** ✓，三条配方被**完全钉住** ✓：

```text
[ok] 乱神符 是**有序**合成（用户说这三种都是）
[ok] 乱神符 的三行图案对得上
[ok] 乱神符 图案里的 P = 符纸 / C = 朱砂 / F = 发酵蛛眼 / M = 幻翼膜 / B = 兽血
[ok] 飞剑符是**有序**合成
[ok] 飞剑符图案是 空符纸空 / 兽血铁剑羽毛 / 空朱砂空
[ok] 飞剑符图案里的 P = 符纸 / B = 兽血 / S = 铁剑 / A = 羽毛 / C = 朱砂
[ok] 反重力符是**有序**合成
[ok] 反重力符图案是 空符纸空 / 潜影壳兽血羽毛 / 空朱砂空
[ok] 反重力符图案里的 P = 符纸 / H = 潜影壳 / B = 兽血 / A = 羽毛 / C = 朱砂
```

**配方真的被加载** ✓：出厂冒烟日志里没有 `Couldn't parse` / `Parsing error` ✓。
构建后回读三个文件 ✓：

```text
chaos_talisman         type=crafting_shaped  pattern=[ P |CFM| B ]
flying_sword_talisman  type=crafting_shaped  pattern=[ P |BSA| C ]
anti_gravity_talisman  type=crafting_shaped  pattern=[ P |HBA| C ]
```

### 顺手修的：自检里有 32 行标签是乱码

因为那几段是用 `Set-Content -Encoding ASCII` 写成脚本后再插进去的 ✗ ——
**中文在 Python 读到之前就已经被写坏了** ✓，标签全变成 `?????` ✗（检查本身是对的 ✓）。
两段整体用**真正的 UTF-8 脚本**重写 ✓，现在乱码行数 **0** ✓。

---

## 第三十六部分：替身阵（一）替身符 +（二）抗性提升

![替身符 + 抗性提升图标](tools/out/stand_in_and_resistance_preview.png)

### 一、替身符

```text
空   符纸   空
朱砂 干草块 兽血      →  fulu:stand_in_talisman ×1
空   羽毛   空
```

有序合成 ✓（`pattern: [" P ", "CHB", " A "]` ✓）。贴图是**小草人** ✓：稻草身体、头腰捆绳、
胸口一枚朱红盾符、两条草腿 ✓（`tools/gen_stand_in_talisman.py` ✓，字符画改起来很方便 ✓）。
模型 ✓、中英文名 ✓、创造页签 ✓ 都齐了 ✓。

### 二、抗性提升

自制效果 ✓，正面 ✓，颜色 `0x59C8B0` ✓（淡青绿 ✓），图标是**盾 + 十字** ✓（18×18 ✓ —— 原版硬性尺寸 ✓ 错了就是紫黑格 ✓）。

**减伤表**（唯一一份 ✓ 在 `ResistanceUpEffect.reductionFor` ✓）：

| 等级 | 减伤 | | 等级 | 减伤 |
|---|---|---|---|---|
| I | 15% | | V | 55% |
| II | 25% | | VI | 65% |
| III | 35% | | **VII** | **80%**（你没指定，我取的 80% ✓） |
| IV | 45% | | **VIII** | **160% = 完全免疫** ✓ |

> **160% 怎么来的**：算出来的剩余伤害是负数 ✓，**夹到 0** ✓ = 完全免疫 ✓
> —— 不是"倒扣血" ✓，这一点是特意处理的 ✓。

**VII / VIII 额外 80% 反弹** ✓：把**原始伤害的 80%** 还给攻击者 ✓，
并且**先清掉攻击者的无敌帧** ✓ —— 不清的话 `hurt()` 会被 10 刻的无敌窗口吞掉 ✗，
表现就是"有时候反弹有时候不反弹" ✗（飞剑十连斩踩过同一个坑 ✓）。

**30 秒** ✓ = 600 刻 ✓，做成常量 `DURATION_TICKS` ✓ ——
效果类自己改不了时长 ✓（时长是**施加时**写进去的 ✓），所以这个常量是给"以后施加它的人"用的 ✓。

**倒计时和悬停提示是原版行为** ✓：只要语言 key 有了 ✓，状态栏就显示"抗性提升 II" ✓ + 倒计时 ✓，
不需要额外写代码 ✓。

### 三、次数盾：我留了一个**接缝**，没有自己编规则

你写的是"减伤后的伤害再走**次数盾**逻辑" ✓ —— 但模组里**没有次数盾** ✗，规则也没定义 ✗
（能挡几下？谁给的？挡完会怎样？）。

所以我没有自己编 ✓，只留了一个单方法接缝 ✓：

```java
HitCountShield.absorb(Player player, float damage)   // 现在：原样返回
```

现在行为就是"减伤后的伤害直接生效" ✓。等你定规则 ✓，只改这一个方法 ✓，调用方不用动 ✓。

### 四、还缺的一环（故意的）

**"替身符 / 替身阵怎么把效果给玩家"你没说** ✗ —— 拿符右键自己？装进阵眼让范围内玩家吃到？
效果和物品都是真的、能用的 ✓，但**两者之间那根线还没接** ✓ —— 你说规则，我接 ✓。

（另外：原版**不渲染**效果描述 ✓，悬停只显示"名字 + 等级" ✓。描述按要求写进语言文件了 ✓，
但游戏里不会显示 ✓。）

### 五、验证

打包自检 **679 项全过** ✓，新增一块 ✓：

```text
[ok] 替身符模型的 layer0 指向 fulu:item/stand_in_talisman
[ok] 替身符的中文名是 '替身符' / 效果中文名是 '抗性提升'
[ok] 替身符是**有序**合成
[ok] 替身符图案里的 P = 符纸 / C = 朱砂 / H = 干草块 / B = 兽血 / A = 羽毛
[ok] 效果是正面效果（MobEffectCategory.BENEFICIAL）
[ok] 有 reductionFor / reflects / reflectRatio / MAX_LEVEL / DURATION_TICKS
[ok] 效果里没有每刻逻辑（减伤在受伤事件里做）
[ok] 效果已登记为 fulu:resistance_up
[ok] 监听受伤事件 / 只对玩家生效 / 减伤之后交给次数盾 / 改的是伤害数值
[ok] 效果图标尺寸 18x18 / 带透明通道
```

出厂冒烟 PASS ✓、无配方或注册错误 ✓。
（**效果注册错了是启动即崩** ✓，所以"服务器干净启动"确实能证明它注册成功了 ✓。）

### 六、只有你能确认的

减伤是**只对玩家**生效的 ✓，而专用服务器没有玩家 ✗ → 这段逻辑这里**没法跑** ✓。进游戏后：

```mcfunction
/effect give @s fulu:resistance_up 30 0     → 15% 减伤
/effect give @s fulu:resistance_up 30 6     → VII：80% 减伤 + 80% 反弹
/effect give @s fulu:resistance_up 30 7     → VIII：免疫 + 反弹
```

（amplifier 0-7 就是 I-VIII 级 ✓。）然后找只怪挨一下 ✓，两边的血都看一下 ✓。

---

## 第三十七部分：替身阵（三～十）

![阵眼五套外观 + 碎裂图案](tools/out/eye_five_looks.png)

### 先改正减伤表（你给的新表）

就是**每级 +20%** ✓，写成 `0.20F * level` ✓（不写八个分支 —— "手写清单漏一项"这个项目已经出过两次事故 ✓）：

| 等级 | 减伤 | 实际受伤 | | 等级 | 减伤 | 实际受伤 |
|---|---|---|---|---|---|---|
| 0 | 0% | 100% | | IV | 80% | 20% |
| I | 20% | 80% | | **V** | **100%** | **0%** |
| II | 40% | 60% | | VI | 120% | 0% |
| III | 60% | 40% | | VII / VIII | 140% / 160% | 0% |

> **一个副作用要说清**：**V 级就已经 100% = 完全免疫** ✓ —— 所以你表里 V 就是免伤了 ✓，
> VI~VIII 只是"更保险" + VII 起带反弹 ✓。

### 三、阵眼支持第四种符

`TalismanKind` 加了 `STAND_IN` ✓ → 阵眼现在**五种**状态 ✓（空/乱神/飞剑/反重力/替身 ✓）。

- **装符 / 取符不用改代码** ✓ —— 本来就是通用的 ✓；
- **存读也不用改** ✓ —— `byName` 是遍历枚举的 ✓（不是手写清单 ✓）；
- 新增方块状态变体 ✓、方块模型 ✓、阵眼贴图（中间一个小草人 + 金色角点 ✓）。

### 四、替身阵的基础效果

每刻：装的是替身符 → **扫范围内的玩家** ✓ → 挂"替身"标记 ✓（每刻刷新 ✓，走出去就清 ✓）；
符被取走或阵碎掉时，把**这个阵眼发的**标记清干净 ✓；空的时候照旧只扫数量 ✓。

### 五、玩家数据

存在玩家的持久数据里 ✓（退出重进不丢 ✓）：替身标记 ✓、替身阵抗性标记 ✓、次数盾点数 ✓
—— **外加一个我自己加的字段：发标记的那个阵眼坐标** ✓。

> 为什么要加：光有布尔值**不知道是哪个阵眼** ✓；两个替身阵重叠时，碎错阵就荒唐了 ✓。

**同步**：模组的**第一个网络包** ✓ —— `NetworkRegistry.newSimpleChannel` +
`SimpleChannel.registerMessage` ✓（47.x 的 API ✓；`ChannelBuilder` 是 1.21 的 ✗，
这个版本根本不存在 ✓ —— 查了 forge 的 jar 才确认 ✓）。

### 六、替死触发

玩家受伤 → 有替身标记 → **这一下会致命** ✓，才：

免疫 ✓ / 回满血 ✓ / 挂抗性提升 VIII 30 秒 ✓ / 次数盾设 20 ✓ / 碎裂标记 ✓ /
§4 血红色动作栏 ✓ / 碎裂音效 + 金色粒子 ✓ / **阵眼清空、符消失不返还** ✓ / 清掉标记 ✓。
不致命 → 什么都不做，标记保留 ✓。一个阵法只能替死一次 ✓ —— 因为符没了就不再发标记 ✓。

### 七、次数盾

**最先判断** ✓（在减伤之前 ✓）：点数 > 0 → 玩家不掉血 ✓、点数 -4 ✓、攻击者照样吃反弹 ✓。
20 点 = 5 下 ✓，不自然衰减 ✓，所有伤害类型都吃 ✓（就一个受伤事件 ✓）。
画成快捷栏上方一条**额外血条** ✓，不占原版血量、不影响回血 ✓。

### 八、次数盾和抗性的关系

盾在 → 不掉血 + 攻击者吃反弹 ✓；**破盾那一下也反弹** ✓；破盾后 30 秒内靠替死时挂的
抗性提升 VIII 继续免疫 + 反弹 ✓；30 秒后恢复正常 ✓。

### 九、全屏碎裂图案 + 十、血红色动作栏

- **128×128** ✓ 碎裂盾牌 ✓（金色描边 + 血红填充 + 裂纹 + 中心爆点 ✓ 透明背景 ✓）
- 屏幕正中 ✓：0.25 秒内 1.5 → 1.0 ✓ → 保持 ✓ → 淡出 ✓，**总长 1.6 秒** ✓
- 动作栏：`§4替身阵已碎裂，你逃过一劫` ✓ —— 原版动作栏只显示约 2 秒 ✗，
  所以**每刻重发、持续 80 刻（4 秒）** ✓ 满足你要的 3~5 秒 ✓
- HUD 是**显式注册** ✓（注解扫描静默失败，反重力倒立那次已经吃过亏 ✓）

### 反弹无视无敌帧

`attacker.invulnerableTime = 0` 再 `hurt()` ✓ —— 和飞剑十连斩同一个坑 ✓：
不清的话同一刻的第二段伤害会被 10 刻无敌窗口吞掉 ✓，表现是"有时反弹有时不反弹" ✓。

### 验证

打包自检 **714 项全过** ✓，新增覆盖：五种状态 ✓、方块状态变体 ✓、阵眼模型与贴图 ✓、
三条语言 key ✓、玩家数据字段与 20/4/VIII 常量 ✓、网络通道 ✓、
**受伤处理顺序**（先盾 → 再减伤 → 再替死 ✓）、`shatterEye`/`spawnShatterParticles`/`actionBarMessage` ✓、
HUD 注册 ✓、碎裂贴图 128×128 + RGBA ✓。

**反重力回归 PASS** ✓（7/7 ✓，阵眼改了必须还过 ✓）；血泊 PASS ✓；出厂冒烟 PASS ✓、无资源/注册错误 ✓。

### 只有你能确认的（六～十全都跑在玩家身上）

专用服务器没有玩家 ✗，所以这一段这里**没法跑** ✓。进游戏后：

1. 合阵眼 + 替身符，把符装进阵眼 ✓
2. 站进阵法（右键阵眼应该能看到玩家数量 ✓）
3. 让僵尸把你打到残血，再接一下**致命伤** ✓ → 你应该满血活下来 ✓、看到碎裂图案 ✓、
   血红色动作栏 ✓、金色粒子 ✓，**阵眼变成空的** ✓
4. 盾在的时候连挨五下 ✓ → 那条额外血条每次 -4 ✓；第六下开始掉血 ✓（但 VIII 还剩 30 秒 ✓）

---

## 第三十八部分：替身阵（十一～十四 + 注意事项）

### 十一、次数盾绿色血条

位置不是猜的 ✓，是照**原版血量条的常量**算的 ✓：1.20.1 里血量条左上角在
`x = width/2 - 91, y = height - 39` ✓，宽 81、高 9 ✓ → 绿色条**同宽同起点** ✓，
底边在血量条上方 **1 像素** ✓，高度 **4**（约一半 ✓）。

5 格 × 4 点 ✓：亮绿 `#40FF40` ✓ / 已消耗暗灰 `#CC2A2A2A` ✓ / 深绿描边 `#208020` ✓ /
格间 1 像素空隙 ✓ / 格内左上角高光 ✓ → **12 点正好是 3 格满 + 1 格半亮 + 1 格暗** ✓（你举的例子 ✓）。

> **和其他模组冲突时**：改 `StandInOverlay.SHIELD_BAR_OFFSET_Y` **一个数**就行 ✓。

**同步**：服务端每次变化都推 ✓（之前就有 ✓）；**新加了登录时补推一次** ✓ ——
不然退出重进客户端会显示 0 点 ✓（你明确要求"重新从服务端拉一次" ✓）。

### 十二、粒子

| 时机 | 粒子 | 位置 |
|---|---|---|
| 有替身标记 | 淡金色 ✓（每 10 刻 2 颗 ✓） | 玩家身上 ✓ |
| 阵法碎裂 | 一圈金色 ✓（60 颗 ✓） | **阵眼位置** ✓ |
| 次数盾破碎 | 绿色 ✓（24 颗 ✓） | 玩家身上 ✓ |

**只发给 32 格内的玩家** ✓ —— 逐个玩家发 ✓，不用维度广播 ✓。

> 一次说明：绿色血条是 **HUD** 元素 ✗，世界里没有坐标 ✓ → 所以破盾的绿粒子放在**玩家身上** ✓。

### 十三、音效

| 时机 | 声音 |
|---|---|
| 装符 | 翻书页 ✓（本来就有的轻微音效 ✓） |
| 阵法碎裂 | 玻璃碎裂 ✓ |
| 替死 | **监守者心跳**（低沉 ✓）+ 玻璃碎裂 ✓ |
| 次数盾破碎 | **铁砧落地**（金属感 ✓） |
| 反弹命中 | 玩家强力攻击 ✓（在攻击者身上播 ✓） |

### 十四、图鉴第四条

数据驱动的 `ALL` 列表加一项 ✓，文案**照你写的** ✓：

- **已见闻**：替身阵 / 半径 10 格 / 替玩家挡一次致命伤害 ✓
- **已精通**：半径 10 格，下方 10 格，上方无限；替死时回满血、获得自制抗性提升 VIII 30 秒、
  获得 20 点次数盾；次数盾每被攻击减 4 点，5 次打破；抗性 VII 以上反弹 80% 伤害；
  替死一次后阵法碎裂；阵基 ×4、阵眼 ×1、替身符 ×2；手持替身符右键阵眼 ✓

### 十六、注意事项（对着构建逐条核过）

- 次数盾是**额外血条** ✓，不碰原版血量值、不影响回血 ✓
- 只在替死触发时获得 ✓，不是永久效果 ✓
- 一个阵法只能替死一次 ✓（阵眼被清空就不再发标记 ✓）
- VIII 和次数盾是**替死的奖励** ✓，不是阵法常驻 ✓
- 反弹只在 VII 以上生效期间（也就是替死后那 30 秒）✓
- 绿色血条有**明确的偏移常量** ✓，方便避开别的模组界面 ✓

### 验证

打包自检 **734 项全过** ✓，新增覆盖：四个颜色常量 + 两个格子常量 ✓、原版血量条位置常量 ✓、
`drawShieldBar` ✓、`sendNearby`/`spawnNearby`（粒子只发附近 ✓）、`PlayerLoggedInEvent`（重进补同步 ✓）、
图鉴六条文案**逐字核对** ✓、图鉴条目与图标 ✓。出厂冒烟 PASS ✓、无 java 残留 ✓。

> 十一～十四全部是**客户端画面 / 玩家身上**的东西 ✓，专用服务器两样都没有 ✗ ——
> 所以这一段只能你进游戏看 ✓。

---

## 第三十九部分：绿条改成"顶替原版血条" + VIII 不消耗 + 碎裂图案重做

### 一、那个视频我没能打开（说清楚）

三种办法都试了 ✓：

| 办法 | 结果 |
|---|---|
| ffmpeg | 机器和捆绑运行时里**都没有** ✗ |
| Python 解码器（cv2 / av / imageio / moviepy / decord） | **全都没装** ✗ |
| Windows 自带 MediaPlayer（WPF + STA） | 能打开、但报 **`HRESULT 0xC00D11D2`** ✗ —— 这台机器的 Media Foundation **缺 H.264 解码器**（精简版 Windows 常见 ✓）；文件本身没问题 ✓（`moov` 里的编码是 `avc1` ✓） |

**所以我没看过那段视频** ✗，也不会说"我照它做了" ✓。
我按"太小 / 不够丝滑"这两个症状该动的方向重做了 ✓，同时**请你给我一张静帧 PNG**（PNG 我能看 ✓）或一句话描述 ✓
（是淡入淡出？旋转？缩放？有没有全屏闪白？）✓ —— 我想对着参考做 ✓，不想猜第二次 ✗。

### 二、绿条的意思我理解错了，现在改成"顶替原版血条"

原来我画的是**血条上方另加一条** ✗ —— 你要的是**血条本身变绿** ✓。现在：

```java
RenderGuiOverlayEvent.Pre  +  PLAYER_HEALTH  →  setCanceled(true)   // 原版血条不画
                                            →  在同一块矩形里画绿色 5 格
```

**同 x（`width/2 - 91`）、同 y（`height - 39`）、同宽（81）、同高（9）** ✓ ——
不是在上方 ✓、不是在旁边 ✓，是**占它原来的位置** ✓。

这也顺带解释了"不会挡住血量条/饥饿条/经验条/快捷栏/动作栏" ✓ ——
它**只占血量条那一块** ✓，别处一格都不碰 ✓。`SHIELD_BAR_OFFSET_Y` 保留 ✓，撞了别的模组改一个数 ✓。

格子：5 × 4 点 ✓、亮绿 `#40FF40` ✓、已消耗暗灰 ✓、1 像素空隙 ✓、`#208020` 描边 ✓、左上高光 ✓、
按剩余点数比例填充 ✓（12 点 = 3 满 + 1 半 + 1 暗 ✓）。

### 三、VIII 期间不消耗次数盾

进入次数盾分支后先看抗性等级 ✓：**有 VIII（amplifier ≥ 7）就吸收但不扣点** ✓
—— 对应你说的"在拥有抗性提升 VIII 期间绿色血条不会被消耗" ✓。

### 四、碎裂图案重做

| 问题 | 改法 |
|---|---|
| 太小 | 尺寸改成**屏幕高度的 0.55 倍** ✓（不再固定 128 像素 ✓，大屏也够大 ✓） |
| 糊 | 贴图重画成 **256×256** ✓（128 放大必糊 ✓） |
| 不够丝滑 | 运动改成**连续曲线** ✓：0.35 秒 easeOutCubic 从 0.7 冲到 **1.5** ✓ → 再平滑回落到 1.0 ✓；另加 0.15 秒淡入 + 0.3 秒淡出 ✓；总长 **1.8 秒** ✓ |

> 中间还踩了一次 ✗：第一版 256 图里**把外光晕画在了盾上面** ✓、而且**裂纹用了暗红**（暗红底上看不见 ✓）
> → 整张图糊成一团 ✓。改成"光晕画在底下 + 裂纹用金白" ✓ 才清楚 ✓。

### 五、验证

打包自检 **738 项全过** ✓（新增/更新）：

```text
[ok] 碎裂图案是 256x256（放大到 1.5 倍仍然清楚，128 会糊）
[ok] **顶替**原版血量条的渲染（不是另画一条在它上面）
[ok] 取消原版血条之后就画绿色 5 格
[ok] 图案大小按屏幕高度算（大屏不会显得太小）
[ok] 抗性提升 VIII 期间**不消耗**次数盾（用户要求）
```

出厂冒烟 PASS ✓（客户端改动依然不炸专用服务端 ✓）。

---

## 第四十部分：绿心 —— "血条上方另加一条" + "原版心形改成绿色"

![上：我们的绿心（3 满 + 1 半 + 1 空） 下：原版红心](tools/out/shield_hearts_preview.png)

### 我前两次都理解偏了

| 我做的 | 你要的 |
|---|---|
| 第一次：血条上方一条**方块格子** ✗ | 上方一条 ✓（但形状不对 ✗） |
| 第二次：**盖掉**原版血条 ✗ | **不要盖** ✓ |

现在：**原版血条一动不动** ✓ + 它**上方**多一条 ✓ + 形状是**原版心形** ✓ + **红色改成绿色** ✓。

### 关键的一次踩坑：`setShaderColor` 染不了红→绿

我第一版想省事：直接拿原版 `icons.png` 的心形 ✓ 加
`RenderSystem.setShaderColor(0.30, 1.00, 0.35, 1.0)` 染绿 ✗。

**但那是乘法** ✗：红心像素 `(1,0,0) × (0.30,1.00,0.35) = (0.30,0,0)` ✓ —— **只会变暗红，不会变绿** ✗✓。

我把它验证了一遍 ✓（从原版 `client-extra.jar` 里抠出三帧、按这个乘法算一遍 ✓ 看结果 ✓）：
确实是暗红 ✓（见 `tools/out/heart_frames.png` ✓）。

### 所以改成"烘一张自己的绿心贴图"

`assets/fulu/textures/gui/shield_hearts.png` ✓，**27×9** ✓，三帧 9×9：

| u | 内容 |
|---|---|
| 0 | 空壳（偏暗绿，看得出"格子空着" ✓） |
| 9 | 满心（绿 ✓） |
| 18 | 半心（绿 ✓） |

生成脚本 `tools/gen_shield_hearts.py` ✓：从 `client-extra.jar` 里的原版 `icons.png` 抠出三帧 ✓
（坐标 `16 / 52 / 61, v=0` ✓ —— **抠出来看过** ✓，不是凭记忆 ✓），
**保留每个像素的亮度、把色相换成绿色** ✓ → 原版心形的形状、明暗、白高光、半心的斜切**全都还在** ✓。

### 画在哪

- `x` = 原版血量条的左边缘（`width/2 - 91` ✓）
- `y` = 血量条顶 − 心高 − **2 像素** ✓（正上方 ✓）
- 5 颗心、每颗间距 **8 像素** ✓（原版就是 9 宽压 1 像素 ✓）
- **4 点一颗心**：20 点 = 5 满 ✓；12 点 = 3 满 ✓；14 点 = 3 满 + 1 半 ✓

**原版血量条既不取消、也不修改** ✓。撞别的模组界面时改 `SHIELD_BAR_OFFSET_Y` ✓。

### 验证

打包自检 **738 项全过** ✓：

```text
[ok] 用的是自己烘的绿色心形贴图（染色做不到）
[ok] 空壳 / 满 / 半 三帧都画（和原版血条同一套叠法）
[ok] 5 格，每格 4 点
[ok] 按原版血条的位置算：画在它**上方**
[ok] 心与心之间 8 像素（原版间距）
[ok] **不**取消原版血条（它照常显示）
[ok] 绿色心形贴图已打包 / 27x9 / RGBA
```

出厂冒烟 PASS ✓。

### 还开着的

**碎裂图案那段视频** ✗ —— 这台机器解不了（Media Foundation 缺 H.264 ✓，报 `0xC00D11D2` ✓），
所以我只能按设计改进 ✓，不能照抄 ✓。**给我一张静帧 PNG 我就能对上** ✓。

---

## 第四十一部分：绿心不显示 —— 先别改图，先让日志说话

### 最可能的原因（在动代码之前）

**次数盾点数只有"替死触发之后"才存在** ✓ —— 它不是一个常驻效果 ✓（这一点你自己在注意事项里写过 ✓）。
而绿心只在**客户端点数 > 0** 时画 ✓。所以：**如果替死没触发过，本来就该什么都不显示** ✓。

替死那一瞬间也会放全屏碎裂图案 ✓ —— 所以一个问题是关键 ✓：

> **你测的时候，全屏碎裂图案出现了吗？**

- **出现了** ✓ → 数据到了客户端 ✓ → 问题在"画的位置/贴图" ✓；
- **没出现** ✗ → 数据压根没到 ✗ → 问题在网络/客户端 ✓。

### 加了两行日志（默认开，不刷屏）

```text
[standin-sync] received: shield=20 standIn=false resistance=true shatter=true
[standin-hud] drawing green hearts: points=20 full=5 half=false at x=370 y=190 (screen 854x480)
[standin-hud] nothing to draw: shield points = 0
```

| 日志 | 含义 |
|---|---|
| `[standin-sync]` | **收到了同步包**（一个包一行 ✓） |
| `[standin-hud] drawing ...` | **确实在画**，而且把算出来的 x/y 一起打出来 ✓ —— "画了但在屏幕外/被挡"和"没画"就能分开 ✓ |
| `[standin-hud] nothing to draw` | 覆盖层**在跑** ✓，只是点数是 0 ✓ |

### 三种结果分别意味着什么

| 现象 | 结论 |
|---|---|
| `sync` + `drawing` 都有，但看不到心 | 数据到了、画也调了 ✓ → **位置或贴图**的问题 ✓（日志里有 x/y ✓，我照着挪 ✓） |
| 只有 `sync` + `nothing to draw` | 覆盖层在跑 ✓，但它拿到的点数是 0 ✗ → 同步/序列化问题 ✗ |
| **一行 `[standin-*]` 都没有** | 客户端**什么都没收到** ✗ → 最可能是**跑的 jar 不是这一版** ✗，或者通道没注册 ✓ |

### 验证

打包自检 **738 项全过** ✓；出厂冒烟 PASS ✓。客户端那点东西无头测不了 ✓ —— 所以才有日志 ✓。

### 要先确认的一件事

**替死到底触发过没有？** ✓ 如果没触发过 ✓，那这就是全部答案 ✓ ——
5 颗绿心只在**挡下致命一击那一刻**出现 ✓，和全屏碎裂图案同一个瞬间 ✓。

如果你想**不死也能看到** ✓（方便调界面 ✓），说一声 ✓，我加一个调试命令直接设点数 ✓。

---

## 第四十二部分：`????` 是我自己的源码坏了 + 绿心的心跳日志

### 一、截图里那串 `?????? = 20?...` 不是字体问题，是真 bug

**编译后的类里就没有中文** ✗ —— 所以客户端只能画成问号 ✓。
根因：我之前有些补丁脚本是用 PowerShell 的 `Set-Content -Encoding ASCII` 写的 ✗
—— **中文在 Python 读到之前就已经变成问号** ✓，于是插进去的注释**和字符串**本身就是坏的 ✓。

一共 **10 行、5 个文件** ✗，其中两行是**玩家真能看到**的：

```java
source.sendFailure(Component.literal("这条命令要在游戏内由一个玩家执行"));
source.sendSuccess(() -> Component.literal("次数盾点数 = " + points + "（4 点一颗心：满 20 点是 5 颗满心）"), false);
```

修好之后**不查源码、查编译产物** ✓（查源码只能证明源码对 ✓）：

```text
CodexCommand.class            次数盾点数                 OK
CodexCommand.class            要在游戏内                  OK
PlayerDamageHandler.class     替身阵已碎裂，你逃过一劫     OK
StandInData.class             FuluStandIn               OK
```

> 顺带一句：我第一次写这个核对脚本时，它**自己**也是 ASCII 写坏的 ✗ ——
> 拿着 `?????` 去搜 ✓，三个全报 MISSING ✗ —— **正是它要查的那个 bug** ✓。
> 这个坑在这个项目里已经踩到第三次了 ✓，所以现在补丁脚本一律用编辑器写 ✓。

> **好消息**：这给了你一个**一秒验证新 jar** 的办法 ✓ ——
> `/fulu shield 20` 如果回的是**正常中文** ✓，说明跑的就是这一版 ✓。

### 二、绿心还是没出现（这条还没定案）

`/fulu shield 20` 明显**执行了** ✓（有回话 ✓）→ 点数设上了 ✓、同步包也发了 ✓。
但绿心没画 ✗。我不猜 ✓ —— 覆盖层里加了一行**心跳日志** ✓（大约每秒一行 ✓）：

```text
[standin-hud] alive: shield=20 (frame 854x480)
```

三种读法 ✓，各自指向不同的修法：

| 日志 | 结论 |
|---|---|
| `alive: shield=0` | 同步**没到客户端** ✗ —— 服务端设了、客户端还是 0 |
| `alive: shield=20` 但没有绿心 | **画了但看不见** ✗ —— 贴图或位置的问题（这条日志带屏幕尺寸 ✓，真正画的时候还会打 x/y ✓） |
| **完全没有** `alive` | 覆盖层**没在跑** ✗ —— 问题在客户端注册，不在画 |

### 三、请给我这些行

```mcfunction
/fulu shield 20
```

然后打开 `logs/latest.log`，搜 **`standin`** ✓，把相关几行发我 ✓（一般就三五行 ✓）。
有了它下一步就是机械操作 ✓，不用再猜 ✓。

### 四、验证

打包自检 **738 项全过** ✓；出厂冒烟 PASS ✓。
编译产物里的中文字符串单独核对过 ✓（jar 自检不方便断言 CJK 内容 ✓）。

---

## 第四十三部分：以后构建完直接部署到「阵法师」实例

### 部署脚本 `tools/deploy.ps1`

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1
```

它做四件事 ✓：

1. 取 `build/libs` 里**最新**的 `fulu-*.jar` ✓；
2. **`java` 还活着就拒绝部署** ✓（半个 jar 比旧 jar 更糟 ✓）；
3. 旧版备份到**实例根目录** ✓（`fulu-backup-<时间戳>.jar` ✓）——
   **不能放 `mods/` 里** ✗，那会被当成**第二个**模组加载 ✓；
4. 复制后**比对大小和 SHA-256** ✓，不一致就报错 ✓（不假装成功 ✓）。

### 这次部署的结果

| 项 | 值 |
|---|---|
| 源 | `build/libs/fulu-1.0.0.jar`，146927 字节，13:23 |
| 目标 | `...\versions\阵法师\mods\fulu-1.0.0.jar` |
| 校验 | 大小相同 ✓、SHA-256 相同 ✓（`D79201A259EDDC67...`） |
| 旧版备份 | `...\versions\阵法师\fulu-backup-20261017-132341.jar`（在实例根目录 ✓ 不在 mods/ ✓） |

实例 `mods` 里共 13 个 jar ✓，fulu 是其中一个 ✓；`.minecraft\mods` 不存在 ✓，所以没有第二处要同步 ✓。

> 脚本**当场证明了它的价值** ✓：第二次运行时因为游戏开着 ✓
> 直接报 `游戏还在运行（java pid 20608），先关掉再部署` ✓ —— 而不是把 jar 复制一半 ✓。

### 写这个脚本时又踩的两个 PowerShell 坑

1. **`.ps1` 没有 BOM 时 PS 5.1 按 ANSI 读** ✗ → 中文注释变乱码 ✓ → **乱码吃掉了引号** ✓ →
   解析失败 ✓（和 `verify_tick.ps1` 一模一样 ✓）。现在写成 **UTF-8 BOM + CRLF** ✓。
2. **`.ps1` 顶上放 Python 风格的三引号文档** ✗ → PowerShell 只把它当**字符串** ✓ →
   `param(...)` 就不是第一条语句了 ✓ → 报 `param is not recognized` ✓。改成 `#` 注释 ✓。

### 你在游戏里这样确认

```mcfunction
/fulu shield 20
```

**一次同时验证两件事** ✓：

- 回话是**正常中文**（`次数盾点数 = 20 ...`）✓ → **说明跑的就是新版** ✓
  （旧版这里会显示 `?????? = 20` ✓）
- 红心上方出现 **5 颗绿心** ✓

---

## 第四十四部分：10 颗绿心 + 阵法消失后清理负面效果

### 一、绿心：5 颗 → 10 颗，和原版血条对齐

```
CELLS = 10, POINTS_PER_CELL = 2      // 20 点 = 10 颗，每颗 2 点
```

- **半颗**：某颗还剩 1 点就画半心 ✓
- **对齐**：同一个左边缘（`width/2 - 91`）✓、**同样 8 像素间距** ✓（原版的间距 ✓）→
  10 颗跨度 **80** 像素 ✓，原版血条是 **81** ✓ → 正好对上 ✓，不再缩在左边半截 ✓

> 之前 5 颗 × 4 点只有 40 像素宽 ✓ —— 那看起来就像"另画的一条小条" ✗，
> 而不是"血条" ✓。这就是为什么改 10 颗之后才像原版 ✓。

### 二、创造模式

**我的覆盖层不检查游戏模式** ✓ —— 只要点数 > 0 就画 ✓。
创造模式里消失的是**原版血条** ✓（vanilla 行为 ✓），我没动它 ✓。

所以 `/fulu shield 20` 在创造模式里**也应该**出现 10 颗绿心 ✓。
如果还是没有 ✓，日志每秒会打一行 `[standin-hud] alive: shield=...` ✓：

| 日志 | 结论 |
|---|---|
| `alive: shield=0` | 命令没到服务端（或跑的不是这一版） |
| `alive: shield>0` | 在画 ✓ → 又是位置/贴图 ✓（改点数时会打出 x/y ✓） |

### 三、真 bug：阵法消失后负面效果还在（已修）

**原因** ✓：反重力标记的时长是 **`-1`（无限）** ✓ —— 因为你要求"阵法内时间无限" ✓。
而唯一会缩短它的，是**每刻**跑的"离场清理" ✓ —— **阵眼一旦没了，每刻就不跑了** ✗ →
无限时长的标记**没有任何东西去回收** ✗ → 阵眼碎了，怪物却永远倒着、永远不能走 ✓。
内讧标记是同一个形状 ✓。

**修法** ✓：`FormationEyeBlockEntity.setRemoved()` 里调 `clearMarksAround()` ✓ ——
把一片区域内怪物的**反重力/内讧**摘掉 ✓、把它们身上的**天花板值**也清掉 ✓、
再把**这个阵眼给过的玩家替身标记**清掉 ✓。

> 用 `setRemoved()` 而不是 `onRemove()` ✓：前者是"这个方块实体不存在了"的**唯一出口** ✓ ——
> 打掉 ✓、被替换 ✓、区块卸载 ✓ 都走它 ✓。

两个阵法重叠也不用担心清错 ✓：另一个阵眼的每刻会把该给的标记**重新挂上** ✓。

### 四、验证与部署

- 构建 OK ✓、打包自检 **738 项全过** ✓、出厂冒烟 PASS ✓
- 编译产物核对 ✓：`HEART_SHEET_W`（9 参数 blit 那个修复 ✓）、`clearMarksAround` ✓；
  源码里两个常量确实是 **10 / 2** ✓
  （`setRemoved` 在重混淆的 jar 里是 SRG 名 ✓，字节检查会报 MISSING ✗ —— 这是老坑 ✓，不是方法没写 ✓）
- **已部署** ✓：147190 字节 → `...\versions\阵法师\mods\fulu-1.0.0.jar` ✓，
  SHA-256 校验通过 ✓，旧版备份到实例根目录 ✓

### 五、你试的时候

```mcfunction
/fulu shield 20    -> 红心上方 10 颗绿心
/fulu shield 1     -> 一颗半心
/fulu shield 0     -> 什么都没有
```

清理那条这样验 ✓：反重力符装进阵眼 ✓、让僵尸站在里面 ✓、**打掉阵眼** ✓ →
僵尸应该**站直、重新开始走** ✓。

---

## 第四十五部分：第五个阵法 —— 嫁祸阵

![嫁祸符（两个对准对方的箭头）与阵眼第五套外观](tools/out/shift_blame_preview.png)

### 一～三：嫁祸符 + 阵眼第五态

```text
空   符纸   空
腐肉 兽血   蜘蛛眼        →  fulu:shift_blame_talisman ×1
空   朱砂   空
```

有序合成 ✓（`pattern: [" P ", "RBS", " C "]` ✓）。贴图是**两个箭头对准彼此** ✓（嫁祸 = 伤害对流 ✓）。

`TalismanKind` 加了 `SHIFT_BLAME("shift_blame")` ✓ → 阵眼现在 **6 种外观** ✓（空 + 5 种符 ✓）。
**装符/取符不用改代码** ✓（本来就是通用的 ✓）；**存读也不用** ✓（`byName` 遍历枚举 ✓）。

### 四：每刻

`case SHIFT_BLAME` → 给范围内**所有 `LivingEntity`** 挂"嫁祸"标记 ✓ ——
**怪物和玩家一视同仁** ✓（这是你明确要的风险 ✓）；盔甲架和旁观者排除 ✓。

### 五：伤害转移（核心）

检查顺序 ✓：

| 步骤 | 规则 |
|---|---|
| 1 | 受伤者身上有嫁祸标记 |
| 2 | **BOSS 免疫**（末影龙、凋灵）—— 我选的策略 |
| 3 | **只转移生物攻击**：没有攻击者的（摔、岩浆、火、虚空、`/kill`）不转 |
| 4 | **每刻每只生物最多被转一次**（UUID 集合，按 gameTime 清空）→ 不会 A→B→C 链式传递 |
| 5 | 10 格内随机挑**另一只**带标记的；没有就不转，正常受伤 |

转移 50%（常量 `TRANSFER_RATIO` ✓）：

- 被转移者**先清 `invulnerableTime`** ✓（你要求不吃无敌帧 ✓）；
- 再 `hurt(source, half)` ✓ → **护甲会减免** ✓（你要求 ✓）；
- 静态门闩 `TRANSFERRING`（`try/finally` 复位 ✓）挡住内层那次再转移 ✓
  —— 事件在服务端线程**同步**触发，一个 boolean 就够 ✓。

### 六～七：粒子与音效

- **紫色魂线**从原目标飞向被转移者 ✓：用 `count=0` 的 `sendParticles` ✓ ——
  这时 `dx,dy,dz` 是**速度**而不是散布 ✓ → **一个包画一条流动的线** ✓，不用每格发包 ✓；
- 被转移者身上冒**红色伤害粒子** ✓；
- 只发 32 格内玩家 ✓；
- 音效 `SOUL_ESCAPE` ✓（低沉的"转移"感 ✓）；被转移者受伤用原版自己的音效 ✓。

### 八 / 十二：两个我替你定的事

1. **BOSS 免疫转移** ✓（你给了两个选项 ✓）—— 理由："打 BOSS 结果旁边牛掉血"很难解释 ✓，
   而"BOSS 太大、祸甩不过去"很好解释 ✓；
2. **标记是普通状态效果、3 秒、每刻刷新** ✓ —— **故意不用无限时长** ✗：
   上一个 bug（负面效果比阵眼活得久 ✓）就是因为无限时长的标记**依赖每刻刷新才会被回收** ✗。
   这个标记**自己会过期** ✓，阵眼被拆时 `clearMarksAround` 也会顺手清 ✓。

### 九：图鉴

第六条 ✓，文案照你写的 ✓（已见闻 / 已精通 / 材料 / 激活方式 ✓），图标用嫁祸符 ✓。

### 十一：和别的阵法配合

不需要代码 ✓ —— 每个阵眼独立 ✓，两个效果在身上就自动叠加 ✓：

| 组合 | 效果 |
|---|---|
| 嫁祸 + 乱神 | 怪内讧，伤害还互转 → 清场更快 |
| 嫁祸 + 飞剑 | 飞剑打的伤害一半转给另一只 |
| 嫁祸 + 替身 | 你被怪打，伤害转给怪；致命伤还有替身兜底 |
| 嫁祸 + 浮空 | 怪浮空不能走，伤害慢慢转死 |

### 验证

- 构建 OK ✓、打包自检 **738 项全过** ✓、出厂冒烟 PASS ✓、**无资源/注册错误** ✓
- 针对这一版的 **28 项专项核对全过** ✓：资源 ✓、配方图案与 5 个字母 ✓、6 种外观 ✓、
  枚举 ✓、效果注册 ✓、handler 里每一条限制（比例常量 / 只攻击伤害 / BOSS 免疫 / 防递归门闩 / 粒子线 ✓）、
  图鉴条目与语言 key ✓
- **已部署** ✓：154774 字节 → `...\versions\阵法师\mods\fulu-1.0.0.jar` ✓，SHA-256 校验 ✓，旧版备份 ✓

### 我没能无头验证的

**转移本身** ✗。它能测（`/damage` 带攻击者 ✓），但这一轮我没加新的测试模式 ✗
—— 核心路径用的就是别的测试模式已经在用的原语 ✓，其余限制都在上面**结构化断言**了 ✓。
**要端到端证明的话，我可以加一个 `-ShiftBlameTest`** ✓。

### 你可以这样试

1. 合嫁祸符 → 空手把符装进阵眼 ✓
2. 阵里放**两只僵尸** ✓（右键阵眼看数量 ✓）
3. 拿剑打其中一只 ✓

**预期**：两只各掉一半 ✓、紫色魂线从你打的那只飞向另一只 ✓、另一只冒红粒子 ✓、
一声低沉的灵魂音 ✓。然后试风险：你自己站进去被僵尸打 ✓ → 伤害转给旁边的僵尸 ✓；
**旁边没怪时你就全吃** ✓。

---

## 第四十六部分：金色半球屏障（让玩家一眼看出阵法范围）

### 形状

以阵眼为中心、**半径 10、高度 10** 的**上半球** ✓（下半埋地下不画 ✓）。
罩面用 **24 段**竖切 + **12 条经线** ✓ → 轮廓是圆的 ✓，不是多边形 ✓；底边单独一条更亮的圈 ✓。

### 外观（照你给的数）

| 层 | 颜色 | 透明度 |
|---|---|---|
| 罩面 | `#FFD060` | **0.24**（你给的区间 0.20~0.30 ✓，一个常量就能改 ✓） |
| 网格 | `#FFE080` | 0.50（每 **30 度**一条：12 条经线 + 3 条纬线 ✓） |
| 底圈 | 最亮的金 | 0.85，**画两条抬高一点的圈** → 看起来更粗 ✓ |
| 顶端 | 十字亮点 | 最后 15% 行程淡入 ✓ |

### 出现 / 消失

每个阵眼一个"水位" 0→1 ✓，**1 秒** ✓（消失时 1→0 ✓）。只画水位以下的部分 ✓ →
看起来就是**从地面长出来** ✓ / **从上往下缩回去** ✓，不闪现 ✓。

### 数据从哪来

**只读方块状态** ✓：阵眼的符是方块状态属性 ✓（`talisman=empty/chaos/sword/antigravity/stand_in/shift_blame` ✓）→
客户端本来就知道有没有符 ✓，**不需要任何网络包** ✓。屏障显示 ⟺ `talisman != empty` ✓（你的第七条 ✓）。

### 性能

- 只处理 **64 格**内的阵眼 ✓
- 阵眼列表**每 10 刻**重扫一次 ✓（不是每帧扫区块 ✓）
- 12 经线 / 3 纬线 / 24 段罩面 ✓ ≈ 900 三角形 + 400 线段 ✓，每屏障 2 次绘制调用 ✓
- **纯客户端** ✓：服务端完全不知道有这回事 ✓
- **不开深度写入** ✓ → 永不遮挡视线 ✓；不参与碰撞 ✓

### 交互（全是连续函数，不跳变）

- 越靠近越亮 ✓（中心最多 +45% ✓）
- 玩家在范围内时底圈**轻轻呼吸** ✓（正弦 ✓ ~1.4 秒周期 ✓）
- 两者都按距离每帧算 ✓ → 穿过边界是平滑过渡 ✓

### 一个值得记下来的实现坑

`RenderStateShard` 里的 `TRANSLUCENT_TRANSPARENCY` / `NO_CULL` / `COLOR_WRITE` 都是 **protected** ✗ ——
包外的类**没法**用它们拼自定义 `RenderType` ✓。我前两次编译就是这么挂的 ✗。

**修法**（社区常规 ✓，不是 mixin ✓、不改原版代码 ✓）：在 **`net.minecraft.client.renderer` 这个包下**放一个极小的助手类 ✓ ——
protected 成员**同包可见** ✓，于是能在那里定义屏障用的两个 `RenderType` ✓（罩面：三角形/不剔除/不写深度 ✓；线段：网格与底圈 ✓）。

### 验证

- 构建 OK ✓、打包自检 **738 项全过** ✓、出厂冒烟启动服务端**无异常、无注册错误** ✓、0 java 残留 ✓
- **客户端渲染本身无头测不了** ✗（没有帧缓冲、没有相机 ✓）：几何是球面公式 `r = sqrt(R² - y²)` ✓、
  状态机是纯时间驱动 ✓，"看起来对不对"要你进游戏看 ✓

### 部署状态：**待完成**

构建好了 ✓，但 `deploy.ps1` **拒绝执行** ✗ —— 你的游戏正开着 ✓（`java pid 16068` ✓），jar 被锁 ✓。

**关掉游戏**后，要么自己跑：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1
```

要么跟我说一声，我来跑 ✓。

### 这一轮**没做**的（你明确划到后面）

- **不同阵法不同颜色**（乱神紫 / 飞剑青 / 替身金 ✓）：你说先统一金色 ✓ —— 颜色常量已经集中在一处 ✓，将来按符分色只改那里 ✓
- **"碎裂时屏障快速碎裂消失"**：碎裂时阵眼变空 ✓ → 屏障按正常的 1 秒退场 ✓，碎裂粒子照常播 ✓。
  要做"快速碎裂"得让渲染器知道"符是被打没的" ✓，这轮没加 ✓。

---

## 第四十七部分：我把游戏搞崩了 —— split package

### 崩溃原文

```text
Exception in thread "main" java.lang.module.ResolutionException:
    Modules minecraft and fulu export package net.minecraft.client.renderer to module lionfishapi
    at java.base/java.lang.module.Resolver.resolveFail(Resolver.java:900)
    ...
    at cpw.mods.modlauncher.ModuleLayerHandler.buildLayer(ModuleLayerHandler.java:75)
```

**在任何模组代码跑之前就挂了** ✓ —— 是**模块解析器**拒绝建层 ✓。

### 原因（我的锅）

为了自定义 `RenderType`，我需要 `RenderStateShard` 里的
`TRANSLUCENT_TRANSPARENCY` / `NO_CULL` / `COLOR_WRITE` / `LEQUAL_DEPTH_TEST` ✓ ——
它们都是 **`protected`** ✗。protected 成员**同包可见** ✓，于是我把助手类放进了
`net.minecraft.client.renderer` 包 ✗ —— 那是我在**非模块化**年代学到的老套路 ✓。

**但 Forge 1.17+ 的游戏本身是 JPMS 模块（`minecraft`）** ✓，我的模组是另一个模块（`fulu`）✓。
**JPMS 禁止两个模块导出同一个包** ✗ —— 我在那个包里放了一个类 ✓，
就等于让 `fulu` 模块声称导出 `net.minecraft.client.renderer` ✗ → 游戏直接拒绝启动 ✓。

> 报错里点名 `lionfishapi` 是解析器碰巧先抱怨它 ✓ —— **真正造成 split package 的是我** ✓。

### 修法

**不再自定义 `RenderType`** ✓。屏障改成**直接设 GL 状态** ✓，一个 protected 成员都不用：

```java
RenderSystem.enableBlend();
RenderSystem.defaultBlendFunc();
RenderSystem.depthMask(false);                     // 不写深度 → 永不遮挡视线
RenderSystem.disableCull();
RenderSystem.setShader(GameRenderer::getPositionColorShader);
// … 写 POSITION_COLOR 顶点 …
BufferUploader.drawWithShader(bufferBuilder.end());
```

线段用 `GameRenderer::getRendertypeLinesShader` + `POSITION_COLOR_NORMAL` 格式 ✓。

**删掉**：`src/main/java/net/minecraft/client/renderer/FuluBarrierRenderType.java` 和
`client/render/FuluBarrierShaders.java`；空的 `net/minecraft` 目录也清了 ✓。

### 验证「根因确实没了」

`tools/check_no_split_package.py` ✓：

```text
net/minecraft/ 下的条目: 0        ← 一个都不能有
屏障渲染类在包里: True
已删除的非法类是否还在: False
```

然后：构建 OK ✓、出厂冒烟 PASS ✓、**已部署** ✓（162542 字节 ✓，SHA-256 校验 ✓）。

### 为什么我自己的冒烟测试**没抓住**这个 —— 这条比 bug 本身重要

**冒烟测试跑的是专用服务端** ✓。专用服务端**永远不加载客户端类** ✗ →
split package 根本不会出现 ✗ → 服务端开开心心起来了 ✓，而**客户端在模块层就死了** ✗✓。

所以：**"冒烟绿了"不等于"客户端能启动"** ✗。
对**纯客户端**的类，便宜的守卫就是上面的**查 jar** ✓（任何类都不许住在 `net.minecraft.*` 下 ✓），
而**客户端能不能启动**，唯一真正的证据是**启动一次客户端** ✓。
—— 我在**部署一个放进原版包的类之前**就该查一次 ✓；那也是我第一次往原版包里放类 ✓。

### 要记住的教训

> **在 Forge 1.17+ 里，绝对不要把自己的类放进 `net.minecraft.*` 包。**
> 为了一个 protected 常量不值得 —— 渲染需要的公开 API 都够用 ✓；
> 真不够，答案是 **mixin / access transformer**，不是**占包** ✓。

### 再试一次

**启动游戏** ✓ —— 能进主菜单就说明模块层没问题 ✓。
然后**给阵眼装任意一张符** ✓，金色半球应该从地面长出来 ✓。

---

## 第四十八部分：屏障扩展（做了一半，另一半说清为什么没做）

### 做了的（纯客户端视觉）

| 条目 | 做法 |
|---|---|
| 1 流动光效 | **16 个能量点**沿表面螺旋上升 ✓，alpha 中间亮两头淡（不在边缘突现 ✓） |
| 2 呼吸明暗 | 亮度按 **4 秒**周期起伏 ✓（0.85~1.00 倍 ✓） |
| 3 边缘加粗 | 底圈画**两条**抬高的圈 ✓，最亮 ✓（0.88 ✓） |
| 5 符文环绕 | **三层菱形**绕竖轴转 ✓，一圈 **12 秒** ✓ |
| 9 按阵法分色 | 乱神紫 / 飞剑青 / 浮空淡蓝 / 替身金 / 嫁祸暗红 ✓ —— **一张表五个颜色** ✓ |
| 12 靠近变亮 | 最多 **+45%** ✓ |
| 13 进入波纹 | 穿过边界时荡开一圈 ✓，0.6 秒淡出 ✓ |
| 21 叠加加深 | 另一个激活阵眼在 2×半径内 → alpha **×1.35** ✓ |
| 23~25 碎裂 | 阵眼变空的瞬间撒 **40 颗碎片** ✓ + `GLASS_BREAK` + `ANVIL_LAND` ✓（本地播放 ✓ 不发包 ✓） |

> 碎裂的触发点：**阵眼从"有符"变成"空"** ✓ —— 取走符 ✓ 或打掉阵眼 ✓ 都算 ✓。
> 客户端每 10 刻重扫一次 ✓，所以在重扫时发现这个转变 ✓。

### 做了的（服务端领域效果，`FormationFieldEffects`）

| 条目 | 做法 |
|---|---|
| 18 / 30 怪减速 | 范围内怪物 **缓慢 I** ✓，每刻刷新 ✓ |
| 29 玩家加速 | **速度 I** ✓，每刻刷新 ✓ |
| 31 夜视 | 夜视 ✓（400 刻 ✓） |
| 28 回血 | 低于满血时每 2 秒 **+1 HP** ✓（你说"看你要不要" ✓ —— 默认开 ✓，一行能关 ✓） |
| 20 进阵警报 | 怪物**第一次**进阵 → 阵内玩家收到动作栏提示 ✓ |
| 27 碎裂冲击波 | 阵眼被拆时把范围内怪物**向外推开 + 抬高** ✓ |

所有时长都是 **2 秒 + 每刻刷新** ✓，**从不用无限时长** ✗ —— 这个项目上一个 bug 就是无限标记比阵眼活得久 ✓。

### 没做的（每条都说清原因）

| 条目 | 为什么 |
|---|---|
| 6 裂纹 | 需要"每个阵眼被打了几下"的计数 ✓，而客户端只有收到同步才看得见 ✓ → 要加**新网络包** ✓。会做，但它动网络层 ✓，不放进"视觉打磨"这一轮 ✓ |
| 7 天气 | 水珠/雪花要按群系和天气每刻发粒子 ✓ → 你说过"粒子不要太多" ✗，这轮先不加重 ✓ |
| 14 右键查看屏障 | **字面上做不到** ✓：屏障不是方块、没有碰撞箱 ✓，**没有东西可以右键** ✗。可行的等价物是**手持图鉴右键阵眼** ✓ —— 说一声我加 ✓ |
| 26 碎裂慢动作 | **1.20.1 做不到** ✗。没有受支持的"把游戏 tick 变慢"的接口 ✓（`/tick rate` 从 1.20.4 才有 ✓）。逐实体伪造慢放要改每个实体的 tick ✓，不值得冒险 ✓ |
| 32 出阵提示 | **这一轮没做** ✗（在计划里 ✓，没进构建 ✓） |
| 33 阵内不刷怪 | 这个 Forge 版本里**没能定位到 `LivingSpawnEvent`** ✗，而我不愿意交付一个"没验证存在"的事件类 ✓ → 需要再查一次事件表 ✓ |

### 验证与部署

- 构建 OK ✓（三轮编译错误：switch 里非法语句 ✓、注解里写了三元表达式 ✓、未验证的刷怪事件 ✓ —— 全部**修掉或移除** ✓ 没有绕过去 ✓）
- 打包自检 **738 项全过** ✓
- 出厂冒烟：registry PASS ✓、verdict PASS ✓、0 java 残留 ✓
- **已部署** ✓：167882 字节 ✓，SHA-256 校验 ✓，旧版备份 ✓
- **客户端渲染无头测不了** ✗ —— "看起来对不对"要你进游戏看 ✓

### 上一轮的崩溃教训仍然生效

这一轮渲染器**只用** `RenderSystem` + 原版 `position_color` shader + `BufferUploader` ✓ ——
**没有自定义 `RenderType`** ✗、**没有碰原版包** ✗ ✓。
`check_no_split_package.py` 报 `net/minecraft/` 条目 **0** ✓。

---

## 一、最关键的一件事：名字要在整条链子上对齐

`formation_eye` 这个名字不是一个名字，是**一条链子上必须写成一模一样的同一个名字**。
任意一处错位，游戏都不崩，只是悄悄地不对：

```
物品（第一步）
  ModItems.register("talisman_paper")
     │
     ├─ assets/fulu/models/item/talisman_paper.json    少了 → 紫黑方块
     │     "layer0": "fulu:item/talisman_paper"
     ├─ assets/fulu/textures/item/talisman_paper.png   少了 → 紫黑格
     └─ assets/fulu/lang/zh_cn.json                    少了 → 名字显示成一串 key
           "item.fulu.talisman_paper": "符纸"

方块（第二步）
  ModBlocks.register("formation_eye")
     │
     ├─ assets/fulu/blockstates/formation_eye.json         少了 → 紫黑方块
     │     "model": "fulu:block/formation_eye"
     ├─ assets/fulu/models/block/formation_eye.json        少了 → 紫黑方块
     │     parent cube_all, "all": "fulu:block/formation_eye"
     ├─ assets/fulu/textures/block/formation_eye.png       少了 → 紫黑格
     │     ⚠️ 方块贴图放 textures/block/，物品贴图放 textures/item/，放错就找不到
     ├─ assets/fulu/models/item/formation_eye.json         少了 → 方块能放下去，
     │     parent = fulu:block/formation_eye                    但物品栏里是紫黑块
     ├─ data/fulu/loot_tables/blocks/formation_eye.json    少了 → 挖了什么都不掉
     └─ assets/fulu/lang/zh_cn.json                        少了 → 名字显示成一串 key
           "block.fulu.formation_eye": "阵眼"
```

三条同样静默的坑：

- **注册表要挂上事件总线**。只在 `ModItems` / `ModBlocks` 里写 `register(...)`、
  却忘了在主类里 `ModItems.register(modBus)`，编译完全正常，进游戏后 `/give` 什么都拿不到。
  所以本工程把 `register(modBus)` 统一放在主类里调用，登记表自己不碰总线。
- **方块 ≠ 方块物品**。`BLOCKS` 注册的是世界里那个方块，玩家背包放不下它；
  要拿在手里必须再登记一个 `BlockItem` 指回它，而且**名字必须完全相同**，
  否则会出现"能放下的拿不起来、能拿起来的放不下去"的错位。
- **方块物品的模型是 `models/item/<n>.json`，内容是 `parent = fulu:block/<n>`**。
  这样它在手上和物品栏里显示的是那个 3D 方块本身，而不是一张平面图。

---

## 二、贴图是怎么来的

### 符纸（物品贴图）

1. 先用搜索引擎找参考图。Bing 的图片结果在这台机器上返回的是一堆无关内容
   （"黄色"被当成颜色而不是符纸），换成 **百度图片** 就正常了 —— 脚本在
   [`tools/baidu_ref.py`](tools/baidu_ref.py)，下载到 `tools/ref/`（不进版本库）。
2. 把候选图拼成一张联络表看效果：[`tools/montage.py`](tools/montage.py) →
   `tools/out/ref_sheet.png`。
3. 挑中最像"符纸"的那张（黄纸长条 + 朱砂竖笔 + 小印章），**按它手绘 16×16 像素画**：
   [`tools/gen_talisman_paper.py`](tools/gen_talisman_paper.py)。
   照片直接压到 16×16 只会得到一团噪点，所以是按结构重画的，不是缩图。

成品是一张 **6×14 像素的黄纸长条**：左边一道深金边、上边被撕掉一角、下边压深，
中间一笔朱砂（短横 → 长横 → 两折 → 收笔），右下角一枚小朱印。
它是**空白符纸**，所以朱砂只画了一笔半。

### 阵眼（方块贴图）

[`tools/gen_formation_eye.py`](tools/gen_formation_eye.py) 生成，按魔法阵的老规矩叠层次：
**石板底 → 金环 → 四向辐条 → 朱砂内圈 → 中央的"眼"**。

- 石板是 3 色调的**确定性噪点**（用坐标算，不用随机数），所以每次生成结果都一样；
- 金环用 `半径 4.5~6.0` 的圆环带，四个正方向上的那几格用亮金色，形成"罗盘"感；
- 中景是一枚红色的眼：上排余烬亮橙、下排深色竖瞳；
- 方块贴图**不能有透明像素**，否则在世界里会看到窟窿，所以每一个像素都是不透明的。

配色和密度是照着你自己的修仙模组（`array_core` / `rune_block` / `spirit_vein_core`）调的，
放一起不违和。

想看它在世界里什么样，用 [`tools/make_block_preview.py`](tools/make_block_preview.py)
把它渲染成 **2:1 等轴测立方体**（顶面 100%、一个侧面 80%、另一个 60% 亮度），
就是本文顶部右边那张图。

---

## 三、构建与自检

```powershell
# 构建（自动用 tools\java-home.txt 里的 JDK 和 tools\.toolchain 里的 Gradle）
powershell -NoProfile -ExecutionPolicy Bypass -File tools\build.ps1 -Task build

# 打包自检：把 jar 拆开，把上面那条链子逐环核对一遍
python tools\verify_jar.py
```

`verify_jar.py` 检查的是编译通过**也照样可能出错**的东西：`mods.toml` 占位符、
语言 key、模型里的 `layer0` / `textures.all`、贴图是不是真的 16×16 RGBA、
掉落表掉的是不是这个方块、镐子标签里有没有它、方块实体是不是绑在阵眼上、
`getRenderShape` / `use` 有没有被覆盖、开关和冷却两个字段在不在。
当前结果：**72 项全部通过**。

> jar 里那些 `m_142194_` / `m_142354_` / `m_7514_` 是**正常的**：打包时 `reobfJar`
> 会把"覆盖原版方法"的方法名换成生产环境使用的 SRG 名（`newBlockEntity` /
> `getTicker` / `getRenderShape`），你自己写的方法名（比如 `tick`）不变。

### 真机验证：一条命令

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify_tick.ps1
```

它做的事：往 `run\verifyworld\` 里塞一个**临时数据包** → 起一个无头专用服务端 →
等它 `Done` → 让阵眼在那儿待 26 秒 → 停服 → 量日志 → 判定 PASS/FAIL → 删掉临时数据包。
数据包里有三样东西：

```mcfunction
# verify:place —— 启动 3 秒后跑
forceload add 0 0                       # 让区块保持 entity-ticking（见下面第 3 条）
setblock 0 -60 0 fulu:formation_eye

# verify:beat —— 一个和模组毫无关系的 5 秒时钟，每 5 秒自己再排一次
say [verify] heartbeat
schedule function verify:beat 5s

# verify:mote —— 粒子 id 的存在性冒烟测试
particle fulu:spirit_mote 0 -59 0 0 0 0 0 1
```

实测（第五步之后，检查的是"新放的阵眼什么都不做"）：

```text
 lines      : 0  block events (expect 0: the switch defaults to OFF)
 heartbeats : 5  datapack clock, proves the server was alive

 block events: none
 heartbeats  :
    [Server thread/INFO] [...MinecraftServer/]: [Not Secure] [Server] [verify] heartbeat
    [Server thread/INFO] [...MinecraftServer/]: [Not Secure] [Server] [verify] heartbeat
    [Server thread/INFO] [...MinecraftServer/]: [Not Secure] [Server] [verify] heartbeat
 registry   : PASS - no 'Failed to load function' in the log
 verdict    : PASS - silent as expected - the switch defaults to OFF (server was alive the whole time)

 [clean] removed the throw-away datapack
```

它一共核四件事（完整记录见 [`tools/out/server_tick_evidence.txt`](tools/out/server_tick_evidence.txt)）：

| 检查 | 怎么核 | 实测 |
|---|---|---|
| ① 默认关着：新放的阵眼什么都不做 | 数模组日志行，必须为 0 | **0 行** |
| ② 这个"沉默"是真的 | 数据包自己的 5 秒时钟在响 → 服务端活着、区块是 ticking 的 | 5 次心跳 |
| ③ 粒子 id 真的注册了 | 数据包里跑一句 `particle fulu:spirit_mote ...`，id 不存在这个函数会**加载失败**并写进日志 | 日志里没有 `Failed to load function` |
| ④ 客户端类没污染服务端 | 专用服务端能正常启动本身就说明 `Dist.CLIENT` 那道墙生效了 | 启动正常 |

> **右键本身在无头服务端里测不了**（没有玩家可点），所以它用**反汇编**来验：
> `tick()` 的头几条指令就是"读 `active` → 为 false 就 return"：
> ```text
> 0: aload_3
> 1: getfield  active:Z        <- 读开关
> 4: ifne      8
> 7: return                    <- 关着 -> 直接结束，后面什么都不做
> 8: aload_3
> 9: getfield  cooldown:I      <- 然后才轮到冷却
> ```
> 而 `FormationEyeBlock` 确实声明了 `use(BlockState, Level, BlockPos, Player,
> InteractionHand, BlockHitResult)`。真正"右键一下有没有反应"，需要你自己点一下。

> 第四步那份"每 5 秒一次"的实测（6 次触发、间隔 4.999/5.048/5.050/5.049/5.051 秒、
> 跟上数据包时钟）仍然有效 —— 那是在开关还没有、阵眼一直工作的状态下量的。
> 现在要看这个效果，先右键把阵法打开。

### 再加一个：真僵尸的行为测试（`-MobTest`）

上面那个只证明了"关着时什么都不做"，证明不了**扫描本身对不对**。所以还有第二个模式：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify_tick.ps1 -MobTest -Seconds 50
```

数据包分七个阶段，每段开头用 `say` 打一个标记，脚本按标记把日志切开、逐条核对。
这一步验的是**扫描范围的新形状**（水平 ±5、垂直 −10 到天空）：

| 阶段 | 数据包干了什么 | 应该发生什么 | 实测 |
|---|---|---|---|
| `ZERO` | 什么都不放 | 报一次 `0`（第一次扫描） | 0 |
| `THREE` | 在阵眼同一高度、±5 内刷 3 只僵尸 | 报 `3` | 3 |
| `DOWN10` | +1 只在**正下方 10 格** | 报 `4`（**在范围内**） | 4 |
| `DOWN11` | +1 只在**正下方 11 格** | **静默**（不在范围内） | 静默 |
| `HIGH` | +1 只在**上方 100 格** | 报 `5`（**上方无限**） | 5 |
| `X7` | +1 只在**侧面 7 格** | **静默**（水平仍然是 ±5） | 静默 |
| `CLEAR` | `kill` 掉全部 | 报 `0`（数量下降也播报） | 0 |

```text
  ZERO     expect 0       saw [0]  OK
  THREE    expect 3       saw [3]  OK
  DOWN10   expect 4       saw [4]  OK
  DOWN11   expect silent  saw []  OK
  HIGH     expect 5       saw [5]  OK
  X7       expect silent  saw []  OK
  CLEAR    expect 0       saw [0]  OK

 lines total: 5  (expect 5: one line per change, no repeats)
 verdict    : PASS
```

**"静默"是当成断言来验的**：`DOWN11` 和 `X7` 两个阶段的"允许出现的数字"是**空集合**，
所以只要那两只范围外的僵尸被算进来，测试就会红。原始日志：

```text
[19:26:05] [scan] monsters=0     <- 第一次扫描
[19:26:17] phase-THREE
[19:26:20] [scan] monsters=3
[19:26:23] phase-DOWN10
[19:26:25] [scan] monsters=4     <- 正下方 10 格：算
[19:26:29] phase-DOWN11          <- 正下方 11 格：整段没有任何输出
[19:26:35] phase-HIGH
[19:26:35] [scan] monsters=5     <- 上方 100 格：算
[19:26:41] phase-X7              <- 侧面 7 格：整段没有任何输出
[19:26:47] phase-CLEAR
[19:26:50] [scan] monsters=0     <- 数量掉下来也播报
```

> 测试用的阵眼放在 **y=100 的空中**，这样"下方 11 格"还是空气而不是基岩；
> 所有测试僵尸都带 `NoGravity` + `NoAI`，停在召唤的位置不动。
> 如果放在地面上测，下方 11 格会埋进基岩里，僵尸会窒息而死 —— 数字就乱了。

> ⚠️ 这个测试要求阵眼真的在跑，而无头服务端没有玩家（收不到动作栏内容）。
> 所以验证用的是**临时把 `active` 和 `DEBUG_SCAN_LOG` 都改成 `true`** 编出来的测试版：
> `active` 让阵法自己跑起来，`DEBUG_SCAN_LOG` 把播报的数字同时写进日志。
> 跑完两个开关都改回去重新编译，并核对出厂字节码：
> ```text
> 16: iconst_0 / 17: putfield  active:Z          <- false，出厂默认关
> 21: iconst_m1 / 22: putfield lastMonsterCount:I <- -1，第一次必报
> ```
> 出厂版本再跑一次标准验证：新放阵眼 26 秒 **0 行输出**，而数据包心跳响了 5 次。

### 再再加一个：真打起来了吗（`-FightTest`）

范围对了不等于"打架"真的发生了 —— `setTarget` 调没调用，和怪物有没有掉血是两件事。
所以还有第三个模式，直接**读怪物的血量**：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify_tick.ps1 -FightTest -Seconds 42
```

数据包搭一个 7×7、两层高的石墙围栏把阵眼围住（防它们走丢），然后：

| 时间 | 干了什么 | 应该发生什么 | 实测 |
|---|---|---|---|
| t=3 | 放阵眼 + 围栏 | —— | —— |
| t=12 | 只刷 **1 只**僵尸 | 没人可打，它必须一直是 20 血 | `ALONE-UNTOUCHED x1, ALONE-HURT x0` |
| t=24 | 再刷 **2 只**（共 3 只） | 三只应该互相打起来 | `FIGHTING x3` |
| 全程 | 记录每次"谁去打谁" | 攻击者和目标**不能是同一个实体** | `11 assignments, 0 self` |
| 全程 | 数每一轮的分配条数 | **每一轮 == 怪物数**（每只都分到目标） | `4 rounds: 3/3, 3/3, 3/3, 2/2` |

```text
  one zombie stays untouched                     OK  ALONE-UNTOUCHED x1, ALONE-HURT x0
  two zombies hurt each other                    OK  FIGHTING x3
  a monster never targets itself                 OK  11 assignments, 0 self
  EVERY monster gets a target each round         OK  4 rounds [assigned/total]: 3/3, 3/3, 3/3, 2/2

 the assignments the block made:
    [fight] 1/3 #1 -2, -60, 2 -> #2 -1, -60, -1
    [fight] 2/3 #2 -1, -60, -1 -> #3 0, -60, 2
    [fight] 3/3 #3 0, -60, 2 -> #2 -1, -60, -1

 verdict    : PASS - a lone monster was left alone; a pair actually fought
```

（最后一轮是 `2/2`，因为那时三只里已经死了一只。）

**"每一只都处理到"是这么验的**：调试日志每行都带 `第几个/共几只`（`1/3`、`2/3`、`3/3`）。
判定规则是"一轮必须从 1 开始、不跳号、最后正好等于 `total`"——
循环少访问一只，或者把某只漏掉，都会立刻被抓出来。

> 这里踩了两个坑：
> 1. **不能用 `[scan] monsters=` 当每轮的分隔符** —— 那行只在**数量变化**时才输出，
>    数量不变时一轮一轮的分配会全被并成一段（一度读成 `6/3`）。
> 2. **身份要用实体 ID，不能用坐标** —— 混战里两只怪经常挤在同一格里，
>    按坐标比会报出"打自己"的**假警报**（`#A -> #B` 但两者坐标相同）。
>    调试行现在写成 `#id 坐标 -> #id 坐标`，两者都看得到。

血量是这么读的：`execute store result score @s fuHp run data get entity @s Health`
（没挨打的僵尸是 20，19 及以下就是真的被打了）——
所以这一条验的是**实际伤害**，不是"我调用了 setTarget"。

日志里还能看到更有意思的一幕 —— 打架打了十几秒之后：

```text
[19:49:23] [Zombie] FIGHTING        <- 有一只掉血了
[19:49:24] [fight] -2, -60, -2 -> -1, -60, 0
[19:49:29] [scan] monsters=1        <- 数量掉回 1：有一只被活活打死了
```

原始记录见 [`tools/out/fight_evidence.txt`](tools/out/fight_evidence.txt)。

### 再再再加一个：存档真的写进磁盘了吗（`-PersistTest`）

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\verify_tick.ps1 -PersistTest -Seconds 50
```

**这一项不需要临时构建** —— 存档和阵法开关没关系，所以它跑的就是你要的那个 jar。
流程：放阵眼 → `data merge block` 把标记设成 1（这一步本身就走 `load()`）→
**`forceload remove` 强制区块卸载**（卸载时方块实体被写进存档）→ 再 `forceload add` 装回来
（方块实体由 `load()` 重建）→ 再读一次。

```text
  the flag can be written and read back          OK  SET-OK x1, SET-FAILED x0
  it survived a chunk unload + reload            OK  PERSIST-OK x1, PERSIST-LOST x0
  the value is physically in the region file     OK  1 hit(s), value byte 1: chunk 0,0
 verdict    : PASS - saved to NBT, read back after a chunk unload, and present on disk
```

**第三条完全不经过游戏**：[`tools/check_region.py`](tools/check_region.py) 直接打开
`run/verifyworld/region/r.0.0.mca`，用 zlib 解压那个区块，在字节里找 `HasTalisman`。
如果 `saveAdditional` 根本没跑，磁盘上就不会有这个键。

> 这条检查器本身出过**三个假警报**，每一个都长得像"模组坏了"，值得记：
> 1. .NET 的 `DeflateStream` 读这些区块**每块都返回 0 字节，还不抛异常**；
> 2. 这个失败被 `catch { continue }` 吞掉，于是"读取器坏了"和"磁盘上没这个键"看起来一模一样；
> 3. PowerShell 会把返回的集合**拆包**，只有一个命中时 `$hits.Count` 是 `$null`，
>    `-gt 0` 判定为假 —— 明明读到了却报"0 hits"。
>
> 现在：区域检查改用 Python 的 zlib（`tools/python-home.txt` 里配解释器路径，
> 和 `java-home.txt` 同一套约定），读取器出问题会**明确报"reading broken"**而不是沉默，
> 调用处用 `@(...)` 强制成数组。

关于这个脚本，有五个坑值得记一下（前几版分别栽在前几条，第四条是这一步才挖出来的）：

1. **不能用旧的 `latest.log` 判断"服务器起来了"**。Minecraft 每次启动会轮转日志，
   上一次运行的 `latest.log` 还躺在那里、里面也有 `Done (`，于是脚本会误判成已启动，
   在世界还没加载完时就把服务器杀掉 —— 表现就是"一行日志都没有"。所以它启动前先删掉
   旧日志，并要求新日志的写入时间晚于启动时刻。
2. **"行数 ÷ 秒数"是个坏指标**。5 秒一次的话，20 多秒的窗口只有 3~6 行，
   四舍五入一下就把 0.136 变成 0.1，直接误判 FAIL。现在改成量**相邻两行的时间差**
   （取中位数），样本少也准。
3. **命令放下的方块，区块会卸载** —— 最坑的一条。`setblock` 只让区块临时加载，
   如果 (0,0) 不在出生点常加载范围内，十几秒后区块卸载，方块实体就**停止 tick**，
   看起来和"冷却写坏了"一模一样。加上 `forceload add 0 0` 之后才 6 次全同步。
   > 这也意味着**前面几步的实测数字要打个折扣**：第三步那两次跑出的
   > "12 秒 288 行 / 20 秒 299 行"，后半段其实在测一个已经睡着的方块
   > （所以速率是 15.3 行/秒而不是 20）。结论（每刻一次、间隔 2 秒）仍然成立 ——
   > 那些间隔是在窗口前半段量的 —— 但当时的"速率"确实偏低。
4. **测试世界是复用的，旧的阵眼还在里面** —— 第七步才挖出来。世界文件夹不会自己清空：
   上一轮放在 (0,−60,0) 的阵眼会被重新加载，在"开关默认为开"的测试版里它**也会开始扫描**，
   于是两个阵眼把各自的数字交错写进日志，候选值互相污染，看起来像是"多算了一只"。
   现在脚本每轮先把世界整个删掉重建。
5. **`taskkill /T` 杀不干净**。ForgeGradle 会 fork 出真正的游戏 JVM，它不一定在
   `powershell → gradle` 这棵进程树里；只杀树的话服务端会一直跑下去，还占着世界锁。
   所以脚本额外停掉"启动之后才出现的 java.exe"（这是个启发式判断）。
   顺手一提：这也是它偶尔会报 `also stopped N forked java process(es)` 的原因。

### 或者自己进游戏看（第七步的完整手动流程）

1. **开发环境**：`powershell -NoProfile -ExecutionPolicy Bypass -File tools\build.ps1 -Task runClient`
   进游戏 → 创造模式"符箓"页签拿阵眼 → 放下 → 右键打开 → **看屏幕下方那条动作栏**。
2. **正式游戏**：把 `build\libs\fulu-1.0.0.jar` 丢进 `.minecraft\mods\` → 进游戏放下阵眼 → 右键。
   出厂版本**不往日志里写任何东西**（结果只进动作栏），所以直接用眼睛看屏幕。
3. **第七步的手动验证流程**：

   | 操作 | 应该看到什么 |
   |---|---|
   | 放下一个阵眼，什么都不做 | 屏幕和日志都**没反应**（默认关着） |
   | 右键一下 | 动作栏出现 `附近怪物：0 只`（第一次一定报一次） |
   | 站着不动等 10 秒 | 动作栏**不再刷新** —— 数量没变就不发 |
   | `/summon minecraft:zombie ~2 ~ ~2` 刷 3 只 | 动作栏变成 `附近怪物：3 只` |
   | 把僵尸 tp 到阵眼**正下方 11 格**（`~ ~-11 ~`） | 数字**不变**（下方超过 10 格不算） |
   | 把僵尸 tp 到**上方 40 格**（`~ ~40 ~`） | 数字**+1**（上方无限） |
   | `kill @e[type=zombie]` | 动作栏变成 `附近怪物：0 只`（数量变化就播报，涨跌都报） |
   | 走远到 17 格以外 | 动作栏**不再更新**（只发给 16 格内的玩家） |
   | 往旁边放一头牛（`/summon minecraft:cow ~ ~ ~`） | 数字**不变** —— 牛不是敌对怪物 |
   | 刷 **2 只以上**僵尸，看它们 | **插了符**时自己打起来；没符只是报数、不打 |
   | 只留 **1 只**僵尸（且已插符） | 怪不会打自己（少于 2 只时第二段跳过） |
   | 拿乱神符右键 / 空手右键 | 插符 / 取符，外观跟着变（第十步第三部分） |
   | 插上符的那一瞬间 | **阵法自动生效**，不用再点任何开关（第十步第七部分） |

   **关于"有没有符"（第十步）**：现在还没有"插符"的操作（那是下一步），
   所以只能用指令验证它存得住：
   ```mcfunction
   data merge block <阵眼坐标> {HasTalisman:1b}   # 设成"有符"
   data get block <阵眼坐标> HasTalisman          # 读出 1b
   ```
   然后走远到区块卸载、再回来，再 `data get` 一次 —— 还是 `1b`，说明存档生效了
   （如果只是内存里的变量，这时会变回 `0b`）。

   > 想看范围边界：水平方向在**左右 5 格**处刷一只僵尸（算）、**7 格**处再刷一只（不算）；
   > 垂直方向在**下方 10 格**刷一只（算）、**下方 11 格**再刷一只（不算）。

明显区分：动作栏完全不出现 = 登记表没挂上 / `getTicker` 没写 / **开关没开**；
方块看不见但动作栏在更新 = 少了 `getRenderShape`；每刻更新两遍 = 客户端也在 tick 了；
**动作栏在更新但屏幕上看不到光点** = 粒子客户端那半没接上。

> 说明：本机没有可用的代理，构建走的是 `tools/.gradle-home` 里的缓存（**离线构建**），
> 15 秒出 jar。`tools/.gradle-home` 是指向 `../swordqi/tools/.gradle-home` 的目录联接，
> 复用同一份 ForgeGradle 缓存，没有重复下载 1.2 GB 依赖。

---

## 四、怎么在游戏里看到它们

1. 把 `build/libs/fulu-1.0.0.jar` 丢进 `.minecraft/mods/`（需要 Forge 47.x + 1.20.1）。
2. 进游戏，创造模式物品栏里会多出一个叫 **"符箓"** 的页签，里面是 **符纸** 和 **阵眼**。
3. 或者 `/give fulu:talisman_paper`、`/give fulu:formation_eye`。
4. 阵眼是完整方块：六个面同一张贴图，会发光（光照等级 7），用镐子挖才掉自己。
5. **右键阵眼可以开 / 关这座阵法**（默认是关的）。开着的时候每 5 秒会从顶上飘出
   一把金色灵气（一次 6 颗，往上飘 1~2 秒后淡出）—— 这个效果只有客户端看得到，
   服务端只负责"发广播"。
6. 开关状态看日志：开着时每 5 秒一行 `阵眼在运行 (x, y, z)`，关着时一行都没有。
   正式游戏在 `.minecraft/logs/latest.log`，开发环境在 `run/logs/latest.log`。

---

## 五、下一步可以做什么

- 符纸 + 朱砂 → 画符：加"朱砂"物品和 `talisman_paper + 朱砂 + 灵石 → 火球符` 的配方
  （配方 json 放 `src/main/resources/data/fulu/recipes/`）。
- 阵眼做成"多方块结构"的核心：右键放符纸启动阵法（需要 `Block` 子类 + 交互逻辑）。
- 给物品加 tooltip 说明（`Item.appendHoverText`）。

---

## 六、工程结构

```
fulu/
├─ build.gradle / settings.gradle / gradle.properties   Forge 1.20.1 构建脚本
├─ src/main/java/com/lengyang/fulu/
│   ├─ FuLu.java                     模组主文件（挂载各登记表）
│   ├─ block/
│   │   ├─ FormationEyeBlock.java        阵眼方块本体   ← 第三步 ⑥ / 第五步 ④⑤
│   │   └─ FormationEyeBlockEntity.java  阵眼的运行逻辑 ← 第三～八步
│   ├─ client/
│   │   ├─ ModClientSetup.java           客户端登记（粒子 provider）← 第四步
│   │   └─ particle/SpiritMoteParticle.java  灵气粒子怎么画      ← 第四步
│   └─ registry/
│       ├─ ModItems.java             物品登记表   ← 第一步 ①②
│       ├─ ModBlocks.java            方块登记表 + 方块物品  ← 第二步 ①②③
│       ├─ ModBlockEntities.java     方块实体类型登记表     ← 第三步 ⑤
│       ├─ ModParticles.java         粒子类型登记表         ← 第四步
│       └─ ModCreativeTabs.java      创造模式物品栏（教程外补的）
├─ src/main/resources/
│   ├─ META-INF/mods.toml            modId / 版本 / 依赖（构建时注入）
│   ├─ pack.mcmeta
│   ├─ assets/fulu/
│   │   ├─ lang/{zh_cn,en_us}.json   中文名"符纸""阵眼"
│   │   ├─ blockstates/formation_eye.json         第二步 ⑥
│   │   ├─ models/block/formation_eye.json        第二步 ⑥
│   │   ├─ models/item/talisman_paper.json        第一步 ⑤
│   │   ├─ models/item/formation_eye.json         第二步 ⑥（方块物品）
│   │   ├─ particles/spirit_mote.json             第四步（粒子贴图声明）
│   │   ├─ textures/item/talisman_paper.png       第一步 ⑦ 16×16
│   │   ├─ textures/block/formation_eye.png       第二步 ⑦ 16×16
│   │   └─ textures/particle/spirit_mote.png      第四步 8×8
│   └─ data/
│       ├─ fulu/loot_tables/blocks/formation_eye.json      掉落表（必备）
│       └─ minecraft/tags/blocks/mineable/pickaxe.json     镐子标签（必备）
└─ tools/
    ├─ build.ps1                     构建入口（避开中文路径 / 自动离线判断）
    ├─ baidu_ref.py / montage.py     网上找参考图 → 联络表
    ├─ gen_talisman_paper.py         生成符纸贴图 + 预览
    ├─ gen_chaos_talisman.py         生成乱神符贴图（冷紫纸 + 紫朱砂的眼睛）+ 对比预览
    ├─ gen_formation_eye.py          生成阵眼贴图 + 预览
    ├─ gen_formation_eye_talisman.py 生成"有符"那套贴图（复制 + 正中贴符）+ 对比预览
    ├─ gen_spirit_mote.py            生成灵气粒子贴图 + 预览
    ├─ make_preview.py               物品预览图（参考图 / 贴图 / 物品栏）
    ├─ make_block_preview.py         方块预览图（等轴测立方体）
    ├─ verify_tick.ps1               真机验证（无头服务端）
    │                                `-MobTest` 扫描范围 / `-FightTest` 互相攻击
    │                                `-PersistTest` 存档 / `-StateTest` 方块状态与物品
    │                                `-TalismanTest` 两段分离（没符不挑事）
    │                                `-DisplayTest` 播报是否走 5 秒节拍
    │                                `-RestartTest -Phase 1/2 [-KeepWorld]` 关掉再打开
    │                                `-EffectTest` 内讧标记的挂/清/过期
    │                                `-CombatTest` 攻击效果传递 + 无限/20 秒
    │                                `-ParticleTest` 粒子只给阵法内的怪冒
    │                                `-OutlineTest` 边界一圈画在哪（80 点 / ±10）
    │                                `-CodexTest` 图鉴三阶段（？？？→名字→详情）
    │                                `-BlockTest` 方块能放/掉落对/物品 id 有效
    │                                `-CodexPersistTest -Phase 1|2 [-GracefulStop]`
    │                                        解锁进度退出重进不丢（两进程实测）
    │                                `-TwoTalismanTest` 乱神符打架/飞剑符不打架（对照实验）
    │                                `-SwordTest` 飞剑：阵眼放出实体、每发 5 点、不挂内讧标记
    │                                `-EntityTest` 飞剑实体：summon 后自己找目标、打 5 点、自灭
    ├─ check_region.py               不经过游戏，直接读区域文件里的 NBT
    ├─ python-home.txt               上面那个脚本用的解释器路径（同 java-home.txt 的约定）
    └─ verify_jar.py                 打包自检（461 项）

run/                                 开发运行目录（runServer 用；不进版本库）
└─ verifyworld/                      第三步真机验证用的超平坦世界
```
