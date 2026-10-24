# -*- coding: utf-8 -*-
"""
碎裂图案（256x256）：沿用 128 那版读得清的构图，放大一倍重画。

要点（上一版画糊了，这次改对）：
  * 外光晕先用三层低透明度的盾形画在**底下**，不是画在上面；
  * 裂纹用**金色**（暗红在暗红底上根本看不见）；
  * 边框加粗，中心爆点三层同心。

脚本纯 ASCII。
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")
S = 256


def shield_poly(scale=1.0):
    """盾形：上宽下尖。在 0..128 的空间里绕 (64,64) 缩放（外面再用 pt() 放大到 256）。"""
    base = [(34, 18), (94, 18), (94, 62), (64, 112), (34, 62)]
    return [(64 + (x - 64) * scale, 64 + (y - 64) * scale) for (x, y) in base]


def main():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    GOLD = (246, 208, 104, 255)
    GOLD_D = (176, 128, 36, 255)
    GOLD_L = (255, 248, 220, 255)
    BLOOD = (150, 20, 26, 235)
    BLOOD_L = (196, 40, 44, 235)

    k = S / 128.0          # 128 -> 256

    def pt(p):
        return (p[0] * k, p[1] * k)

    # ---- 外光晕：在底下画三层放大的盾 ----
    for scale, alpha in ((1.14, 22), (1.08, 34), (1.03, 52)):
        poly = [pt(p) for p in shield_poly(scale)]
        d.polygon(poly, fill=(GOLD[0], GOLD[1], GOLD[2], alpha))

    # ---- 盾本体 ----
    body = [pt(p) for p in shield_poly(1.0)]
    d.polygon(body, fill=BLOOD)
    d.line(body + [body[0]], fill=GOLD, width=int(7 * k / 2), joint="curve")

    # 内层细描边
    inner = [pt(p) for p in shield_poly(0.86)]
    d.line(inner + [inner[0]], fill=GOLD_D, width=max(2, int(2 * k / 2)))

    # ---- 裂纹：金色，从中心放射 ----
    cracks = [
        [(64, 62), (57, 73), (61, 86), (52, 99)],
        [(64, 62), (75, 71), (70, 84), (79, 95)],
        [(64, 62), (64, 42), (55, 32), (57, 22)],
        [(64, 62), (51, 55), (43, 47), (38, 38)],
        [(64, 62), (78, 55), (86, 49), (92, 40)],
    ]
    for i, line in enumerate(cracks):
        pts = [pt(p) for p in line]
        width = max(2, int((3 if i == 0 else 2) * k / 2))
        d.line(pts, fill=GOLD_L if i % 2 == 0 else GOLD, width=width, joint="curve")

    # ---- 中心爆点 ----
    c = pt((64, 62))
    r1, r2, r3 = 16 * k / 2, 11 * k / 2, 6 * k / 2
    d.ellipse([c[0] - r1, c[1] - r1, c[0] + r1, c[1] + r1], fill=GOLD_D)
    d.ellipse([c[0] - r2, c[1] - r2, c[0] + r2, c[1] + r2], fill=GOLD)
    d.ellipse([c[0] - r3, c[1] - r3, c[0] + r3, c[1] + r3], fill=GOLD_L)

    p = os.path.join(ASSETS, "textures", "gui", "stand_in_shatter.png")
    img.save(p)
    print("wrote", os.path.relpath(p, ROOT), img.size)

    # 预览：100%（1x）与放大到 1.3 倍各一张，检查放大后是否还清楚
    prev = os.path.join(ROOT, "tools", "out", "shatter_256_preview.png")
    big = img.resize((int(S * 1.3), int(S * 1.3)), Image.LANCZOS)
    sheet = Image.new("RGB", (S + big.width + 36, max(S, big.height) + 24), (30, 30, 36))
    sheet.paste(img, (12, 12), img)
    sheet.paste(big, (S + 24, 12), big)
    sheet.save(prev)
    print("wrote", os.path.relpath(prev, ROOT))


if __name__ == "__main__":
    main()
