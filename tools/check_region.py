# -*- coding: utf-8 -*-
"""
Look for an NBT key inside a Minecraft region file (*.mca) and report the byte
that follows it - i.e. read the saved value straight off the disk.

Why this exists:
  A block entity's saveAdditional() only proves itself if the bytes really end up
  in the world save. The in-game checks (data get block, chunk unload/reload)
  all go through Minecraft's own code, so if Minecraft happened not to save, the
  test could still look green. This reads the region file with a plain zlib
  decompressor instead - no Minecraft involved.

Region format: 8 KiB header (1024 location entries of 3-byte sector offset +
1-byte sector count, then 1024 timestamps), then chunk records of
[4-byte length][1-byte compression][payload]. Compression 1=gzip, 2=zlib, 3=raw.

Usage: python tools/check_region.py <region file> [NBT key] [expected value]
         -> exit 0 if the key was found (and the value matches when given)
"""

import gzip
import io
import os
import sys
import zlib

KEY = sys.argv[2] if len(sys.argv) > 2 else "Talisman"
# The expected value can be a number (a TAG_Byte, e.g. step 9's HasTalisman:1b)
# or a word (a TAG_String, e.g. this step's Talisman:"chaos").
EXPECT = None
if len(sys.argv) > 3:
    try:
        EXPECT = int(sys.argv[3])
    except ValueError:
        EXPECT = sys.argv[3]

path = sys.argv[1] if len(sys.argv) > 1 else None
if not path or not os.path.exists(path):
    print("no region file at %r - was the world ever written to disk?" % path)
    sys.exit(2)

needle = KEY.encode("utf-8")
hits = []
with open(path, "rb") as fh:
    header = fh.read(4096)
    for i in range(1024):
        offset = int.from_bytes(header[i * 4:i * 4 + 3], "big")
        count = header[i * 4 + 3]
        if offset == 0 or count == 0:
            continue
        fh.seek(offset * 4096)
        raw_len = fh.read(4)
        if len(raw_len) < 4:
            continue
        length = int.from_bytes(raw_len, "big")
        ctype = fh.read(1)[0]
        blob = fh.read(length - 1)
        try:
            if ctype == 1:
                raw = gzip.decompress(blob)
            elif ctype == 2:
                raw = zlib.decompress(blob)
            else:
                raw = blob
        except Exception as exc:
            print("  chunk %d,%d: decompress failed: %s" % (i % 32, i // 32, exc))
            continue
        pos = 0
        while True:
            pos = raw.find(needle, pos)
            if pos < 0:
                break
            after = raw[pos + len(needle)] if pos + len(needle) < len(raw) else None
            # The byte right after the name is the first payload byte. For a
            # TAG_Byte that IS the value (the old behaviour). For a TAG_String the
            # payload is <length:2><utf8>, so also try to read the text - that is
            # what "which talisman is in it" is stored as since step 9's boolean
            # became a three-way kind.
            text = None
            if after is not None and pos + len(needle) + 2 < len(raw):
                length = (raw[pos + len(needle)] << 8) | raw[pos + len(needle) + 1]
                if 0 < length <= 64:
                    chunk = raw[pos + len(needle) + 2: pos + len(needle) + 2 + length]
                    try:
                        decoded = chunk.decode("utf-8")
                        if decoded.isprintable():
                            text = decoded
                    except UnicodeDecodeError:
                        text = None
            hits.append((i % 32, i // 32, after, text))
            pos += len(needle)

print("region file : %s (%d bytes)" % (path, os.path.getsize(path)))
print("looking for : %s" % KEY)
if not hits:
    print("RESULT: key NOT found in any chunk on disk")
    sys.exit(1)
for cx, cz, after, text in hits:
    print("  found in chunk %d,%d -> value byte right after the name: %s%s"
          % (cx, cz, after, ("  | as string: %r" % text) if text else ""))
if EXPECT is not None:
    ok = all(after == EXPECT for _, _, after, _ in hits) if isinstance(EXPECT, int) \
        else all(text == EXPECT for _, _, _, text in hits)
    print("RESULT: %s (expected %r)" % ("OK" if ok else "MISMATCH", EXPECT))
    sys.exit(0 if ok else 1)
print("RESULT: OK")
