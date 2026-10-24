#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成水墨符相关的两张 16×16 贴图。

- textures/item/ink_talisman.png   ：一支毛笔 + 一滴晕开的墨
- textures/block/formation_eye_ink.png：在原有阵眼贴图上，把中央的红芯换成墨滴

重跑本脚本会覆盖这两张图；改配色只改下面的 PALETTE。
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'fulu', 'textures')

INK = (0x1A, 0x1F, 0x2E, 255)      # 墨
INK_SOFT = (0x1A, 0x1F, 0x2E, 150)  # 淡墨
INK_WASH = (0x1A, 0x1F, 0x2E, 70)   # 晕开的边
WOOD = (0x6B, 0x44, 0x23, 255)     # 笔杆
WOOD_LIT = (0x8A, 0x5A, 0x2B, 255)
SILVER = (0x8A, 0x8A, 0x9A, 255)   # 笔箍
WHITE = (0xED, 0xEF, 0xF2, 255)


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), c)


def gen_item():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    # 笔杆：从右上到中间，两像素宽
    for i in range(6):
        x, y = 12 - i, 2 + i
        put(im, x, y, WOOD)
        put(im, x - 1, y, WOOD_LIT)
    # 笔箍
    put(im, 6, 8, SILVER)
    put(im, 5, 9, SILVER)
    # 笔头（墨色，向下收尖）
    put(im, 5, 10, INK)
    put(im, 4, 11, INK)
    put(im, 4, 12, INK)
    put(im, 3, 12, INK_SOFT)
    # 墨滴：中心实、外圈半透明
    cx, cy = 8, 12
    for dx in range(-2, 3):
        for dy in range(-1, 2):
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 1.0:
                put(im, cx + dx, cy + dy, INK)
            elif d <= 1.9:
                put(im, cx + dx, cy + dy, INK_SOFT)
    # 高光
    put(im, cx - 1, cy - 1, WHITE)
    # 晕开的淡墨边
    for dx, dy in [(-3, 0), (3, 0), (0, -2), (0, 2), (-2, -2), (2, 2)]:
        put(im, cx + dx, cy + dy, INK_WASH)
    im.save(os.path.join(ASSETS, 'item', 'ink_talisman.png'))


def gen_block():
    """在阵眼原图基础上，把中央换成墨滴（保留外圈金红纹样）。"""
    base = Image.open(os.path.join(ASSETS, 'block', 'formation_eye.png')).convert('RGBA')
    im = base.copy()
    cx, cy = 8, 8
    for dx in range(-3, 4):
        for dy in range(-3, 4):
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 1.6:
                put(im, cx + dx, cy + dy, INK)
            elif d <= 2.6:
                put(im, cx + dx, cy + dy, INK_SOFT)
            elif d <= 3.2:
                put(im, cx + dx, cy + dy, INK_WASH)
    # 墨滴上的高光 + 一点冷色，和星空那套呼应
    put(im, cx - 1, cy - 1, WHITE)
    put(im, cx - 2, cy - 1, (0x4F, 0xC3, 0xF7, 200))
    im.save(os.path.join(ASSETS, 'block', 'formation_eye_ink.png'))


if __name__ == '__main__':
    gen_item()
    gen_block()
    print('ok')
