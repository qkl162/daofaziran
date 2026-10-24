# -*- coding: utf-8 -*-
"""
Draw the 16x16 item texture for 符纸 (talisman_paper).

Reference: photos pulled from the web into tools/ref/ (a real Daoist talisman is
a narrow strip of yellow paper with cinnabar writing down the middle and a small
red seal). Because the item is the *blank* paper, the writing is kept faint and
partial - it reads as "paper you still have to write on", not as a finished 符.

Outputs:
  src/main/resources/assets/fulu/textures/item/talisman_paper.png   (the real asset)
  tools/out/talisman_paper_x8.png                                   (8x zoom preview)
  tools/out/talisman_paper_slots.png                                (inventory mock)

Usage: python tools/gen_talisman_paper.py
"""

import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSET = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                     "textures", "item", "talisman_paper.png")
OUT = os.path.join(HERE, "out")

# ---------------------------------------------------------------- palette ----
C = {
    ".": (0, 0, 0, 0),            # transparent
    "l": (250, 227, 141, 255),    # paper, lit
    "m": (240, 205, 100, 255),    # paper, mid
    "s": (222, 180, 74, 255),     # paper, shaded
    "d": (190, 145, 46, 255),     # paper, edge (warm gold, not muddy brown)
    "D": (152, 110, 34, 255),     # paper, deep edge
    "r": (196, 46, 36, 255),      # cinnabar
    "R": (146, 28, 22, 255),      # cinnabar, dry / dark
    "y": (214, 180, 88, 255),     # crease
}

W = H = 16


def base_paper():
    """The strip itself: 6 px wide, 14 px tall, top and bottom torn 1 px in."""
    px = [["." for _ in range(W)] for _ in range(H)]
    for y in range(1, 15):
        if y in (1, 14):
            continue                     # handled below (torn top / bottom)
        # light falls from the left: a hard edge on the left, a soft one right
        for x, ch in ((5, "d"), (6, "l"), (7, "l"), (8, "l"), (9, "m"), (10, "s")):
            px[y][x] = ch
    for x in range(6, 10):               # torn-in top / bottom rows
        px[1][x] = "s"
        px[14][x] = "D"
    return px


def stamp(px, points, char):
    for x, y in points:
        px[y][x] = char


def add_rune(px):
    """A tall cinnabar stroke down the middle - suggestive, not a real glyph."""
    stamp(px, [(7, 3), (8, 3)], "r")                       # 起笔：短横
    stamp(px, [(8, 4)], "R")
    stamp(px, [(6, 5), (7, 5), (8, 5), (9, 5)], "r")       # 长横
    stamp(px, [(8, 6)], "r")
    stamp(px, [(7, 7), (8, 7)], "R")                       # 左折
    stamp(px, [(8, 8)], "r")
    stamp(px, [(7, 9), (8, 9)], "r")                       # 窄折
    stamp(px, [(8, 10)], "R")
    stamp(px, [(8, 11)], "r")
    stamp(px, [(7, 12), (8, 12)], "r")                     # 收笔


def add_seal(px):
    """Small red seal stamped at the foot of the strip, the way real 符 carry one."""
    for y in (12, 13):
        for x in (9, 10):
            px[y][x] = "R"


def render(px):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for y in range(H):
        for x in range(W):
            img.putpixel((x, y), C[px[y][x]])
    return img


def ascii_art(px):
    return "\n".join("".join(px[y]) for y in range(H))


def preview(img, tile, bg):
    sheet = Image.new("RGB", (W * tile, H * tile), bg)
    big = img.resize((W * tile, H * tile), Image.NEAREST)
    sheet.paste(big, (0, 0), big)
    return sheet


def slot_mock(img):
    """What it looks like sitting in an inventory slot (16 px), plus 2x."""
    slot = 20
    sheet = Image.new("RGB", (slot * 2 + 30, slot * 2 + 20), (198, 198, 198))
    d = ImageDraw.Draw(sheet)
    for i, (ox, scale) in enumerate(((10, 1), (10 + slot + 10, 2))):
        d.rectangle([ox, 10, ox + slot - 1, 10 + slot - 1], fill=(139, 139, 139))
        d.rectangle([ox, 10, ox + slot - 1, 10], fill=(55, 55, 55))
        d.rectangle([ox, 10, ox, 10 + slot - 1], fill=(55, 55, 55))
        art = img.resize((16 * scale, 16 * scale), Image.NEAREST)
        # centre it in the 18x18 inner area
        px_ = ox + 1 + (18 - 16 * scale) // 2
        py_ = 11 + (18 - 16 * scale) // 2
        if scale == 1:
            sheet.paste(art, (px_, py_), art)
            # vanilla draws the item 1 px up-right of centre
            sheet.paste(art, (ox + 2, 11), art)
        else:
            sheet.paste(art, (ox + 1, 11), art)
    return sheet


def main():
    px = base_paper()
    add_seal(px)
    add_rune(px)

    os.makedirs(os.path.dirname(ASSET), exist_ok=True)
    os.makedirs(OUT, exist_ok=True)

    img = render(px)
    img.save(ASSET)
    print(ascii_art(px))
    print()
    print("wrote %s (%dx%d)" % (ASSET, img.width, img.height))

    preview(img, 8, (46, 46, 52)).save(os.path.join(OUT, "talisman_paper_x8.png"))
    preview(img, 8, (232, 232, 232)).save(os.path.join(OUT, "talisman_paper_x8_light.png"))
    slot_mock(img).resize((slot_mock(img).width * 3, slot_mock(img).height * 3),
                          Image.NEAREST).save(os.path.join(OUT, "talisman_paper_slots.png"))
    print("wrote previews into tools/out/")


if __name__ == "__main__":
    main()
