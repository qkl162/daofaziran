# -*- coding: utf-8 -*-
"""阵魔 / 阵仆 / 阵魔核心 / 破碎的阵纹 贴图。"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")


def demon():
    """32x64 的人形轮廓：黑紫长袍 + 破碎阵纹 + 发光的红眼。"""
    img = Image.new("RGBA", (32, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    DARK = (24, 16, 38, 255)
    DARK2 = (40, 26, 62, 255)
    PURPLE = (110, 60, 190, 255)
    GOLD = (235, 190, 80, 255)
    RED = (255, 60, 60, 255)

    # 长袍（下摆张开的三角）
    d.polygon([(10, 22), (21, 22), (27, 62), (4, 62)], fill=DARK)
    d.polygon([(12, 24), (19, 24), (23, 58), (8, 58)], fill=DARK2)
    # 肩膀
    d.ellipse([3, 20, 28, 34], fill=DARK)
    # 头（旋转的阵眼）
    d.ellipse([10, 4, 21, 15], fill=DARK)
    d.ellipse([12, 6, 19, 13], fill=(60, 30, 90, 255))
    # 眼睛（发光的红）
    d.ellipse([13, 8, 16, 11], fill=RED)
    d.ellipse([17, 8, 20, 11], fill=RED)
    d.point((14, 9), fill=(255, 230, 230, 255))
    # 破碎阵纹：几条断裂的金色线条
    for (x1, y1, x2, y2) in ((6, 30, 12, 40), (25, 32, 19, 43), (14, 46, 20, 52), (9, 52, 15, 58)):
        d.line([(x1, y1), (x2, y2)], fill=GOLD)
    for (x1, y1, x2, y2) in ((7, 34, 11, 38), (24, 36, 20, 40)):
        d.line([(x1, y1), (x2, y2)], fill=PURPLE)
    # 飘动的锁链
    for i in range(6):
        y = 26 + i * 5
        d.point((2 + (i % 2) * 2, y), fill=PURPLE)
        d.point((29 - (i % 2) * 2, y), fill=PURPLE)
    p = os.path.join(ASSETS, "textures", "entity", "formation_demon.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def core():
    """阵魔核心：黑底上一颗金色/紫色旋转的球。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(30, 20, 50, 255))
    d.ellipse([4, 4, 11, 11], fill=(120, 70, 210, 255))
    d.ellipse([6, 6, 9, 9], fill=(250, 220, 130, 255))
    d.point((7, 5), fill=(255, 255, 240, 255))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.point((x, y), fill=(200, 160, 255, 255))
    p = os.path.join(ASSETS, "textures", "item", "demon_core.png")
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def shard():
    """破碎的阵纹：一块黑色碎片，上面有断裂的金紫纹路。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(4, 2), (12, 3), (13, 11), (7, 14), (3, 9)], fill=(28, 20, 44, 255))
    d.polygon([(5, 4), (11, 5), (11, 10), (7, 12)], fill=(44, 30, 66, 255))
    for (a, b) in (((5, 6), (10, 7)), ((10, 7), (8, 11)), ((6, 9), (9, 8))):
        d.line([a, b], fill=(235, 190, 80, 255))
    d.point((6, 5), fill=(180, 120, 255, 255))
    d.point((9, 11), fill=(180, 120, 255, 255))
    p = os.path.join(ASSETS, "textures", "item", "demon_shard.png")
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def main():
    a = demon()
    b = core()
    c = shard()
    # 预览：阵魔放大 + 两个物品
    sheet = Image.new("RGB", (32 * 5 + 16 * 5 * 2 + 60, 64 * 5 + 20), (10, 8, 18))
    sheet.paste(a.resize((32 * 5, 64 * 5), Image.NEAREST), (10, 10), a.resize((32 * 5, 64 * 5), Image.NEAREST))
    x = 32 * 5 + 30
    sheet.paste(b.resize((16 * 5, 16 * 5), Image.NEAREST), (x, 10), b.resize((16 * 5, 16 * 5), Image.NEAREST))
    sheet.paste(c.resize((16 * 5, 16 * 5), Image.NEAREST), (x + 16 * 5 + 10, 10), c.resize((16 * 5, 16 * 5), Image.NEAREST))
    pp = os.path.join(ROOT, "tools", "out", "demon_preview.png")
    sheet.save(pp)
    print("  wrote", os.path.relpath(pp, ROOT))


if __name__ == "__main__":
    main()
