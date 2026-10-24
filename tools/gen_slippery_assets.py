# -*- coding: utf-8 -*-
"""滑地符 / 阵眼第七套 / 冰面 三张贴图。"""
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")


def snowflake(d, cx, cy, size, col, thick=1):
    """六角雪花（三条穿过中心的线 + 六根分叉）。"""
    for k in range(3):
        a = k * 60
        import math
        dx = math.cos(math.radians(a)) * size
        dy = math.sin(math.radians(a)) * size
        d.line([(cx - dx, cy - dy), (cx + dx, cy + dy)], fill=col, width=thick)
    import math
    for k in range(6):
        a = k * 60
        ex = cx + math.cos(math.radians(a)) * size
        ey = cy + math.sin(math.radians(a)) * size
        for s in (-30, 30):
            b = a + s
            bx = ex - math.cos(math.radians(b)) * (size * 0.35)
            by = ey - math.sin(math.radians(b)) * (size * 0.35)
            d.line([(ex, ey), (bx, by)], fill=col, width=thick)


def item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    ICE = (160, 220, 255, 255)
    ICE2 = (90, 170, 230, 255)
    WH = (240, 250, 255, 255)
    # 底：一块薄冰（菱形）
    d.polygon([(8, 2), (14, 8), (8, 14), (2, 8)], fill=(120, 190, 240, 150))
    d.line([(8, 2), (14, 8)], fill=WH)
    d.line([(14, 8), (8, 14)], fill=ICE2)
    # 雪花
    snowflake(d, 8, 8, 5, WH, 1)
    p = os.path.join(ASSETS, "textures", "item", "slippery_talisman.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return img


def eye():
    base = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    out = base.copy()
    d = ImageDraw.Draw(out)
    d.ellipse([2, 2, 13, 13], fill=(120, 200, 255, 80))
    snowflake(d, 8, 8, 5, (240, 250, 255, 255), 1)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        out.putpixel((x, y), (200, 240, 255, 255))
    p = os.path.join(ASSETS, "textures", "block", "formation_eye_slippery.png")
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def ice():
    """冰面覆盖贴图：32x32 的薄冰纹理（半透明白蓝 + 裂纹）。"""
    S = 32
    img = Image.new("RGBA", (S, S), (150, 210, 255, 90))
    d = ImageDraw.Draw(img)
    for i in range(10):
        x = 2 + i * 3
        d.line([(x, 0), (x + 6, S)], fill=(210, 240, 255, 120), width=1)
    snowflake(d, 9, 9, 5, (235, 248, 255, 170), 1)
    snowflake(d, 23, 21, 4, (235, 248, 255, 150), 1)
    glow = img.filter(ImageFilter.GaussianBlur(1))
    out = Image.alpha_composite(glow, img)
    p = os.path.join(ASSETS, "textures", "block", "ice_sheet.png")
    out.save(p)
    print("  wrote", os.path.relpath(p, ROOT))
    return out


def main():
    a = item()
    b = eye()
    c = ice()
    scale = 10
    sheet = Image.new("RGB", (16 * scale * 2 + 32 * scale + 60, 32 * scale + 24), (10, 14, 26))
    sheet.paste(a.resize((16 * scale, 16 * scale), Image.NEAREST), (10, 10), a.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(b.resize((16 * scale, 16 * scale), Image.NEAREST), (16 * scale + 20, 10), b.resize((16 * scale, 16 * scale), Image.NEAREST))
    sheet.paste(c.resize((32 * scale, 32 * scale), Image.NEAREST), (32 * scale + 30, 10), c.resize((32 * scale, 32 * scale), Image.NEAREST))
    pp = os.path.join(ROOT, "tools", "out", "slippery_preview.png")
    sheet.save(pp)
    print("  wrote", os.path.relpath(pp, ROOT))


if __name__ == "__main__":
    main()
