# -*- coding: utf-8 -*-
"""
Search Bing Images for a 符纸 (Chinese talisman paper) reference photo and
download a handful of candidates into tools/ref/.

Why this script exists:
  PowerShell's Invoke-WebRequest on this machine dies with
  "basic connection closed" (old .NET TLS stack), and Wikimedia is not
  reachable from here at all. The bundled Python talks to cn.bing.com fine,
  so the search + download happens here instead.

Usage:
  python tools/refsearch.py "符纸" 8
"""

import json
import os
import re
import ssl
import sys
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "ref")
CTX = ssl.create_default_context()
CTX.check_hostname = False
CTX.verify_mode = ssl.CERT_NONE
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")


def get(url, timeout=20):
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "*/*",
        "Accept-Language": "zh-CN,zh;q=0.9",
    })
    return urllib.request.urlopen(req, timeout=timeout, context=CTX)


def search(query, limit):
    url = ("https://cn.bing.com/images/search?q=" + urllib.parse.quote(query)
           + "&form=HDRSC2&first=1")
    html = get(url).read().decode("utf-8", "ignore")
    # Bing embeds each result as JSON inside an m="{...}" attribute.
    raw = re.findall(r'm="(\{[^"]*?\})"', html)
    found = []
    for chunk in raw:
        try:
            meta = json.loads(chunk.replace("&quot;", '"'))
        except Exception:
            continue
        murl = meta.get("murl")
        if not murl:
            continue
        found.append({
            "murl": murl,
            "turl": meta.get("turl", ""),
            "title": (meta.get("t") or "")[:60],
            "w": meta.get("mw"), "h": meta.get("mh"),
        })
        if len(found) >= limit:
            break
    return found


def main():
    query = sys.argv[1] if len(sys.argv) > 1 else "符纸"
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else 8
    os.makedirs(OUT, exist_ok=True)
    hits = search(query, limit * 3)
    print("candidates: %d" % len(hits))
    saved = 0
    for i, hit in enumerate(hits):
        if saved >= limit:
            break
        ext = os.path.splitext(urllib.parse.urlparse(hit["murl"]).path)[1].lower()
        if ext not in (".jpg", ".jpeg", ".png", ".webp"):
            ext = ".jpg"
        path = os.path.join(OUT, "ref_%02d%s" % (saved + 1, ext))
        try:
            data = get(hit["murl"], timeout=25).read()
        except Exception as exc:
            print("  skip  %s  (%s)" % (hit["murl"][:70], type(exc).__name__))
            continue
        if len(data) < 2000:
            print("  tiny  %s  (%d bytes)" % (hit["murl"][:70], len(data)))
            continue
        with open(path, "wb") as fh:
            fh.write(data)
        saved += 1
        print("  saved %s  %d bytes  %sx%s  <- %s"
              % (os.path.basename(path), len(data), hit["w"], hit["h"],
                 hit["murl"][:80]))
        print("        title: %s" % hit["title"])
    print("downloaded %d file(s) into tools/ref/" % saved)


if __name__ == "__main__":
    main()
