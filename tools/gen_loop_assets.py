# -*- coding: utf-8 -*-
"""闭环符 / 阵眼第九套 贴图（首尾相接的圆环 / 漩涡）。"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")


def ring(d, cx, cy, r, col, width=1, start=0, end=360):
    d.arc([cx - r, cy - r, cx + r, cy + r], start, end, fill=col, width=width)


def swirl(d, cx, cy, r, col, turns=1.6, steps=40):
    """螺旋线：从中心向外绕。"""
    pts = []
    for i in range(steps):
        t = i / (steps - 1)
        a = t * turns * 2 * math.pi
        rr = r * (0.25 + 0.75 * t)
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.line(pts, fill=col, width=1)


def item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    DEEP = (70, 70, 200, 255)
    LIGHT = (150, 130, 255, 255)
    WH = (220, 210, 255, 255)
    # 外环（首尾相接）
    ring(d, 8, 8, 6, DEEP, 2)
    ring(d, 8, 8, 6, LIGHT, 1, 200, 520)
    # 内部的漩涡
    swirl(d, 8, 8, 4, WH, 1.4, 26)
    # 环上的接缝小点（表示"首尾相接"）
    d.point((8, 2), fill=WH)
    p = os.path.join(ASSETS, "textures", "item", "loop_talisman.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def eye():
    base = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    out = base.copy()
    d = ImageDraw.Draw(out)
    d.ellipse([2, 2, 13, 13], fill=(60, 60, 180, 120))
    ring(d, 8, 8, 5, (150, 130, 255, 255), 2)
    swirl(d, 8, 8, 3.5, (220, 210, 255, 255), 1.3, 22)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        out.putpixel((x, y), (170, 150, 255, 255))
    p = os.path.join(ASSETS, "textures", "block", "formation_eye_loop.png")
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def main():
    a = item()
    b = eye()
    scale = 10
    sheet = Image.new("RGB", (16 * scale * 2 + 40, 16 * scale + 24), (10, 10, 22))
    sheet.paste(a.resize((16 * scale, 16 * scale), Image.NEAREST), (10, 10), a.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(b.resize((16 * scale, 16 * scale), Image.NEAREST), (16 * scale + 20, 10), b.resize((16 * scale, 16 * scale), Image.NEAREST))
    pp = os.path.join(ROOT, "tools", "out", "loop_preview.png")
    sheet.save(pp)
    print("  wrote", os.path.relpath(pp, ROOT))


if __name__ == "__main__":
    main()
