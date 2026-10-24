# -*- coding: utf-8 -*-
"""咫尺符 / 阵眼第十套 贴图（两个永远碰不到的点 / 被拉长的线）。"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")


def item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    PUR = (170, 120, 255, 255)
    DEEP = (90, 60, 190, 255)
    WH = (235, 225, 255, 255)
    # 被拉长的线（斜着贯穿）
    d.line([(2, 13), (13, 2)], fill=DEEP, width=2)
    d.line([(3, 12), (12, 3)], fill=PUR, width=1)
    # 两端各一个点（永不接触：中间留空）
    d.ellipse([1, 11, 5, 15], fill=WH)
    d.ellipse([11, 1, 15, 5], fill=WH)
    # 中间的"断裂"符号
    d.line([(7, 6), (9, 8)], fill=(30, 20, 50, 255))
    d.line([(6, 9), (8, 11)], fill=(30, 20, 50, 255))
    p = os.path.join(ASSETS, "textures", "item", "distance_talisman.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def eye():
    base = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    out = base.copy()
    d = ImageDraw.Draw(out)
    d.ellipse([2, 2, 13, 13], fill=(140, 100, 230, 110))
    # 两个点，中间一道空隙
    d.ellipse([3, 6, 6, 9], fill=(240, 235, 255, 255))
    d.ellipse([10, 6, 13, 9], fill=(240, 235, 255, 255))
    d.line([(6, 7), (10, 7)], fill=(60, 40, 110, 255))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        out.putpixel((x, y), (200, 180, 255, 255))
    p = os.path.join(ASSETS, "textures", "block", "formation_eye_distance.png")
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def main():
    a = item()
    b = eye()
    scale = 10
    sheet = Image.new("RGB", (16 * scale * 2 + 40, 16 * scale + 24), (12, 10, 22))
    sheet.paste(a.resize((16 * scale, 16 * scale), Image.NEAREST), (10, 10), a.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(b.resize((16 * scale, 16 * scale), Image.NEAREST), (16 * scale + 20, 10), b.resize((16 * scale, 16 * scale), Image.NEAREST))
    pp = os.path.join(ROOT, "tools", "out", "distance_preview.png")
    sheet.save(pp)
    print("  wrote", os.path.relpath(pp, ROOT))


if __name__ == "__main__":
    main()
