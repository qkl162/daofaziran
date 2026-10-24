# -*- coding: utf-8 -*-
"""
Draw the 飞剑符 (flying sword talisman) item texture, and the matching third look
for the formation eye.

Same recipe as the other talismans in this mod: take the existing 符纸 artwork and
draw on it, so the paper is literally the same paper. Here the drawing is a small
sword: a steel blade, a guard, a jade grip and a pommel, in the mod's palette.

    textures/item/flying_sword_talisman.png     the item
    textures/block/formation_eye_sword.png      the eye with that talisman in it
                                                (the item art, scaled into the
                                                 centre, exactly like the 乱神符
                                                 variant does)

Outputs are 16x16, RGBA - the jar check enforces that.

Usage: python tools/gen_flying_sword_talisman.py
"""

import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "textures", "item")
BLOCK_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "textures", "block")
OUT_DIR = os.path.join(HERE, "out")

PAPER = os.path.join(ITEM_DIR, "talisman_paper.png")
EYE = os.path.join(BLOCK_DIR, "formation_eye.png")
OUT_ITEM = os.path.join(ITEM_DIR, "flying_sword_talisman.png")
OUT_BLOCK = os.path.join(BLOCK_DIR, "formation_eye_sword.png")

# The sword: drawn in item-local coordinates, then blitted wherever it goes.
#   .  transparent   K  dark outline   w  blade light   W  blade shade
#   g  guard (brass) G  guard shade    j  jade grip     J  jade shade
SWORD = [
    "..K..",
    ".KwK.",
    ".KwK.",
    ".KwK.",
    ".KwK.",
    ".KwK.",
    ".KwK.",
    "KwwwK",
    "KgggK",
    ".KjK.",
    ".KjK.",
    ".KJK.",
    "..K..",
]

PALETTE = {
    "K": (38, 34, 46, 255),
    "w": (226, 230, 238, 255),
    "W": (150, 158, 172, 255),
    "g": (196, 152, 62, 255),
    "G": (140, 104, 34, 255),
    "j": (108, 208, 170, 255),
    "J": (56, 148, 118, 255),
}


def sword_sprite():
    """A tiny image of the sword on a transparent background."""
    w = len(SWORD[0])
    h = len(SWORD)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(SWORD):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), PALETTE[ch])
    return img


def draw_item():
    """The paper slip with the sword inked onto it."""
    paper = Image.open(PAPER).convert("RGBA")
    box = paper.getbbox()
    if box:
        paper = paper.crop(box)                    # the slip, margin removed
    canvas = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    # centre the slip horizontally, drop it a little so the blade has room
    px = (16 - paper.width) // 2
    py = (16 - paper.height) // 2 + 1
    canvas.paste(paper, (px, py), paper)

    sword = sword_sprite()
    sx = (16 - sword.width) // 2
    sy = py + 1
    canvas.paste(sword, (sx, sy), sword)
    return canvas


def draw_block_variant(item_art):
    """
    The eye with this talisman in it: everything outside the centre is byte-for-byte
    the plain eye - the same rule the 乱神符 variant follows.
    """
    base = Image.open(EYE).convert("RGBA")
    if base.size != (16, 16):
        raise SystemExit("expected a 16x16 eye texture, got %s" % (base.size,))

    paste_w, paste_h = 6, 12
    small = item_art.crop(item_art.getbbox()).resize((paste_w, paste_h), Image.NEAREST)
    new = base.copy()
    x = (16 - paste_w) // 2
    y = (16 - paste_h) // 2
    new.paste(small, (x, y), small)

    outside = sum(1 for yy in range(16) for xx in range(16)
                  if base.getpixel((xx, yy)) != new.getpixel((xx, yy))
                  and not (x <= xx < x + paste_w and y <= yy < y + paste_h))
    print("eye pixels changed outside the talisman area: %d  <- must be 0" % outside)
    return new


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    item_art = draw_item()
    item_art.save(OUT_ITEM)
    print("wrote %s (%dx%d)" % (OUT_ITEM, item_art.width, item_art.height))

    block_art = draw_block_variant(item_art)
    block_art.save(OUT_BLOCK)
    print("wrote %s (%dx%d)" % (OUT_BLOCK, block_art.width, block_art.height))

    # preview: the item, and the three eye looks side by side
    sys.path.insert(0, HERE)
    from make_block_preview import cube, FLAT_SCALE

    panels = []
    flat = item_art.resize((16 * FLAT_SCALE, 16 * FLAT_SCALE), Image.NEAREST)
    panels.append((flat, "item: flying sword talisman"))
    for name, path in (("empty", EYE),
                       ("chaos talisman", os.path.join(BLOCK_DIR, "formation_eye_talisman.png")),
                       ("flying sword", OUT_BLOCK)):
        tex = Image.open(path).convert("RGBA")
        panels.append((cube(tex), "eye: " + name))

    gap = 18
    width = sum(p.width for p, _ in panels) + gap * (len(panels) + 1)
    height = max(p.height for p, _ in panels) + gap * 2 + 14
    sheet = Image.new("RGB", (width, height), (34, 34, 40))
    x = gap
    from PIL import ImageDraw
    d = ImageDraw.Draw(sheet)
    for img, label in panels:
        sheet.paste(img, (x, gap + (height - gap * 2 - 14 - img.height) // 2), img)
        d.text((x, height - gap - 8), label, fill=(200, 200, 200))
        x += img.width + gap
    out = os.path.join(OUT_DIR, "flying_sword_three_eyes.png")
    sheet.save(out)
    print("wrote %s (%dx%d)" % (os.path.relpath(out, ROOT), sheet.width, sheet.height))


if __name__ == "__main__":
    main()
