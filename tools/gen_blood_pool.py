# -*- coding: utf-8 -*-
"""
血泊 (blood_pool) 的 16x16 贴图：一块不规则的深红血迹，边缘半透明。

为什么程序化生成：血迹要"不规则"才像，手点像素反而更难。用几个叠起来的椭圆 +
几滴飞溅 + 一层软边就够了。

脚本保持纯 ASCII（这台机器上 PowerShell 写文件会把中文写坏）。
"""
import os
import random

from PIL import Image, ImageDraw, ImageFilter

W = H = 16
# 16x16 下的血迹：不要用纯色，中心深、边缘亮一点，才有"液体"的感觉
DARK = (86, 10, 16, 255)
MID = (124, 18, 24, 255)
EDGE = (154, 30, 34, 255)


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    rnd = random.Random(20261016)          # 固定种子：每次生成同一张图

    # 先用 4 倍分辨率画，再缩回来 —— 这样边缘才有自然的半透明过渡
    S = 4
    big = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)

    # 主体：几个错开的椭圆叠成不规则块
    blobs = [(6.5, 8.0, 4.6, 3.4), (9.5, 7.0, 3.4, 2.8), (7.0, 11.0, 3.0, 2.2),
             (11.5, 10.0, 2.2, 1.8)]
    for cx, cy, rx, ry in blobs:
        d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], fill=MID)

    # 高光：偏左上一点，像反光
    d.ellipse([4.0 * S, 5.5 * S, 8.0 * S, 8.5 * S], fill=EDGE)

    # 飞溅的小滴
    for (x, y, r) in [(2.5, 12.5, 1.1), (13.5, 4.5, 0.9), (12.0, 13.0, 0.8),
                      (3.0, 4.0, 0.7), (14.0, 8.5, 0.6)]:
        d.ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=EDGE)

    img = big.resize((W, H), Image.LANCZOS)

    # 中心压深：让血迹有厚度感
    core = Image.new("RGBA", (W * S, H * S), (0, 0, 0, 0))
    ImageDraw.Draw(core).ellipse([5.0 * S, 6.5 * S, 10.5 * S, 11.0 * S], fill=DARK)
    core = core.resize((W, H), Image.LANCZOS)
    img = Image.alpha_composite(img, core)

    # 只保留 alpha 在边缘的那点羽化，别让中间出现透明洞
    px = img.load()
    for y in range(H):
        for x in range(W):
            r, g, b, a = px[x, y]
            if a < 24:
                px[x, y] = (0, 0, 0, 0)
            else:
                # 颜色统一到血迹色系，alpha 稍微加强
                px[x, y] = (r, g, b, min(255, int(a * 1.25)))

    out_dir = os.path.join(root, "src", "main", "resources", "assets", "fulu", "textures", "entity")
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, "blood_pool.png")
    img.save(path)
    print("wrote", os.path.relpath(path, root))

    # 预览：黑底上放大，模拟"贴在地上"的样子
    prev_dir = os.path.join(root, "tools", "out")
    os.makedirs(prev_dir, exist_ok=True)
    scale = 10
    sheet = Image.new("RGB", (W * scale, H * scale + 18), (46, 44, 54))
    up = img.resize((W * scale, H * scale), Image.NEAREST)
    sheet.paste(up, (0, 0), up)
    ImageDraw.Draw(sheet).text((6, H * scale + 4), "blood_pool 16x16", fill=(200, 200, 200))
    prev = os.path.join(prev_dir, "blood_pool_preview.png")
    sheet.save(prev)
    print("wrote", os.path.relpath(prev, root))


if __name__ == "__main__":
    main()
