# -*- coding: utf-8 -*-
"""
Build the flying sword's artwork.

Three outputs, all from one palette so they look like the same weapon:

  textures/entity/flying_sword.png     the ENTITY texture (UV regions for the model)
  textures/item/flying_sword_talisman.png  the item: YOUR uploaded parchment as the
                                       paper (black keyed out, watermark removed) with
                                       the sword drawn in the middle
  textures/block/formation_eye_sword.png   the same item art pasted into the eye, so
                                       the block keeps showing what is fitted

The shape follows the vanilla diamond sword (long flat blade, cross guard, wrapped
handle) with the mod's own palette: a jade blade, a brass guard, a dark grip -
which also matches the cyan of the sword-light particle.

Usage: python tools/gen_flying_sword_assets.py
"""

import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")
OUT_DIR = os.path.join(HERE, "out")
SOURCE = os.path.join(ROOT, "ref", "talisman_source.png")

# ---- palette (jade blade / brass guard / dark grip) -----------------------
BLADE_LIGHT = (188, 240, 236, 255)
BLADE = (108, 208, 200, 255)
BLADE_DARK = (58, 150, 148, 255)
EDGE = (26, 54, 58, 255)
GUARD = (222, 178, 78, 255)
GUARD_DARK = (150, 110, 38, 255)
GRIP = (86, 56, 40, 255)
GRIP_DARK = (58, 36, 26, 255)


# ---- 1. the entity texture -------------------------------------------------
# UV regions must match the texOffs in FlyingSwordModel:
#   blade  texOffs (0, 0)  size (2, 12, 1)  -> region 6 x 13
#   guard  texOffs (8, 0)  size (7, 1, 2)   -> region 18 x 3
#   grip   texOffs (0, 16) size (1, 4, 1)   -> region 4 x 5
ENTITY_SIZE = 32


def box_region(tex_off, size):
    """The rectangle a vanilla box unwrap occupies: (2*d + 2*w) by (d + h)."""
    u, v = tex_off
    w, h, d = size
    return u, v, u + 2 * d + 2 * w, v + d + h


def build_entity_texture():
    img = Image.new("RGBA", (ENTITY_SIZE, ENTITY_SIZE), (0, 0, 0, 0))
    from PIL import ImageDraw
    d = ImageDraw.Draw(img)

    def fill(tex_off, size, colour):
        d.rectangle(box_region(tex_off, size), fill=colour)

    # The unwrap is a strip of faces; painting the whole strip in one colour is
    # enough here (a sword is a flat object, nobody will see the seams).
    fill((0, 0), (2, 12, 1), BLADE)
    # a lighter band down the middle of the blade for a bit of shape
    d.rectangle((0, 1, 1, 12), fill=BLADE_LIGHT)
    d.rectangle((5, 0, 5, 12), fill=BLADE_DARK)
    d.rectangle((0, 12, 5, 12), fill=EDGE)

    fill((8, 0), (7, 1, 2), GUARD)
    d.rectangle((8, 2, 25, 2), fill=GUARD_DARK)

    fill((0, 16), (1, 4, 1), GRIP)
    d.rectangle((3, 16, 3, 20), fill=GRIP_DARK)
    return img


# ---- 2. the 飞剑符 item: your parchment + the sword ------------------------
ITEM = 16
PAPER_W, PAPER_H = 10, 14


def key_out_black(src):
    """
    Your upload is a parchment on a black background, with an AI watermark in the
    bottom-right corner. Two passes:
      1. anything dark becomes transparent (that is the black background);
      2. in the bottom-right corner, anything that is bright and NOT yellowish is
         the watermark - drop it too, or it would float in mid-air once the black
         is gone.
    """
    img = src.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if r < 46 and g < 46 and b < 46:
                px[x, y] = (0, 0, 0, 0)
                continue
            # the watermark lives in the bottom-right ~18% and is grey/white
            if x > w * 0.80 and y > h * 0.94:
                mx, mn = max(r, g, b), min(r, g, b)
                if mn > 130 and (mx - mn) < 40:
                    px[x, y] = (0, 0, 0, 0)
    return img


def build_item_texture():
    src = Image.open(SOURCE)
    keyed = key_out_black(src)
    box = keyed.getbbox()
    if not box:
        raise SystemExit("nothing left after keying the background out")
    paper = keyed.crop(box).resize((PAPER_W, PAPER_H), Image.LANCZOS)

    canvas = Image.new("RGBA", (ITEM, ITEM), (0, 0, 0, 0))
    px = (ITEM - PAPER_W) // 2
    py = (ITEM - PAPER_H) // 2
    canvas.paste(paper, (px, py), paper)

    # the sword, drawn over the middle of the paper
    sword = sword_sprite()
    canvas.paste(sword, ((ITEM - sword.width) // 2, (ITEM - sword.height) // 2 + 1), sword)
    return canvas, keyed


SWORD = [
    "..K..",
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
SWORD_PALETTE = {
    "K": EDGE,
    "w": BLADE_LIGHT,
    "g": GUARD,
    "j": BLADE,
    "J": BLADE_DARK,
}


def sword_sprite():
    w = len(SWORD[0])
    h = len(SWORD)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y, row in enumerate(SWORD):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), SWORD_PALETTE[ch])
    return img


# ---- 3. the eye's third look ----------------------------------------------
def build_block_variant(item_art):
    eye = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    paste_w, paste_h = 6, 12
    small = item_art.crop(item_art.getbbox()).resize((paste_w, paste_h), Image.NEAREST)
    out = eye.copy()
    x = (ITEM - paste_w) // 2
    y = (ITEM - paste_h) // 2
    out.paste(small, (x, y), small)
    changed = sum(1 for yy in range(ITEM) for xx in range(ITEM)
                  if eye.getpixel((xx, yy)) != out.getpixel((xx, yy)))
    print("eye pixels changed: %d (expected <= %d)" % (changed, paste_w * paste_h))
    return out


def preview(images, labels, path, scale=8):
    from PIL import ImageDraw
    gap = 14
    panels = []
    for img in images:
        panels.append(img.resize((img.width * scale, img.height * scale), Image.NEAREST))
    w = sum(p.width for p in panels) + gap * (len(panels) + 1)
    h = max(p.height for p in panels) + gap * 2 + 12
    sheet = Image.new("RGB", (w, h), (34, 34, 40))
    d = ImageDraw.Draw(sheet)
    x = gap
    for p, label in zip(panels, labels):
        sheet.paste(p, (x, gap), p)
        d.text((x, h - gap - 6), label, fill=(210, 210, 210))
        x += p.width + gap
    sheet.save(path)
    print("wrote %s" % os.path.relpath(path, ROOT))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "textures", "entity"), exist_ok=True)

    entity = build_entity_texture()
    entity.save(os.path.join(ASSETS, "textures", "entity", "flying_sword.png"))
    print("wrote textures/entity/flying_sword.png (%dx%d)" % entity.size)

    item, keyed = build_item_texture()
    item.save(os.path.join(ASSETS, "textures", "item", "flying_sword_talisman.png"))
    print("wrote textures/item/flying_sword_talisman.png (%dx%d)" % item.size)

    block = build_block_variant(item)
    block.save(os.path.join(ASSETS, "textures", "block", "formation_eye_sword.png"))
    print("wrote textures/block/formation_eye_sword.png (%dx%d)" % block.size)

    # a small preview of what came off the upload
    keyed_thumb = keyed.copy()
    keyed_thumb.thumbnail((160, 240))
    keyed_thumb.save(os.path.join(OUT_DIR, "talisman_keyed_thumb.png"))
    preview([entity, item, block], ["entity texture", "item (your paper)", "eye: sword"],
            os.path.join(OUT_DIR, "flying_sword_assets.png"), scale=8)


if __name__ == "__main__":
    main()
