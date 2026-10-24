# -*- coding: utf-8 -*-
"""
Render a 16x16 block texture as an isometric cube, so you can judge what the
block will look like *in the world* instead of as a flat square.

Projection: pixel-art 2:1 isometric. One texture pixel maps to
    +x -> ( 2k,  k)      +y -> (-2k,  k)      +z (up) -> (0, -2.4k)
so a whole 16-pixel block is 192 px wide and about 211 px tall at k = 3.
All three visible faces are fed by the same texture (cube_all), with the side
faces darkened the way Minecraft's own lighting darkens them: top 100%, one
side 80%, the other 60%.

Outputs: tools/out/formation_eye_cube.png  (texture at x8 next to the cube)

Usage: python tools/make_block_preview.py [texture.png] [out.png]
"""

import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DEFAULT_TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                           "textures", "block", "formation_eye.png")
OUT = os.path.join(HERE, "out", "formation_eye_cube.png")

T = 16                      # texture size in pixels
K = 3                       # output pixels per texture pixel
FLAT_SCALE = 8              # zoom for the flat texture panel


def face(texture, origin, u_vec, v_vec, out_size, shade):
    """
    Paste `texture` onto the parallelogram origin + u*U + v*V.

    PIL's AFFINE transform maps output (x, y) back to input (u, v):
        u = a*x + b*y + c        v = d*x + e*y + f
    so we invert the parallelogram's own matrix.
    """
    u_vec = np.array(u_vec, dtype=float)
    v_vec = np.array(v_vec, dtype=float)
    inv = np.linalg.inv(np.array([u_vec, v_vec]).T)   # [[a, b], [d, e]]
    a, b = inv[0]
    d, e = inv[1]
    c = -(a * origin[0] + b * origin[1])
    f = -(d * origin[0] + e * origin[1])

    src = texture
    if shade < 1.0:
        arr = np.asarray(texture).astype(float)
        arr[..., :3] *= shade
        src = Image.fromarray(arr.astype("uint8"), "RGBA")
    warped = src.transform(out_size, Image.AFFINE, (a, b, c, d, e, f),
                           resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))

    mask = Image.new("L", out_size, 0)
    p = np.array(origin, dtype=float)
    corners = [p, p + u_vec * T, p + u_vec * T + v_vec * T, p + v_vec * T]
    ImageDraw.Draw(mask).polygon([tuple(pt) for pt in corners], fill=255)
    return warped, mask


def cube(texture):
    ex = np.array([2 * K, K], dtype=float)
    ey = np.array([-2 * K, K], dtype=float)
    ez = np.array([0, -2.4 * K], dtype=float)

    pad = 18
    width = int(T * (abs(ex[0]) + abs(ey[0]))) + pad * 2
    height = int(T * (ex[1] + ey[1]) + T * abs(ez[1])) + pad * 2
    out_size = (width, height)

    tb = np.array([pad + T * abs(ex[0]), pad], dtype=float)   # top/back vertex
    tl = tb + ey * T
    tr = tb + ex * T
    down = -ez

    parts = (
        (tuple(tb), ex, ey, 1.00),      # top face
        (tuple(tr), ey, down, 0.80),    # x = 1 face (top edge tr -> tf)
        (tuple(tl), ex, down, 0.60),    # y = 1 face (top edge tl -> tf)
    )
    canvas = Image.new("RGBA", out_size, (0, 0, 0, 0))
    for origin, u_vec, v_vec, shade in parts:
        img, mask = face(texture, origin, u_vec, v_vec, out_size, shade)
        canvas.paste(img, (0, 0), mask)
    return canvas


def main():
    tex_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_TEX
    out_path = sys.argv[2] if len(sys.argv) > 2 else OUT
    tex = Image.open(tex_path).convert("RGBA")
    if tex.size != (T, T):
        raise SystemExit("expected a %dx%d texture, got %s" % (T, T, tex.size))

    flat = tex.resize((T * FLAT_SCALE, T * FLAT_SCALE), Image.NEAREST)
    iso = cube(tex)

    gap = 24
    sheet = Image.new("RGB", (flat.width + iso.width + gap * 3,
                              max(flat.height, iso.height) + gap * 2 + 14),
                      (34, 34, 40))
    sheet.paste(flat, (gap, gap + (iso.height - flat.height) // 2))
    sheet.paste(iso, (gap * 2 + flat.width, gap), iso)
    d = ImageDraw.Draw(sheet)
    d.text((gap, sheet.height - 16), "texture 16x16 (x%d)" % FLAT_SCALE,
           fill=(200, 200, 200))
    d.text((gap * 2 + flat.width, sheet.height - 16),
           "in world: top lit / left 80%% / right 60%%", fill=(200, 200, 200))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    sheet.save(out_path)
    print("wrote %s (%dx%d)" % (out_path, sheet.width, sheet.height))


if __name__ == "__main__":
    main()
