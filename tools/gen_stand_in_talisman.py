# -*- coding: utf-8 -*-
"""
替身符的物品贴图（16x16）与「抗性提升」的效果图标（18x18）。

两张都用字符画拼，改起来一眼能看懂。脚本保持纯 ASCII
（这台机器上 PowerShell 写文件会把中文写坏，之前踩过）。
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")
OUT = os.path.join(ROOT, "tools", "out")

# ---------------- 替身符：一个小草人 ----------------
# 稻草色身体 + 捆绳 + 中间一枚朱红盾符。12x12，居中放进 16x16。
DOLL = {
    ".": (0, 0, 0, 0),
    "s": (196, 168, 96, 255),     # 稻草
    "S": (226, 200, 130, 255),    # 稻草高光
    "d": (150, 122, 60, 255),     # 稻草暗部（轮廓）
    "t": (92, 66, 34, 255),       # 捆绳
    "r": (176, 34, 40, 255),      # 盾符
    "R": (220, 96, 90, 255),      # 盾符高光
}

DOLL_ART = [
    "....dddd....",
    "...dSSSSd...",
    "...dSssSd...",
    "...dssssd...",
    "....tttt....",
    ".ssttttttss.",
    ".sstRrrRtts.",
    ".sstRrrRtts.",
    ".ssttttttss.",
    "...tttttt...",
    "....s..s....",
    "....s..s....",
]

# ---------------- 抗性提升：一面盾 ----------------
# 18x18 是原版效果图标的硬性尺寸（不是 16，写错就显示紫黑格）。
SHIELD = {
    ".": (0, 0, 0, 0),
    "e": (44, 120, 108, 255),     # 盾的深色边
    "m": (89, 200, 176, 255),     # 盾面（和效果颜色 0x59C8B0 一致）
    "h": (170, 236, 220, 255),    # 高光
    "c": (30, 92, 84, 255),       # 中间的十字
}


def shift_reduce(center, layers):
    """按"离中心越远颜色越深"的同心层画一张对称的盾（简单、像素味够）。"""
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    # 盾的轮廓：上宽下尖。用逐行宽度描述。
    mask = [
        "..........",
        "..........",
        "..mmmmmm..",
        ".mmmmmmmm.",
        ".mmmmmmmm.",
        ".mmmmmmmm.",
        ".mmmmmmmm.",
        "..mmmmmm..",
        "..mmmmmm..",
        "..mmmmmm..",
        "...mmmm...",
        "...mmmm...",
        "....mm....",
        "....mm....",
        "..........",
        "..........",
        "..........",
        "..........",
    ]
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == "m":
                img.putpixel((x + 4, y + 2), SHIELD["m"])
    return img


def main():
    # ---- 替身符 ----
    item = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(DOLL_ART):
        assert len(row) == 12, "doll row %d is %d chars" % (y, len(row))
        for x, ch in enumerate(row):
            item.putpixel((2 + x, 2 + y), DOLL[ch])
    p = os.path.join(ASSETS, "textures", "item", "stand_in_talisman.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    item.save(p)
    print("wrote", os.path.relpath(p, ROOT))

    # ---- 抗性提升图标 ----
    icon = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    d = ImageDraw.Draw(icon)
    # 盾形：上边平、下边收成尖
    d.polygon([(4, 3), (13, 3), (13, 9), (8.5, 15), (4, 9)], fill=SHIELD["m"], outline=SHIELD["e"])
    # 十字（护甲的意象）
    d.rectangle([8, 5, 9, 12], fill=SHIELD["c"])
    d.rectangle([6, 7, 11, 8], fill=SHIELD["c"])
    # 高光：左上两点
    icon.putpixel((6, 4), SHIELD["h"])
    icon.putpixel((7, 4), SHIELD["h"])
    icon.putpixel((6, 5), SHIELD["h"])

    p = os.path.join(ASSETS, "textures", "mob_effect", "resistance_up.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    icon.save(p)
    print("wrote", os.path.relpath(p, ROOT))

    # ---- 预览 ----
    os.makedirs(OUT, exist_ok=True)
    scale = 10
    w = 16 * scale + 18 * scale + 30
    h = 18 * scale + 20
    sheet = Image.new("RGB", (w, h), (46, 44, 54))
    a = item.resize((16 * scale, 16 * scale), Image.NEAREST)
    b = icon.resize((18 * scale, 18 * scale), Image.NEAREST)
    sheet.paste(a, (10, 10), a)
    sheet.paste(b, (16 * scale + 20, 10), b)
    sd = ImageDraw.Draw(sheet)
    sd.text((10, h - 14), "stand_in_talisman", fill=(200, 200, 200))
    sd.text((16 * scale + 20, h - 14), "resistance_up icon", fill=(200, 200, 200))
    prev = os.path.join(OUT, "stand_in_and_resistance_preview.png")
    sheet.save(prev)
    print("wrote", os.path.relpath(prev, ROOT))


if __name__ == "__main__":
    main()
