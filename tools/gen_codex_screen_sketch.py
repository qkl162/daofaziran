# -*- coding: utf-8 -*-
"""
Draw a TEXT-ACCURATE mock-up of the codex GUI in its three states.

THIS IS NOT A SCREENSHOT. Nothing here renders Minecraft. But the *strings* are
not invented either: the detail lines are composed from
    src/main/resources/assets/fulu/lang/zh_cn.json
using the same keys and the same order as FormationCodex.detailLines(), and the
list lines use the same rule (LOCKED shows ???, otherwise the name). So this is a
faithful picture of WHAT the screen says, and only a guess at how it looks.

Usage: python tools/gen_codex_screen_sketch.py
"""

import json
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu", "lang", "zh_cn.json")
OUT = os.path.join(HERE, "out", "codex_screen_states.png")

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

# (label, value) key pairs, in the same order FormationCodex.detailLines() uses.
# The label is the generic "名称：%s"-style wording; the value is this formation's.
SEEN_PAIRS = [("name", "name"), ("range", "range_short"), ("effect", "effect_short")]
MASTERED_PAIRS = [("name", "name"), ("range", "range"), ("effect", "effect"),
                  ("material", "material"), ("activate", "activate"),
                  ("deactivate", "deactivate")]


def load_font(size):
    """PIL's built-in font has no CJK glyphs, so borrow one from Windows."""
    for name in ("msyh.ttc", "simhei.ttf", "simsun.ttc", "msyhl.ttc"):
        path = os.path.join(os.environ.get("WINDIR", r"C:\Windows"), "Fonts", name)
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def build_lines(lang):
    """The exact strings the three states put on screen."""
    def compose(pairs):
        return [lang["codex.fulu.detail." + label] % lang["codex.fulu.infighting." + value]
                for label, value in pairs]

    return {
        "未解锁": [lang["codex.fulu.screen.not_unlocked"]],
        "已见闻": compose(SEEN_PAIRS),
        "已精通": compose(MASTERED_PAIRS),
    }


def draw_panel(state, lines, lang, font, small):
    img = Image.new("RGB", (PANEL_W * SCALE, PANEL_H * SCALE), BG)
    d = ImageDraw.Draw(img)
    s = SCALE

    def rect(x0, y0, x1, y1, colour):
        d.rectangle([x0 * s, y0 * s, x1 * s, y1 * s], fill=colour)

    rect(0, 0, PANEL_W, PANEL_H, PANEL)
    rect(0, 0, PANEL_W, 2, ACCENT)
    d.text((8 * s, 7 * s), lang["codex.fulu.title"], fill=GOLD, font=font)

    # left list
    rect(4, 22, LIST_W, PANEL_H - 32, (0, 0, 0))
    row_y = 24
    if state == "未解锁":
        d.text((8 * s, (row_y + 6) * s), "?", fill=DIM, font=font)
        d.text((28 * s, (row_y + 6) * s), lang["codex.fulu.locked"], fill=DIM, font=font)
    else:
        # the item icon, veiled for 已见闻 and clear for 已精通
        rect(8, row_y + 2, 24, row_y + 18, (120, 60, 30))
        if state == "已见闻":
            rect(8, row_y + 2, 24, row_y + 18, (40, 40, 40))   # half bright
        d.text((28 * s, (row_y + 6) * s), lang["codex.fulu.infighting.name"],
               fill=TEXT, font=font)
    rect(6, row_y, LIST_W - 2, row_y + ROW_H, SEL)

    # right panel
    x, y = LIST_W + 8, 28
    for line in lines:
        # 17 chars: the right column is PANEL_W - LIST_W - 16 = 174px and a CJK
        # glyph here is about 9px wide.
        for chunk in wrap(line, 17):
            d.text((x * s, y * s), chunk, fill=(210, 210, 210), font=small)
            y += 10
        y += 2

    # bottom: search + close
    rect(8, PANEL_H - 24, PANEL_W - 72, PANEL_H - 8, (0, 0, 0))
    d.text((10 * s, (PANEL_H - 21) * s), lang["codex.fulu.screen.search"],
           fill=DIM, font=small)
    rect(PANEL_W - 64, PANEL_H - 24, PANEL_W - 8, PANEL_H - 8, (70, 70, 78))
    d.text(((PANEL_W - 44) * s, (PANEL_H - 21) * s), lang["codex.fulu.screen.close"],
           fill=TEXT, font=small)

    # state label next to the title (top), honesty banner above the search box
    d.text((150 * s, 7 * s), "状态：" + state, fill=(200, 120, 220), font=font)
    d.text((8 * s, (PANEL_H - 40) * s), "文字取自语言文件；画面是示意图",
           fill=(200, 80, 80), font=small)
    return img


def wrap(text, width):
    """Crude wrap by character count - the real screen wraps by pixel width."""
    out = []
    while len(text) > width:
        out.append(text[:width])
        text = text[width:]
    out.append(text)
    return out


def main():
    with open(LANG, encoding="utf-8") as fh:
        lang = json.load(fh)
    states = build_lines(lang)

    font = load_font(11 * SCALE)
    small = load_font(9 * SCALE)
    panels = [draw_panel(state, lines, lang, font, small) for state, lines in states.items()]

    gap = 12 * SCALE
    width = sum(p.width for p in panels) + gap * (len(panels) - 1)
    height = max(p.height for p in panels)
    sheet = Image.new("RGB", (width, height), BG)
    x = 0
    for p in panels:
        sheet.paste(p, (x, 0))
        x += p.width + gap

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    print("wrote %s (%dx%d)" % (os.path.relpath(OUT, ROOT), sheet.width, sheet.height))
    for state, lines in states.items():
        print("  [%s]" % state)
        for line in lines:
            print("      " + line)


if __name__ == "__main__":
    main()
