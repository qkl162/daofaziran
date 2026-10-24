# -*- coding: utf-8 -*-
"""
Compose the "here is what it looks like" sheet for 符纸:

  [ web reference photo ]   [ 16x16 in a real inventory slot ]   [ 8x zoom ]

Usage: python tools/make_preview.py [reference image]
"""

import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSET = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                     "textures", "item", "talisman_paper.png")
OUT = os.path.join(HERE, "out", "talisman_paper_preview.png")
BG = (34, 34, 40)
LABEL = (196, 196, 200)
SCALE = 4          # how much the slot grid is zoomed up for viewing


def slot_grid(item, cols=3, rows=3, filled=(1, 1)):
    """A vanilla-ish 18px slot grid; `filled` is the slot that holds the item."""
    s = 18
    grid = Image.new("RGB", (cols * s, rows * s), (198, 198, 198))
    d = ImageDraw.Draw(grid)
    for cy in range(rows):
        for cx in range(cols):
            x0, y0 = cx * s, cy * s
            d.rectangle([x0, y0, x0 + s - 1, y0 + s - 1], fill=(139, 139, 139))
            d.line([x0, y0, x0 + s - 1, y0], fill=(55, 55, 55))
            d.line([x0, y0, x0, y0 + s - 1], fill=(55, 55, 55))
    art = item.copy()
    grid.paste(art, (filled[0] * s + 1, filled[1] * s + 1), art)
    grid = grid.resize((grid.width * SCALE, grid.height * SCALE), Image.NEAREST)
    d = ImageDraw.Draw(grid)
    for cy in range(rows):
        for cx in range(cols):
            x0, y0 = cx * s * SCALE, cy * s * SCALE
            d.rectangle([x0, y0, x0 + s * SCALE - 1, y0 + s * SCALE - 1],
                        outline=(120, 120, 120))
    return grid


def label(img, text):
    band = Image.new("RGB", (img.width, img.height + 22), BG)
    band.paste(img, (0, 0))
    d = ImageDraw.Draw(band)
    d.text((4, img.height + 5), text, fill=LABEL)
    return band


def main():
    ref_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "ref", "bd_06.jpg")
    item = Image.open(ASSET).convert("RGBA")

    panels = []

    if os.path.exists(ref_path):
        ref = Image.open(ref_path).convert("RGB")
        ref.thumbnail((190, 190), Image.LANCZOS)
        canvas = Image.new("RGB", (190, 190), BG)
        canvas.paste(ref, ((190 - ref.width) // 2, (190 - ref.height) // 2))
        panels.append(label(canvas, "web reference photo"))

    zoom = item.resize((item.width * 8, item.height * 8), Image.NEAREST)
    canvas = Image.new("RGB", (190, 190), BG)
    canvas.paste(zoom, ((190 - zoom.width) // 2, (190 - zoom.height) // 2), zoom)
    panels.append(label(canvas, "asset 16x16  (x8 zoom)"))

    grid = slot_grid(item)
    canvas = Image.new("RGB", (190, 190), BG)
    canvas.paste(grid, ((190 - grid.width) // 2, (190 - grid.height) // 2))
    panels.append(label(canvas, "in an inventory slot"))

    gap = 14
    sheet = Image.new("RGB", (sum(p.width for p in panels) + gap * (len(panels) + 1),
                               190 + 22 + gap * 2), BG)
    x = gap
    for p in panels:
        sheet.paste(p, (x, gap))
        x += p.width + gap
    sheet.save(OUT)
    print("wrote %s  (%dx%d)" % (OUT, sheet.width, sheet.height))


if __name__ == "__main__":
    main()
