# -*- coding: utf-8 -*-
"""在 Forge jar 里找所有和"刷怪"有关的事件类。"""
import glob
import zipfile

cands = []
for pat in ("tools/.gradle-home/caches/**/forge-1.20.1-47*.jar",
            "tools/.gradle-home/caches/**/*forge*srg*.jar",
            "tools/.gradle-home/caches/**/*forge*universal*.jar",
            "tools/.gradle-home/caches/**/*forge*.jar"):
    cands += glob.glob(pat, recursive=True)

seen = set()
for c in cands:
    if c in seen:
        continue
    seen.add(c)
    try:
        z = zipfile.ZipFile(c)
    except Exception:
        continue
    hits = [n for n in z.namelist()
            if "SpawnEvent" in n or "SpawnPlacement" in n]
    if hits:
        print("==", c.split("\\")[-1], "==")
        for h in sorted(hits)[:20]:
            print("   ", h)
