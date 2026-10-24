# -*- coding: utf-8 -*-
"""读崩溃报告：解压并打印最关键的几段。"""
import io
import os
import zipfile

SRC = r"C:\Users\lengyang\Desktop\.minecraft\versions\阵法师\mods\错误报告-2026-10-17_22.43.40.zip"
OUT = os.path.join("tools", "out", "crash")

os.makedirs(OUT, exist_ok=True)
z = zipfile.ZipFile(SRC)
print("包含文件:")
for n in z.namelist():
    print("  ", n, z.getinfo(n).file_size)

for n in z.namelist():
    if not n.lower().endswith((".txt", ".log")):
        continue
    data = z.read(n).decode("utf-8", errors="replace")
    with io.open(os.path.join(OUT, os.path.basename(n)), "w", encoding="utf-8") as f:
        f.write(data)
    if "crash" in n.lower() or "report" in n.lower():
        print()
        print("=" * 70)
        print(n)
        print("=" * 70)
        print(data[:3000])
