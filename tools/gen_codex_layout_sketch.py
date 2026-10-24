# -*- coding: utf-8 -*-
"""
Draw a LAYOUT SKETCH of the codex GUI.

THIS IS NOT A SCREENSHOT. It is a mock-up of what CodexScreen draws, drawn from
the same numbers (panel size, row height, column width) so the layout can be
reviewed without launching the game. The real thing is rendered by Minecraft and
may differ in fonts, colours and spacing.

Usage: python tools/gen_codex_layout_sketch.py
"""

import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out", "codex_layout_sketch.png")

SCALE = 2
PANEL_W, PANEL_H = 320, 200
LIST_W = 130
ROW_H = 20

BG = (16, 16, 16)
PANEL = (24, 22, 28)
ACCENT = (139, 47, 201)
GOLD = (217, 160, 58)
TEXT = (237, 237, 237)
DIM = (128, 128, 128)
SEL = (60, 60, 60)


def main():
    img = Image.new("RGB", (PANEL_W * SCALE, PANEL_H * SCALE), BG)
    d = ImageDraw.Draw(img)
    s = SCALE

    def rect(x0, y0, x1, y1, colour):
        d.rectangle([x0 * s, y0 * s, x1 * s, y1 * s], fill=colour)

    rect(0, 0, PANEL_W, PANEL_H, PANEL)
    rect(0, 0, PANEL_W, 2, ACCENT)
    d.text((8 * s, 8 * s), "Formation Codex", fill=GOLD)

    # left list
    rect(4, 22, LIST_W, PANEL_H - 32, (0, 0, 0))
    rows = [("?", "???", DIM, 0), ("I", "Infighting", TEXT, 1), ("I", "Infighting", TEXT, 2)]
    for i, (icon, name, colour, bright) in enumerate(rows):
        y = 24 + i * ROW_H
        if i == 1:
            rect(6, y, LIST_W - 2, y + ROW_H, SEL)
        d.text((8 * s, (y + 6) * s), icon, fill=DIM if bright == 0 else colour)
        d.text((28 * s, (y + 6) * s), name, fill=colour)
        if bright == 1:                       # half-bright: a dark veil
            rect(8, y + 2, 24, y + 18, (40, 40, 40))

    # right detail
    d.text(((LIST_W + 8) * s, 28 * s), "Not unlocked yet", fill=DIM)

    # bottom: search + close
    rect(8, PANEL_H - 24, PANEL_W - 72, PANEL_H - 8, (0, 0, 0))
    d.text((10 * s, (PANEL_H - 20) * s), "Search formations...", fill=DIM)
    rect(PANEL_W - 64, PANEL_H - 24, PANEL_W - 8, PANEL_H - 8, (70, 70, 78))
    d.text(((PANEL_W - 42) * s, (PANEL_H - 20) * s), "Close", fill=TEXT)

    # labels last, so they sit on top
    d.text((8 * s, (PANEL_H - 44) * s), "LAYOUT SKETCH - not a screenshot", fill=(200, 80, 80))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
