# -*- coding: utf-8 -*-
"""
生成自己的「绿色心形」贴图：从原版 icons.png 抠出空/满/半三帧，重新着色成绿色。

为什么必须做贴图而不是染色：`RenderSystem.setShaderColor` 是**乘法**，
红心 (1,0,0) 乘 (0.3,1,0.35) 得到 (0.3,0,0) —— 只是变暗红，不会变绿。
要把红改成绿只能换颜色通道，所以干脆把三帧烘成一张自己的贴图（32x9：0/9/18）。

着色方法：保留原来的**明暗**（亮度），把色相换成绿色 ——
这样原版心形的高光、暗边、半心的斜切都还在，只是变绿了。
"""
import io
import os
import zipfile
import glob

from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")

FRAMES = [("empty", 16), ("full", 52), ("half", 61)]
SIZE = 9


def find_icons():
    cands = []
    for pat in ("tools/.gradle-home/caches/forge_gradle/**/*client-extra*.jar",
                "tools/.gradle-home/caches/forge_gradle/**/joined-*.jar"):
        cands += glob.glob(pat, recursive=True)
    for jar_path in cands:
        try:
            z = zipfile.ZipFile(jar_path)
        except Exception:
            continue
        name = "assets/minecraft/textures/gui/icons.png"
        if name in z.namelist():
            return Image.open(io.BytesIO(z.read(name))).convert("RGBA")
    raise SystemExit("icons.png not found")


def recolour(frame, empty=False):
    """按亮度重新着色成绿色。"""
    out = Image.new("RGBA", frame.size, (0, 0, 0, 0))
    src = frame.load()
    dst = out.load()
    for y in range(frame.height):
        for x in range(frame.width):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            # 亮度：原版心形是红 + 白高光 + 深红暗边，用 max 通道近似明暗
            lum = max(r, g, b) / 255.0
            if empty:
                # 空壳：暗灰绿，看得出"格子空着"
                dst[x, y] = (int(40 * lum), int(90 * lum), int(48 * lum), a)
            else:
                # 满/半：绿色为主，红色压很低，蓝色一点点让高光不发黄
                dst[x, y] = (int(70 * lum), int(255 * lum), int(95 * lum), a)
    return out


def main():
    icons = find_icons()
    strip = Image.new("RGBA", (SIZE * len(FRAMES), SIZE), (0, 0, 0, 0))
    for i, (label, u) in enumerate(FRAMES):
        frame = icons.crop((u, 0, u + SIZE, SIZE))
        strip.paste(recolour(frame, empty=(label == "empty")), (i * SIZE, 0))
        print("  frame %-5s -> u=%d" % (label, i * SIZE))

    p = os.path.join(ASSETS, "textures", "gui", "shield_hearts.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    strip.save(p)
    print("wrote", os.path.relpath(p, ROOT), strip.size)

    # 预览：放大 + 模拟"血条上方一条绿心"的样子
    scale = 8
    sheet = Image.new("RGB", (SIZE * 5 * scale + 40, SIZE * scale * 2 + 60), (44, 42, 52))
    d = ImageDraw.Draw(sheet)
    # 上面：绿心 5 颗（3 满 + 1 半 + 1 空，模拟 14 点）
    order = [1, 1, 1, 2, 0]     # 0=empty 1=full 2=half
    for i, which in enumerate(order):
        sub = strip.crop((which * SIZE, 0, which * SIZE + SIZE, SIZE))
        big = sub.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        sheet.paste(big, (20 + i * SIZE * scale, 12), big)
    # 下面：原版红心（对照）
    for i, which in enumerate(order):
        u = FRAMES[which][1]
        sub = icons.crop((u, 0, u + SIZE, SIZE))
        big = sub.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        sheet.paste(big, (20 + i * SIZE * scale, 12 + SIZE * scale + 18), big)
    d.text((20, SIZE * scale * 2 + 36), "top: our green hearts   bottom: vanilla red", fill=(210, 210, 210))
    prev = os.path.join(ROOT, "tools", "out", "shield_hearts_preview.png")
    sheet.save(prev)
    print("wrote", os.path.relpath(prev, ROOT))


if __name__ == "__main__":
    main()
