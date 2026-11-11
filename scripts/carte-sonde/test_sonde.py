#!/usr/bin/env python3
"""Essai local de la sonde : une archive PMTiles v3 synthétique (deux niveaux de répertoires, une tuile dupliquée,
métadonnées gzip) servie par un petit serveur qui gère HEAD, Range, ETag et If-Match. Aucun accès Internet.
Usage : python3 scripts/carte-sonde/test_sonde.py"""
import gzip
import json
import os
import struct
import subprocess
import sys
import tempfile
import threading
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer

sys.path.insert(0, os.path.dirname(__file__))
import sonde  # noqa: E402

ETAG = '"synthetique-1"'


def write_varint(out, v):
    while v >= 0x80:
        out.append((v & 0x7F) | 0x80)
        v >>= 7
    out.append(v)


def encode_directory(entries):
    out = bytearray()
    write_varint(out, len(entries))
    last = 0
    for tid, _, _, _ in entries:
        write_varint(out, tid - last)
        last = tid
    for _, _, _, run in entries:
        write_varint(out, run)
    for _, _, length, _ in entries:
        write_varint(out, length)
    prev_end = None
    for _, offset, length, _ in entries:
        write_varint(out, 0 if prev_end == offset else offset + 1)
        prev_end = offset + length
    return gzip.compress(bytes(out))


def build_archive():
    _, _, circle = sonde.region_tiles(45.917, 24.800, 30, 15)
    ids = sorted({sonde.tile_id(z, x, y) for tiles in circle.values() for (z, x, y) in tiles})
    tile_data = bytearray()
    entries = []
    shared = gzip.compress(b"ocean" * 50)
    shared_at = None
    for n, tid in enumerate(ids):
        if n % 7 == 3:  # contenu partagé (mer, vide) : même décalage, plusieurs tuiles
            if shared_at is None:
                shared_at = len(tile_data)
                tile_data += shared
            entries.append((tid, shared_at, len(shared), 1))
        else:
            blob = gzip.compress(bytes([n % 251]) * (200 + (n * 37) % 900))
            entries.append((tid, len(tile_data), len(blob), 1))
            tile_data += blob
    leaves, root = bytearray(), []
    for i in range(0, len(entries), 400):
        chunk = entries[i:i + 400]
        enc = encode_directory(chunk)
        root.append((chunk[0][0], len(leaves), len(enc), 0))
        leaves += enc
    root_bytes = encode_directory(root)
    meta = gzip.compress(json.dumps({
        "name": "Protomaps Basemap (synthétique)", "version": "4.0.0", "attribution": "© OpenStreetMap",
        "vector_layers": [{"id": n, "fields": {}} for n in
                          ["boundaries", "buildings", "earth", "landcover", "landuse", "places", "pois", "roads", "water", "extra"]],
    }).encode())
    off_root = 127
    off_meta = off_root + len(root_bytes)
    off_leaf = off_meta + len(meta)
    off_tiles = off_leaf + len(leaves)
    header = bytearray(b"PMTiles" + bytes([3]))
    for off, ln in [(off_root, len(root_bytes)), (off_meta, len(meta)), (off_leaf, len(leaves)), (off_tiles, len(tile_data))]:
        header += struct.pack("<QQ", off, ln)
    header += struct.pack("<QQQ", len(ids), len(entries), len(entries))
    header += bytes([1, 2, 2, 1, 0, 15]) + struct.pack("<iiii", 240000000, 456000000, 252000000, 462000000)
    header += bytes([8]) + struct.pack("<ii", 248000000, 459000000)
    assert len(header) == 127, len(header)
    return bytes(header) + root_bytes + meta + bytes(leaves) + bytes(tile_data), entries


class Handler(BaseHTTPRequestHandler):
    archive = b""
    wanted_day = ""

    def log_message(self, *args):
        pass

    def _serve(self, head):
        if not self.path.endswith(f"{self.wanted_day}.pmtiles"):
            self.send_response(404)
            self.end_headers()
            return
        data, rng = self.archive, self.headers.get("Range")
        if_match = self.headers.get("If-Match")
        if if_match and if_match != ETAG:
            self.send_response(412)
            self.end_headers()
            return
        self.send_header_common = lambda: None
        if rng and "," not in rng and not head:
            a, b = rng.removeprefix("bytes=").split("-")
            a, b = int(a), min(int(b), len(data) - 1)
            body = data[a:b + 1]
            self.send_response(206)
            self.send_header("Content-Range", f"bytes {a}-{b}/{len(data)}")
        else:
            body = data
            self.send_response(200)
        self.send_header("ETag", ETAG)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if not head:
            self.wfile.write(body)

    def do_GET(self):
        self._serve(False)

    def do_HEAD(self):
        self._serve(True)


def main():
    archive, entries = build_archive()
    today = datetime.now(timezone.utc).date()
    Handler.archive = archive
    Handler.wanted_day = f"{today - timedelta(days=2):%Y%m%d}"
    server = HTTPServer(("127.0.0.1", 0), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    out = os.path.join(tempfile.mkdtemp(), "sonde.json")
    result = subprocess.run(
        [sys.executable, "-I", os.path.join(os.path.dirname(__file__), "sonde.py"), "--base",
         f"http://127.0.0.1:{server.server_port}", "--json-out", out],
        capture_output=True, text=True,
    )
    print(result.stdout)
    print(result.stderr, file=sys.stderr)
    assert result.returncode == 0, "la sonde a échoué"
    summary = json.load(open(out))
    expected_unique = len({(o, ln) for _, o, ln, _ in entries})
    expected_bytes = sum(ln for _, ln in {(o, ln) for _, o, ln, _ in entries})
    region = summary["region"]
    assert region["tiles_found"] == len(entries), region
    assert region["tile_bytes"] == expected_bytes, (region["tile_bytes"], expected_bytes)
    assert region["levels"] == 2 and region["leaf_dirs"] >= 2, region
    assert summary["range"]["range_status"] == 206
    assert summary["metadata"]["style_missing"], "le style devait être comparé"
    print(f"OK : {len(entries)} tuiles, {expected_unique} contenus distincts, {expected_bytes} octets")


if __name__ == "__main__":
    main()
