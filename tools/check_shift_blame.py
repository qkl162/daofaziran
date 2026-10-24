# -*- coding: utf-8 -*-
"""嫁祸阵：直接核对编译产物与资源（比改自检脚本稳）。"""
import glob
import zipfile

jar = sorted(glob.glob("build/libs/fulu-*.jar"))[-1]
z = zipfile.ZipFile(jar)
names = z.namelist()

ok = True


def check(cond, label):
    global ok
    ok = ok and bool(cond)
    print("  [%s] %s" % ("ok" if cond else "FAIL", label))


print("-- 物品与资源 --")
for p in ("assets/fulu/models/item/shift_blame_talisman.json",
          "assets/fulu/textures/item/shift_blame_talisman.png",
          "assets/fulu/models/block/formation_eye_shift_blame.json",
          "assets/fulu/textures/block/formation_eye_shift_blame.png",
          "data/fulu/recipes/shift_blame_talisman.json"):
    check(p in names, p)

print("-- 配方内容 --")
import io
import json

recipe = json.loads(z.read("data/fulu/recipes/shift_blame_talisman.json").decode("utf-8"))
check(recipe["type"] == "minecraft:crafting_shaped", "有序合成")
check(recipe["pattern"] == [" P ", "RBS", " C "], "图案 空符纸空 / 腐肉兽血蜘蛛眼 / 空朱砂空")
for letter, item, label in (("P", "fulu:talisman_paper", "符纸"), ("R", "minecraft:rotten_flesh", "腐肉"),
                            ("B", "fulu:beast_blood", "兽血"), ("S", "minecraft:spider_eye", "蜘蛛眼"),
                            ("C", "fulu:cinnabar", "朱砂")):
    check(recipe["key"][letter]["item"] == item, "图案 %s = %s" % (letter, label))
check(recipe["result"]["item"] == "fulu:shift_blame_talisman", "产出嫁祸符")

print("-- 第六种外观 / 枚举 --")
state = json.loads(z.read("assets/fulu/blockstates/formation_eye.json").decode("utf-8"))
check("talisman=shift_blame" in state["variants"], "方块状态有 talisman=shift_blame")
check(len(state["variants"]) >= 6, "阵眼外观数 = %d（空 + 5 种符）" % len(state["variants"]))

raw = z.read("com/lengyang/fulu/block/TalismanKind.class")
check(b"SHIFT_BLAME" in raw and b"shift_blame" in raw, "TalismanKind 第五种")

print("-- 效果与转移 --")
raw = z.read("com/lengyang/fulu/registry/ModMobEffects.class")
check(b"SHIFT_BLAME" in raw and b"shift_blame" in raw, "效果已登记 fulu:shift_blame")
raw = z.read("com/lengyang/fulu/combat/ShiftBlameHandler.class")
for needle, label in ((b"TRANSFER_RATIO", "转移比例常量 0.5"), (b"LivingHurtEvent", "监听受伤"),
                      (b"pickOtherMarked", "随机挑另一只"), (b"isAttackDamage", "只转移攻击伤害"),
                      (b"isBoss", "BOSS 免疫"), (b"TRANSFERRING", "防递归门闩"),
                      (b"spawnTransferFx", "粒子线"), (b"mark", "挂标记方法")):
    check(needle in raw, label)
raw = z.read("com/lengyang/fulu/block/FormationEyeBlockEntity.class")
check(b"applyShiftBlame" in raw, "阵眼每刻挂标记")
check(b"SHIFT_BLAME" in raw, "阵眼移除清理也含嫁祸")

print("-- 语言与图鉴 --")
lang = json.loads(z.read("assets/fulu/lang/zh_cn.json").decode("utf-8"))
check(lang.get("item.fulu.shift_blame_talisman") == "嫁祸符", "物品名 嫁祸符")
check(lang.get("codex.fulu.shift_blame.name") == "嫁祸阵", "图鉴名 嫁祸阵")
check(lang.get("codex.fulu.shift_blame.material") == "阵基 ×4、阵眼 ×1、嫁祸符 ×2", "图鉴材料")
raw = z.read("com/lengyang/fulu/codex/FormationCodex.class")
check(b"shift_blame" in raw and b"fulu:shift_blame_talisman" in raw, "图鉴条目 + 图标")

print()
print("全部通过" if ok else "有失败项")
