# -*- coding: utf-8 -*-
"""
Draw the rune particle: the little glowing sigils that spiral around the flying
sword from the hilt to the tip.

8x8, transparent background, a bright sigil with a soft halo - it has to read as
a symbol at a glance while spinning.

Outputs:
  src/main/resources/assets/fulu/textures/particle/rune_mote.png
  tools/out/rune_mote_x12.png

Usage: python tools/gen_rune_texture.py
"""

import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                       "textures", "particle", "rune_mote.png")
OUT_DIR = os.path.join(HERE, "out")

SIZE = 8
GLYPH = (214, 244, 255, 255)   # pale cyan-white, matches the sword
HALO = (110, 190, 240, 255)

# A small angular sigil - an Algiz-style rune (two branches over a stem), which
# reads as "a rune" even at 8x8 and matches nothing in vanilla.
PATTERN = [
    "#......#",
    ".#....#.",
    "..#..#..",
    "...#.#..",
    "...#....",
    "...#....",
    "...#....",
    "...##...",
]


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(PATTERN):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x, y), GLYPH)
    # a faint halo one pixel out, so it glows instead of looking like a sticker
    src = img.copy()
    for y in range(SIZE):
        for x in range(SIZE):
            if src.getpixel((x, y))[3] != 0:
                continue
                # (the sigil itself is left alone)
            near = False
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < SIZE and 0 <= ny < SIZE and src.getpixel((nx, ny))[3] > 0:
                        near = True
            if near:
                img.putpixel((x, y), (HALO[0], HALO[1], HALO[2], 70))

    os.makedirs(os.path.dirname(OUT_TEX), exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)
    img.save(OUT_TEX)
    print("wrote %s (%dx%d)" % (OUT_TEX, img.width, img.height))

    scale = 12
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    sheet = Image.new("RGB", (big.width + 16, big.height + 16), (26, 30, 40))
    sheet.paste(big, (8, 8), big)
    sheet.save(os.path.join(OUT_DIR, "rune_mote_x12.png"))
    print("wrote tools/out/rune_mote_x12.png")


if __name__ == "__main__":
    main()
