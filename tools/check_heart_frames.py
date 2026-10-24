# -*- coding: utf-8 -*-
"""
从原版资源里把 icons.png 的心形帧截出来，确认坐标（16/52/61, v=0）是不是对的。
顺便拼一张预览，肉眼确认"绿色心形"长什么样。
"""
import io
import os
import zipfile
import glob

from PIL import Image, ImageDraw

# 原版客户端资源可能在 client-extra 里
cands = []
for pat in ("tools/.gradle-home/caches/forge_gradle/**/*client-extra*.jar",
            "tools/.gradle-home/caches/forge_gradle/**/*client*.jar",
            "tools/.gradle-home/caches/forge_gradle/**/joined-*.jar"):
    cands += glob.glob(pat, recursive=True)
print("找到 %d 个候选 jar" % len(cands))

icons = None
for jar_path in cands:
    try:
        z = zipfile.ZipFile(jar_path)
    except Exception:
        continue
    for name in ("assets/minecraft/textures/gui/icons.png", "textures/gui/icons.png"):
        if name in z.namelist():
            icons = Image.open(io.BytesIO(z.read(name))).convert("RGBA")
            print("从 %s 取到 %s  %s" % (os.path.basename(jar_path), name, icons.size))
            break
    if icons is not None:
        break

if icons is None:
    print("没找到 icons.png")
    raise SystemExit(1)

out = os.path.join("tools", "out", "heart_frames.png")
scale = 8
frames = [("empty", 16), ("full", 52), ("half", 61)]
sheet = Image.new("RGB", (3 * 9 * scale + 40, 9 * scale + 40), (40, 38, 48))
d = ImageDraw.Draw(sheet)
x = 10
for label, u in frames:
    sub = icons.crop((u, 0, u + 9, 9))
    big = sub.resize((9 * scale, 9 * scale), Image.NEAREST)
    sheet.paste(big, (x, 10), big)
    d.text((x, 9 * scale + 16), "%s u=%d" % (label, u), fill=(220, 220, 220))
    # 染绿版本
    tinted = Image.new("RGBA", sub.size, (0, 0, 0, 0))
    px = sub.load()
    tp = tinted.load()
    for yy in range(9):
        for xx in range(9):
            r, g, b, a = px[xx, yy]
            tp[xx, yy] = (int(r * 0.30), int(g * 1.0), int(b * 0.35), a)
    big2 = tinted.resize((9 * scale, 9 * scale), Image.NEAREST)
    sheet.paste(big2, (x, 10 + 9 * scale + 22), big2)
    x += 9 * scale + 10
sheet.save(out)
print("wrote", out)
