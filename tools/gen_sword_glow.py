# ---------------------------------------------------------------------------
# NO LONGER USED (kept for the record).
# The sword-light trail particle was removed at the user's request - the flying
# sword is a real entity now and its trail is gone. Do NOT run this script: it
# would write textures/particle/sword_glow.png back into the asset tree and that
# file is deliberately not shipped any more (see the jar check).
# ---------------------------------------------------------------------------

# -*- coding: utf-8 -*-
"""
Draw the 剑光 (sword-light) particle texture: a thin long light band that is bright
on the left and fades to fully transparent on the right - the afterimage of a
sword flying past.

Palette: white-cyan at the head, cooling to pale blue along the tail.

The game draws a particle as a square quad that always faces the camera, so the
"band" lives in the middle rows of a 16x16 sheet and the rest is transparent; the
square is what makes it read as a streak rather than a dot.

Outputs:
  src/main/resources/assets/fulu/textures/particle/sword_glow.png
  tools/out/sword_glow_x8.png

Usage: python tools/gen_sword_glow.py
"""

import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                       "textures", "particle", "sword_glow.png")
OUT_DIR = os.path.join(HERE, "out")

SIZE = 16
# The band: 4 rows tall in the middle, tapering at both ends.
BAND_ROWS = 4

# head (x=0) white-cyan -> tail (x=15) pale blue
HEAD = (226, 252, 255)
TAIL = (74, 142, 236)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    top = (SIZE - BAND_ROWS) // 2          # 6

    for x in range(SIZE):
        t = x / (SIZE - 1)
        colour = lerp(HEAD, TAIL, t)
        # fade hard to the right: fully opaque at the head, gone by the tail
        alpha = int(255 * (1.0 - t) ** 1.6)
        if alpha <= 3:
            continue
        for y in range(SIZE):
            # distance from the band's centre line, 0 at the middle row(s)
            d = abs((y + 0.5) - (top + BAND_ROWS / 2.0))
            if d > BAND_ROWS / 2.0:
                continue
            # soften the top and bottom edge of the band so it is not a hard bar
            edge = 1.0 if d <= 1.0 else 0.55
            a = int(alpha * edge)
            if a > 0:
                img.putpixel((x, y), (colour[0], colour[1], colour[2], a))

    os.makedirs(os.path.dirname(OUT_TEX), exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)
    img.save(OUT_TEX)
    print("wrote %s (%dx%d)" % (OUT_TEX, img.width, img.height))

    scale = 8
    sheet = Image.new("RGB", (SIZE * scale, SIZE * scale), (32, 34, 44))
    big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
    sheet.paste(big, (0, 0), big)
    sheet.save(os.path.join(OUT_DIR, "sword_glow_x8.png"))
    print("wrote tools/out/sword_glow_x8.png")


if __name__ == "__main__":
    main()
