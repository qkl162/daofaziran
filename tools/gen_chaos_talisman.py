# -*- coding: utf-8 -*-
"""
Draw the 16x16 item texture for 乱神符 (chaos_talisman).

It is the same strip of paper as 符纸, but this is a FINISHED talisman, so:
  - the paper is a colder, violet-tinted stock (乱神 = the mind is muddled);
  - the ink is a bright violet instead of cinnabar;
  - the motif in the middle is the same "eye" as the formation eye block, so the
    item and the block it goes into look like they belong together.

Outputs:
  src/main/resources/assets/fulu/textures/item/chaos_talisman.png   (the asset)
  tools/out/chaos_talisman_x8.png                                   (8x preview)
  tools/out/chaos_talisman_next_to_paper.png                        (compared to 符纸)

Usage: python tools/gen_chaos_talisman.py
"""

import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "textures", "item")
ASSET = os.path.join(ITEM_DIR, "chaos_talisman.png")
PAPER = os.path.join(ITEM_DIR, "talisman_paper.png")
OUT = os.path.join(HERE, "out")

# ---------------------------------------------------------------- palette ----
C = {
    ".": (0, 0, 0, 0),            # transparent
    "l": (222, 208, 240, 255),    # paper, lit (cold violet-white)
    "m": (203, 186, 226, 255),    # paper, mid
    "s": (176, 156, 202, 255),    # paper, shaded
    "d": (138, 116, 168, 255),    # paper, edge
    "D": (104, 84, 134, 255),     # paper, deep edge
    "r": (150, 62, 200, 255),     # violet ink
    "R": (104, 36, 150, 255),     # violet ink, dry / dark
    "b": (226, 150, 255, 255),    # the glow in the pupil
}

W = H = 16

# same strip as 符纸: 6 px wide, 14 px tall, top and bottom torn 1 px in
BASE_COLUMNS = ((5, "d"), (6, "l"), (7, "l"), (8, "l"), (9, "m"), (10, "s"))


def base_paper():
    px = [["." for _ in range(W)] for _ in range(H)]
    for y in range(2, 14):
        for x, ch in BASE_COLUMNS:
            px[y][x] = ch
    for x in range(6, 10):
        px[1][x] = "s"
        px[14][x] = "D"
        px[2][x] = "l"
        px[13][x] = "m"
    return px


def stamp(px, points, char):
    for x, y in points:
        px[y][x] = char


def add_eye(px):
    """The formation eye's motif, in miniature: a diamond outline + a bright pupil."""
    # diamond outline (2 px apart at the corners, like a squeezed eye)
    stamp(px, [(7, 4), (8, 4)], "R")                       # top
    stamp(px, [(6, 5), (9, 5)], "R")
    stamp(px, [(5, 6), (6, 6), (9, 6), (10, 6)], "r")      # widest row
    stamp(px, [(5, 7), (6, 7), (9, 7), (10, 7)], "R")
    stamp(px, [(6, 8), (9, 8)], "r")
    stamp(px, [(7, 9), (8, 9)], "r")                       # bottom
    # pupil: a bright core with a dark ring, the way the block's eye reads
    stamp(px, [(7, 6), (8, 6)], "R")
    stamp(px, [(7, 7), (8, 7)], "b")
    stamp(px, [(7, 8), (8, 8)], "R")
    # two short chaotic ticks above and below, so it reads as 乱 rather than calm
    stamp(px, [(9, 3)], "r")
    stamp(px, [(6, 11), (7, 11)], "r")
    stamp(px, [(9, 12)], "R")


def render(px):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for y in range(H):
        for x in range(W):
            img.putpixel((x, y), C[px[y][x]])
    return img


def preview(img, tile, bg):
    sheet = Image.new("RGB", (W * tile, H * tile), bg)
    big = img.resize((W * tile, H * tile), Image.NEAREST)
    sheet.paste(big, (0, 0), big)
    return sheet


def side_by_side(paper, chaos, tile):
    gap = 20
    w = W * tile * 2 + gap * 3
    h = H * tile + gap * 2 + 14
    sheet = Image.new("RGB", (w, h), (40, 38, 48))
    d = ImageDraw.Draw(sheet)
    for i, (img, label) in enumerate(((paper, "talisman_paper (blank)"),
                                      (chaos, "chaos_talisman (finished)"))):
        big = img.resize((W * tile, H * tile), Image.NEAREST)
        x = gap + i * (W * tile + gap)
        sheet.paste(big, (x, gap), big)
        d.text((x, h - gap - 6), label, fill=(210, 205, 220))
    return sheet


def main():
    px = base_paper()
    add_eye(px)
    img = render(px)

    os.makedirs(ITEM_DIR, exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    img.save(ASSET)
    print("\n".join("".join(px[y]) for y in range(H)))
    print()
    print("wrote %s (%dx%d)" % (ASSET, img.width, img.height))

    preview(img, 8, (46, 46, 52)).save(os.path.join(OUT, "chaos_talisman_x8.png"))
    paper = Image.open(PAPER).convert("RGBA")
    side_by_side(paper, img, 6).save(os.path.join(OUT, "chaos_talisman_next_to_paper.png"))
    print("wrote previews into tools/out/")


if __name__ == "__main__":
    main()
