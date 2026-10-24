# -*- coding: utf-8 -*-
"""
阵魔 Boss 血条贴图 v3：**不分段**，换成阵魔的专属纹样。

阵魔的特征元素：
  * **阵眼** —— 血条正中央一只独眼（金框、竖瞳、血红瞳孔）；
  * **锁链** —— 上下边缘一串链环（它身上缠着断裂的阵基锁链）；
  * **符文** —— 下沿四组暗金篆形短笔（阵纹）；
  * **爪形端头** —— 左右各三根金爪，像它的手；
  * **紫雾收边** —— 两端叠一层紫雾，呼应它的紫黑色调。

中间窗口仍留空，由填充层透出；没有任何竖向分隔线 —— 血条是**连续**的。
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

OUT_DIR = r"src\main\resources\assets\fulu\textures\gui"
W, H = 384, 32
WIN_TOP, WIN_BOT = 9, 23

MAROON_DARK = (26, 8, 12, 255)
MAROON = (62, 16, 22, 255)
MAROON_LIT = (96, 26, 32, 255)
GOLD_DARK = (120, 90, 28, 255)
GOLD = (198, 158, 70, 255)
GOLD_BRIGHT = (246, 218, 138, 255)
GOLD_WHITE = (255, 244, 208, 255)
BLOOD = (196, 30, 38, 255)
PURPLE = (128, 72, 200)


def frame():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # ---- 外框底
    d.rounded_rectangle([0, 0, W - 1, H - 1], radius=7, fill=MAROON_DARK)
    for y in range(H):
        t = y / (H - 1.0)
        d.line([(2, y), (W - 3, y)], fill=MAROON_LIT if t < 0.45 else MAROON)

    # ---- 挖空中间窗口（连续，不做分段）
    d.rectangle([4, WIN_TOP, W - 5, WIN_BOT], fill=(0, 0, 0, 0))

    # ---- 双层金线
    for y in (1, 29):
        d.line([(5, y), (W - 6, y)], fill=GOLD_DARK)
        d.line([(5, y + 1), (W - 6, y + 1)], fill=GOLD)
    d.line([(5, 3), (W - 6, 3)], fill=GOLD_WHITE)
    d.line([(5, 28), (W - 6, 28)], fill=GOLD_WHITE)

    # ---- 锁链：上下各一串链环（交替方向）
    for i, x in enumerate(range(12, W - 10, 7)):
        d.rounded_rectangle([x, 4, x + 4, 8], radius=2, outline=GOLD, width=1)
        d.rounded_rectangle([x + 1, 5, x + 3, 7], radius=1, fill=GOLD_DARK)
        d.rounded_rectangle([x, H - 9, x + 4, H - 5], radius=2, outline=GOLD, width=1)
        d.rounded_rectangle([x + 1, H - 8, x + 3, H - 6], radius=1, fill=GOLD_DARK)

    # ---- 四角卷云角饰
    for (cx, cy, sx, sy) in ((4, 4, 1, 1), (W - 5, 4, -1, 1), (4, H - 5, 1, -1), (W - 5, H - 5, -1, -1)):
        d.line([(cx, cy), (cx + 5 * sx, cy)], fill=GOLD, width=2)
        d.line([(cx, cy), (cx, cy + 3 * sy)], fill=GOLD, width=2)

    # ---- 正中：阵眼
    ex, ey = W // 2, 4
    d.polygon([(ex - 11, ey), (ex, ey - 3), (ex + 11, ey), (ex, ey + 3)], fill=GOLD_DARK)
    d.polygon([(ex - 9, ey), (ex, ey - 2), (ex + 9, ey), (ex, ey + 2)], fill=BLOOD)
    d.ellipse([ex - 1, ey - 2, ex + 1, ey + 2], fill=(255, 214, 120, 255))
    d.line([(ex, ey - 2), (ex, ey + 2)], fill=(90, 10, 14, 255))
    d.rectangle([ex - 12, ey + 3, ex + 12, ey + 4], fill=GOLD)

    # ---- 下沿符文（四组，篆形短笔）
    for gx in (int(W * 0.22), int(W * 0.38), int(W * 0.62), int(W * 0.78)):
        by = H - 11
        d.line([(gx, by), (gx + 6, by)], fill=GOLD_DARK)
        d.line([(gx + 3, by), (gx + 3, by + 4)], fill=GOLD)
        d.line([(gx + 1, by + 2), (gx + 5, by + 2)], fill=GOLD)
        d.line([(gx, by + 4), (gx + 6, by + 4)], fill=GOLD_DARK)

    # ---- 爪形端头（左右各三根）
    for side in (0, 1):
        base = 0 if side == 0 else W - 1
        dirn = 1 if side == 0 else -1
        for k, dy in enumerate((-6, 0, 6)):
            tip = base + dirn * 6
            d.polygon([(base, H // 2 + dy - 3), (tip, H // 2 + dy), (base, H // 2 + dy + 3)],
                      fill=GOLD_DARK if k != 1 else GOLD)
        d.rectangle([base - (0 if side == 0 else 4), 3, base + (4 if side == 0 else 0), H - 4],
                    fill=GOLD_DARK)
        d.line([(base + dirn, 6), (base + dirn, H - 7)], fill=GOLD_BRIGHT)

    # ---- 紫雾收边
    mist = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    md = ImageDraw.Draw(mist)
    for x in range(0, 34):
        a = int(70 * (1.0 - x / 34.0))
        md.line([(x, 2), (x, H - 3)], fill=PURPLE + (a,))
    for x in range(0, 34):
        a = int(70 * (1.0 - x / 34.0))
        md.line([(W - 1 - x, 2), (W - 1 - x, H - 3)], fill=PURPLE + (a,))
    mist = mist.filter(ImageFilter.GaussianBlur(4))
    img = Image.alpha_composite(img, mist)

    # ---- 窗口内沿细线
    d = ImageDraw.Draw(img)
    d.line([(8, WIN_TOP - 1), (W - 9, WIN_TOP - 1)], fill=GOLD_DARK)
    d.line([(8, WIN_BOT + 1), (W - 9, WIN_BOT + 1)], fill=GOLD_DARK)
    return img


def fill():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    h = WIN_BOT - WIN_TOP + 1
    for y in range(h):
        t = y / (h - 1.0)
        k = max(0.25, min(1.0, 1.0 - abs(t - 0.45) * 1.5))
        d.line([(0, WIN_TOP + y), (W - 1, WIN_TOP + y)],
               fill=(int(120 + 135 * k), int(26 + 140 * k), int(30 + 70 * k), 255))
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    for x in range(W):
        f = x / (W - 1.0)
        od.line([(x, WIN_TOP), (x, WIN_BOT)], fill=(255, 236, 170, int(30 * f)))
    img = Image.alpha_composite(img, ov)
    d = ImageDraw.Draw(img)
    d.line([(1, WIN_TOP + 1), (W - 2, WIN_TOP + 1)], fill=(255, 232, 172, 130))
    d.line([(1, WIN_BOT - 1), (W - 2, WIN_BOT - 1)], fill=(60, 10, 14, 120))
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    f1, f2 = frame(), fill()
    f1.save(os.path.join(OUT_DIR, "boss_bar_frame.png"), "PNG", optimize=True)
    f2.save(os.path.join(OUT_DIR, "boss_bar_fill.png"), "PNG", optimize=True)

    prev = Image.new("RGBA", (W * 2 + 40, H * 2 + 50), (18, 18, 22, 255))
    bar = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    bar.alpha_composite(f2.crop((0, 0, int(W * 0.62), H)), (0, 0))
    bar.alpha_composite(f1, (0, 0))
    prev.alpha_composite(bar.resize((W * 2, H * 2), Image.NEAREST), (10, 20))
    prev.convert("RGB").save(r"tools\out\boss_bar_preview.png")
    print("  wrote v3 frame/fill + preview")


if __name__ == "__main__":
    main()
