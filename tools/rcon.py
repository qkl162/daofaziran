# -*- coding: utf-8 -*-
"""
Minimal RCON client, so the harness can ask a running test server to do one thing:
save the world.

Why this exists: the dedicated server only writes the scoreboard to disk on a full
world save, and it saves from a shutdown hook. The harness cannot close it politely
(taskkill without /F does nothing to a windowless process), so instead we speak the
RCON protocol and ask for "save-all flush" - the same thing a clean shutdown does
for the data we care about.

Usage: python tools/rcon.py "save-all flush"
Reads host/port/password from run/server.properties unless given:
       python tools/rcon.py --port 25575 --password abc "save-all flush"
"""

import argparse
import os
import re
import socket
import struct
import sys

TYPE_AUTH = 3
TYPE_COMMAND = 2

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PROPERTIES = os.path.join(ROOT, "run", "server.properties")


def read_properties(path):
    props = {}
    if not os.path.exists(path):
        return props
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def send(sock, req_id, pkt_type, body):
    payload = struct.pack("<ii", req_id, pkt_type) + body.encode("utf-8") + b"\x00\x00"
    sock.sendall(struct.pack("<i", len(payload)) + payload)


def recv(sock):
    raw_len = sock.recv(4)
    if len(raw_len) < 4:
        return None, None, ""
    (length,) = struct.unpack("<i", raw_len)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            break
        data += chunk
    req_id, pkt_type = struct.unpack("<ii", data[:8])
    body = data[8:-2].decode("utf-8", errors="replace")
    return req_id, pkt_type, body


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command")
    parser.add_argument("--host", default=None)
    parser.add_argument("--port", type=int, default=None)
    parser.add_argument("--password", default=None)
    parser.add_argument("--timeout", type=float, default=10.0)
    args = parser.parse_args()

    props = read_properties(PROPERTIES)
    host = args.host or "127.0.0.1"
    port = args.port or int(props.get("rcon.port", 25575))
    password = args.password if args.password is not None else props.get("rcon.password", "")
    if not password:
        print("rcon: no password (is enable-rcon/ rcon.password set in run/server.properties?)")
        return 2

    try:
        with socket.create_connection((host, port), timeout=args.timeout) as sock:
            send(sock, 1, TYPE_AUTH, password)
            req_id, _, _ = recv(sock)
            if req_id == -1:
                print("rcon: authentication failed")
                return 3
            send(sock, 2, TYPE_COMMAND, args.command)
            _, _, body = recv(sock)
            print("rcon: %s" % (body.strip() or "(no output)"))
            return 0
    except OSError as exc:
        print("rcon: %s" % exc)
        return 1


if __name__ == "__main__":
    sys.exit(main())
