# -*- coding: utf-8 -*-
"""招魂符 / 阵眼第六套 / 魂 三张贴图。"""
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")


def item():
    """招魂符：一个飘浮的鬼魂（淡青，带两只眼睛和一条尾巴）。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    CY = (128, 224, 224, 255)
    CY2 = (90, 190, 200, 255)
    WH = (255, 255, 255, 255)
    DK = (40, 90, 100, 255)

    # 鬼魂身体：上圆下摆（波浪底）
    d.ellipse([4, 2, 11, 9], fill=CY)
    d.rectangle([4, 6, 11, 11], fill=CY)
    for i in range(4):
        x = 4 + i * 2
        d.polygon([(x, 11), (x + 1, 13), (x + 2, 11)], fill=CY)
    # 高光/暗边
    d.arc([4, 2, 11, 9], 200, 340, fill=WH)
    d.line([(4, 6), (4, 11)], fill=CY2)
    d.line([(11, 6), (11, 11)], fill=CY2)
    # 眼睛
    d.rectangle([6, 5, 7, 7], fill=DK)
    d.rectangle([9, 5, 10, 7], fill=DK)
    d.point((6, 5), fill=WH)
    d.point((9, 5), fill=WH)
    # 小尾巴
    d.line([(8, 13), (10, 15)], fill=CY2)

    p = os.path.join(ASSETS, "textures", "item", "summon_talisman.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def eye():
    """阵眼第六套：中间一个骷髅头。"""
    base = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    out = base.copy()
    d = ImageDraw.Draw(out)
    # 淡青光晕
    d.ellipse([3, 3, 12, 12], fill=(60, 200, 200, 70))
    # 骷髅头：圆颅 + 方下颌
    d.ellipse([4, 4, 11, 10], fill=(226, 232, 220, 255))
    d.rectangle([5, 9, 10, 12], fill=(226, 232, 220, 255))
    # 眼窝 + 鼻
    d.rectangle([5, 6, 6, 8], fill=(20, 30, 34, 255))
    d.rectangle([9, 6, 10, 8], fill=(20, 30, 34, 255))
    d.rectangle([7, 8, 8, 9], fill=(20, 30, 34, 255))
    # 牙缝
    for x in (6, 8):
        d.line([(x, 10), (x, 12)], fill=(120, 130, 125, 255))
    # 四角淡青光点
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        out.putpixel((x, y), (160, 240, 240, 255))

    p = os.path.join(ASSETS, "textures", "block", "formation_eye_summon.png")
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def soul():
    """魂：32x32 的半透明白幽灵（画的时候带柔光）。"""
    S = 32
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    CY = (150, 235, 235, 255)
    CY2 = (110, 205, 215, 255)
    WH = (255, 255, 255, 255)

    d.ellipse([8, 4, 23, 19], fill=CY)
    d.rectangle([8, 13, 23, 22], fill=CY)
    for i in range(5):
        x = 8 + i * 3
        d.polygon([(x, 22), (x + 1.5, 26), (x + 3, 22)], fill=CY)
    d.arc([8, 4, 23, 19], 200, 340, fill=WH)
    d.line([(8, 13), (8, 22)], fill=CY2)
    d.line([(23, 13), (23, 22)], fill=CY2)
    # 两只亮白眼睛
    d.ellipse([11, 11, 14, 15], fill=WH)
    d.ellipse([17, 11, 20, 15], fill=WH)

    glow = img.filter(ImageFilter.GaussianBlur(3))
    glow.putalpha(glow.getchannel("A").point(lambda v: int(v * 0.55)))
    out = Image.alpha_composite(glow, img)

    p = os.path.join(ASSETS, "textures", "entity", "soul.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def main():
    a = item()
    b = eye()
    c = soul()

    scale = 10
    sheet = Image.new("RGB", (16 * scale * 2 + 34 * scale + 60, 16 * scale + 24), (10, 12, 22))
    sheet.paste(a.resize((16 * scale, 16 * scale), Image.NEAREST), (10, 10), a.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(b.resize((16 * scale, 16 * scale), Image.NEAREST), (16 * scale + 20, 10), b.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(c.resize((32 * scale, 32 * scale), Image.NEAREST), (32 * scale + 30, 10), c.resize((32 * scale, 32 * scale), Image.NEAREST))
    pp = os.path.join(ROOT, "tools", "out", "summon_preview.png")
    sheet.save(pp)
    print("  wrote", os.path.relpath(pp, ROOT))


if __name__ == "__main__":
    main()
