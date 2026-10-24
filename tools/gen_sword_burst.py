# -*- coding: utf-8 -*-
"""
Draw the 命中爆发 (hit burst) particle texture: a small spark - bright in the
middle, transparent at the edges, white shading into gold - like a little star.

8x8 on purpose: this one has to read as a *small* spark next to the long sword
light, and it is spawned ten-odd times per hit.

Outputs:
  src/main/resources/assets/fulu/textures/particle/sword_burst.png
  tools/out/sword_burst_x8.png

Usage: python tools/gen_sword_burst.py
"""

import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                       "textures", "particle", "sword_burst.png")
OUT_DIR = os.path.join(HERE, "out")

SIZE = 8
CENTRE = (255, 250, 232, 255)  # near-white, a touch warm - the middle of the spark
GOLD = (255, 186, 64, 255)     # and it shades into gold at the edges


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    c = (SIZE - 1) / 2.0

    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - c, y - c
            dist = (dx * dx + dy * dy) ** 0.5
            # a 4-point star: pull the distance in along the axes so the spark has rays
            axis = min(abs(dx), abs(dy))
            star_dist = dist - (0.9 - axis) * 0.55 if axis < 0.9 else dist

            if star_dist > 2.6:
                continue
            t = max(0.0, min(1.0, star_dist / 2.6))
            # white in the middle -> gold at the edge
            r = round(CENTRE[0] + (GOLD[0] - CENTRE[0]) * t)
            g = round(CENTRE[1] + (GOLD[1] - CENTRE[1]) * t)
            b = round(CENTRE[2] + (GOLD[2] - CENTRE[2]) * t)
            alpha = int(255 * (1.0 - t) ** 1.1)
            if alpha > 4:
                img.putpixel((x, y), (r, g, b, alpha))

    os.makedirs(os.path.dirname(OUT_TEX), exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)
    img.save(OUT_TEX)
    print("wrote %s (%dx%d)" % (OUT_TEX, img.width, img.height))

    # preview on a dark background, blown up and also at "in-world" size
    scale = 16
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    sheet = Image.new("RGB", (SIZE * scale * 2 + 24, SIZE * scale), (32, 34, 44))
    sheet.paste(big, (0, 0), big)
    small = img.resize((SIZE * 3, SIZE * 3), Image.NEAREST)
    sheet.paste(small, (SIZE * scale + 24, SIZE * scale - SIZE * 3 - 8), small)
    sheet.save(os.path.join(OUT_DIR, "sword_burst_x8.png"))
    print("wrote tools/out/sword_burst_x8.png")


if __name__ == "__main__":
    main()
