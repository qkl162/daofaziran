# -*- coding: utf-8 -*-
"""核对编译后的中文字符串是否完好（用编辑器写，保证搜索文本本身是 UTF-8）。"""
import glob
import zipfile

jar = sorted(glob.glob("build/libs/fulu-*.jar"))[-1]
z = zipfile.ZipFile(jar)

CHECKS = [
    ("com/lengyang/fulu/command/CodexCommand.class", "次数盾点数"),
    ("com/lengyang/fulu/command/CodexCommand.class", "要在游戏内"),
    ("com/lengyang/fulu/combat/PlayerDamageHandler.class", "替身阵已碎裂，你逃过一劫"),
    ("com/lengyang/fulu/standin/StandInData.class", "FuluStandIn"),
]

ok = True
for cls, text in CHECKS:
    raw = z.read(cls)
    found = text.encode("utf-8") in raw
    ok = ok and found
    print("  %-42s %-22s %s" % (cls.split("/")[-1], text, "OK" if found else "MISSING"))

print()
print("全部完好" if ok else "还有缺失")
