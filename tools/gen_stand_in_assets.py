# -*- coding: utf-8 -*-
"""
替身符那一套资源：
  1. 阵眼的第四套贴图（中间一个小草人）
  2. 全屏碎裂图案 128x128（碎裂的盾，金红带裂纹，背景透明）
  3. 方块状态加一条、方块模型加一个

脚本保持纯 ASCII，内容里需要中文的用 \\u 转义（JSON 允许）。
"""
import io
import json
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")
OUT = os.path.join(ROOT, "tools", "out")

# ---------------------------------------------------------------- 1. 阵眼贴图
def build_eye():
    eye = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    out = eye.copy()
    d = ImageDraw.Draw(out)

    STRAW = (208, 180, 112, 255)
    STRAW_D = (156, 128, 70, 255)
    TIE = (92, 66, 34, 255)
    RUNE = (176, 34, 40, 255)

    # 小草人：头 2x2、身体 3x4、腰上一道捆绳、胸口一点朱红
    d.rectangle([7, 3, 8, 4], fill=STRAW, outline=STRAW_D)        # 头
    d.rectangle([6, 5, 9, 8], fill=STRAW, outline=STRAW_D)        # 身体
    d.line([6, 6, 9, 6], fill=TIE)                                # 捆绳
    out.putpixel((7, 7), RUNE)                                    # 胸口符文
    out.putpixel((8, 7), RUNE)
    d.rectangle([7, 9, 8, 10], fill=STRAW_D)                      # 两条腿
    # 光晕：一圈淡金，和另外三种外观区分开
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        out.putpixel((x, y), (240, 214, 140, 255))

    p = os.path.join(ASSETS, "textures", "block", "formation_eye_stand_in.png")
    out.save(p)
    print("wrote", os.path.relpath(p, ROOT))
    return out


# ------------------------------------------------------- 2. 全屏碎裂图案 128x128
def build_shatter():
    W = H = 128
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    GOLD = (245, 205, 96, 255)
    GOLD_D = (188, 140, 40, 255)
    BLOOD = (150, 18, 26, 255)

    # 盾形：上宽下尖
    shield = [(34, 18), (94, 18), (94, 62), (64, 112), (34, 62)]
    d.polygon(shield, fill=(120, 16, 22, 210), outline=GOLD)

    # 内层描边（金色，带一点厚度）
    inner = [(40, 24), (88, 24), (88, 60), (64, 104), (40, 60)]
    d.line(inner + [inner[0]], fill=GOLD_D, width=2)

    # 裂纹：从中心往外几条折线，一条粗、几条细
    cracks = [
        [(64, 62), (58, 72), (62, 84), (54, 96)],
        [(64, 62), (74, 70), (70, 82), (78, 92)],
        [(64, 62), (64, 44), (56, 34), (58, 24)],
        [(64, 62), (52, 56), (44, 48), (40, 40)],
        [(64, 62), (76, 56), (84, 50), (90, 42)],
    ]
    for i, line in enumerate(cracks):
        d.line(line, fill=GOLD if i == 0 else BLOOD, width=3 if i == 0 else 2)

    # 中心爆点
    d.ellipse([58, 56, 70, 68], fill=GOLD)
    d.ellipse([61, 59, 67, 65], fill=(255, 246, 220, 255))

    p = os.path.join(ASSETS, "textures", "gui", "stand_in_shatter.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("wrote", os.path.relpath(p, ROOT))
    return img


# ------------------------------------------------------- 3. 方块状态 + 模型
def build_block_files():
    # 模型：沿用其他三种的写法（阵眼本体 + 一张覆盖贴图）
    base = json.load(io.open(os.path.join(ASSETS, "models", "block", "formation_eye_antigravity.json"),
                             encoding="utf-8"))
    base["textures"] = {k: v.replace("formation_eye_antigravity", "formation_eye_stand_in")
                        for k, v in base.get("textures", {}).items()}
    p = os.path.join(ASSETS, "models", "block", "formation_eye_stand_in.json")
    with io.open(p, "w", encoding="utf-8") as f:
        json.dump(base, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("wrote", os.path.relpath(p, ROOT))

    # 方块状态：加 talisman=stand_in
    p = os.path.join(ASSETS, "blockstates", "formation_eye.json")
    data = json.load(io.open(p, encoding="utf-8"))
    data["variants"]["talisman=stand_in"] = {"model": "fulu:block/formation_eye_stand_in"}
    with io.open(p, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("blockstate variants:", len(data["variants"]))


def main():
    os.makedirs(OUT, exist_ok=True)
    eye = build_eye()
    shatter = build_shatter()
    build_block_files()

    # 预览：阵眼五套外观 + 碎裂图案
    names = ["formation_eye", "formation_eye_talisman", "formation_eye_sword",
             "formation_eye_antigravity", "formation_eye_stand_in"]
    labels = ["empty", "chaos", "sword", "antigravity", "stand_in"]
    scale = 6
    gap = 10
    panels = []
    for n in names:
        im = Image.open(os.path.join(ASSETS, "textures", "block", n + ".png")).convert("RGBA")
        panels.append(im.resize((im.width * scale, im.height * scale), Image.NEAREST))
    sh = shatter.resize((96, 96), Image.LANCZOS)
    w = sum(p.width for p in panels) + gap * (len(panels) + 1) + sh.width + gap
    h = max(max(p.height for p in panels), sh.height) + gap * 2 + 12
    sheet = Image.new("RGB", (w, h), (34, 34, 40))
    sd = ImageDraw.Draw(sheet)
    x = gap
    for p, label in zip(panels, labels):
        sheet.paste(p, (x, gap), p)
        sd.text((x, h - gap - 6), label, fill=(210, 210, 210))
        x += p.width + gap
    sheet.paste(sh, (x, gap), sh)
    sd.text((x, h - gap - 6), "shatter 128", fill=(210, 210, 210))
    sheet.save(os.path.join(OUT, "eye_five_looks.png"))
    print("wrote", os.path.relpath(os.path.join(OUT, "eye_five_looks.png"), ROOT))


if __name__ == "__main__":
    main()
