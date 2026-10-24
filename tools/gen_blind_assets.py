# -*- coding: utf-8 -*-
"""无明符 / 阵眼第八套 贴图。"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")


def closed_eye(d, cx, cy, w, h, col, dark):
    """一只闭着的眼睛：上眼睑一道弧 + 眼线。"""
    d.arc([cx - w, cy - h, cx + w, cy + h], 200, 340, fill=col)
    d.line([(cx - w, cy), (cx + w, cy)], fill=col)
    # 眼线（睫毛）
    for i in range(-2, 3):
        x = cx + i * (w * 0.35)
        d.line([(x, cy + h * 0.45), (x - 1, cy + h * 0.95)], fill=dark)


def item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    DK = (40, 40, 52, 255)
    DK2 = (24, 24, 34, 255)
    GREY = (110, 110, 130, 255)
    # 一片黑暗（紫黑渐变）
    d.ellipse([2, 3, 13, 13], fill=DK)
    d.ellipse([4, 5, 11, 11], fill=DK2)
    # 闭眼
    closed_eye(d, 8, 8, 4, 2, GREY, DK2)
    p = os.path.join(ASSETS, "textures", "item", "blind_talisman.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def eye():
    base = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    out = base.copy()
    d = ImageDraw.Draw(out)
    d.ellipse([2, 2, 13, 13], fill=(30, 30, 42, 150))
    closed_eye(d, 8, 8, 5, 2.5, (150, 150, 170, 255), (10, 10, 16, 255))
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        out.putpixel((x, y), (140, 130, 170, 255))
    p = os.path.join(ASSETS, "textures", "block", "formation_eye_blind.png")
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def main():
    a = item()
    b = eye()
    scale = 10
    sheet = Image.new("RGB", (16 * scale * 2 + 40, 16 * scale + 24), (12, 12, 18))
    sheet.paste(a.resize((16 * scale, 16 * scale), Image.NEAREST), (10, 10), a.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(b.resize((16 * scale, 16 * scale), Image.NEAREST), (16 * scale + 20, 10), b.resize((16 * scale, 16 * scale), Image.NEAREST))
    pp = os.path.join(ROOT, "tools", "out", "blind_preview.png")
    sheet.save(pp)
    print("  wrote", os.path.relpath(pp, ROOT))


if __name__ == "__main__":
    main()
