# -*- coding: utf-8 -*-
"""
守护检查：**每一个注册的实体类型都必须在客户端注册了渲染器**。

为什么要有这个脚本：
  漏注册渲染器时，游戏启动不报错、专用服务端冒烟也不报错，
  只有客户端真的画到那个实体时才 NPE（`entityrenderer is null`）——
  这次就是这样崩的（招魂阵的魂）。

用法：
    <python> tools\check_entity_renderers.py
退出码 0 = 全都有；1 = 有遗漏。
"""
import io
import re
import sys

REG = "src/main/java/com/lengyang/fulu/registry/ModEntities.java"
SETUP = "src/main/java/com/lengyang/fulu/client/ModClientSetup.java"

reg = io.open(REG, encoding="utf-8").read()
types = re.findall(r'ENTITIES\.register\("([a-z_]+)"', reg)
setup = io.open(SETUP, encoding="utf-8").read()

print("实体类型:", types)
missing = []
for t in types:
    up = t.upper()
    has = ("ModEntities.%s.get()" % up) in setup
    print("  %-16s renderer: %s" % (t, "YES" if has else "**NO**"))
    if not has:
        missing.append(t)

if missing:
    print()
    print("遗漏:", missing)
    print("补法：在 ModClientSetup.onRegisterRenderers 里加")
    for t in missing:
        print("    event.registerEntityRenderer(ModEntities.%s.get(), XxxRenderer::new);" % t.upper())
    sys.exit(1)

print()
print("OK - 每个实体都有渲染器")
