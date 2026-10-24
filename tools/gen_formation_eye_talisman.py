# -*- coding: utf-8 -*-
"""
Draw the SECOND appearance of the formation eye: the same block, with a talisman
stuck in the middle.

Steps the task asked for, in code:
  1. copy the existing texture            -> open formation_eye.png
  2. draw a talisman pattern in the MIDDLE, everything else identical
  3. (a separate file, copied by hand)    -> models/block/formation_eye_talisman.json

How the talisman is drawn: the mod already has a talisman texture (the 符纸 item,
6x14 px). Rather than inventing a second rune style, that artwork is scaled down
with NEAREST and pasted into the centre - so the paper, the ink and the seal on
the block are literally the same ones as on the item. Everything outside the
paste rectangle is byte-for-byte the old texture.

Outputs:
  src/main/resources/assets/fulu/textures/block/formation_eye_talisman.png
  tools/out/formation_eye_two_looks.png   (old vs new, flat + in-world)

Usage: python tools/gen_formation_eye_talisman.py
"""

import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
BLOCK_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "textures", "block")
BASE = os.path.join(BLOCK_DIR, "formation_eye.png")
OUT_TEX = os.path.join(BLOCK_DIR, "formation_eye_talisman.png")
TALISMAN = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                        "textures", "item", "talisman_paper.png")
OUT_DIR = os.path.join(HERE, "out")

# where the talisman goes: 4 px wide, 10 px tall, centred on a 16x16 block
PASTE_W, PASTE_H = 4, 10
PASTE_X = (16 - PASTE_W) // 2       # = 6
PASTE_Y = (16 - PASTE_H) // 2       # = 3


def main():
    base = Image.open(BASE).convert("RGBA")
    if base.size != (16, 16):
        raise SystemExit("expected a 16x16 block texture, got %s" % (base.size,))

    talisman = Image.open(TALISMAN).convert("RGBA")
    # crop away the transparent margin so the paper fills the paste area
    box = talisman.getbbox()
    if box:
        talisman = talisman.crop(box)
    small = talisman.resize((PASTE_W, PASTE_H), Image.NEAREST)

    new = base.copy()
    new.paste(small, (PASTE_X, PASTE_Y), small)

    # how much of the old texture survived untouched?
    changed = 0
    for y in range(16):
        for x in range(16):
            if base.getpixel((x, y)) != new.getpixel((x, y)):
                changed += 1
    total = 16 * 16
    inside = PASTE_W * PASTE_H
    outside = total - inside

    new.save(OUT_TEX)
    print("wrote %s (%dx%d)" % (OUT_TEX, new.width, new.height))
    print("pixels inside the talisman area : %d (all may differ)" % inside)
    print("pixels outside it that differ   : %d  <- must be 0"
          % sum(1 for y in range(16) for x in range(16)
                if base.getpixel((x, y)) != new.getpixel((x, y))
                and not (PASTE_X <= x < PASTE_X + PASTE_W and PASTE_Y <= y < PASTE_Y + PASTE_H)))
    print("total changed pixels: %d / %d" % (changed, total))

    # preview: flat old, flat new, old cube, new cube
    sys.path.insert(0, HERE)
    from make_block_preview import cube, FLAT_SCALE

    panels = []
    for tex, label in ((base, "no talisman"), (new, "with talisman")):
        flat = tex.resize((16 * FLAT_SCALE, 16 * FLAT_SCALE), Image.NEAREST)
        panels.append((flat, "texture - " + label))
        panels.append((cube(tex), "in world - " + label))

    gap = 20
    w = sum(p.width for p, _ in panels) + gap * (len(panels) + 1)
    h = max(p.height for p, _ in panels) + gap * 2 + 16
    sheet = Image.new("RGB", (w, h), (34, 34, 40))
    x = gap
    d = ImageDraw.Draw(sheet)
    for img, label in panels:
        sheet.paste(img, (x, gap + (h - gap * 2 - 16 - img.height) // 2), img)
        d.text((x, h - gap - 8), label, fill=(200, 200, 200))
        x += img.width + gap
    os.makedirs(OUT_DIR, exist_ok=True)
    sheet.save(os.path.join(OUT_DIR, "formation_eye_two_looks.png"))
    print("wrote tools/out/formation_eye_two_looks.png (%dx%d)" % (sheet.width, sheet.height))


if __name__ == "__main__":
    main()
