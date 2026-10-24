# -*- coding: utf-8 -*-
"""
Baidu image search -> tools/ref/.

Bing's scraped image results on this machine come back as unrelated junk
("黄色" is read as the colour, not as talisman paper), so this uses Baidu's
JSON endpoint instead, which honours the query much better.

Usage:  python tools/baidu_ref.py "符纸" 6
"""

import json
import os
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
HEAD = {
    "User-Agent": UA,
    "Accept": "text/plain, */*; q=0.01",
    "Accept-Language": "zh-CN,zh;q=0.9",
    "Referer": "https://image.baidu.com/",
}


def get(url, timeout=25):
    return urllib.request.urlopen(urllib.request.Request(url, headers=HEAD),
                                  timeout=timeout, context=CTX)


def search(query, rn=30):
    params = {
        "tn": "resultjson_com", "ipn": "rj", "ct": "201326592", "is": "",
        "fp": "result", "fr": "", "word": query, "queryWord": query,
        "cl": "2", "lm": "-1", "ie": "utf-8", "oe": "utf-8",
        "st": "-1", "ic": "0", "face": "0", "istype": "2",
        "nc": "1", "pn": "0", "rn": str(rn), "gsm": "1e",
    }
    url = "https://image.baidu.com/search/acjson?" + urllib.parse.urlencode(params)
    body = get(url).read().decode("utf-8", "ignore")
    body = body.replace("\\'", "'")
    try:
        data = json.loads(body)
    except Exception:
        # Baidu sometimes returns invalid escapes; salvage object by object.
        data = {"data": []}
        for chunk in body.split('{"thumbURL"')[1:]:
            try:
                data["data"].append(json.loads('{"thumbURL"' + chunk.split('},')[0] + "}"))
            except Exception:
                pass
    out = []
    for item in data.get("data", []):
        if not item:
            continue
        url_ = item.get("middleURL") or item.get("hoverURL") or item.get("thumbURL")
        if url_:
            out.append({"url": url_, "w": item.get("width"), "h": item.get("height"),
                        "from": item.get("fromPageTitleEnc", "")[:50]})
    return out


def main():
    query = sys.argv[1] if len(sys.argv) > 1 else "符纸"
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else 6
    os.makedirs(OUT, exist_ok=True)
    hits = search(query)
    print("query=%s  candidates=%d" % (query, len(hits)))
    saved = 0
    for hit in hits:
        if saved >= limit:
            break
        ext = os.path.splitext(urllib.parse.urlparse(hit["url"]).path)[1].lower()
        if ext not in (".jpg", ".jpeg", ".png", ".webp"):
            ext = ".jpg"
        path = os.path.join(OUT, "bd_%02d%s" % (saved + 1, ext))
        try:
            data = get(hit["url"], timeout=20).read()
        except Exception as exc:
            print("  skip (%s) %s" % (type(exc).__name__, hit["url"][:70]))
            continue
        if len(data) < 3000:
            continue
        with open(path, "wb") as fh:
            fh.write(data)
        saved += 1
        print("  saved %s %6d bytes  %sx%s" % (os.path.basename(path), len(data),
                                               hit["w"], hit["h"]))
    print("downloaded %d" % saved)


if __name__ == "__main__":
    main()
