# -*- coding: utf-8 -*-
"""
Draw the 16x16 block texture for 阵眼 (formation_eye).

Design (magic-circle conventions, outer ring -> spokes -> inner halo -> eye):
  * dark slate plate, subtly noisy so it does not look like flat plastic
  * a gold ring with four bright cardinal spokes running out to the block edge
  * a thin crimson halo, and in the middle a red "eye" with a bright ember core
    and a dark slit pupil

Reference: the net gave the usual magic-circle anatomy (outer circle + inner
circle + inscribed figure + central glyph) and the author's own xianxia mod
textures (array_core / rune_block / spirit_vein_core) supplied the palette and
the 16x16 density that this mod should match.

Outputs:
  src/main/resources/assets/fulu/textures/block/formation_eye.png   (the asset)
  tools/out/formation_eye_x8.png                                     (8x zoom)

Usage: python tools/gen_formation_eye.py
"""

import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSET = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                     "textures", "block", "formation_eye.png")
OUT = os.path.join(HERE, "out")

C = {
    "a": (44, 46, 56, 255),      # slate, deep
    "b": (56, 60, 73, 255),      # slate
    "c": (70, 75, 90, 255),      # slate, lit
    "g": (188, 144, 56, 255),    # gold, shaded
    "h": (240, 202, 110, 255),   # gold, lit / glowing
    "R": (124, 28, 26, 255),     # crimson, dark
    "r": (190, 48, 40, 255),     # crimson
    "B": (246, 158, 98, 255),    # ember core
    "K": (38, 14, 14, 255),      # pupil
}

W = H = 16
CX = CY = 7.5        # block textures are symmetric about the pixel corners


def radius(x, y):
    return math.hypot(x - CX, y - CY)


def stone(x, y):
    """Deterministic 3-tone slate noise - no RNG, so the asset is reproducible."""
    n = (x * 7 + y * 13 + (x // 3) * 5 + (y // 4) * 3) % 7
    if n in (0, 4):
        return "a"
    if n in (2, 5):
        return "c"
    return "b"


def build():
    px = [[stone(x, y) for x in range(W)] for y in range(H)]

    # block edge: one shade darker, so the block reads as a cube in world
    for i in range(W):
        px[0][i] = px[15][i] = "a"
        px[i][0] = px[i][15] = "a"

    # four cardinal spokes: edge -> ring
    for x, y in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):
        px[y][x] = "h"

    # gold ring + a brighter tone on the axes (the "compass" of the formation)
    for y in range(H):
        for x in range(W):
            r = radius(x, y)
            if 4.5 <= r <= 6.0:
                axis = min(abs(x - CX), abs(y - CY)) < 1.2
                px[y][x] = "h" if axis else "g"

    # inner crimson halo around the eye
    for y in range(H):
        for x in range(W):
            r = radius(x, y)
            if 2.9 <= r <= 3.5:
                px[y][x] = "R"

    # the eye itself
    for x, ch in ((6, "R"), (7, "r"), (8, "r"), (9, "R")):
        px[6][x] = ch
        px[9][x] = ch
    for x, ch in ((5, "R"), (6, "r"), (7, "B"), (8, "B"), (9, "r"), (10, "R")):
        px[7][x] = ch
    for x, ch in ((5, "R"), (6, "r"), (7, "K"), (8, "K"), (9, "r"), (10, "R")):
        px[8][x] = ch
    return px


def render(px):
    img = Image.new("RGBA", (W, H))
    for y in range(H):
        for x in range(W):
            img.putpixel((x, y), C[px[y][x]])
    return img


def main():
    px = build()
    img = render(px)
    os.makedirs(os.path.dirname(ASSET), exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    img.save(ASSET)
    print("\n".join("".join(row) for row in px))
    print("\nwrote %s (%dx%d)" % (ASSET, img.width, img.height))

    big = img.resize((W * 8, H * 8), Image.NEAREST)
    big.save(os.path.join(OUT, "formation_eye_x8.png"))
    print("wrote tools/out/formation_eye_x8.png")


if __name__ == "__main__":
    main()
