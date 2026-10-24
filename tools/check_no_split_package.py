# -*- coding: utf-8 -*-
"""确认 jar 里绝对没有 net/minecraft/** 的类（那会让 JPMS 报"两个模块导出同一个包"）。"""
import glob
import zipfile

jar = sorted(glob.glob("build/libs/fulu-*.jar"))[-1]
z = zipfile.ZipFile(jar)
bad = [n for n in z.namelist() if n.startswith("net/minecraft/")]
print("jar:", jar)
print("  net/minecraft/ 下的条目:", len(bad))
for n in bad:
    print("   !", n)
print("  屏障渲染类在包里:", "com/lengyang/fulu/client/render/FormationBarrierRenderer.class" in z.namelist())
print("  已删除的非法类是否还在:",
      "net/minecraft/client/renderer/FuluBarrierRenderType.class" in z.namelist())
print()
print("OK - 没有 split package" if not bad else "FAIL - 还有 split package")
