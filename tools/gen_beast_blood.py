# -*- coding: utf-8 -*-
"""
兽血 (beast_blood) 的 16x16 贴图。

按字符画来画：一张 16x16 的字符表 + 一个调色板，改起来一眼能看懂。
（脚本本身保持纯 ASCII：这台机器上 PowerShell 写文件时中文会被写坏，
之前吃过这个亏，所以工具脚本里不放中文。）
"""
import os

from PIL import Image, ImageDraw

# 字符 -> 颜色(RGBA)
PALETTE = {
    ".": (0, 0, 0, 0),            # 透明
    "c": (150, 116, 74, 255),     # 软木塞
    "C": (188, 152, 100, 255),    # 软木塞高光
    "n": (196, 206, 214, 255),    # 瓶颈玻璃
    "N": (232, 240, 245, 255),    # 玻璃高光
    "g": (120, 132, 142, 255),    # 玻璃暗边
    "b": (94, 12, 20, 255),       # 血，最深
    "r": (140, 20, 28, 255),      # 血
    "R": (176, 34, 40, 255),      # 血，亮
    "S": (214, 74, 74, 255),      # 血面高光
}

ART = [
    "................",
    ".....cccccc.....",
    "....cCCCCCCc....",
    "....cCccccCc....",
    ".....nnnnnn.....",
    "....ngNNNNgn....",
    "...ngbRRRRgn....",
    "..ngbRRSSRRgn...",
    "..ngbRSSSSRbgn..",
    "..ngbRSSSRRbgn..",
    "..ngbRRRRRRbgn..",
    "..ngbbRRRRbbgn..",
    "...ngbbbbbbgn...",
    "....nggggggn....",
    ".....gggggg.....",
    "................",
]


def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    out_dir = os.path.join(root, "src", "main", "resources", "assets", "fulu", "textures", "item")
    os.makedirs(out_dir, exist_ok=True)

    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ART):
        assert len(row) == 16, "row %d is %d chars" % (y, len(row))
        for x, ch in enumerate(row):
            img.putpixel((x, y), PALETTE[ch])

    path = os.path.join(out_dir, "beast_blood.png")
    img.save(path)
    print("wrote", os.path.relpath(path, root))

    # 放大预览，方便肉眼检查
    prev = os.path.join(root, "tools", "out", "beast_blood_preview.png")
    os.makedirs(os.path.dirname(prev), exist_ok=True)
    scale = 12
    sheet = Image.new("RGB", (16 * scale, 16 * scale + 20), (40, 38, 48))
    sheet.paste(img.resize((16 * scale, 16 * scale), Image.NEAREST), (0, 0), img.resize((16 * scale, 16 * scale), Image.NEAREST))
    ImageDraw.Draw(sheet).text((6, 16 * scale + 4), "beast_blood 16x16", fill=(200, 200, 200))
    sheet.save(prev)
    print("wrote", os.path.relpath(prev, root))


if __name__ == "__main__":
    main()
