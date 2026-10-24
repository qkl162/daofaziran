# -*- coding: utf-8 -*-
"""
Draw the 18x18 icon for the 内讧 (infighting) mob effect.

Vanilla effect icons are 18x18 PNGs in assets/<mod>/textures/mob_effect/<id>.png
(note: mob_effect, singular - every other asset folder here is plural).

The picture: two arrowheads driving at each other with a clash between them -
"these two are going at each other", which is exactly what the effect marks.
Palette follows the mod's own 乱神符 (violet ink) with a red clash, so the icon
reads as "red or purple" the way the effect's category (HARMFUL, red frame) plus
its tint colour (violet) do.

Outputs:
  src/main/resources/assets/fulu/textures/mob_effect/infighting.png
  tools/out/infighting_icon_x8.png     (8x on a dark and a light background)

Usage: python tools/gen_infighting_icon.py
"""

import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSET = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                     "textures", "mob_effect", "infighting.png")
OUT = os.path.join(HERE, "out")

C = {
    ".": (0, 0, 0, 0),            # transparent
    "l": (226, 208, 246, 255),    # light violet (arrow highlight)
    "m": (184, 140, 226, 255),    # violet
    "s": (138, 62, 196, 255),     # deep violet
    "d": (96, 38, 148, 255),      # darkest violet (outline)
    "r": (214, 62, 52, 255),      # the clash
    "R": (150, 30, 26, 255),      # the clash, dark
}

W = H = 18


def render():
    """Two arrowheads driving at each other, with a red clash between the tips."""
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # (x, top y, bottom y) columns of a left-to-right filled triangle.
    # Left arrow: base at x=2, tip at x=7. Right arrow: mirror image.
    left = [(2, 5, 13), (3, 6, 12), (4, 7, 11), (5, 8, 10), (6, 9, 9), (7, 9, 9)]
    right = [(15 - x, y0, y1) for x, y0, y1 in left]

    for cols in (left, right):
        for x, y0, y1 in cols:
            for y in range(y0, y1 + 1):
                # light on top, mid below: the usual top-left light source
                img.putpixel((x, y), C["l"] if y <= 9 else C["m"])
            img.putpixel((x, y0), C["d"])
            img.putpixel((x, y1), C["d"])

    # the clash: a small red diamond right between the two tips
    spark = [(8, 9), (9, 9), (8, 8), (9, 8), (8, 10), (9, 10), (7, 9), (10, 9)]
    for x, y in spark:
        img.putpixel((x, y), C["r"])
    img.putpixel((8, 9), C["R"])
    img.putpixel((9, 9), C["R"])
    return img


def main():
    img = render()

    os.makedirs(os.path.dirname(ASSET), exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    img.save(ASSET)

    # an ascii dump, so the shape is reviewable in a terminal too
    for y in range(H):
        row = ""
        for x in range(W):
            px = img.getpixel((x, y))
            row += "." if px[3] == 0 else ("r" if px[:3] == C["r"][:3] else
                                           "R" if px[:3] == C["R"][:3] else
                                           "d" if px[:3] == C["d"][:3] else
                                           "l" if px[:3] == C["l"][:3] else "m")
        print(row)
    print()
    print("wrote %s (%dx%d)" % (ASSET, img.width, img.height))

    tile, gap = 8, 12
    sheet = Image.new("RGB", (W * tile, H * tile * 2 + gap * 3), (34, 34, 40))
    d = ImageDraw.Draw(sheet)
    big = img.resize((W * tile, H * tile), Image.NEAREST)
    # on the dark panel an effect icon sits on, and on a light one
    d.rectangle([0, 0, sheet.width, H * tile + gap], fill=(24, 22, 30))
    sheet.paste(big, (0, gap), big)
    sheet.paste(big, (0, H * tile + gap * 2), big)
    d.text((4, H * tile + gap * 2 + H * tile - 10), "dark / light", fill=(200, 200, 200))
    sheet.save(os.path.join(OUT, "infighting_icon_x8.png"))
    print("wrote tools/out/infighting_icon_x8.png (%dx%d)" % (sheet.width, sheet.height))


if __name__ == "__main__":
    main()
