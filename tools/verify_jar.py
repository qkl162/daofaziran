# -*- coding: utf-8 -*-
"""
Packaging self-check for the built mod jar.

A Forge mod compiles perfectly and still shows up in game as a purple-black
"missing texture" cube, a raw translation key, or a block that drops nothing
when mined - because one resource path did not line up. This script re-derives
the whole chain from inside the jar instead of trusting the source tree.

Checked for every content entry:
  物品: 注册名 -> models/item/<n>.json -> textures/item/<n>.png (16x16)
  方块: 注册名 -> blockstates/<n>.json -> models/block/<n>.json
                -> textures/block/<n>.png (16x16)
                -> models/item/<n>.json（方块物品）
                -> loot_tables/blocks/<n>.json（否则挖下来什么都不掉）
                -> mineable/pickaxe 标签（requiresCorrectToolForDrops 需要它）
  两者: lang key 里有没有中文名

Usage: python tools/verify_jar.py [path/to/jar]
"""

import io
import json
import os
import re
import sys
import zipfile

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MOD_ID = "fulu"
JAR = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    ROOT, "build", "libs", "%s-1.0.0.jar" % MOD_ID)

# 注册名 -> (中文名, 是不是方块)
ITEMS = {"talisman_paper": "\u7b26\u7eb8",                  # 符纸
         "chaos_talisman": "\u4e71\u795e\u7b26",            # 乱神符
         "flying_sword_talisman": "\u98de\u5251\u7b26",     # 飞剑符
         "cinnabar": "\u6731\u7802",                        # 朱砂
         "formation_codex": "\u9635\u6cd5\u56fe\u9274"}     # 阵法图鉴
EFFECTS = {"infighting": "\u5185\u8ba7"}                   # 内讧
BLOCKS = {"formation_eye": "\u9635\u773c",                  # 阵眼
          "formation_base": "\u9635\u57fa"}                 # 阵基
PARTICLES = {"spirit_mote": "\u7075\u6c14",                      # 灵气
             "sword_burst": "\u547d\u4e2d\u7206\u53d1",          # 命中爆发
             "rune_mote": "\u7b26\u6587",
             "anti_gravity": "\u53cd\u91cd\u529b"}                        # 符文

failures = []


def check(ok, message):
    print(("  [ok]   " if ok else "  [FAIL] ") + message)
    if not ok:
        failures.append(message)


def read_json(jar, names, path):
    if path not in names:
        return None
    return json.loads(jar.read(path).decode("utf-8"))


def check_texture(jar, names, path, size=(16, 16)):
    check(path in names, "贴图 %s 已打进 jar" % path)
    if path not in names:
        return
    img = Image.open(io.BytesIO(jar.read(path)))
    check(img.size == size, "%s 尺寸 %sx%s（必须 %sx%s）" % (path, *img.size, *size))
    check(img.mode == "RGBA", "%s 色彩模式 %s（必须 RGBA）" % (path, img.mode))


def main():
    print("jar: %s" % JAR)
    if not os.path.exists(JAR):
        print("  [FAIL] jar not found - run tools/build.ps1 first")
        return 1

    with zipfile.ZipFile(JAR) as jar:
        names = set(jar.namelist())
        print("\n-- contents (%d entries) --" % len(names))
        for n in sorted(names):
            if not n.endswith("/"):
                print("   %s" % n)

        print("\n-- mods.toml --")
        toml = jar.read("META-INF/mods.toml").decode("utf-8") if "META-INF/mods.toml" in names else ""
        check(bool(toml), "META-INF/mods.toml 已打包")
        check("${" not in toml, "mods.toml 占位符已被替换")
        check('modId = "%s"' % MOD_ID in toml, 'mods.toml modId = "%s"' % MOD_ID)

        print("\n-- java --")
        for cls in ("FuLu", "registry/ModItems", "registry/ModBlocks", "registry/ModBlockEntities",
                    "registry/ModParticles", "registry/ModCreativeTabs",
                    "block/FormationEyeBlock", "block/FormationEyeBlockEntity",
                    "client/ModClientSetup", "client/particle/SpiritMoteParticle",
                    # 第十二步：阵法图鉴（数据+文本 / 物品 / 命令）
                    "codex/FormationCodex", "item/FormationCodexItem", "command/CodexCommand",
                    # 第十三步：图鉴界面（菜单 = 两端共有；界面 = 纯客户端）
                    "menu/CodexMenu", "registry/ModMenus", "client/screen/CodexScreen",
                    # 第十三步：飞剑实体（实体 / 模型 / 渲染器 / 模型层）
                    "entity/FlyingSwordEntity", "registry/ModEntities", "registry/ModModelLayers",
                    "client/model/FlyingSwordModel", "client/render/FlyingSwordRenderer",
                    # 第十步第十部分：怪物打怪物的事件处理
                    "combat/InfightingCombatHandler", "effect/InfightingEffect"):
            path = "com/lengyang/fulu/%s.class" % cls
            check(path in names, "%s 已打进 jar" % path)
        if "com/lengyang/fulu/registry/ModItems.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModItems.class")
            check(b"talisman_paper" in raw, "ModItems 里注册了 talisman_paper")
            check(b"chaos_talisman" in raw, "ModItems 里注册了 chaos_talisman（乱神符）")
            check(b"CHAOS_TALISMAN" in raw, "有 CHAOS_TALISMAN 字段")
        if "com/lengyang/fulu/registry/ModBlocks.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModBlocks.class")
            for name in BLOCKS:
                check(name.encode() in raw, "ModBlocks.class 里确实注册了 %s" % name)
        if "com/lengyang/fulu/registry/ModBlockEntities.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModBlockEntities.class")
            # 只有"有大脑"的方块才有方块实体类型。阵基是普通方块，没有。
            for name in ("formation_eye",):
                check(name.encode() in raw,
                      "ModBlockEntities.class 里把方块实体类型登记为 %s（与方块同名）" % name)
            check(b"FORMATION_EYE" in raw, "ModBlockEntities 里有 FORMATION_EYE 常量")
            check(b"formation_base" not in raw,
                  "阵基没有方块实体（它是普通方块，不需要每刻逻辑 —— 加了反而多一份开销）")
        # 第三步的三根线：谁能创建方块实体、谁每刻被调用、方块怎么显示。
        #
        # 注意这里比对的是 SRG 名（m_xxxxx_），不是源码里的 getTicker ——
        # 打包时 reobfJar 会把**覆盖原版方法**的方法名换成生产环境用的 SRG 名，
        # 所以 jar 里看到的名字和源码不一样是正常的，不是打包坏了。
        SRG = {
            b"m_142194_": "newBlockEntity（创建方块实体）",
            b"m_142354_": "getTicker（每刻调用一次）",
            b"m_7514_": "getRenderShape（不覆盖会隐形）",
        }
        if "com/lengyang/fulu/block/FormationEyeBlock.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlock.class")
            for needle, why in SRG.items():
                check(needle in raw, "覆盖了 " + why)
            check(b"m_6227_" in raw, "覆盖了 use（右键交互）")
            check(b"toggleActive" not in raw,
                  "use 里已经没有 toggleActive（第十步第七部分：符就是开关）")
            check(b"FormationEyeBlockEntity" in raw, "newBlockEntity 里创建的是阵眼的方块实体")
            check(b"RenderShape" in raw, "有 RenderShape 相关字节码（模型显示路径）")
            # 第十步第三部分：装符 / 取符
            #
            # ⚠️ 不要用 raw.count(b"xxx") 去数"调用了几次"：class 文件的常量池会把
            # 名字**去重**，同名方法被调用 5 次，池里也只有 1 份 UTF8。所以这里
            # 只查"有没有引用"，"两条分支都在"靠下面两条**不同的**提示文案来证明
            # （装符一条、取符一条，两个不同的字符串，去重也不会合并）。
            check(b"TalismanKind" in raw,
                  "use 里用 TalismanKind 判断装的是哪种符（不再是 boolean 的有/没有）")
            check(b"setTalisman" in raw, "use 里调用了 setTalisman（同时改 NBT 与外观）")
            check(b"msg.fulu.formation_eye.talisman_on" in raw, "有“装上乱神符”的提示文案")
            check(b"msg.fulu.formation_eye.sword_on" in raw, "有“装上飞剑符”的提示文案")
            check(b"msg.fulu.formation_eye.already_has_talisman" in raw,
                  "有“已经有符了”的提示（不让装第二种符）")
            # 第十一步第三部分：空手右键（没符）显示阵法信息
            check(b"showFormationInfo" in raw, "有 showFormationInfo（空手且没符时报信息）")
            check(b"getMonsterCount" in raw, "读的是方块实体里存下来的怪物数量")
            check(b"getScanRadius" in raw, "读的是范围的唯一来源（不再写死一个 10）")
            # 关键否定断言：这个类里**不能**出现扫描方法。
            # findMonsters 是方块实体里的 private static，外面本来也调不到；
            # 钉住它，是为了防止以后有人图省事在这儿再扫一遍世界。
            check(b"findMonsters" not in raw, "没有重新扫描世界（findMonsters 不出现在方块类里）")
            # 这三条信息的文案必须真的被这个类引用（写错 key 的话，游戏里会直接
            # 显示 "msg.fulu.formation_eye.info.radius" 那串字，不报错）。
            # "状态"那一行的值是按符的种类算出来的（TalismanKind.stateKey），
            # 所以这里查的是 stateKey 这个方法引用，而不是写死的 state_on/off。
            for suffix in ("info.state", "info.radius", "info.monsters"):
                key = ("msg.fulu.formation_eye.%s" % suffix).encode("utf-8")
                check(key in raw, "方块类里引用了文案 %s" % key.decode("utf-8"))
            check(b"stateKey" in raw, "状态文案是按符的种类算出来的（TalismanKind.stateKey）")
            # 第十一步第二部分：右键显示范围边界（先用木棍）
            check(b"requestOutline" in raw, "use 里调用了 requestOutline（右键请求画边界）")
            check(b"OUTLINE_TOOL" in raw, "有 OUTLINE_TOOL 常量（要换成专属工具只改这一行）")
            check(b"outline_wait" in raw, "连点时有提示文案")
            check(b"isClientSide" in raw or b"m_5776_" in raw, "仍然只在服务端发起（客户端不重复请求）")
            check(b"msg.fulu.formation_eye.talisman_off" in raw, "有“取下了”的提示文案（取符分支）")
            # 原版字段/方法在 jar 里是 SRG 名：
            #   f_11887_ = SoundEvents.ENCHANTMENT_TABLE_USE（装符的音效）
            #   f_11713_ = SoundEvents.BOOK_PAGE_TURN（取符的音效，翻纸声）
            #   m_5776_  = Level.isClientSide（只在服务端改状态）
            #   m_36341_ = Player.isSecondaryUseActive（潜行右键才是开关阵法）
            #   m_21120_ = Player.getItemInHand（看手里拿的是什么）
            check(b"f_11887_" in raw, "装符时播了音效（SRG 名 f_11887_）")
            check(b"f_11713_" in raw, "取符时播了音效（SRG 名 f_11713_）")
            check(b"m_5776_" in raw, "用 isClientSide 挡住客户端（SRG 名 m_5776_）")
            check(b"m_36341_" in raw, "判断了潜行右键（SRG 名 m_36341_）")
            check(b"m_21120_" in raw, "读了玩家手上的物品（SRG 名 m_21120_）")
        if "com/lengyang/fulu/block/FormationEyeBlockEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
            check(b"toggleActive" not in raw,
                  "toggleActive 已删（第十步第七部分：符就是开关，不再有手动开关）")
            check(b"active" not in raw, "active 字段也删干净了")
            check(b"tick" in raw, "方块实体里有 tick 方法（自己写的方法名不会被改名）")
            check(b"SPIRIT_MOTE" in raw, "tick 里引用了灵气粒子 SPIRIT_MOTE")
            check(b"active" not in raw, "没有 active 字段（手动开关已删，符就是开关）")
            check(b"toggleActive" not in raw, "没有 toggleActive 方法")
            check(b"cooldown" not in raw,
                  "cooldown 字段已经删干净（第十步第五部分：去掉冷却，每刻都干活）")
            # NOTE: a plain "COOLDOWN_TICKS" not-in-raw test would also match
            # OUTLINE_COOLDOWN_TICKS (step 11 part 2) and report a failure that
            # is not there. Use a lookbehind so only the bare old name counts.
            check(re.search(rb"(?<![A-Z_])COOLDOWN_TICKS", raw) is None,
                  "COOLDOWN_TICKS 常量也删掉了（OUTLINE_COOLDOWN_TICKS 不算）")
            # 第六部分：播报 + 粒子单独走 5 秒节拍，扫描和打架仍然是每刻
            check(b"DISPLAY_INTERVAL_TICKS" in raw, "有 DISPLAY_INTERVAL_TICKS（显示/粒子的 5 秒节拍）")
            check(b"displayTimer" in raw, "有 displayTimer 字段（只节流播报与粒子）")
            check(b"lastMonsterCount" in raw, "有 lastMonsterCount 字段（只在数量变化时播报）")
            check(b"findMonsters" in raw, "有 findMonsters 方法（扫描并返回列表）")
            check(b"makeThemFight" in raw, "有 makeThemFight 方法（挑两只怪打起来）")
            # 这一步：飞剑符那一路
            # 音效也是原版常量，打包后同样只剩 SRG 名：
            #   f_12317_ = PLAYER_ATTACK_SWEEP（剑挥砍）—— 在 launchSword 里
            #   f_11668_ = ANVIL_LAND（铁砧落地）      —— 在 advanceSword 命中那一下
            # （用 javap 按方法分开看出来的，不是猜的）
            check(b"clearAllInfighting" in raw,
                  "有 clearAllInfighting（换符/取符之后把旧的内讧标记清掉）")
            # 下面两条是"调用原版方法"，jar 里名字已经换成 SRG 名：
            #   m_45976_ = Level.getEntitiesOfClass(Class, AABB) -> List
            #   m_6710_  = Mob.setTarget(LivingEntity)
            check(b"m_45976_" in raw, "调用了 getEntitiesOfClass（SRG 名 m_45976_）")
            check(b"m_6710_" in raw, "调用了 setTarget（SRG 名 m_6710_，让怪物去打目标）")
            check(b"announceMonsterCount" in raw, "有 announceMonsterCount 方法（发给附近玩家）")
            check("msg.fulu.formation_eye.monsters".encode() in raw,
                  "动作栏文案走语言文件 key")
            # 第 7 步把控制台打印换成了动作栏：这条中文句子不该再出现在字节码里
            check("\u627e\u5230".encode("utf-8") not in raw,
                  "旧的“找到 X 只怪物”控制台打印已经从类里删掉")
            # 存档：saveAdditional / load 必须覆盖，而且键名要一致。
            # 这两个是**覆盖原版方法**，jar 里名字已经换成 SRG 名：
            #   m_183515_ = BlockEntity.saveAdditional(CompoundTag)
            #   m_142466_ = BlockEntity.load(CompoundTag)
            check(b"m_183515_" in raw, "覆盖了 saveAdditional（SRG 名 m_183515_）把开关存进 NBT")
            check(b"m_142466_" in raw, "覆盖了 load（SRG 名 m_142466_）把开关读回来")
            check(b"HasTalisman" in raw,
                  "还认得旧 NBT 键 HasTalisman（第 9 步的 boolean 存档能迁移过来）")
            check(b"Talisman" in raw, "NBT 键 Talisman（存的是种类名）")
            check(b"TalismanKind" in raw, "字段是 TalismanKind（空 / 乱神符 / 飞剑符 / 反重力符）")
            check(b"byName" in raw, "有 byName：从存档里的名字读回来，认不出就当空的")
            check(b"setTalisman" in raw, "有 setTalisman 方法（插符 / 取下符）")

        print("\n-- particles --")
        for name in PARTICLES:
            json_path = "assets/%s/particles/%s.json" % (MOD_ID, name)
            data = read_json(jar, names, json_path)
            check(data is not None, "%s 已打包" % json_path)
            if data:
                check("%s:%s" % (MOD_ID, name) in data.get("textures", []),
                      "particles/%s.json 里的贴图写作 %s:%s" % (name, MOD_ID, name))
            tex = "assets/%s/textures/particle/%s.png" % (MOD_ID, name)
            check(tex in names, "粒子贴图 %s 已打包" % tex)
            if tex in names:
                img = Image.open(io.BytesIO(jar.read(tex)))
                # 粒子贴图是一张方形精灵；具体边长不强制（灵气用 8x8 的小点，
                # 剑光用 16x16 —— 它要画一条细长的光带，8x8 太短）。
                check(img.width == img.height and img.width in (8, 16),
                      "%s 尺寸 %sx%s（方形，8 或 16）" % (tex, *img.size))
                check(img.mode == "RGBA", "%s 色彩模式 %s" % (tex, img.mode))
        if "com/lengyang/fulu/registry/ModParticles.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModParticles.class")
            check(b"spirit_mote" in raw, "ModParticles.class 里登记了 spirit_mote")
        if "com/lengyang/fulu/client/ModClientSetup.class" in names:
            raw = jar.read("com/lengyang/fulu/client/ModClientSetup.class")
            check(b"RegisterParticleProvidersEvent" in raw, "客户端挂上了粒子 provider 事件")
            check(b"CLIENT" in raw, "客户端登记标了 Dist.CLIENT（服务端不会加载它）")

        print("\n-- lang --")
        for lang in ("zh_cn", "en_us"):
            path = "assets/%s/lang/%s.json" % (MOD_ID, lang)
            data = read_json(jar, names, path)
            check(data is not None, "%s 已打包" % path)
            if data is None:
                continue
            for name, cn in ITEMS.items():
                key = "item.%s.%s" % (MOD_ID, name)
                check(key in data, "%s 里有 %s" % (path, key))
                if lang == "zh_cn":
                    check(data.get(key) == cn,
                          "%s -> %r（应为 %r）" % (key, data.get(key), cn))
            for name, cn in BLOCKS.items():
                key = "block.%s.%s" % (MOD_ID, name)
                check(key in data, "%s 里有 %s" % (path, key))
                if lang == "zh_cn":
                    check(data.get(key) == cn,
                          "%s -> %r（应为 %r）" % (key, data.get(key), cn))
            # 状态效果的名字也是 item/block 之外的一套 key：effect.<mod>.<id>
            for name, cn in EFFECTS.items():
                key = "effect.%s.%s" % (MOD_ID, name)
                check(key in data, "%s 里有 %s" % (path, key))
                if lang == "zh_cn":
                    check(data.get(key) == cn,
                          "%s -> %r（应为 %r）" % (key, data.get(key), cn))
            for suffix in ("monsters", "talisman_on", "talisman_off", "outline_wait",
                           "state_on", "state_off",
                           "info.state", "info.radius", "info.monsters"):
                key = "msg.%s.%s.%s" % (MOD_ID, "formation_eye", suffix)
                check(key in data, "%s 里有 %s（提示）" % (path, key))
            # 第十步第七部分：手动开关删掉了，"阵法已开启/已关闭"两条文案也随之退休
            for gone in ("on", "off"):
                key = "msg.%s.%s.%s" % (MOD_ID, "formation_eye", gone)
                check(key not in data, "%s 里已经没有 %s（手动开关已删）" % (path, key))
            check("itemGroup.%s.main" % MOD_ID in data,
                  "%s 里有创造模式页签名" % path)

        print("\n-- items --")
        for name in ITEMS:
            model_path = "assets/%s/models/item/%s.json" % (MOD_ID, name)
            model = read_json(jar, names, model_path)
            check(model is not None, "%s 已打包" % model_path)
            if model:
                layer = model.get("textures", {}).get("layer0")
                check(layer == "%s:item/%s" % (MOD_ID, name),
                      "models/item/%s.json layer0 = %r" % (name, layer))
                check(model.get("parent") == "minecraft:item/generated",
                      "models/item/%s.json parent = %r" % (name, model.get("parent")))
            check_texture(jar, names, "assets/%s/textures/item/%s.png" % (MOD_ID, name))

        print("\n-- mob effects --")
        # 第十步第九部分：内讧（纯标记，不掉血）
        for name in EFFECTS:
            path = "com/lengyang/fulu/effect/InfightingEffect.class"
            check(path in names, "%s 已打进 jar（专门的效果类）" % path)
            # 图标目录是 mob_effect（单数！），尺寸 18x18 —— 和方块/物品的 16x16 不一样
            check_texture(jar, names,
                          "assets/%s/textures/mob_effect/%s.png" % (MOD_ID, name), size=(18, 18))
        if "com/lengyang/fulu/registry/ModMobEffects.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModMobEffects.class")
            check(b"infighting" in raw, "ModMobEffects 里注册了 infighting")
            check(b"INFIGHTING" in raw, "有 INFIGHTING 字段")
        if "com/lengyang/fulu/effect/InfightingEffect.class" in names:
            raw = jar.read("com/lengyang/fulu/effect/InfightingEffect.class")
            # ⚠️ 关键断言：这个效果不能覆盖那两个"会被周期调用"的方法，
            #    覆盖了就意味着它会每刻对怪物做点什么（比如扣血）。
            check(b"isDurationEffectTick" not in raw, "没有覆盖 isDurationEffectTick")
            check(b"MobEffectCategory" in raw, "构造函数里指定了分类（负面）")
        if "com/lengyang/fulu/block/FormationEyeBlockEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
            check(b"markInfighting" in raw, "阵眼里有 markInfighting（给范围内的怪挂标记）")
            check(b"clearInfightingOutside" in raw, "阵眼里有 clearInfightingOutside（清掉出圈的标记）")
            check(b"isInsideFormation" in raw, "有 isInsideFormation（标记与清除共用同一个范围判断）")
            check(b"formationBox" in raw, "有 formationBox（范围只定义一次）")
            # 第十步第十一部分：内讧粒子
            check(b"emitInfightingParticles" in raw, "有 emitInfightingParticles（给内讧怪冒粒子）")
            check(b"MARK_PARTICLE_COUNT" in raw, "有 MARK_PARTICLE_COUNT（每只每刻 1~2 个）")
            check(b"MARK_PARTICLE_HEAD_OFFSET" in raw, "有头顶偏移常量（粒子从头顶往上冒）")
            # 原版字段/方法在 jar 里是 SRG 名：
            #   f_123792_ = ParticleTypes.ANGRY_VILLAGER（愤怒的村民那个小云）
            #   m_8624_   = ServerLevel.sendParticles(ServerPlayer, ...) —— **单个玩家**的那版
            #   m_8767_   = ServerLevel.sendParticles(...) —— 广播版（距离上限 32 格）
            # 断言用的是"发给单个玩家"的那一版：广播版自己会在 32 格内乱发，
            # 正好不符合"只发给阵眼周围 16 格"的要求。
            check(b"f_123792_" in raw, "粒子用的是原版 ParticleTypes.ANGRY_VILLAGER（SRG 名 f_123792_）")
            check(b"m_8624_" in raw, "用**单个玩家**版本发粒子（SRG 名 m_8624_），不是 32 格广播版")
            # 第十一步第二部分：范围边界
            check(b"emitOutline" in raw, "有 emitOutline（画范围边界那一圈）")
            check(b"requestOutline" in raw, "有 requestOutline（右键时登记请求）")
            check(b"OUTLINE_DURATION_TICKS" in raw, "有 OUTLINE_DURATION_TICKS（画 3 秒）")
            check(b"OUTLINE_INTERVAL_TICKS" in raw, "有 OUTLINE_INTERVAL_TICKS（每 0.5 秒补一轮）")
            check(b"OUTLINE_COOLDOWN_TICKS" in raw, "有 OUTLINE_COOLDOWN_TICKS（连点保护）")
            check(b"outlineViewer" in raw, "记了 outlineViewer（只画给右键的那个人）")
            # f_123810_ = ParticleTypes.END_ROD（边界用的粒子）
            check(b"f_123810_" in raw, "边界粒子用的是原版 END_ROD（SRG 名 f_123810_）")

        if "com/lengyang/fulu/combat/InfightingCombatHandler.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/InfightingCombatHandler.class")
            check(b"onLivingHurt" in raw, "有 onLivingHurt（监听“生物被打”事件）")
            check(b"LivingHurtEvent" in raw, "订阅的是 LivingHurtEvent")
            check(b"INFIGHTING" in raw, "处理里判断了内讧效果（没内讧就走原版）")
            check(b"Player" in raw, "有玩家的分支（内讧怪打玩家不掉血）")
            check(b"Monster" in raw, "受害者的判断里有 Monster（照抄原版那张表用的）")
            # 原版把"打人附带什么效果"写死在每个怪物类里，没有接口可问，
            # 所以这里是一张照抄原版的表。
            for mob in ("Husk", "CaveSpider", "WitherSkeleton", "Stray"):
                check(mob.encode() in raw, "攻击附带效果的对照表里有 %s" % mob)
            check(b"addEffect" in raw or b"m_7292_" in raw,
                  "把效果加到被打的那只怪物身上（SRG 名 m_7292_）")
            # 关键否定断言：内讧只能由阵眼发，不能靠打架传播。
            # 这个类里只允许有一处"给生物加效果"，而且加的是攻击附带效果。
            adds = raw.count(b"m_7292_")
            check(adds <= 1,
                  "只给生物加效果一次（加的是攻击附带效果，不是把内讧传染出去）")

        print("\n-- formation codex --")
        # 第十二步：阵法图鉴（物品 + 配方 + 三阶段解锁 + 文本）
        if "com/lengyang/fulu/effect/AntiGravityEffect.class" in names:
            raw = jar.read("com/lengyang/fulu/effect/AntiGravityEffect.class")
            # 用户要求"反重力是负面效果"
            check(b"HARMFUL" in raw, "效果是负面效果（MobEffectCategory.HARMFUL）")
            check(b"setTarget" not in raw and b"m_6710_" not in raw,
                  "效果里不清攻击目标（能不能打由每刻事件决定）")
            check(b"ANTI_GRAVITY" not in raw,
                  "效果本体不碰粒子/位置（每刻的事都在 AntiGravityTickHandler，"
                  "因为无限时长的效果不保证每刻被 tick）")
            check(b"net/minecraft/client" not in raw, "效果是公共类，服务端要能跑")
        if "com/lengyang/fulu/combat/AntiGravityRenderHandler.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/AntiGravityRenderHandler.class")
            check(b"RenderLivingEvent" in raw, "监听怪物渲染事件")
            check(b"PoseStack" in raw, "翻的是渲染姿态（不是碰撞箱、不是位置）")
            check(b"blaze3d" in raw or b"Dist" in raw,
                  "纯客户端（PoseStack 来自 blaze3d，服务端没有它）")
            # ★ 显式注册：由 ModClientSetup 调 register() 挂到游戏事件总线。
            #   （原来靠 @Mod.EventBusSubscriber 扫描，而扫描是静默失败的：
            #    没命中就什么都不发生 —— "怪物没倒立"排查到最后只剩这一环可疑。）
            check(b"register" in raw and b"EVENT_BUS" in raw,
                  "有 register()：显式挂到游戏事件总线")
            # ★ 不能再带注解 —— 两边都注册回调会跑两遍，翻两次等于没翻
            check(b"EventBusSubscriber" not in raw,
                  "**没有**加注解（否则会和显式注册叠加，翻两次等于没翻）")
            check(b"antigravity-render" in raw, "翻转时写一条日志（让'跑没跑'可验证）")
            check(b"ANTI_GRAVITY" in raw, "翻转时放一颗粒子（画面上就能看出回调跑没跑）")
            # ★ 必须是**镜像**（scale 带负号），不能是旋转：
            #   原版渲染器自己那句 scale(-1,-1,1) 在矩阵上就等于绕 Z 转 180 度，
            #   再用旋转去翻会被它抵消成"什么都没改" —— 这一版之前连着错了三次，
            #   现象就是"粒子出来了、模型纹丝不动"。这条断言专门防再次改回去。
            check(b"m_85841_" in raw, "用的是 scale（镜像）")
            check(b"m_85844_" not in raw and b"m_252781_" not in raw
                  and b"m_252931_" not in raw,
                  "**没有**用 mulPose —— 旋转会被原版那句 scale(-1,-1,1) 抵消掉")
        if "com/lengyang/fulu/client/ModClientSetup.class" in names:
            raw = jar.read("com/lengyang/fulu/client/ModClientSetup.class")
            check(b"AntiGravityRenderHandler" in raw,
                  "客户端启动时调用 AntiGravityRenderHandler.register()")
        if "com/lengyang/fulu/combat/AntiGravityTickHandler.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/AntiGravityTickHandler.class")
            check(b"LivingTickEvent" in raw, "监听每刻事件")
            check(b"getNavigation" in raw or b"m_21573_" in raw, "禁止寻路")
            check(b"RISE" in raw and b"HOVER" in raw,
                  "升空速度是常量（每刻 0.02；到顶 HOVER=0 悬停）")
            check(b"CEILING_KEY" in raw, "天花板从阵眼传来（阵眼 y + 5）")
            check(b"ANTI_GRAVITY" in raw, "升空时出反重力粒子")
            check(b"setTarget" not in raw and b"m_6710_" not in raw,
                  "不清攻击目标 —— 保留攻击能力（用户要求的重点）")
            # "水平速度清零 / 禁止跳跃"调的是 Entity/Mob 上的原版方法，名字都进了 SRG，
            # 查不到字面名 —— 这两条由 -AntiGravityTest 的行为检查证明（X 600 刻没动）
        if "com/lengyang/fulu/combat/FormationTarget.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/FormationTarget.class")
            check(b"canInvert" in raw, "共享的 canInvert（阵眼/渲染/每刻三处同一个标准）")
        if "com/lengyang/fulu/block/FormationEyeBlockEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
            check(b"ANTI_GRAVITY_SOUND_INTERVAL" in raw,
                  "低沉音效是**整个阵法一声**（不是每个怪一声）")
            check(b"FLOAT_DURATION_TICKS" in raw, "离场后的倒计时是常量（3 秒）")
            check(b"FLOAT_DURATION_INFINITE" in raw, "阵法内时长无限（-1）")
            check(b"releaseAntiGravity" in raw, "离场的怪改成 3 秒倒计时（然后掉下来）")

        if "com/lengyang/fulu/codex/FormationCodex.class" in names:
            raw = jar.read("com/lengyang/fulu/codex/FormationCodex.class")
            check(b"infighting" in raw, "图鉴表里收录了乱神阵（id=infighting）")
            check(b"UNLOCK_PLACED" in raw, "有 UNLOCK_PLACED（放下阵眼那一位）")
            check(b"UNLOCK_ACTIVATED" in raw, "有 UNLOCK_ACTIVATED（装符激活那一位）")
            check(b"fuluCodex" in raw, "解锁状态也镜像到计分板 fuluCodex（按名字可查）")
            check(b"FuLuCodex" in raw,
                  "解锁状态写进玩家 NBT 的 FuLuCodex 键（= playerdata/<uuid>.dat，退出重进不丢）")
            check(b"getPersistentData" in raw, "用的是玩家自己的持久 NBT（Forge 的 getPersistentData）")
            check(b"bitsForName" in raw, "有 bitsForName：在线读玩家 NBT，离线退回计分板镜像")
            check(b"RUN_COMMAND" in raw, "列表那行带点击事件（点一下看详情）")
            check(b"buildList" in raw and b"buildDetail" in raw, "有列表和详情两个渲染入口")
        if "com/lengyang/fulu/item/FormationCodexItem.class" in names:
            raw = jar.read("com/lengyang/fulu/item/FormationCodexItem.class")
            check(b"item.fulu.formation_codex.desc" in raw, "物品有说明文案（desc）")
            check(b"item.fulu.formation_codex.howto" in raw, "物品有用法文案（howto）")
            # 修 bug 的那一环：开界面时把**服务端的**解锁位一起交给菜单，
            # 而不是让界面自己去客户端计分板里翻（翻不到，那个目标不会同步）。
            check(b"FormationCodex" in raw and b"unlocked" in raw,
                  "开界面时把解锁位从服务端取出来（FormationCodex.unlocked）交给菜单")
        if "com/lengyang/fulu/item/FormationCodexItem.class" in names:
            raw = jar.read("com/lengyang/fulu/item/FormationCodexItem.class")
            # 第十三步之前，图鉴是把文本打到聊天栏里的；现在它改为**开界面**，
            # 所以这两条老断言（"引用 FormationCodex" / "调 displayClientMessage"）
            # 已经过时了 —— 留着它们只会一直报一个不存在的错。
            # 现在该断言的是：它开的是那个菜单，而且两端安全（见下面的 codex screen 段）。
            check(b"CodexMenu" in raw, "图鉴物品开的是 CodexMenu（界面），不再自己发聊天文本")
            check(b"net/minecraft/client" not in raw,
                  "而且它不引用客户端类（开界面走 NetworkHooks，服务端也能加载）")
        if "com/lengyang/fulu/command/CodexCommand.class" in names:
            raw = jar.read("com/lengyang/fulu/command/CodexCommand.class")
            check(b"RegisterCommandsEvent" in raw, "注册了命令（RegisterCommandsEvent）")
            check(b"fulu" in raw and b"codex" in raw and b"show" in raw,
                  "命令是 /fulu codex show（点击事件点的就是它）")
            check(b"buildList" in raw and b"buildDetail" in raw, "命令和图鉴物品用的是同一套渲染")
        if "com/lengyang/fulu/block/FormationEyeBlock.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlock.class")
            # 两个解锁触发点都在这个类里：
            #   放下阵眼 -> 覆盖 setPlacedBy（SRG 名 m_6402_）
            #   装符激活 -> 在 use（m_6227_）里
            # 注意不能断言 UNLOCK_PLACED / UNLOCK_ACTIVATED 这两个名字：
            # 它们是 static final int，编译时就被**内联成字面量 1 / 2** 了，
            # 常量池里根本没有这两个名字（和"数调用次数"是同一类坑）。
            # 断言方法引用本身：javap 能看到两处 FormationCodex.unlock 调用。
            check(b"FormationCodex" in raw and b"unlock" in raw,
                  "方块里调了 FormationCodex.unlock（解锁入口）")
            check(b"m_6402_" in raw, "覆盖了 setPlacedBy（放下阵眼时解锁；SRG 名 m_6402_）")
            check(b"m_6227_" in raw, "use 里也有解锁（装符激活那一次；SRG 名 m_6227_）")
        recipe = read_json(jar, names, "data/%s/recipes/formation_codex.json" % MOD_ID)
        check(recipe is not None, "data/fulu/recipes/formation_codex.json（合成配方）已打包")
        if recipe:
            blob = json.dumps(recipe)
            check("%s:formation_codex" % MOD_ID in blob, "配方产出的是 fulu:formation_codex")
            check("%s:talisman_paper" % MOD_ID in blob, "配方用到了 fulu:talisman_paper（符纸）")
            check("minecraft:book" in blob, "配方用到了书")
            # 这一步的重点：配方从"红石粉"改成了真正的"朱砂"
            check("%s:cinnabar" % MOD_ID in blob, "配方用到了 fulu:cinnabar（朱砂）")
            check("minecraft:redstone" not in blob,
                  "配方里已经没有红石粉了（朱砂补上之后就撤了代用品）")

        print("\n-- beast blood and the chaos talisman recipe --")
        # 兽血：新物品（这一步才加的，因为乱神符配方里写着它）
        for path in ("assets/%s/models/item/beast_blood.json" % MOD_ID,
                     "assets/%s/textures/item/beast_blood.png" % MOD_ID):
            check(path in names, "%s（兽血）已打包" % path)
        model = read_json(jar, names, "assets/%s/models/item/beast_blood.json" % MOD_ID)
        if model:
            check(model.get("textures", {}).get("layer0") == "%s:item/beast_blood" % MOD_ID,
                  "兽血模型的 layer0 指向 fulu:item/beast_blood")
        lang = read_json(jar, names, "assets/%s/lang/zh_cn.json" % MOD_ID)
        if lang:
            check(lang.get("item.fulu.beast_blood") == "兽血",
                  "兽血的中文名是 '兽血'")
        # ★ 三种符的配方都是**有序**（九宫格），这一步改成用户给的新图案
        def check_shaped(path, expected_pattern, keys, result_item, label):
            """有序配方：图案、每个字母指向什么、产出什么。"""
            recipe = read_json(jar, names, path)
            check(recipe is not None, "%s（%s）已打包" % (path, label))
            if not recipe:
                return
            check(recipe.get("type") == "minecraft:crafting_shaped",
                  "%s 是**有序**合成（用户说这三种都是）" % label)
            check(recipe.get("pattern") == expected_pattern,
                  "%s 的三行图案对得上" % label)
            key_map = recipe.get("key", {})
            for letter, item_id, item_label in keys:
                check(key_map.get(letter, {}).get("item") == item_id,
                      "%s 图案里的 %s = %s" % (label, letter, item_label))
            check(recipe.get("result", {}).get("item") == result_item
                  and recipe.get("result", {}).get("count") == 1,
                  "%s 产出 ×1" % label)

        check_shaped("data/%s/recipes/chaos_talisman.json" % MOD_ID,
                     [" P ", "CFM", " B "],
                     [("P", "%s:talisman_paper" % MOD_ID, "符纸"),
                      ("C", "%s:cinnabar" % MOD_ID, "朱砂"),
                      ("F", "minecraft:fermented_spider_eye", "发酵蛛眼"),
                      ("M", "minecraft:phantom_membrane", "幻翼膜"),
                      ("B", "%s:beast_blood" % MOD_ID, "兽血")],
                     "%s:chaos_talisman" % MOD_ID, "乱神符")

        print("\n-- formation eye and talisman paper recipes --")
        # 阵眼：唯一一条**有序**合成
        recipe = read_json(jar, names, "data/%s/recipes/formation_eye.json" % MOD_ID)
        check(recipe is not None, "阵眼配方已打包（之前根本没有）")
        if recipe:
            check(recipe.get("type") == "minecraft:crafting_shaped",
                  "阵眼是**有序**合成（用户说只有它是）")
            check(recipe.get("pattern") == ["BIB", "ICI", "BIB"],
                  "图案是 阵基铁粒阵基 / 铁粒朱砂铁粒 / 阵基铁粒阵基")
            keys = recipe.get("key", {})
            check(keys.get("B", {}).get("item") == "%s:formation_base" % MOD_ID,
                  "图案里的 B = 阵基")
            check(keys.get("I", {}).get("item") == "minecraft:iron_nugget",
                  "图案里的 I = 铁粒")
            check(keys.get("C", {}).get("item") == "%s:cinnabar" % MOD_ID,
                  "图案里的 C = 朱砂")
            check(recipe.get("result", {}).get("item") == "%s:formation_eye" % MOD_ID
                  and recipe.get("result", {}).get("count") == 1,
                  "产出 阵眼 ×1")
        # 符纸：纸 + 羊毛 + 纸（新增，之前没有）
        recipe = read_json(jar, names, "data/%s/recipes/talisman_paper.json" % MOD_ID)
        check(recipe is not None, "符纸配方已打包（之前根本没有）")
        if recipe:
            blob = json.dumps(recipe)
            check(recipe.get("type") == "minecraft:crafting_shapeless",
                  "符纸是无序合成（用户写的是 纸+羊毛+纸）")
            check(blob.count("minecraft:paper") == 2, "符纸用**两张**纸")
            check("minecraft:white_wool" in blob, "符纸用白色羊毛")
            check(recipe.get("result", {}).get("item") == "%s:talisman_paper" % MOD_ID,
                  "产出 符纸")

        print("\n-- stand-in: shield bar, particles, codex --")
        # 十一：绿色次数盾血条
        if "com/lengyang/fulu/client/StandInOverlay.class" in names:
            raw = jar.read("com/lengyang/fulu/client/StandInOverlay.class")
            # 用户先要"原版血条上方另加一条"，再要"形状用原版心形、红色改成绿色"：
            # 所以现在**不动**原版血条，只在自己那一条里画 5 颗绿心（自己烘的贴图）。
            check(b"SHIELD_HEARTS" in raw, "用的是自己烘的绿色心形贴图（染色做不到）")
            check(b"HEART_EMPTY_U" in raw and b"HEART_FULL_U" in raw and b"HEART_HALF_U" in raw,
                  "空壳 / 满 / 半 三帧都画（和原版血条同一套叠法）")
            check(b"CELLS" in raw and b"POINTS_PER_CELL" in raw, "5 格，每格 4 点")
            check(b"VANILLA_BAR_TOP" in raw and b"VANILLA_BAR_WIDTH" in raw,
                  "按原版血条的位置算：画在它**上方**")
            check(b"HEART_STEP" in raw, "心与心之间 8 像素（原版间距）")
            check(b"SHIELD_BAR_OFFSET_Y" in raw,
                  "有偏移量常量（和别的模组界面冲突时改这一个数）")
            check(b"setCanceled" not in raw and b"PLAYER_HEALTH" not in raw,
                  "**不**取消原版血条（它照常显示）")
            check(b"PATTERN_SCREEN_RATIO" in raw,
                  "图案大小按屏幕高度算（大屏不会显得太小）")
        # 十二 / 十三：粒子与音效
        if "com/lengyang/fulu/combat/PlayerDamageHandler.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/PlayerDamageHandler.class")
            check(b"sendNearby" in raw, "粒子只发给附近玩家（32 格内）")
            check(b"spawnNearby" in raw, "有按粒子类型发包的封装")
            check(b"PlayerLoggedInEvent" in raw,
                  "登录时补一次同步（退出重进不会显示成 0 点）")
        lang = read_json(jar, names, "assets/%s/lang/zh_cn.json" % MOD_ID)
        if lang:
            # 十四：图鉴第四条
            for key, expected in (
                    ("codex.fulu.stand_in.name", "替身阵"),
                    ("codex.fulu.stand_in.range_short", "半径 10 格"),
                    ("codex.fulu.stand_in.range", "半径 10 格，下方 10 格，上方无限"),
                    ("codex.fulu.stand_in.effect_short", "替玩家挡一次致命伤害"),
                    ("codex.fulu.stand_in.material", "阵基 ×4、阵眼 ×1、替身符 ×2"),
                    ("codex.fulu.stand_in.activate", "手持替身符右键阵眼")):
                check(lang.get(key) == expected, "图鉴 key %s = '%s'" % (key, expected))
            check("codex.fulu.stand_in.effect" in lang, "已精通的详细效果文案也在")
        if "com/lengyang/fulu/codex/FormationCodex.class" in names:
            raw = jar.read("com/lengyang/fulu/codex/FormationCodex.class")
            check(b"stand_in" in raw, "图鉴列表里有替身阵这一条")
            check(b"fulu:stand_in_talisman" in raw, "替身阵的列表图标用替身符")

        print("\n-- stand-in formation (fourth) --")
        # 阵眼的第四套外观
        for path in ("assets/%s/models/block/formation_eye_stand_in.json" % MOD_ID,
                     "assets/%s/textures/block/formation_eye_stand_in.png" % MOD_ID):
            check(path in names, "%s 已打包" % path)
        lang = read_json(jar, names, "assets/%s/lang/zh_cn.json" % MOD_ID)
        if lang:
            check(lang.get("msg.fulu.formation_eye.state.stand_in") == "替身符",
                  "阵眼状态里显示 '替身符'")
            check(lang.get("msg.fulu.formation_eye.stand_in_on") == "装上替身符",
                  "装上替身符的提示文案")
            check(lang.get("msg.fulu.formation_eye.stand_in_off") == "取下替身符",
                  "取下替身符的提示文案")
        # 玩家数据 + 网络同步
        if "com/lengyang/fulu/standin/StandInData.class" in names:
            raw = jar.read("com/lengyang/fulu/standin/StandInData.class")
            for field in (b"K_STAND_IN", b"K_RESISTANCE", b"K_SHIELD"):
                check(field in raw, "玩家数据里有 %s（三个要求之一）" % field.decode())
            check(b"SHIELD_FULL" in raw, "次数盾满值是常量（20）")
            check(b"SHIELD_COST_PER_HIT" in raw, "每挨一下扣多少是常量（4）")
            check(b"STAND_IN_RESISTANCE_LEVEL" in raw, "替死挂的抗性等级是常量（VIII）")
            check(b"syncIfServer" in raw, "改动后主动同步给客户端")
        if "com/lengyang/fulu/network/ModNetwork.class" in names:
            raw = jar.read("com/lengyang/fulu/network/ModNetwork.class")
            check(b"NetworkRegistry" in raw, "用 47.x 的 NetworkRegistry（不是 1.21 的 ChannelBuilder）")
            check(b"registerMessage" in raw, "注册了同步消息")
            check(b"PLAYER" in raw, "只发给那个玩家自己（不广播）")
        check("com/lengyang/fulu/standin/ClientStandInData.class" in names,
              "客户端有数据缓存（画血条/图案用）")
        # 受伤处理：顺序 + 三块逻辑
        if "com/lengyang/fulu/combat/PlayerDamageHandler.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/PlayerDamageHandler.class")
            check(b"hitShield" in raw, "先看次数盾")
            check(b"maxResistance" in raw,
                  "抗性提升 VIII 期间**不消耗**次数盾（用户要求）")
            check(b"RESISTANCE_UP" in raw, "再算抗性提升减伤")
            check(b"hasStandIn" in raw, "再看替身标记")
            check(b"triggerStandIn" in raw, "致命伤触发替死")
            check(b"setTalisman" in raw, "替死后把阵眼的符弄没（一个阵法只能替死一次）")
            check(b"invulnerableTime" in raw or b"f_19802_" in raw,
                  "反弹前清无敌帧（用户要求：反弹无视无敌帧）")
            check(b"PlayerTickEvent" in raw, "有玩家每刻维护（动作栏重发、碎裂倒计时）")
            # 金色粒子和碎裂音效都是**原版**常量，正式 jar 里被混淆成 f_xxxxx_，查字面名必然失败。
            # 所以改查我自己写的方法名（这两件事各有一个方法，jar 里看得到）。
            check(b"spawnShatterParticles" in raw, "替死时放金色粒子（spawnShatterParticles）")
            check(b"shatterEye" in raw, "替死时碎掉阵眼（shatterEye）")
            check(b"actionBarMessage" in raw, "替死时有动作栏文案（actionBarMessage）")
        # 全屏碎裂图案
        if "com/lengyang/fulu/client/StandInOverlay.class" in names:
            raw = jar.read("com/lengyang/fulu/client/StandInOverlay.class")
            check(b"RenderGuiEvent" in raw, "监听 HUD 渲染（全屏图案 + 附加血条）")
            check(b"stand_in_shatter" in raw, "用的是 gfx/gui/stand_in_shatter.png")
            check(b"hitShield" in raw, "次数盾也画在 HUD 上")
            check(b"register" in raw and b"EVENT_BUS" in raw, "显式注册（不靠注解扫描）")
        hearts = "assets/%s/textures/gui/shield_hearts.png" % MOD_ID
        check(hearts in names, "?????????")
        if hearts in names:
            himg = Image.open(io.BytesIO(jar.read(hearts)))
            check(himg.size == (27, 9), "????? 27x9??? 9x9?? / ? / ??")
            check(himg.mode == "RGBA", "??????????RGBA?")
        tex = "assets/%s/textures/gui/stand_in_shatter.png" % MOD_ID
        check(tex in names, "碎裂图案贴图已打包")
        if tex in names:
            img = Image.open(io.BytesIO(jar.read(tex)))
            check(img.size == (256, 256),
                  "碎裂图案是 256x256（放大到 1.5 倍仍然清楚，128 会糊）")
            check(img.mode == "RGBA", "碎裂图案带透明背景（RGBA）")

        print("\n-- stand-in talisman and resistance up --")
        # 替身符：物品 + 模型 + 贴图 + 语言 + 页签 + 有序配方
        for path in ("assets/%s/models/item/stand_in_talisman.json" % MOD_ID,
                     "assets/%s/textures/item/stand_in_talisman.png" % MOD_ID):
            check(path in names, "%s 已打包" % path)
        model = read_json(jar, names, "assets/%s/models/item/stand_in_talisman.json" % MOD_ID)
        if model:
            check(model.get("textures", {}).get("layer0") == "%s:item/stand_in_talisman" % MOD_ID,
                  "替身符模型的 layer0 指向 fulu:item/stand_in_talisman")
        lang = read_json(jar, names, "assets/%s/lang/zh_cn.json" % MOD_ID)
        if lang:
            check(lang.get("item.fulu.stand_in_talisman") == "替身符",
                  "替身符的中文名是 '替身符'")
            check(lang.get("effect.fulu.resistance_up") == "抗性提升",
                  "效果中文名是 '抗性提升'")
            check("effect.fulu.resistance_up.description" in lang,
                  "效果描述也写进语言文件了（原版悬停不渲染它，见下）")
        # 配方：空 符纸 空 / 朱砂 干草块 兽血 / 空 羽毛 空
        recipe = read_json(jar, names, "data/%s/recipes/stand_in_talisman.json" % MOD_ID)
        check(recipe is not None, "替身符配方已打包")
        if recipe:
            check(recipe.get("type") == "minecraft:crafting_shaped",
                  "替身符是**有序**合成")
            check(recipe.get("pattern") == [" P ", "CHB", " A "],
                  "图案是 空符纸空 / 朱砂干草块兽血 / 空羽毛空")
            keys = recipe.get("key", {})
            for letter, item_id, label in (
                    ("P", "%s:talisman_paper" % MOD_ID, "符纸"),
                    ("C", "%s:cinnabar" % MOD_ID, "朱砂"),
                    ("H", "minecraft:hay_block", "干草块"),
                    ("B", "%s:beast_blood" % MOD_ID, "兽血"),
                    ("A", "minecraft:feather", "羽毛")):
                check(keys.get(letter, {}).get("item") == item_id,
                      "替身符图案里的 %s = %s" % (letter, label))
            check(recipe.get("result", {}).get("item") == "%s:stand_in_talisman" % MOD_ID
                  and recipe.get("result", {}).get("count") == 1,
                  "产出 替身符 ×1")
        if "com/lengyang/fulu/registry/ModItems.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModItems.class")
            check(b"STAND_IN_TALISMAN" in raw and b"stand_in_talisman" in raw,
                  "替身符物品已登记（字段 + 注册名）")
        # 抗性提升：效果类 + 数值表 + 注册 + 受伤处理
        if "com/lengyang/fulu/effect/ResistanceUpEffect.class" in names:
            raw = jar.read("com/lengyang/fulu/effect/ResistanceUpEffect.class")
            check(b"BENEFICIAL" in raw, "效果是正面效果（MobEffectCategory.BENEFICIAL）")
            check(b"reductionFor" in raw, "有 reductionFor：按等级算减伤")
            check(b"reflects" in raw, "有 reflects：VII/VIII 才反弹")
            check(b"reflectRatio" in raw, "有 reflectRatio：反弹比例是常量")
            check(b"MAX_LEVEL" in raw, "有 MAX_LEVEL（最高 VIII）")
            check(b"DURATION_TICKS" in raw, "有 DURATION_TICKS（30 秒 = 600 刻，施加时用同一个数）")
            check(b"applyEffectTick" not in raw and b"isDurationEffectTick" not in raw,
                  "效果里没有每刻逻辑（减伤在受伤事件里做）")
        if "com/lengyang/fulu/registry/ModMobEffects.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModMobEffects.class")
            check(b"RESISTANCE_UP" in raw and b"resistance_up" in raw,
                  "效果已登记为 fulu:resistance_up")
        if "com/lengyang/fulu/combat/PlayerDamageHandler.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/PlayerDamageHandler.class")
            check(b"LivingHurtEvent" in raw, "监听受伤事件")
            check(b"Player" in raw, "只对玩家生效（用户要求）")
            check(b"RESISTANCE_UP" in raw, "读的是抗性提升这个效果")
            check(b"HitCountShield" in raw, "减伤之后交给次数盾（接缝）")
            check(b"setAmount" in raw, "改的是伤害数值")
            check(b"reflectRatio" in raw, "反弹用的是效果类里的常量")
        if "com/lengyang/fulu/combat/HitCountShield.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/HitCountShield.class")
            check(b"absorb" in raw, "次数盾有一个明确的入口 absorb(player, damage)")
        # 效果图标：18x18（原版硬性尺寸），不然是紫黑格
        tex = "assets/%s/textures/mob_effect/resistance_up.png" % MOD_ID
        check(tex in names, "抗性提升的效果图标已打包")
        if tex in names:
            img = Image.open(io.BytesIO(jar.read(tex)))
            check(img.size == (18, 18), "效果图标尺寸 %sx%s（原版要求 18x18）" % img.size)
            check(img.mode == "RGBA", "效果图标带透明通道（RGBA）")

        print("\n-- blood pool entity --")
        # 动物被玩家打死就地留一滩；玻璃瓶右键换兽血
        if "com/lengyang/fulu/entity/BloodPoolEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/entity/BloodPoolEntity.class")
            check(b"BEAST_BLOOD" in raw, "接出来的是兽血（我自己登记的字段，jar 里看得到）")
            check(b"BLOOD_PER_BOTTLE" in raw, "一瓶换几个是常量")
            check(b"NetworkHooks" in raw,
                  "用 NetworkHooks.getEntitySpawningPacket（不然客户端收不到它）")
            check(b"net/minecraft/client" not in raw, "实体本身不碰客户端类（两端安全）")
            # 注意：玻璃瓶 / discard() / isPickable() 这些**原版**名字在正式 jar 里都被
            # SRG 混淆成 f_xxxxx_/m_xxxxx_，查字面名一定失败（这个坑踩过好几次）。
            # 所以"拿瓶子换血"那一半只能由 -BloodPoolTest 的行为 + 你手上试来证明。
        if "com/lengyang/fulu/event/BloodPoolSpawner.class" in names:
            raw = jar.read("com/lengyang/fulu/event/BloodPoolSpawner.class")
            check(b"LivingDeathEvent" in raw, "监听生物死亡事件")
            check(b"Animal" in raw, "只对动物生成（猪牛羊鸡……）")
            check(b"Player" in raw, "只认玩家击杀（getKillCredit 是原版名，jar 里查不到字面名）")
            check(b"BLOOD_POOL" in raw, "在死亡位置生成 fulu:blood_pool")
        if "com/lengyang/fulu/client/render/BloodPoolRenderer.class" in names:
            raw = jar.read("com/lengyang/fulu/client/render/BloodPoolRenderer.class")
            check(b"blood_pool" in raw, "贴图是 fulu:textures/entity/blood_pool.png")
            check(b"PoseStack" in raw and b"MultiBufferSource" in raw,
                  "渲染的是四边形（拿 PoseStack + MultiBufferSource 直接画）")
            check(b"Matrix4f" in raw, "顶点用矩阵变换（不是模型）")
        check("assets/fulu/textures/entity/blood_pool.png" in names, "血泊贴图已打包")
        if "com/lengyang/fulu/registry/ModEntities.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModEntities.class")
            check(b"BLOOD_POOL" in raw, "血泊实体类型已登记")
        if "com/lengyang/fulu/client/ModClientSetup.class" in names:
            raw = jar.read("com/lengyang/fulu/client/ModClientSetup.class")
            check(b"BloodPoolRenderer" in raw, "客户端注册了血泊渲染器")

        print("\n-- cinnabar and formation base --")
        # 朱砂：物品 + 模型 + 贴图 + 语言 + 配方
        recipe = read_json(jar, names, "data/%s/recipes/cinnabar.json" % MOD_ID)
        check(recipe is not None, "data/fulu/recipes/cinnabar.json（朱砂配方）已打包")
        if recipe:
            blob = json.dumps(recipe)
            check("%s:cinnabar" % MOD_ID in blob, "配方产出的是 fulu:cinnabar")
            check("minecraft:redstone" in blob, "配方用到了红石")
            # 这一步把火药换成了煤炭（用户给的配方）
            check("minecraft:coal" in blob, "配方用到了煤炭")
            check("minecraft:gunpowder" not in blob, "配方**不再**用火药（这一步改的）")
        # 阵基：方块 + 方块物品 + 掉落表 + 配方（模型/贴图/语言由上面的 BLOCKS 循环覆盖）
        recipe = read_json(jar, names, "data/%s/recipes/formation_base.json" % MOD_ID)
        check(recipe is not None, "data/fulu/recipes/formation_base.json（阵基配方）已打包")
        if recipe:
            blob = json.dumps(recipe)
            check("%s:formation_base" % MOD_ID in blob, "配方产出的是 fulu:formation_base")
            check("minecraft:stone_bricks" in blob, "配方用到了石砖")
            check("minecraft:iron_nugget" in blob, "配方用到了铁粒")
            check("%s:cinnabar" % MOD_ID in blob, "配方用到了朱砂")
            check(recipe.get("result", {}).get("count", 1) == 4, "一次出 4 个阵基")
        # 阵基必须是"方块物品"：注册名和方块一致，否则放不下去/拿不起来
        base_item = read_json(jar, names, "assets/%s/models/item/formation_base.json" % MOD_ID)
        check(base_item is not None, "阵基的物品模型已打包")
        if base_item:
            check(base_item.get("parent") == "%s:block/formation_base" % MOD_ID,
                  "阵基的物品模型指向方块模型（这样手里和地上的样子一致）")
        tag = read_json(jar, names, "data/minecraft/tags/blocks/mineable/pickaxe.json")
        if tag:
            check("%s:formation_base" % MOD_ID in tag.get("values", []),
                  "阵基在镐子可挖标签里（少了它，挖掉什么都不掉 —— 这是最常见的一个坑）")

        for key in ("codex.fulu.title", "codex.fulu.locked", "codex.fulu.hint",
                    "codex.fulu.click_hint", "codex.fulu.unknown", "codex.fulu.need_player",
                    "codex.fulu.detail.name", "codex.fulu.detail.range",
                    "codex.fulu.detail.effect", "codex.fulu.detail.material",
                    "codex.fulu.detail.activate", "codex.fulu.detail.deactivate",
                    "codex.fulu.infighting.name", "codex.fulu.infighting.range_short",
                    "codex.fulu.infighting.range", "codex.fulu.infighting.effect_short",
                    "codex.fulu.infighting.effect", "codex.fulu.infighting.material",
                    "codex.fulu.infighting.activate", "codex.fulu.infighting.deactivate"):
            for lang in ("zh_cn", "en_us"):
                data = read_json(jar, names, "assets/%s/lang/%s.json" % (MOD_ID, lang))
                check(data is not None and key in data, "%s 里有 %s" % (lang, key))
        # 物品的"名字和说明"：名字在上面的 ITEMS 循环里查过了，这里查说明两行
        for key in ("item.fulu.formation_codex.desc", "item.fulu.formation_codex.howto"):
            for lang in ("zh_cn", "en_us"):
                data = read_json(jar, names, "assets/%s/lang/%s.json" % (MOD_ID, lang))
                check(data is not None and key in data, "%s 里有 %s" % (lang, key))

        print("\n-- codex screen (step 13) --")
        # 界面的两类危险：
        #   1. 客户端类被公共类引用 -> 专用服务端 NoClassDefFoundError
        #   2. 菜单类型没注册 -> 右键之后界面打不开
        #
        # 另外钉死一个**已经犯过的错**：界面曾经自己去读客户端的计分板，
        # 而原版只把"显示中的目标"的分数同步给客户端 —— 隐藏目标收不到，
        # 于是进度明明记下了、界面却一直显示"？？？"（实测截图确认过）。
        # 所以：解锁位必须走菜单的 ContainerData，界面里**不许出现 Scoreboard**。
        if "com/lengyang/fulu/client/screen/CodexScreen.class" in names:
            raw = jar.read("com/lengyang/fulu/client/screen/CodexScreen.class")
            check(b"Scoreboard" not in raw and b"getOrCreatePlayerScore" not in raw,
                  "界面不碰计分板（隐藏目标不会同步给客户端 —— 这是踩过的坑）")
            check(b"getData" in raw, "解锁位是从菜单的同步数据里读的（getData）")
            # 注意：DATA_UNLOCKS 是 static final int，编译时被**内联成字面量 3**了，
            # 这个类里根本没有这个名字 —— 想确认读的是哪一格，只能靠
            # "菜单那边确实把解锁位放在 DATA_UNLOCKS 上"（下面 CodexMenu 那一段）。
            # （同一个内联坑这次已经是第三次踩到了。）
            check(b"renderBackground" not in raw or b"super" in raw,
                  "背景交给父类画一次（原来自己画了又被父类盖掉）")
        if "com/lengyang/fulu/menu/CodexMenu.class" in names:
            raw = jar.read("com/lengyang/fulu/menu/CodexMenu.class")
            check(b"net/minecraft/client" not in raw,
                  "CodexMenu 不引用任何客户端类（它在服务端也要能加载）")
            check(b"MODE_CODEX" in raw and b"MODE_EYE" in raw, "两种模式：图鉴 / 阵眼")
            check(b"ContainerData" in raw, "阵眼那几个数字走 ContainerData（逐刻同步，数字是活的）")
            check(b"DATA_UNLOCKS" in raw, "解锁位也在同步数据里（DATA_UNLOCKS）")
            check(b"net/minecraft/world/inventory/Slot" not in raw,
                  "菜单里没有物品格（原来 36 个格子全在 (0,0)，左上角悬停会弹出物品提示）")
            check(b"quickMoveStack" in raw or b"m_7648_" in raw, "实现了 quickMoveStack（没有它界面一关会掉东西）")
        if "com/lengyang/fulu/item/FormationCodexItem.class" in names:
            raw = jar.read("com/lengyang/fulu/item/FormationCodexItem.class")
            check(b"net/minecraft/client" not in raw,
                  "图鉴物品不引用任何客户端类（右键开界面必须两端安全）")
            check(b"NetworkHooks" in raw, "用 NetworkHooks.openScreen 通知客户端开界面")
            check(b"m_6227_" in raw or b"useOn" in raw, "右键方块也能开（useOn：拿图鉴对阵眼用）")
        if "com/lengyang/fulu/registry/ModMenus.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModMenus.class")
            check(b"net/minecraft/client" not in raw, "菜单登记表本身不碰客户端类")
            check(b"codex" in raw, "注册了 codex 这个菜单类型")
        if "com/lengyang/fulu/client/ModClientSetup.class" in names:
            raw = jar.read("com/lengyang/fulu/client/ModClientSetup.class")
            check(b"CodexScreen" in raw, "客户端 setup 里把菜单类型接到界面上")
            check(b"MenuScreens" in raw, "用的是 MenuScreens.register")
        if "com/lengyang/fulu/client/screen/CodexScreen.class" in names:
            raw = jar.read("com/lengyang/fulu/client/screen/CodexScreen.class")
            check(b"FormationCodex" in raw, "界面用的是那份共用数据（没有第二份阵法表）")
            check(b"EditBox" in raw, "底部的搜索框是原版 EditBox")
            check(b"Button" in raw, "有关闭按钮")
            check(b"detailLines" in raw, "右侧详情调的是那套可测的纯逻辑")
            check(b"renderItem" in raw or b"m_280164_" in raw or b"m_280614_" in raw,
                  "列表图标是渲染的物品图标")
        for key in ("codex.fulu.screen.search", "codex.fulu.screen.close",
                    "codex.fulu.screen.not_unlocked", "codex.fulu.screen.no_match"):
            for lang in ("zh_cn", "en_us"):
                data = read_json(jar, names, "assets/%s/lang/%s.json" % (MOD_ID, lang))
                check(data is not None and key in data, "%s 里有 %s" % (lang, key))

        print("\n-- codex content (the wording you specified) --")
        # 第四部分：图鉴里乱神阵的正文。这里**逐字**核对 —— 你要的是这些字，
        # 那自检就认这些字（写错一个字、改了标点都会红）。
        EXPECTED_ZH = {
            "codex.fulu.detail.name": "名称：%s",
            "codex.fulu.detail.range": "范围：%s",
            "codex.fulu.detail.effect": "效果：%s",
            "codex.fulu.detail.material": "材料：%s",
            "codex.fulu.detail.activate": "激活方式：%s",
            "codex.fulu.detail.deactivate": "关闭方式：%s",
            "codex.fulu.infighting.name": "乱神阵",
            "codex.fulu.infighting.range_short": "半径 10 格",
            "codex.fulu.infighting.range": "半径 10 格，下方 10 格，上方无限",
            "codex.fulu.infighting.effect_short": "让范围内怪物互相攻击",
            "codex.fulu.infighting.effect": "范围内怪物互相攻击，内讧时会把自身攻击负面效果传给其他怪物",
            "codex.fulu.infighting.material": "阵基 ×4、阵眼 ×1、乱神符 ×2",
            "codex.fulu.infighting.activate": "手持乱神符右键阵眼",
            "codex.fulu.infighting.deactivate": "空手右键阵眼取回乱神符",
        }
        zh = read_json(jar, names, "assets/%s/lang/zh_cn.json" % MOD_ID)
        if zh:
            for key, want in EXPECTED_ZH.items():
                got = zh.get(key)
                check(got == want, "%s = %r%s" % (key, want,
                      "" if got == want else "  （实际是 %r）" % got))
            # 再把两档正文拼出来核对一遍：这才是玩家真正看到的那几行
            def line(label, value):
                return EXPECTED_ZH["codex.fulu.detail." + label] % EXPECTED_ZH[value]

            seen_lines = [line("name", "codex.fulu.infighting.name"),
                          line("range", "codex.fulu.infighting.range_short"),
                          line("effect", "codex.fulu.infighting.effect_short")]
            mastered_lines = [line("name", "codex.fulu.infighting.name"),
                              line("range", "codex.fulu.infighting.range"),
                              line("effect", "codex.fulu.infighting.effect"),
                              line("material", "codex.fulu.infighting.material"),
                              line("activate", "codex.fulu.infighting.activate"),
                              line("deactivate", "codex.fulu.infighting.deactivate")]
            check(len(seen_lines) == 3, "已见闻是 3 行：%s" % " / ".join(seen_lines))
            check(len(mastered_lines) == 6, "已精通是 6 行：%s" % " / ".join(mastered_lines))
            # 拼出来的 9 行打印出来给人对一眼（自检的输出也是证据）
            for l in seen_lines:
                print("    [已见闻] " + l)
            for l in mastered_lines:
                print("    [已精通] " + l)

        print("\n-- flying sword talisman --")
        # 飞剑符：物品 + 模型 + 贴图（上面的 ITEMS 循环覆盖）+ 配方
        recipe = read_json(jar, names, "data/%s/recipes/flying_sword_talisman.json" % MOD_ID)
        check(recipe is not None, "data/fulu/recipes/flying_sword_talisman.json（飞剑符配方）已打包")
        if recipe:
            check(recipe.get("type") == "minecraft:crafting_shaped",
                  "飞剑符是**有序**合成")
            check(recipe.get("pattern") == [" P ", "BSA", " C "],
                  "飞剑符图案是 空符纸空 / 兽血铁剑羽毛 / 空朱砂空")
            key_map = recipe.get("key", {})
            for letter, item_id, label in (
                    ("P", "%s:talisman_paper" % MOD_ID, "符纸"),
                    ("B", "%s:beast_blood" % MOD_ID, "兽血"),
                    ("S", "minecraft:iron_sword", "铁剑"),
                    ("A", "minecraft:feather", "羽毛"),
                    ("C", "%s:cinnabar" % MOD_ID, "朱砂")):
                check(key_map.get(letter, {}).get("item") == item_id,
                      "飞剑符图案里的 %s = %s" % (letter, label))
            check(recipe.get("result", {}).get("item") == "%s:flying_sword_talisman" % MOD_ID,
                  "产出飞剑符")
        if "com/lengyang/fulu/block/TalismanKind.class" in names:
            raw = jar.read("com/lengyang/fulu/block/TalismanKind.class")
            # ★ 四种都要查。这里原来只有前三种 —— 于是"反重力符装不进去"那次，
            #   自检也没能提醒我：测试里写了一份手写清单，和代码里那份一样会漏。
            for kind in (b"EMPTY", b"CHAOS", b"SWORD", b"ANTIGRAVITY", b"STAND_IN"):
                check(kind in raw, "TalismanKind 有 %s" % kind.decode())
            check(b"empty" in raw and b"chaos" in raw and b"sword" in raw
                  and b"antigravity" in raw and b"stand_in" in raw,
                  "五种状态在方块状态里写的是小写名（含 stand_in）")
            # 注册名只出现在 ModItems 里；这里查的是**字段名**（枚举指向哪个物品）
            check(b"FLYING_SWORD_TALISMAN" in raw, "SWORD 指向飞剑符那个物品字段")
            check(b"CHAOS_TALISMAN" in raw, "CHAOS 指向乱神符那个物品字段")
        for key in ("msg.fulu.formation_eye.sword_on", "msg.fulu.formation_eye.sword_off",
                    "msg.fulu.formation_eye.already_has_talisman",
                    "msg.fulu.formation_eye.state.empty", "msg.fulu.formation_eye.state.chaos",
                    "msg.fulu.formation_eye.state.sword"):
            for lang in ("zh_cn", "en_us"):
                data = read_json(jar, names, "assets/%s/lang/%s.json" % (MOD_ID, lang))
                check(data is not None and key in data, "%s 里有 %s" % (lang, key))

        print("\n-- sword glow particle: removed on purpose --")
        # 用户后来要求"剑光粒子不要了"：拖尾那一套（粒子类型 / 客户端类 / 定义 JSON /
        # 贴图）整个删掉。这里断言它们**确实不在包里**，免得哪天又被加回来。
        if "com/lengyang/fulu/registry/ModParticles.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModParticles.class")
            check(b"sword_glow" not in raw, "ModParticles 里不再登记 sword_glow")
        check("com/lengyang/fulu/client/particle/SwordGlowParticle.class" not in names,
              "剑光的粒子类已经删掉")
        check("assets/%s/particles/sword_glow.json" % MOD_ID not in names,
              "剑光的粒子定义 JSON 已经删掉")
        check("assets/%s/textures/particle/sword_glow.png" % MOD_ID not in names,
              "剑光的贴图已经删掉")
        if "com/lengyang/fulu/entity/FlyingSwordEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/entity/FlyingSwordEntity.class")
            check(b"emitTrail" not in raw and b"TRAIL_PER_TICK" not in raw,
                  "飞剑实体里不再撒拖尾粒子（命中爆发保留）")
            check(b"SWORD_BURST" in raw, "命中爆发还在用（这个是要保留的）")

        if "com/lengyang/fulu/block/FormationEyeBlockEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
            # 飞剑这一步：方块实体不再自己算飞行，它只负责"放出实体"。
            check(b"FlyingSwordEntity" in raw, "阵眼放出的是一把飞剑实体")
            check(b"SWORD_INTERVAL_TICKS" in raw, "还是每秒放一把")
            check(b"SWORD_DAMAGE" in raw, "伤害仍然是 5 点（作为参数传给实体）")
            # 齐射：一次 10 把、按怪物数量分配、出生点散开
            check(b"SWORDS_PER_VOLLEY" in raw, "有每发几把的常量（10）")
            check(b"MAX_TARGETS" in raw, "有最多打几只的常量（10）")
            check(b"launchVolley" in raw, "有 launchVolley（一次齐射）")
            check(b"spawnSword" in raw, "有 spawnSword（单把的出生点与瞄准）")
            check(b"SWORD_SPREAD" in raw, "有散开幅度常量（10 把不叠在一点）")
            check(b"Comparator" in raw, "按到阵眼的距离排序（超过 10 只时只打最近的）")
            check(b"launchSword" not in raw, "旧的单发 launchSword 已经删掉")
            # f_11687_ = SoundEvents.ARROW_SHOOT（实测：这个类里只有这一个音效常量）
            check(b"f_11687_" in raw, "发射时放的是箭矢声（SRG f_11687_）= 一次齐射只播一次")
            check(b"SWORD_GLOW" not in raw, "阵眼那边也不再提剑光粒子")
            # 注意：飞行/命中/消失的断言在 flying sword entity 那一段（类已经搬走了）

        if "com/lengyang/fulu/client/particle/SwordBurstParticle.class" in names:
            raw = jar.read("com/lengyang/fulu/client/particle/SwordBurstParticle.class")
            check(b"TextureSheetParticle" in raw, "命中爆发粒子也继承原版基类")
            check(b"m_7556_" in raw, "实现了 getRenderType（SRG m_7556_）")
            check(b"SwordBurstParticle$Provider" in raw
                  or "com/lengyang/fulu/client/particle/SwordBurstParticle$Provider.class" in names,
                  "有 Provider")
            # 原版字段名（friction 之类）打包后是 SRG 名，不好猜也不好读；
            # 这里改查**我自己的常量** —— 它们不会被混淆，而且直接编码了用户的要求：
            # 生命 10~15 刻、颜色从白(0.98/0.90)渐变到金(0.72/0.25)。
            check(b"MIN_LIFE" in raw and b"MAX_LIFE" in raw, "生命上下限写成常量（10~15 刻）")
            check(b"START_R" in raw and b"END_R" in raw, "起止颜色写成常量（白 -> 金）")

        print("\n-- flying sword entity --")
        if "com/lengyang/fulu/registry/ModEntities.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModEntities.class")
            check(b"flying_sword" in raw, "登记了实体类型 fulu:flying_sword")
            check(b"FlyingSwordEntity" in raw, "实体类型指向 FlyingSwordEntity")
            check(b"net/minecraft/client" not in raw, "实体登记表不碰客户端类（服务端也要能加载）")
        if "com/lengyang/fulu/entity/FlyingSwordEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/entity/FlyingSwordEntity.class")
            for name, label in ((b"SPEED", "飞行速度常量"),
                                (b"DAMAGE", "伤害值常量"),
                                (b"targetUuid", "记住目标怪物（UUID，区块重载也能找回来）"),
                                (b"SEEK_RANGE", "没目标时自己找怪的范围"),
                                (b"MAX_LIFE", "最长寿命（5 秒没打到就消失）"),
                                (b"isBlocked", "撞到方块就消失"),
                                (b"locked", "锁定目标（锁定的剑不改追别人）"),
                                (b"HIT_SOUND", "命中时的音效常量")):
                check(name in raw, "飞剑实体有 %s" % label)
            # f_12316_ = SoundEvents.PLAYER_ATTACK_STRONG（这个类里只有这一个音效常量）
            check(b"f_12316_" in raw, "命中时放的是剑砍声（SRG f_12316_）")
            # f_19802_ = LivingEntity.invulnerableTime（用 javap 在 hit() 里看到的）：
            # 打之前清零，否则同一轮齐射只有第一把的伤害有效
            check(b"f_19802_" in raw, "命中前把目标的无敌帧清零（每把剑各算各的 5 点）")
            check(b"TRAIL_SPACING" not in raw, "旧的『按格撒粒子』已改成『每刻 1~2 颗』")
            check(b"hurt" in raw or b"m_6469_" in raw, "命中时造成伤害（hurt）")
            check(b"net/minecraft/client" not in raw,
                  "实体类不引用客户端类（服务端也要能跑）")
        if "com/lengyang/fulu/registry/ModModelLayers.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModModelLayers.class")
            check(b"flying_sword" in raw, "模型层登记了 flying_sword")
        if "com/lengyang/fulu/client/model/FlyingSwordShape.class" in names:
            raw = jar.read("com/lengyang/fulu/client/model/FlyingSwordShape.class")
            check(b"LayerDefinition" in raw, "形状用 LayerDefinition 定义")
            # 25 个方块没有各自的名字了 —— 它们被并进三根骨骼里，
            # 所以能查的是骨骼名和"确实在摆方块"。
            for bone in (b"blade", b"guard", b"hilt"):
                check(bone in raw, "有 %s 骨骼" % bone.decode())
            check(b"CubeListBuilder" in raw, "骨骼里在摆方块（用了 CubeListBuilder）")
        if "com/lengyang/fulu/client/model/FlyingSwordModel.class" in names:
            raw = jar.read("com/lengyang/fulu/client/model/FlyingSwordModel.class")
            check(b"setupAnim" in raw, "动画写在 setupAnim 里（每实体每帧一次）")
            # 四段动画 + 三根骨骼的引用
            for name in (b"animIdle", b"animFly", b"animLaunch", b"animHit",
                         b"blade", b"guard", b"hilt"):
                check(name in raw, "动画里有 %s" % name.decode())
            # 真的在写骨骼字段：f_104200_..f_104205_ 是 ModelPart 的 x/y/z + 三个旋转，
            # f_233553_..f_233555_ 是三个缩放（用 javap 看 putfield 得到的 SRG 名）
            check(b"f_104204_" in raw or b"f_104203_" in raw,
                  "动画会写骨骼的旋转（自转 / 摆动）")
            check(b"f_233553_" in raw, "动画会写骨骼的缩放（launch / hit 的伸缩）")
            check(b"HIT_SHAKE" in raw, "命中震动表是常量")
            check(b"BOB_UNITS" in raw, "浮动幅度是常量（0.05 格）")
            check(b"COUNTER_SPIN" in raw, "护手/剑柄反向旋转是常量")
        if "com/lengyang/fulu/client/render/FlyingSwordRenderer.class" in names:
            raw = jar.read("com/lengyang/fulu/client/render/FlyingSwordRenderer.class")
            check(b"EntityRenderer" in raw, "渲染器继承 EntityRenderer")
            check(b"flying_sword" in raw, "指向 textures/entity/flying_sword.png")
            check(b"PoseStack" in raw, "朝向施加在 PoseStack 上（动画和朝向分开）")
            check(b"BLADE_LENGTH" in raw, "有剑长常量（和符文环绕共用同一个数）")
            # ★ 最关键的一条：继承 EntityRenderer 时没人会替你调用 setupAnim，
            #   渲染器必须自己调 —— 不调的话动画一行都不生效，而且不报错。
            check(b"setupAnim" in raw, "渲染器**自己调用**了 setupAnim（否则动画不会生效）")
            check(b"animAge" in raw, "用的是实体的动画进度（animAge）")
        if "com/lengyang/fulu/entity/FlyingSwordEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/entity/FlyingSwordEntity.class")
            check(b"emitRunes" in raw, "有 emitRunes（符文环绕）")
            check(b"RUNE_COUNT" in raw, "符文有几颗是常量")
            check(b"RUNE_SPIRALS" in raw, "绕几圈是常量")
            check(b"RUNE_RADIUS" in raw, "离剑轴多远是常量")
            check(b"RUNE_PERIOD_TICKS" in raw, "转一圈多少刻是常量（一直转）")
            check(b"calculateViewVector" in raw or b"m_20182_" in raw,
                  "用实体朝向当剑轴（算符文位置）")
            # 动画状态机
            for name in (b"ANIM_IDLE", b"ANIM_FLY", b"ANIM_LAUNCH", b"ANIM_HIT",
                         b"ANIM_LAUNCH_TICKS", b"ANIM_HIT_TICKS", b"animState", b"animAge"):
                check(name in raw, "实体里有 %s（动画状态/时长）" % name.decode())
            check(b"DATA_ANIM" in raw and b"DATA_HIT_AGE" in raw,
                  "动画状态同步给客户端（否则客户端不知道该播哪段）")
        if "com/lengyang/fulu/entity/FlyingSwordEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/entity/FlyingSwordEntity.class")
            check(b"emitRunes" in raw, "有 emitRunes（符文环绕）")
            check(b"RUNE_COUNT" in raw, "符文有几颗是常量")
            check(b"RUNE_SPIRALS" in raw, "绕几圈是常量")
            check(b"RUNE_RADIUS" in raw, "离剑轴多远是常量")
            check(b"RUNE_PERIOD_TICKS" in raw, "转一圈多少刻是常量（一直转）")
            check(b"calculateViewVector" in raw or b"m_20182_" in raw,
                  "用实体朝向当剑轴（算符文位置）")
        if "com/lengyang/fulu/client/particle/RuneMoteParticle.class" in names:
            raw = jar.read("com/lengyang/fulu/client/particle/RuneMoteParticle.class")
            check(b"TextureSheetParticle" in raw, "符文粒子继承原版基类")
            check(b"m_7556_" in raw, "实现了 getRenderType（SRG m_7556_）")
        if "com/lengyang/fulu/client/render/FlyingSwordRenderer.class" in names:
            raw = jar.read("com/lengyang/fulu/client/render/FlyingSwordRenderer.class")
            check(b"EntityRenderer" in raw, "渲染器继承 EntityRenderer")
            check(b"flying_sword" in raw, "指向 textures/entity/flying_sword.png")
        if "com/lengyang/fulu/client/ModClientSetup.class" in names:
            raw = jar.read("com/lengyang/fulu/client/ModClientSetup.class")
            check(b"FlyingSwordRenderer" in raw, "客户端注册了飞剑渲染器")
            check(b"FlyingSwordModel" in raw, "客户端注册了飞剑模型层")
        tex = "assets/%s/textures/entity/flying_sword.png" % MOD_ID
        check(tex in names, "实体贴图 %s 已打包" % tex)
        if tex in names:
            img = Image.open(io.BytesIO(jar.read(tex)))
            # 尺寸由转换脚本决定（现在的模型来自 bbmodel，贴图 128x128）；
            # 关键不是具体数字，而是"方形 + 2 的幂"——Java 模型贴图必须能整除。
            check(img.width == img.height and (img.width & (img.width - 1)) == 0,
                  "实体贴图 %dx%d（方形，2 的幂）" % img.size)
            check(img.mode == "RGBA", "实体贴图色彩模式 %s" % img.mode)

        print("\n-- who counts as an enemy (the scan bug) --")
        # 阵眼原来用 Monster 类筛怪，结果史莱姆/岩浆怪/恶魂/幻翼/疣猪兽（只实现 Enemy
        # 接口）一个都扫不到。现在规则统一在 FormationTarget 里，用 Enemy 接口。
        if "com/lengyang/fulu/combat/FormationTarget.class" in names:
            raw = jar.read("com/lengyang/fulu/combat/FormationTarget.class")
            check(b"Enemy" in raw, "用 Enemy 接口判断（不是 Monster 类）")
            check(b"TamableAnimal" in raw,
                  "排除了可驯服的动物（isTame 的调用被混淆成 SRG 名，行为由测试证明）")
            check(b"net/minecraft/client" not in raw, "这是公共类，服务端也要用")
        if "com/lengyang/fulu/block/FormationEyeBlockEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
            check(b"FormationTarget" in raw, "阵眼的扫描用了共享的判定（不再自己写一份）")
            check(b"net/minecraft/world/entity/Mob" in raw, "扫的是 Mob（Enemy 的实现者）")
        if "com/lengyang/fulu/entity/FlyingSwordEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/entity/FlyingSwordEntity.class")
            check(b"FormationTarget" in raw, "飞剑自己找目标时也用同一个判定")

        print("\n-- anti-gravity formation --")
        if "com/lengyang/fulu/block/TalismanKind.class" in names:
            raw = jar.read("com/lengyang/fulu/block/TalismanKind.class")
            check(b"ANTIGRAVITY" in raw, "符的种类里有反重力符（第四种状态）")
            check(b"antigravity" in raw, "方块状态的取值是 antigravity")
            check(b"msg.fulu.formation_eye.state." in raw,
                  "状态文案 key 是拼出来的（前缀在，antigravity 就是第四种）")
        if "com/lengyang/fulu/registry/ModMobEffects.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModMobEffects.class")
            check(b"antigravity" in raw, "登记了状态效果 fulu:antigravity")
            check(b"AntiGravityEffect" in raw, "效果指向 AntiGravityEffect")
        if "com/lengyang/fulu/effect/AntiGravityEffect.class" in names:
            raw = jar.read("com/lengyang/fulu/effect/AntiGravityEffect.class")
            check(b"applyEffectTick" not in raw and b"isDurationEffectTick" not in raw,
                  "效果里没有任何每刻逻辑（纯标记；无限时长不保证被 tick）")
            check(b"net/minecraft/client" not in raw, "效果是公共类，服务端要能跑")
        if "com/lengyang/fulu/block/FormationEyeBlockEntity.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
            check(b"applyAntiGravity" in raw, "有反重力那一支（applyAntiGravity）")
            check(b"FLOAT_DURATION_TICKS" in raw, "持续时间是常量（3 秒）")
        if "com/lengyang/fulu/codex/FormationCodex.class" in names:
            raw = jar.read("com/lengyang/fulu/codex/FormationCodex.class")
            for entry in (b"infighting", b"sword", b"antigravity"):
                check(entry in raw, "图鉴里有 %s 这一条" % entry.decode())
            check(b"codex.fulu.antigravity.material" in raw
                  or b"codex.fulu.antigravity.effect" in raw,
                  "反重力阵的详情 key 都写对了")
        blockstate = read_json(jar, names, "assets/%s/blockstates/formation_eye.json" % MOD_ID)
        if blockstate:
            variants = blockstate.get("variants", {})
            for state in ("talisman=empty", "talisman=chaos", "talisman=sword",
                          "talisman=antigravity", "talisman=stand_in"):
                check(state in variants, "方块状态里有 %s" % state)
        recipe = read_json(jar, names, "data/%s/recipes/anti_gravity_talisman.json" % MOD_ID)
        check(recipe is not None, "反重力符配方已打包")
        if recipe:
            check(recipe.get("type") == "minecraft:crafting_shaped",
                  "反重力符是**有序**合成")
            check(recipe.get("pattern") == [" P ", "HBA", " C "],
                  "反重力符图案是 空符纸空 / 潜影壳兽血羽毛 / 空朱砂空")
            key_map = recipe.get("key", {})
            for letter, item_id, label in (
                    ("P", "fulu:talisman_paper", "符纸"),
                    ("H", "minecraft:shulker_shell", "潜影壳"),
                    ("B", "fulu:beast_blood", "兽血"),
                    ("A", "minecraft:feather", "羽毛"),
                    ("C", "fulu:cinnabar", "朱砂")):
                check(key_map.get(letter, {}).get("item") == item_id,
                      "反重力符图案里的 %s = %s" % (letter, label))
            check(recipe.get("result", {}).get("item") == "fulu:anti_gravity_talisman",
                  "配方产出反重力符")
        tex = "assets/%s/textures/mob_effect/antigravity.png" % MOD_ID
        check(tex in names, "状态效果图标 %s 已打包" % tex)
        if tex in names:
            img = Image.open(io.BytesIO(jar.read(tex)))
            check(img.size == (18, 18), "效果图标尺寸 %sx%s（原版要求 18x18）" % img.size)

        print("\n-- creative tab (the 'no items' report) --")
        if "com/lengyang/fulu/registry/ModCreativeTabs.class" in names:
            raw = jar.read("com/lengyang/fulu/registry/ModCreativeTabs.class")
            check(b"DeferredRegister" in raw or b"CreativeModeTab" in raw,
                  "登记的是创造模式物品栏（DeferredRegister）")
            # ★ 本模组自己的页签里必须列全九个物品，一个都不能漏
            for field, label in ((b"TALISMAN_PAPER", "符纸"),
                                 (b"CINNABAR", "朱砂"),
                                 (b"BEAST_BLOOD", "兽血"),
                                 (b"CHAOS_TALISMAN", "乱神符"),
                                 (b"FLYING_SWORD_TALISMAN", "飞剑符"),
                                 (b"ANTI_GRAVITY_TALISMAN", "反重力符"),
                                 (b"FORMATION_CODEX", "阵法图鉴"),
                                 (b"FORMATION_BASE_ITEM", "阵基"),
                                 (b"FORMATION_EYE_ITEM", "阵眼")):
                check(field in raw, "自己的页签里有 %s" % label)
            # ★ 还要进原版页签，否则玩家在原版那一排里翻不到
            check(b"BuildCreativeModeTabContentsEvent" in raw,
                  "订阅了 BuildCreativeModeTabContentsEvent（把物品也加进原版页签）")
            # 原版页签的那三个键是 CreativeModeTabs 的静态字段，打包后只剩 SRG 名
            # （javap 查出来的）：f_256968_ = INGREDIENTS、
            # f_256869_ = TOOLS_AND_UTILITIES、f_256788_ = BUILDING_BLOCKS
            check(b"CreativeModeTabs" in raw, "引用了原版页签表")
            check(b"f_256968_" in raw, "材料进原版『材料』页（SRG f_256968_）")
            check(b"f_256869_" in raw, "符和图鉴进原版『工具与实用物品』页（SRG f_256869_）")
            check(b"f_256788_" in raw, "阵眼阵基进原版『建筑方块』页（SRG f_256788_）")
            check(b"itemGroup.fulu.main" in raw, "页签标题有语言 key")
        # 页签标题的中英文案
        for lang, label in ((MOD_ID + ":assets/" + MOD_ID + "/lang/zh_cn.json", "中文"),
                            (MOD_ID + ":assets/" + MOD_ID + "/lang/en_us.json", "英文")):
            path = lang.split(":", 1)[1]
            data = read_json(jar, names, path)
            if data:
                check("itemGroup.fulu.main" in data,
                      "页签标题 %s 文案在（%s）" % (label, path))

        print("\n-- 'the anti-gravity talisman cannot be fitted' --")
        if "com/lengyang/fulu/block/TalismanKind.class" in names:
            raw = jar.read("com/lengyang/fulu/block/TalismanKind.class")
            # 四种符的物品必须**都**在这个类里（item() 的 switch 是穷尽的，
            # 而 of() 现在反过来问 item() —— 只有一处列举符的种类，加新符不会再漏）
            for field, label in ((b"CHAOS_TALISMAN", "乱神符"),
                                 (b"FLYING_SWORD_TALISMAN", "飞剑符"),
                                 (b"ANTI_GRAVITY_TALISMAN", "反重力符")):
                check(field in raw, "TalismanKind 认得 %s 的物品" % label)
            check(b"byName" in raw, "存档里按名字找回种类")
            # ★ byName 和 of() 都靠 values() 遍历整个枚举，而不是手写清单 ——
            #   这正是上次"反重力符装不进去"的根因：手写的 if 链漏了新符。
            #   只要还在用 values()，加新符就不可能再对不上。
            check(b"values" in raw, "用 values() 遍历枚举（不是手写清单，加新符不会再漏）")
        if "com/lengyang/fulu/block/FormationEyeBlock.class" in names:
            raw = jar.read("com/lengyang/fulu/block/FormationEyeBlock.class")
            check(b"TalismanKind" in raw, "装符读的是手里的符种类")
            check(b"antigravity_on" in raw and b"antigravity_off" in raw,
                  "装上/取下反重力符各有自己的文案（不是复用飞剑符那句）")

        print("\n-- blocks --")
        for name in BLOCKS:
            state_path = "assets/%s/blockstates/%s.json" % (MOD_ID, name)
            state = read_json(jar, names, state_path)
            check(state is not None, "%s 已打包" % state_path)
            if state:
                variants = state.get("variants", {})
                if name == "formation_eye":
                    # 第十步是一套"有符"外观；这一步符变成两种，于是三套模型。
                    check("talisman=empty" in variants and "talisman=chaos" in variants
                          and "talisman=sword" in variants,
                          "blockstates/%s.json 有 talisman 的三种变体" % name)
                    check(variants.get("talisman=empty", {}).get("model") == "%s:block/%s" % (MOD_ID, name),
                          "空 -> %s:block/%s" % (MOD_ID, name))
                    check(variants.get("talisman=chaos", {}).get("model") == "%s:block/%s_talisman" % (MOD_ID, name),
                          "乱神符 -> %s:block/%s_talisman" % (MOD_ID, name))
                    check(variants.get("talisman=sword", {}).get("model") == "%s:block/%s_sword" % (MOD_ID, name),
                          "飞剑符 -> %s:block/%s_sword" % (MOD_ID, name))
                else:
                    # 没有属性的方块：变体 key 是空字符串，只有一个模型
                    check(variants.get("", {}).get("model") == "%s:block/%s" % (MOD_ID, name),
                          "blockstates/%s.json 只有一个变体（没有属性），指向 %s:block/%s"
                          % (name, MOD_ID, name))

            block_model_path = "assets/%s/models/block/%s.json" % (MOD_ID, name)
            block_model = read_json(jar, names, block_model_path)
            check(block_model is not None, "%s 已打包" % block_model_path)
            if block_model:
                check(block_model.get("parent") == "minecraft:block/cube_all",
                      "models/block/%s.json parent = %r" % (name, block_model.get("parent")))
                all_tex = block_model.get("textures", {}).get("all")
                check(all_tex == "%s:block/%s" % (MOD_ID, name),
                      "models/block/%s.json textures.all = %r" % (name, all_tex))

            item_model_path = "assets/%s/models/item/%s.json" % (MOD_ID, name)
            item_model = read_json(jar, names, item_model_path)
            check(item_model is not None, "%s（方块物品模型）已打包" % item_model_path)
            if item_model:
                check(item_model.get("parent") == "%s:block/%s" % (MOD_ID, name),
                      "models/item/%s.json parent = %r" % (name, item_model.get("parent")))

            check_texture(jar, names, "assets/%s/textures/block/%s.png" % (MOD_ID, name))
            # 第十步第二部分：第二套外观（有符）—— 只有阵眼有"插了符"这个状态，
            # 阵基是普通方块，没有第二套模型，所以这里要按名字限定。
            if name == "formation_eye":
                alt_model_path = "assets/%s/models/block/%s_talisman.json" % (MOD_ID, name)
                alt_model = read_json(jar, names, alt_model_path)
                check(alt_model is not None, "%s（有符那套模型）已打包" % alt_model_path)
                if alt_model:
                    all_tex = alt_model.get("textures", {}).get("all")
                    check(all_tex == "%s:block/%s_talisman" % (MOD_ID, name),
                          "models/block/%s_talisman.json 指向新贴图（现在指向 %r）" % (name, all_tex))
                check_texture(jar, names, "assets/%s/textures/block/%s_talisman.png" % (MOD_ID, name))
                # 这一步加的第三套：飞剑符
                sword_model_path = "assets/%s/models/block/%s_sword.json" % (MOD_ID, name)
                sword_model = read_json(jar, names, sword_model_path)
                check(sword_model is not None, "%s（飞剑符那套模型）已打包" % sword_model_path)
                if sword_model:
                    all_tex = sword_model.get("textures", {}).get("all")
                    check(all_tex == "%s:block/%s_sword" % (MOD_ID, name),
                          "models/block/%s_sword.json 指向飞剑符贴图（现在指向 %r）" % (name, all_tex))
                check_texture(jar, names, "assets/%s/textures/block/%s_sword.png" % (MOD_ID, name))

            loot_path = "data/%s/loot_tables/blocks/%s.json" % (MOD_ID, name)
            loot = read_json(jar, names, loot_path)
            check(loot is not None, "%s（掉落表）已打包" % loot_path)
            if loot:
                blob = json.dumps(loot)
                check("%s:%s" % (MOD_ID, name) in blob,
                      "掉落表里掉的是 %s:%s" % (MOD_ID, name))
                check("survives_explosion" in blob, "掉落表带 survives_explosion")

            tag_path = "data/minecraft/tags/blocks/mineable/pickaxe.json"
            tag = read_json(jar, names, tag_path)
            check(tag is not None, "%s 已打包" % tag_path)
            if tag:
                check("%s:%s" % (MOD_ID, name) in tag.get("values", []),
                      "%s 在镐子可挖标签里" % name)

    print()
    if failures:
        print("RESULT: %d problem(s)" % len(failures))
        for f in failures:
            print("   - %s" % f)
        return 1
    print("RESULT: all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
