# -*- coding: utf-8 -*-
"""
"领域展开"风格水墨全景（2048x1024，左右无缝）。

冷调、高对比：
  * 墨黑天幕 + 地平线**冷白逆光**（把山打成剪影）；
  * 三层**纯黑山峰**，愈近愈黑；
  * **白色笔锋**：长短粗细不一的甩笔（不是等大的彗星），配飞溅墨点；
  * 山腰**雾带**（半透明白雾），把纵深拉开；
  * 血红细笔作杀意点缀；
  * 底部墨湖，白色水纹。

无缝：山脊用整数频率正弦（周期 = 画宽），笔触左右环绕绘制。
"""
import math
import os
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter

W, H = 4096, 2048
OUT_TEX = r"src\main\resources\assets\fulu\textures\block\ink_scroll.png"
OUT_PREVIEW = r"tools\out\ink_scroll_preview.jpg"

SKY_TOP = (4, 5, 9)
SKY_HOR = (24, 28, 36)
RIDGE_FAR = (7, 9, 13)
RIDGE_MID = (3, 4, 7)
RIDGE_NEAR = (1, 1, 3)
GROUND = (2, 2, 4)
INK_WHITE = (232, 239, 248)
RED = (178, 34, 42)

HORIZON = 0.60


def vgrad(w, h, top, bottom):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = (y / (h - 1.0)) ** 1.7
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (w, y)], fill=c)
    return img


def ridge(x, base_y, comps):
    y = base_y
    for freq, a, ph in comps:
        y -= a * math.sin(2.0 * math.pi * freq * x / W + ph)
    return y


def comps_for(rnd, amp, peaks):
    comps = []
    for _ in range(5):
        comps.append((rnd.randint(1, 6), amp * rnd.uniform(0.3, 1.0), rnd.uniform(0, math.tau)))
    for _ in range(7):
        comps.append((rnd.randint(9, 19), amp * rnd.uniform(0.15, 0.40), rnd.uniform(0, math.tau)))
    for _ in range(peaks):
        comps.append((rnd.randint(2, 9), amp * rnd.uniform(1.2, 3.0), rnd.uniform(0, math.tau)))
    return comps


def draw_layer(draw, rnd, base_y, amp, color, peaks):
    comps = comps_for(rnd, amp, peaks)
    pts = [(x, ridge(x, base_y, comps)) for x in range(0, W + 4, 4)]
    for shift in (-W, 0, W):
        draw.polygon([(x + shift, y) for x, y in pts] + [(W + shift, H), (shift, H)], fill=color)
    return pts


def stroke_path(x0, y0, angle, length, curve):
    steps = 34
    pts = []
    for i in range(steps):
        f = i / (steps - 1.0)
        x = x0 + math.cos(angle) * length * f
        y = y0 + math.sin(angle) * length * f + curve * length * (f ** 2)
        pts.append((x, y))
    return pts


def draw_stroke(draw, rnd, x0, y0, angle, length, width, color):
    """一道由粗到细的笔锋，末端再带几滴飞溅。"""
    pts = stroke_path(x0, y0, angle, length, rnd.uniform(-0.35, 0.35))
    for i, (x, y) in enumerate(pts):
        f = i / (len(pts) - 1.0)
        r = width * (1.0 - f) ** 1.4 + 1.2
        for shift in (-W, 0, W):
            draw.ellipse([x + shift - r, y - r * 0.8, x + shift + r, y + r * 0.8], fill=color)
    tx, ty = pts[-1]
    for _ in range(rnd.randint(2, 6)):
        r = rnd.uniform(1.0, 4.5)
        dx = rnd.uniform(-40, 40)
        dy = rnd.uniform(-25, 25)
        for shift in (-W, 0, W):
            draw.ellipse([tx + dx + shift - r, ty + dy - r, tx + dx + shift + r, ty + dy + r], fill=color)


def main():
    rnd = random.Random(7731)

    img = vgrad(W, H, SKY_TOP, SKY_HOR)

    # ---- 远景淡光（山之前）
    base_glow = Image.new("RGB", (W, H), (0, 0, 0))
    gd = ImageDraw.Draw(base_glow)
    for i in range(9):
        rx = W * (0.45 + i * 0.06)
        ry = H * (0.05 + i * 0.014)
        v = int(120 * (1.0 - i / 9.0))
        gd.ellipse([W / 2 - rx, H * HORIZON - ry, W / 2 + rx, H * HORIZON + ry],
                   fill=(v, v, int(v * 1.06)))
    base_glow = base_glow.filter(ImageFilter.GaussianBlur(80))
    img = ImageChops.add(img, base_glow)

    # ---- 远山
    draw = ImageDraw.Draw(img)
    draw_layer(draw, rnd, H * (HORIZON - 0.02), 46, RIDGE_FAR, 3)

    # ---- 远山之后的强逆光（只在地平线附近）
    rim = Image.new("RGB", (W, H), (0, 0, 0))
    rd = ImageDraw.Draw(rim)
    for i in range(6):
        rx = W * (0.60 + i * 0.05)
        ry = H * (0.020 + i * 0.008)
        v = int(190 * (1.0 - i / 6.0))
        rd.ellipse([W / 2 - rx, H * HORIZON - ry, W / 2 + rx, H * HORIZON + ry],
                   fill=(v, int(v * 1.03), int(v * 1.10)))
    rim = rim.filter(ImageFilter.GaussianBlur(50))
    img = ImageChops.add(img, rim)
    draw = ImageDraw.Draw(img)

    # ---- 中景 / 近景
    draw_layer(draw, rnd, H * (HORIZON + 0.045), 66, RIDGE_MID, 4)
    draw_layer(draw, rnd, H * (HORIZON + 0.135), 52, RIDGE_NEAR, 3)

    # ---- 天空白色笔锋：粗细长短都不一
    for _ in range(9):
        draw_stroke(draw, rnd, rnd.uniform(0, W), rnd.uniform(H * 0.05, H * 0.50),
                    rnd.uniform(-0.8, 0.8), rnd.uniform(W * 0.07, W * 0.15),
                    rnd.uniform(8, 22), INK_WHITE)
    for _ in range(5):
        draw_stroke(draw, rnd, rnd.uniform(0, W), rnd.uniform(H * 0.10, H * 0.45),
                    rnd.uniform(-0.6, 0.6), rnd.uniform(W * 0.02, W * 0.05),
                    rnd.uniform(26, 48), INK_WHITE)
    for _ in range(160):
        x = rnd.uniform(0, W)
        y = rnd.uniform(H * 0.02, H * 0.56)
        r = rnd.uniform(0.8, 4.0)
        draw.ellipse([x - r, y - r, x + r, y + r], fill=INK_WHITE)

    # ---- 血红细笔
    for _ in range(6):
        draw_stroke(draw, rnd, rnd.uniform(0, W), rnd.uniform(H * 0.18, H * 0.52),
                    rnd.uniform(-0.45, 0.45), rnd.uniform(W * 0.03, W * 0.08),
                    rnd.uniform(2.5, 6.0), RED)

    # ---- 山腰雾带
    mist = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    md = ImageDraw.Draw(mist)
    for _ in range(22):
        cx = rnd.uniform(0, W)
        cy = rnd.uniform(H * (HORIZON - 0.02), H * (HORIZON + 0.16))
        rx = rnd.uniform(W * 0.05, W * 0.20)
        ry = rnd.uniform(H * 0.006, H * 0.030)
        a = rnd.randint(26, 78)
        md.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(236, 243, 251, a))
    mist = mist.filter(ImageFilter.GaussianBlur(28))
    img = Image.alpha_composite(img.convert("RGBA"), mist).convert("RGB")
    draw = ImageDraw.Draw(img)

    # ---- 墨湖 + 白色水纹
    pool = H * 0.815
    draw.rectangle([0, pool, W, H], fill=GROUND)
    for _ in range(30):
        cx = rnd.uniform(0, W)
        cy = rnd.uniform(pool + 8, H - 4)
        rx = rnd.uniform(W * 0.02, W * 0.10)
        ry = rnd.uniform(3, 13)
        v = rnd.randint(110, 205)
        draw.arc([cx - rx, cy - ry, cx + rx, cy + ry], rnd.randint(0, 110),
                 rnd.randint(215, 340), fill=(v, v, min(255, v + 8)), width=3)

    img = img.filter(ImageFilter.GaussianBlur(0.9))
    small = img.resize((2048, 1024), Image.LANCZOS)
    small.save(OUT_TEX, "PNG", optimize=True)
    print("  texture:", small.size, os.path.getsize(OUT_TEX), "bytes")
    small.resize((1024, 512), Image.LANCZOS).save(OUT_PREVIEW, "JPEG", quality=92)
    print("  preview:", OUT_PREVIEW)


if __name__ == "__main__":
    main()
