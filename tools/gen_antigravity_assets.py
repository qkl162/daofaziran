# -*- coding: utf-8 -*-
"""
Art for the anti-gravity formation: the 反重力符 item, the eye's fourth look, and
the anti-gravity particle.

Palette: pale violet into pale cyan - reads as "levitation" next to the eye's
brass and the jade sword, and it is the one colour nothing else in the mod uses.

Outputs:
  textures/item/anti_gravity_talisman.png
  textures/block/formation_eye_antigravity.png
  textures/particle/anti_gravity.png
  tools/out/antigravity_assets.png        preview of all three

Usage: python tools/gen_antigravity_assets.py
"""

import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu")
OUT_DIR = os.path.join(HERE, "out")
PARCHMENT = os.path.join(ROOT, "ref", "talisman_source.png")

ITEM = 16
PAPER_W, PAPER_H = 10, 14

RUNE_LIGHT = (240, 226, 255, 255)   # pale violet, almost white
RUNE = (188, 150, 240, 255)
RUNE_DARK = (126, 92, 190, 255)


def key_out_black(src):
    """Same treatment as the flying-sword talisman: drop the black background."""
    img = src.convert("RGBA")
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if r < 46 and g < 46 and b < 46:
                px[x, y] = (0, 0, 0, 0)
                continue
            if x > w * 0.80 and y > h * 0.94:
                mx, mn = max(r, g, b), min(r, g, b)
                if mn > 130 and (mx - mn) < 40:
                    px[x, y] = (0, 0, 0, 0)
    return img


# An upward arrow over a small base line: "this thing lifts you".
ARROW = [
    "...##...",
    "..####..",
    ".######.",
    "##.##.##",
    "...##...",
    "...##...",
    "...##...",
    "..####..",
]


def draw_rune(size, scale=1):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    step = size // 8 if size >= 8 else 1
    for y, row in enumerate(ARROW):
        for x, ch in enumerate(row):
            if ch == "#":
                colour = RUNE_LIGHT if y <= 2 else RUNE
                d.rectangle((x * step, y * step, x * step + step - 1, y * step + step - 1),
                            fill=colour)
    return img


def build_item_texture():
    src = key_out_black(Image.open(PARCHMENT))
    box = src.getbbox()
    if not box:
        raise SystemExit("nothing left after keying the background out")
    paper = src.crop(box).resize((PAPER_W, PAPER_H), Image.LANCZOS)

    canvas = Image.new("RGBA", (ITEM, ITEM), (0, 0, 0, 0))
    px = (ITEM - PAPER_W) // 2
    py = (ITEM - PAPER_H) // 2
    canvas.paste(paper, (px, py), paper)

    # the arrow sits on the paper, a touch above centre so the "up" reads clearly
    rune = draw_rune(10, 1)
    canvas.paste(rune, ((ITEM - rune.width) // 2, (ITEM - rune.height) // 2 - 1), rune)
    return canvas


def build_block_variant(item_art):
    eye = Image.open(os.path.join(ASSETS, "textures", "block", "formation_eye.png")).convert("RGBA")
    paste_w, paste_h = 6, 12
    small = item_art.crop(item_art.getbbox()).resize((paste_w, paste_h), Image.NEAREST)
    out = eye.copy()
    out.paste(small, ((ITEM - paste_w) // 2, (ITEM - paste_h) // 2), small)
    return out


def build_particle():
    """A small upward wisp: bright at the top, fading down into the violet."""
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    rows = [
        (0, "...##...", RUNE_LIGHT),
        (1, "..####..", RUNE_LIGHT),
        (2, ".#.##.#.", RUNE),
        (3, "...##...", RUNE),
        (4, "...##...", RUNE),
        (5, "..####..", RUNE_DARK),
        (6, ".#....#.", RUNE_DARK),
        (7, "........", RUNE_DARK),
    ]
    for y, pattern, colour in rows:
        for x, ch in enumerate(pattern):
            if ch == "#":
                img.putpixel((x, y), colour)
    return img


def preview(images, labels, path, scale=8):
    panels = [im.resize((im.width * scale, im.height * scale), Image.NEAREST) for im in images]
    gap = 14
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

    item = build_item_texture()
    item.save(os.path.join(ASSETS, "textures", "item", "anti_gravity_talisman.png"))
    print("wrote textures/item/anti_gravity_talisman.png (%dx%d)" % item.size)

    block = build_block_variant(item)
    block.save(os.path.join(ASSETS, "textures", "block", "formation_eye_antigravity.png"))
    print("wrote textures/block/formation_eye_antigravity.png (%dx%d)" % block.size)

    particle = build_particle()
    particle.save(os.path.join(ASSETS, "textures", "particle", "anti_gravity.png"))
    print("wrote textures/particle/anti_gravity.png (%dx%d)" % particle.size)

    preview([item, block, particle],
            ["item (anti-gravity)", "eye: anti-gravity", "particle"],
            os.path.join(OUT_DIR, "antigravity_assets.png"), scale=8)


if __name__ == "__main__":
    main()
