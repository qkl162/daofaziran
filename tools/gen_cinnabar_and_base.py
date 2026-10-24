# -*- coding: utf-8 -*-
"""
Draw the two textures this step needs.

  朱砂 cinnabar             a small heap of deep vermilion powder
  阵基 formation_base       a dressed stone tile with a cinnabar seam

Both are 16x16, drawn from the mod's own palette so they sit next to 符纸 /
乱神符 / 阵眼 without clashing.

Usage: python tools/gen_cinnabar_and_base.py
"""

import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "textures", "item")
BLOCK_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "textures", "block")
OUT = os.path.join(HERE, "out")

# ---- cinnabar: red powder -------------------------------------------------
CINNABAR = {
    ".": (0, 0, 0, 0),
    "r": (208, 44, 34, 255),      # bright vermilion
    "R": (162, 26, 20, 255),      # mid
    "d": (110, 16, 14, 255),      # shadow / grain
    "h": (246, 120, 96, 255),     # highlight fleck
}

POWDER = [
    "................",
    "................",
    "................",
    ".....h..........",
    "....hrr....h....",
    "...hrrrr..rr....",
    "..rrrrrrrrrr....",
    "..rrrrrrrrrrr...",
    ".rrrrrrrrrrrrr..",
    ".rrrrRrrrrrrrr..",
    "rrrrrRRrrrrrrrr.",
    "rrrRRRRRrrrrrrr.",
    "rrRRRRRRRrrrrrr.",
    ".RdRRRRRRRrrrRd.",
    "..ddRRRRRRRdd...",
    "...ddddddddd....",
]

# ---- formation base: stone bricks with a cinnabar rune on top -------------
STONE_LIGHT = (150, 148, 142, 255)
STONE_MID = (128, 126, 121, 255)
STONE_DARK = (96, 94, 90, 255)
MORTAR = (72, 70, 67, 255)
RUNE = (206, 46, 34, 255)          # the rune lines themselves
RUNE_DIM = (150, 28, 20, 255)      # the outer ring, a shade darker


def base_pixel(x, y):
    """A stone-brick pattern: 4px courses with 1px mortar, staggered joints."""
    # horizontal mortar every 4 rows
    if y % 4 == 0:
        return MORTAR
    band = y // 4
    # vertical joints every 8 columns, offset by 4 on alternating courses
    offset = 4 if band % 2 else 0
    if (x + offset) % 8 == 0:
        return MORTAR
    # shade the bricks a little so they do not look flat
    brick_id = ((x + offset) // 8) * 3 + band
    shade = (x * 5 + y * 3 + brick_id * 7) % 11
    if shade == 0:
        return STONE_LIGHT
    if shade <= 2:
        return STONE_DARK
    return STONE_MID


def base_rune(x, y):
    """
    Thin cinnabar line work, one pixel wide:

      - a circle (every pixel whose distance from the centre rounds to 6)
      - a diamond outline (|dx| + |dy| == 3)
      - a 2x2 dot in the middle

    Keeping it to *lines* is the point: an earlier attempt filled the middle and
    read as one red blob instead of a rune drawn on the stone.
    """
    dx, dy = x - 7.5, y - 7.5
    dist = (dx * dx + dy * dy) ** 0.5
    if round(dist) == 6:
        return RUNE
    if abs(dx) + abs(dy) == 3.0:
        return RUNE
    if abs(dx) < 1.0 and abs(dy) < 1.0:
        return RUNE
    return None


def draw_base(path, scale=8):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            colour = base_rune(x, y) or base_pixel(x, y)
            img.putpixel((x, y), colour)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))
    preview(img, path, scale)


def preview(img, path, scale=8):
    sheet = Image.new("RGB", (16 * scale, 16 * scale), (40, 38, 48))
    big = img.resize((16 * scale, 16 * scale), Image.NEAREST)
    sheet.paste(big, (0, 0), big)
    out = os.path.join(OUT, os.path.basename(path).replace(".png", "_x8.png"))
    sheet.save(out)
    print("wrote", os.path.relpath(out, ROOT))


def draw(art, palette, path, scale=8):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch])
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))
    preview(img, path, scale)


def main():
    os.makedirs(OUT, exist_ok=True)
    draw(POWDER, CINNABAR, os.path.join(ITEM_DIR, "cinnabar.png"))
    draw_base(os.path.join(BLOCK_DIR, "formation_base.png"))


if __name__ == "__main__":
    main()
