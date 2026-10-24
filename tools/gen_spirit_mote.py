# -*- coding: utf-8 -*-
"""
Draw the particle texture for 灵气 (spirit_mote) and preview it.

Assets:
  src/main/resources/assets/fulu/textures/particle/spirit_mote.png   8x8
  tools/out/spirit_mote_preview.png                                  texture + block mock

Why 8x8 and white-ish:
  * Minecraft particle sheets are 8x8 per frame. One single-frame file means
    the SpriteSet holds exactly one sprite, so pickSprite() has nothing to
    shuffle - no accidental flicker.
  * The particle is tinted in code (SpiritMoteParticle.setColor: gold), so the
    texture itself stays near-white; changing the colour later means editing one
    constant, not repainting the texture.

Usage: python tools/gen_spirit_mote.py
"""

import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSET = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                     "textures", "particle", "spirit_mote.png")
OUT = os.path.join(HERE, "out")

SIZE = 8
CENTER = (SIZE - 1) / 2.0
RADIUS = 3.7


def make_texture():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y in range(SIZE):
        for x in range(SIZE):
            r = math.hypot(x - CENTER, y - CENTER)
            t = 1.0 - (r / RADIUS)
            if t <= 0:
                continue
            alpha = int(255 * (t ** 1.7))
            # warm white; the gold tint is applied by the particle itself
            warm = 1.0 - 0.10 * (r / RADIUS)
            img.putpixel((x, y), (255, int(255 * warm), int(238 * warm), max(0, min(255, alpha))))
    return img


def preview(texture):
    """Left: the raw 8x8 at x16.  Right: the block with a burst of motes."""
    sys.path.insert(0, HERE)
    from make_block_preview import cube          # reuse the isometric renderer

    block_tex = Image.open(os.path.join(ROOT, "src", "main", "resources", "assets",
                                        "fulu", "textures", "block", "formation_eye.png")).convert("RGBA")
    iso = cube(block_tex)

    # the particle tints the (near-white) texture gold in code - do the same here,
    # otherwise the mock shows a colour the game never draws
    tinted = texture.copy()
    px = tinted.load()
    for y in range(tinted.height):
        for x in range(tinted.width):
            r, g, b, a = px[x, y]
            px[x, y] = (int(r * 0.98), int(g * 0.84), int(b * 0.46), a)

    rng = random.Random(20261014)
    scene = iso.copy()
    top_y = int(iso.height * 0.30)               # roughly the top face of the cube
    for i in range(9):
        size = rng.randint(10, 22)
        mote = tinted.resize((size, size), Image.LANCZOS)
        px_ = rng.randint(int(iso.width * 0.28), int(iso.width * 0.72)) - size // 2
        py_ = top_y - rng.randint(0, int(iso.height * 0.28)) - size // 2
        scene.alpha_composite(mote, (max(0, px_), max(0, py_)))

    flat = texture.resize((SIZE * 16, SIZE * 16), Image.NEAREST)
    gap = 22
    sheet = Image.new("RGB", (flat.width + scene.width + gap * 3,
                              max(flat.height, scene.height) + gap * 2), (34, 34, 40))
    sheet.paste(flat, (gap, gap + (scene.height - flat.height) // 2), flat)
    sheet.paste(scene, (gap * 2 + flat.width, gap), scene)
    return sheet


def main():
    os.makedirs(os.path.dirname(ASSET), exist_ok=True)
    os.makedirs(OUT, exist_ok=True)
    tex = make_texture()
    tex.save(ASSET)
    print("wrote %s (%dx%d)" % (ASSET, tex.width, tex.height))
    print("alpha map:")
    for y in range(SIZE):
        print("   " + " ".join("%3d" % tex.getpixel((x, y))[3] for x in range(SIZE)))
    sheet = preview(tex)
    sheet.save(os.path.join(OUT, "spirit_mote_preview.png"))
    print("wrote tools/out/spirit_mote_preview.png (%dx%d)" % (sheet.width, sheet.height))


if __name__ == "__main__":
    main()
