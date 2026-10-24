# -*- coding: utf-8 -*-
"""
Draw the 16x16 item texture for 阵法图鉴 (formation_codex): a closed book with a
cinnabar rune painted on the cover.

The rune is computed, not hand-placed, so it comes out as a real one-pixel circle
instead of a blocky square - the same trick the formation base texture uses
(tools/gen_cinnabar_and_base.py).

Outputs:
  src/main/resources/assets/fulu/textures/item/formation_codex.png
  tools/out/formation_codex_x8.png

Usage: python tools/gen_formation_codex.py
"""

import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSET = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                     "textures", "item", "formation_codex.png")
OUT = os.path.join(HERE, "out")

COVER = (92, 56, 40, 255)        # leather, lit
COVER_DARK = (66, 38, 28, 255)   # leather, shaded (bottom of the cover)
EDGE = (44, 24, 18, 255)         # the dark rim around the whole book
SPINE = (58, 32, 24, 255)        # the spine strip on the left
PAGE = (226, 214, 178, 255)      # the page block, seen on the right
PAGE_DARK = (186, 172, 136, 255)
RUNE = (206, 46, 34, 255)        # cinnabar

# The book occupies columns 2..14. The cover is the block of leather between the
# spine (col 3) and the page block (col 13).
SPINE_COL = 3
COVER_X0, COVER_X1 = 4, 12
COVER_Y0, COVER_Y1 = 2, 12


def rune_at(x, y):
    """
    A diamond outline plus a centre dot - one pixel thick.

    Not a circle: at this size a radius-3 circle *is* a square, and a rotated
    square reads as a sigil where an axis-aligned one reads as a picture frame.
    It also matches the inner diamond on the formation base texture, so the two
    blocks look like they belong to the same mod.
    """
    cx = (COVER_X0 + COVER_X1) / 2.0 + 0.5      # 8.5
    cy = (COVER_Y0 + COVER_Y1) / 2.0 + 0.5      # 7.5
    dx, dy = x - cx, y - cy
    if round(abs(dx) + abs(dy)) == 4:
        return RUNE
    if abs(dx) < 1.0 and abs(dy) < 1.0:
        return RUNE
    return None


def book_pixel(x, y):
    if x < 2 or x > 14 or y < 1 or y > 13:
        return None
    if x in (2, 14) or y in (1, 13):
        return EDGE                              # outer rim
    if x == SPINE_COL:
        return SPINE                             # spine
    if x == 13:
        return PAGE if y < 11 else PAGE_DARK     # page block on the right
    if COVER_X0 <= x <= COVER_X1 and COVER_Y0 <= y <= COVER_Y1:
        return rune_at(x, y) or (COVER if y < 11 else COVER_DARK)
    return None


def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            colour = book_pixel(x, y)
            if colour:
                img.putpixel((x, y), colour)

    os.makedirs(os.path.dirname(ASSET), exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    img.save(ASSET)
    print("wrote %s (%dx%d)" % (ASSET, img.width, img.height))

    tile = 8
    sheet = Image.new("RGB", (16 * tile, 16 * tile), (40, 38, 48))
    big = img.resize((16 * tile, 16 * tile), Image.NEAREST)
    sheet.paste(big, (0, 0), big)
    sheet.save(os.path.join(OUT, "formation_codex_x8.png"))
    print("wrote tools/out/formation_codex_x8.png")


if __name__ == "__main__":
    main()
