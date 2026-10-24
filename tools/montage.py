# -*- coding: utf-8 -*-
"""Tile every image in tools/ref into one contact sheet for a quick look.

Usage:  python tools/montage.py [outfile] [cell]
"""

import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
REF = os.path.join(HERE, "ref")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out", "ref_sheet.png")
CELL = int(sys.argv[2]) if len(sys.argv) > 2 else 220


def main():
    files = sorted(f for f in os.listdir(REF)
                   if f.lower().endswith((".png", ".jpg", ".jpeg", ".webp")))
    if not files:
        print("no reference images in tools/ref")
        return
    cols = min(4, len(files))
    rows = (len(files) + cols - 1) // cols
    pad = 22
    sheet = Image.new("RGB", (cols * (CELL + pad) + pad, rows * (CELL + pad) + pad),
                      (24, 24, 28))
    draw = ImageDraw.Draw(sheet)
    for i, name in enumerate(files):
        try:
            img = Image.open(os.path.join(REF, name)).convert("RGB")
        except Exception as exc:
            print("skip %s (%s)" % (name, exc))
            continue
        img.thumbnail((CELL, CELL), Image.LANCZOS)
        cx = pad + (i % cols) * (CELL + pad)
        cy = pad + (i // cols) * (CELL + pad)
        sheet.paste(img, (cx + (CELL - img.width) // 2,
                          cy + (CELL - img.height) // 2))
        draw.text((cx + 2, cy + CELL + 4), name, fill=(210, 210, 210))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    print("wrote %s  (%d files)" % (OUT, len(files)))


if __name__ == "__main__":
    main()
