# -*- coding: utf-8 -*-
"""
Convert the Blockbench model ref/mohei_xuanjian.bbmodel into the flying sword's
Java GEOMETRY plus a texture the Java box-unwrap can actually use.

(The animation is hand-written in FlyingSwordModel.java; this script only owns the
shape and the texture, so re-running it never clobbers the animation.)

Two format gaps have to be bridged, and they are the whole reason this script
exists:

  1. the bbmodel is BEDROCK format with per-face UVs (box_uv: false), while Java
     entity models only support the automatic BOX unwrap. So for every cuboid the
     six atlas regions are copied into the six slots the box unwrap expects.
     Java's layout for a box (w, h, d) at texOffs (u, v) - the polygon order in
     ModelPart.Cube is DOWN, UP, WEST, NORTH, EAST, SOUTH (disassembled, not
     remembered), and the slots are:
         row 1 (height d):  [u, v]         d x d   DOWN
                            [u+d, v]       w x d   UP
         row 2 (height h):  [u, v+d]       d x h   WEST
                            [u+d, v+d]     w x h   NORTH
                            [u+d+w, v+d]   d x h   EAST
                            [u+d+w+d, v+d] w x h   SOUTH
  2. Bedrock models stand on y=0 with y pointing UP; Java model space has y
     pointing DOWN from the entity's feet. Every y is therefore negated.

The atlas is 6x the model's resolution (a 1x8 blade face is stored as 6x48), so
each face is resized down to its true texel size on the way in.

The bones follow the user's spec rather than the bbmodel's own grouping: the BLADE
group is the PARENT and the guard and hilt hang off it, so spinning the blade
carries the whole sword.

Outputs:
  src/main/java/com/lengyang/fulu/client/model/FlyingSwordShape.java   (generated)
  src/main/resources/assets/fulu/textures/entity/flying_sword.png
  tools/out/mohei_converted.png        preview of the new texture
  tools/out/mohei_model_check.txt      the numbers, for eyeballing

Usage: python tools/bbmodel_to_java.py
"""

import base64
import io
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
BBMODEL = os.path.join(ROOT, "ref", "mohei_xuanjian.bbmodel")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "com", "lengyang", "fulu",
                        "client", "model", "FlyingSwordShape.java")
TEX_OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "fulu",
                       "textures", "entity", "flying_sword.png")
OUT_DIR = os.path.join(HERE, "out")

TEX_W, TEX_H = 128, 128
ATLAS_SCALE = 6          # the bbmodel stores faces at 6x the model resolution

FACES = ["down", "up", "west", "north", "east", "south"]


def slot(name, w, h, d):
    """Where the box unwrap puts one face, relative to the cube's texOffs."""
    if name == "down":
        return 0, 0, d, d
    if name == "up":
        return d, 0, w, d
    if name == "west":
        return 0, d, d, h
    if name == "north":
        return d, d, w, h
    if name == "east":
        return d + w, d, d, h
    if name == "south":
        return d + w + d, d, w, h
    raise ValueError(name)


def bone_of(name):
    """Which of the three bones a cuboid belongs to (the user's grouping)."""
    if name.startswith("guard"):
        return "guard"
    if name in ("pommel", "handle"):
        return "hilt"
    return "blade"           # the blade segments and the tip steps


def main():
    data = json.load(open(BBMODEL, encoding="utf-8"))
    src = data["textures"][0]["source"]
    if "," in src:
        src = src.split(",", 1)[1]
    atlas = Image.open(io.BytesIO(base64.b64decode(src))).convert("RGBA")
    print("atlas: %s" % (atlas.size,))

    elements = data["elements"]

    # ---- 1. decide a texOffs for every cube, packing them into the texture ----
    layout = []
    x = y = row_h = 0
    for el in elements:
        f, t = el["from"], el["to"]
        w, h, d = (round(abs(t[i] - f[i])) for i in range(3))
        span_w = 2 * d + 2 * w
        span_h = d + h
        if x + span_w > TEX_W:
            x = 0
            y += row_h
            row_h = 0
        if y + span_h > TEX_H:
            raise SystemExit("texture too small: run out of room at %s" % el["name"])
        layout.append((el, x, y, w, h, d, span_w, span_h))
        x += span_w
        row_h = max(row_h, span_h)
    print("cubes: %d, texture rows used: %d px of %d" % (len(elements), y + row_h, TEX_H))

    # ---- 2. build the new texture ----
    out = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    report = []
    for el, tx, ty, w, h, d, span_w, span_h in layout:
        faces = el.get("faces", {})
        report.append("  %-14s bone=%-5s texOffs(%3d,%3d) size(%d,%d,%d)  span %dx%d"
                      % (el["name"], bone_of(el["name"]), tx, ty, w, h, d, span_w, span_h))
        for name in FACES:
            face = faces.get(name)
            if not face:
                report.append("      %-5s MISSING in the bbmodel" % name)
                continue
            u0, v0, u1, v1 = face["uv"]
            if u1 < u0:
                u0, u1 = u1, u0
            if v1 < v0:
                v0, v1 = v1, v0
            region = atlas.crop((round(u0), round(v0), round(u1), round(v1)))
            sx, sy, sw, sh = slot(name, w, h, d)
            if region.width == 0 or region.height == 0:
                report.append("      %-5s empty region" % name)
                continue
            if region.width >= sw * ATLAS_SCALE and region.height >= sh * ATLAS_SCALE:
                region = region.resize((sw, sh), Image.LANCZOS)
            else:
                region = region.resize((sw, sh), Image.NEAREST)
            out.paste(region, (tx + sx, ty + sy))
            report.append("      %-5s atlas %s -> slot +(%d,%d) %dx%d"
                          % (name, (u0, v0, u1, v1), sx, sy, sw, sh))

    os.makedirs(os.path.dirname(TEX_OUT), exist_ok=True)
    os.makedirs(os.path.dirname(JAVA_OUT), exist_ok=True)
    os.makedirs(OUT_DIR, exist_ok=True)
    out.save(TEX_OUT)
    print("wrote textures/entity/flying_sword.png (%dx%d)" % out.size)

    # ---- 3. generate the geometry, grouped into the three bones ----
    bones = {"blade": [], "guard": [], "hilt": []}
    for entry in layout:
        bones[bone_of(entry[0]["name"])].append(entry)
    print("bones: blade=%d guard=%d hilt=%d"
          % (len(bones["blade"]), len(bones["guard"]), len(bones["hilt"])))

    L = []
    L.append("package com.lengyang.fulu.client.model;")
    L.append("")
    L.append("import net.minecraft.client.model.geom.PartPose;")
    L.append("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    L.append("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    L.append("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    L.append("import net.minecraft.client.model.geom.builders.PartDefinition;")
    L.append("")
    L.append("/**")
    L.append(" * 飞剑的**几何**：由 {@code tools/bbmodel_to_java.py} 从")
    L.append(" * {@code ref/mohei_xuanjian.bbmodel}（Blockbench / 基岩格式）生成，请勿手改。")
    L.append(" *")
    L.append(" * <p>转换时处理了两处格式差异：")
    L.append(" * <ol>")
    L.append(" *   <li>基岩格式用**逐面 UV**，Java 实体模型只支持**盒式展开** ——")
    L.append(" *       贴图是重新排版过的（每个方块的六个面搬进盒式展开的槽位），")
    L.append(" *       换贴图必须重跑转换脚本；</li>")
    L.append(" *   <li>基岩模型 y 向上、站在 y=0；Java 模型 y 向下 —— 所有 y 取负。</li>")
    L.append(" * </ol>")
    L.append(" *")
    L.append(" * <p><b>骨骼</b>（按用户规范重组，和 bbmodel 里的分组不完全一样）：")
    L.append(" * <pre>")
    L.append(" *   blade  剑身（含剑尖）—— **父骨骼**，转它整把剑跟着转")
    L.append(" *    ├─ guard  护手")
    L.append(" *    └─ hilt   剑柄（含末端）")
    L.append(" * </pre>")
    L.append(" * 动画写在 {@link FlyingSwordModel} 里 —— 这个文件只描述形状。")
    L.append(" */")
    L.append("public final class FlyingSwordShape {")
    L.append("")
    L.append("    private FlyingSwordShape() {")
    L.append("    }")
    L.append("")
    L.append("    /** 形状定义（贴图 %dx%d）。 */" % (TEX_W, TEX_H))
    L.append("    public static LayerDefinition createBodyLayer() {")
    L.append("        MeshDefinition mesh = new MeshDefinition();")
    L.append("        PartDefinition root = mesh.getRoot();")
    L.append("")
    for bone in ("blade", "guard", "hilt"):
        parent = "root" if bone == "blade" else "blade"
        L.append('        PartDefinition %s = %s.addOrReplaceChild("%s",'
                 % (bone, parent, bone))
        L.append("                CubeListBuilder.create()")
        for el, tx, ty, w, h, d, span_w, span_h in bones[bone]:
            f, t = el["from"], el["to"]
            x0 = round(min(f[0], t[0]), 4)
            z0 = round(min(f[2], t[2]), 4)
            y_top = -round(max(f[1], t[1]), 4)
            L.append("                        .texOffs(%d, %d).addBox(%sf, %sf, %sf, %d, %d, %d)"
                     % (tx, ty, x0, y_top, z0, w, h, d))
        L.append("                        .mirror(),")
        L.append("                PartPose.ZERO);")
        L.append("")
    L.append("        return LayerDefinition.create(mesh, %d, %d);" % (TEX_W, TEX_H))
    L.append("    }")
    L.append("}")
    L.append("")
    open(JAVA_OUT, "w", encoding="utf-8", newline="\r\n").write("\n".join(L))
    print("wrote client/model/FlyingSwordShape.java (%d lines)" % len(L))

    # ---- 4. report + preview ----
    with open(os.path.join(OUT_DIR, "mohei_model_check.txt"), "w", encoding="utf-8") as fh:
        fh.write("bbmodel -> Java box unwrap\n")
        fh.write("atlas %s, model faces stored at %dx\n" % (atlas.size, ATLAS_SCALE))
        fh.write("cubes %d (blade %d / guard %d / hilt %d), texture %dx%d, used %d px\n\n"
                 % (len(layout), len(bones["blade"]), len(bones["guard"]),
                    len(bones["hilt"]), TEX_W, TEX_H, y + row_h))
        fh.write("\n".join(report))
        fh.write("\n")
    big = out.resize((TEX_W * 3, TEX_H * 3), Image.NEAREST)
    sheet = Image.new("RGB", (big.width, big.height), (40, 42, 52))
    sheet.paste(big, (0, 0), big)
    sheet.save(os.path.join(OUT_DIR, "mohei_converted.png"))
    print("wrote tools/out/mohei_converted.png and mohei_model_check.txt")


if __name__ == "__main__":
    main()
