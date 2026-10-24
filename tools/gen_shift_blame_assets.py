# -*- coding: utf-8 -*-
"""
嫁祸符贴图重画：用绘图图元画两个对准对方的箭头（→ ←），比字符画清楚。
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")

PURPLE = (138, 79, 181, 255)
LIGHT = (196, 140, 230, 255)
DARK = (92, 48, 126, 255)
KNOT = (70, 34, 96, 255)


def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # 尾杆：中间一段
    d.rectangle([4, 7, 11, 8], fill=PURPLE)

    # 左箭头：朝右
    d.polygon([(3, 7), (6, 4), (6, 10)], fill=LIGHT, outline=DARK)
    # 右箭头：朝左
    d.polygon([(12, 7), (9, 4), (9, 10)], fill=LIGHT, outline=DARK)

    # 中间一个"结"：伤害在这里被分出去
    d.rectangle([7, 6, 8, 9], fill=KNOT)

    # 高光：两个箭头尖端各一点白紫
    img.putpixel((5, 5), (236, 214, 250, 255))
    img.putpixel((10, 5), (236, 214, 250, 255))

    p = os.path.join(ASSETS, "textures", "item", "shift_blame_talisman.png")
    img.save(p)
    print("wrote", os.path.relpath(p, ROOT))

    # 预览（物品 + 阵眼贴图一起）
    eye = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye_shift_blame.png")).convert("RGBA")
    scale = 10
    sheet = Image.new("RGB", (16 * scale * 2 + 40, 16 * scale + 22), (44, 42, 52))
    a = img.resize((16 * scale, 16 * scale), Image.NEAREST)
    b = eye.resize((16 * scale, 16 * scale), Image.NEAREST)
    sheet.paste(a, (10, 10), a)
    sheet.paste(b, (16 * scale + 30, 10), b)
    ImageDraw.Draw(sheet).text((10, 16 * scale + 8), "shift_blame_talisman / eye", fill=(210, 210, 210))
    prev = os.path.join(ROOT, "tools", "out", "shift_blame_preview.png")
    sheet.save(prev)
    print("wrote", os.path.relpath(prev, ROOT))


if __name__ == "__main__":
    main()
