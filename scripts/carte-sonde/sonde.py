#!/usr/bin/env python3
"""Sonde d'une édition Protomaps (tâche 7, docs/PLAN-CARTE-TELECHARGEMENT.md, décision D1).

Lit, par requêtes HTTP Range, uniquement : l'en-tête et le répertoire racine de l'édition, ses métadonnées, les
répertoires feuilles utiles à une zone (centre + rayon, par défaut Cincu 30 km, zoom 15) et quelques tuiles d'essai.
Aucune tuile de la zone n'est téléchargée : la sonde établit la taille exacte à partir des seuls répertoires.

Établit :
  * quelles éditions répondent (les 16 derniers jours) et jusqu'où remontent les anciennes ;
  * l'acceptation des requêtes Range (206, Content-Range, ETag, If-Match, plages multiples) ;
  * l'en-tête et les métadonnées réels, dont les couches de tuiles et la version du schéma, comparés aux couches
    utilisées par le style embarqué (androidApp/src/fdroid/assets/offline-map/styles) ;
  * la taille et le nombre des répertoires lus pour la zone, le nombre de tuiles, la taille exacte des tuiles et le
    nombre de requêtes après fusion des plages (excédent toléré 5 %, comme `pmtiles extract`).

Rien de secret n'est utilisé ni affiché. Bibliothèque standard seulement.
"""
import argparse
import bisect
import gzip
import json
import math
import struct
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

HEADER_LEN = 127
ROOT_PREFETCH = 16384  # la spécification limite en-tête + répertoire racine à 16 384 octets
USER_AGENT = "NetworkTact-sonde/1 (+tache 7, mesure ponctuelle)"
COMPRESSION = {0: "inconnue", 1: "aucune", 2: "gzip", 3: "brotli", 4: "zstd"}
TILE_TYPE = {0: "inconnu", 1: "MVT", 2: "PNG", 3: "JPEG", 4: "WebP", 5: "AVIF", 6: "MLT"}

transferred = {"bytes": 0, "requests": 0}


# --------------------------------------------------------------------------------------------------- HTTP


def request(url, method="GET", headers=None, limit=None, timeout=60):
    """Renvoie (statut, en-têtes, corps). `limit` : n'en lit que ce nombre d'octets puis coupe (jamais le fichier entier)."""
    req = urllib.request.Request(url, method=method, headers={"User-Agent": USER_AGENT, **(headers or {})})
    transferred["requests"] += 1
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            body = resp.read() if limit is None else resp.read(limit)
            transferred["bytes"] += len(body)
            return resp.status, {k.lower(): v for k, v in resp.headers.items()}, body
    except urllib.error.HTTPError as e:
        body = e.read(2000)
        return e.code, {k.lower(): v for k, v in e.headers.items()}, body


def read_range(url, offset, length, headers=None):
    status, hdr, body = request(
        url, headers={"Range": f"bytes={offset}-{offset + length - 1}", **(headers or {})}, limit=length + 1
    )
    if status != 206:
        raise RuntimeError(f"plage {offset}+{length} : statut {status} au lieu de 206 ({hdr.get('content-range')})")
    if len(body) != length:
        raise RuntimeError(f"plage {offset}+{length} : {len(body)} octets reçus")
    return body


# --------------------------------------------------------------------------------------------------- PMTiles


def u64(b, o):
    return struct.unpack_from("<Q", b, o)[0]


def parse_header(b):
    if b[:7] != b"PMTiles":
        raise RuntimeError("pas un fichier PMTiles")
    h = {"version": b[7]}
    names = ["root", "metadata", "leaf", "tiledata"]
    for i, n in enumerate(names):
        h[n + "_offset"] = u64(b, 8 + 16 * i)
        h[n + "_length"] = u64(b, 16 + 16 * i)
    h["addressed_tiles"], h["tile_entries"], h["tile_contents"] = u64(b, 72), u64(b, 80), u64(b, 88)
    h["clustered"], h["internal_compression"], h["tile_compression"], h["tile_type"] = b[96], b[97], b[98], b[99]
    h["min_zoom"], h["max_zoom"] = b[100], b[101]
    h["bounds"] = [v / 1e7 for v in struct.unpack_from("<iiii", b, 102)]  # minLon, minLat, maxLon, maxLat
    h["center_zoom"] = b[118]
    h["center"] = [v / 1e7 for v in struct.unpack_from("<ii", b, 119)]
    return h


def decompress(data, code):
    if code == 1:
        return data
    if code == 2:
        return gzip.decompress(data)
    raise RuntimeError(f"compression {COMPRESSION.get(code, code)} non gérée par la sonde")


def varint(b, pos):
    result, shift = 0, 0
    while True:
        byte = b[pos]
        pos += 1
        result |= (byte & 0x7F) << shift
        if byte < 0x80:
            return result, pos
        shift += 7


def decode_directory(data):
    """Liste d'entrées (tile_id, offset, length, run_length), spécification v3 §4.2."""
    pos = 0
    n, pos = varint(data, pos)
    ids, runs, lengths, offsets = [], [], [], []
    last = 0
    for _ in range(n):
        d, pos = varint(data, pos)
        last += d
        ids.append(last)
    for _ in range(n):
        v, pos = varint(data, pos)
        runs.append(v)
    for _ in range(n):
        v, pos = varint(data, pos)
        lengths.append(v)
    for i in range(n):
        v, pos = varint(data, pos)
        if v == 0 and i > 0:
            offsets.append(offsets[i - 1] + lengths[i - 1])
        else:
            offsets.append(v - 1)
    return list(zip(ids, offsets, lengths, runs))


def hilbert_index(z, x, y):
    n = 1 << z
    d = 0
    s = n // 2
    while s > 0:
        rx = 1 if (x & s) > 0 else 0
        ry = 1 if (y & s) > 0 else 0
        d += s * s * ((3 * rx) ^ ry)
        if ry == 0:
            if rx == 1:
                x = n - 1 - x
                y = n - 1 - y
            x, y = y, x
        s //= 2
    return d


def tile_id(z, x, y):
    return ((1 << (2 * z)) - 1) // 3 + hilbert_index(z, x, y)


# --------------------------------------------------------------------------------------------------- zone


def lon_to_x(lon, z):
    return (lon + 180.0) / 360.0 * (1 << z)


def lat_to_y(lat, z):
    return (1 - math.asinh(math.tan(math.radians(lat))) / math.pi) / 2 * (1 << z)


def x_to_lon(x, z):
    return x / (1 << z) * 360.0 - 180.0


def y_to_lat(y, z):
    return math.degrees(math.atan(math.sinh(math.pi * (1 - 2 * y / (1 << z)))))


def region_tiles(lat0, lon0, radius_km, zoom_max):
    """{zoom: (tuiles du rectangle circonscrit, tuiles dont le carré coupe le cercle)}, tuiles du cercle en liste."""
    dlat = radius_km / 111.32
    dlon = radius_km / (111.32 * math.cos(math.radians(lat0)))
    west, east, south, north = lon0 - dlon, lon0 + dlon, lat0 - dlat, lat0 + dlat
    per_zoom, circle = {}, {}
    for z in range(0, zoom_max + 1):
        x0, x1 = int(lon_to_x(west, z)), int(lon_to_x(east, z))
        y0, y1 = int(lat_to_y(north, z)), int(lat_to_y(south, z))
        keep = []
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                lo, hi = x_to_lon(x, z), x_to_lon(x + 1, z)
                top, bottom = y_to_lat(y, z), y_to_lat(y + 1, z)
                near_lon = min(max(lon0, lo), hi)
                near_lat = min(max(lat0, bottom), top)
                dx = (near_lon - lon0) * 111.32 * math.cos(math.radians(lat0))
                dy = (near_lat - lat0) * 111.32
                if dx * dx + dy * dy <= radius_km * radius_km:
                    keep.append((z, x, y))
        per_zoom[z] = ((x1 - x0 + 1) * (y1 - y0 + 1), len(keep))
        circle[z] = keep
    return (west, south, east, north), per_zoom, circle


# --------------------------------------------------------------------------------------------------- étapes


def probe_editions(base, today, days_back, older):
    print("\n== 1. Éditions (HEAD) ==")
    found = None
    for back in list(range(days_back)) + older:
        d = today - timedelta(days=back)
        url = f"{base}/{d:%Y%m%d}.pmtiles"
        try:
            status, hdr, _ = request(url, method="HEAD", timeout=20)
        except Exception as e:  # noqa: BLE001 - la sonde rapporte tout
            print(f"  J-{back:<3} {d:%Y-%m-%d} : erreur {type(e).__name__}: {e}")
            continue
        extra = f" taille {int(hdr['content-length']) / 1e9:.1f} Go" if status == 200 and "content-length" in hdr else ""
        print(f"  J-{back:<3} {d:%Y-%m-%d} : HTTP {status}{extra}")
        if status == 200 and found is None:
            found = (url, hdr)
    return found


def probe_range_behaviour(url, head_hdr):
    print("\n== 2. Requêtes Range ==")
    for k in ["accept-ranges", "etag", "content-length", "last-modified", "server", "cf-cache-status", "via", "cache-control"]:
        if k in head_hdr:
            print(f"  HEAD {k}: {head_hdr[k]}")
    status, hdr, body = request(url, headers={"Range": f"bytes=0-{ROOT_PREFETCH - 1}"}, limit=ROOT_PREFETCH + 1)
    print(f"  Range 0-{ROOT_PREFETCH - 1} : HTTP {status}, {len(body)} octets, Content-Range {hdr.get('content-range')}, ETag {hdr.get('etag')}")
    if status != 206 or len(body) != ROOT_PREFETCH:
        raise RuntimeError("le serveur ne répond pas 206 avec la plage demandée : l'extraction par plages est impossible")
    etag = hdr.get("etag")
    # Plages multiples : la sonde le note, l'extracteur n'en a pas besoin (une plage par requête).
    status_m, hdr_m, _ = request(url, headers={"Range": "bytes=0-9,100-109"}, limit=300)
    print(f"  Plages multiples (0-9,100-109) : HTTP {status_m}, type {hdr_m.get('content-type')}")
    if etag:
        s_ok, _, _ = request(url, headers={"Range": "bytes=0-9", "If-Match": etag}, limit=10)
        s_ko, _, _ = request(url, headers={"Range": "bytes=0-9", "If-Match": '"sonde-faux"'}, limit=10)
        print(f"  If-Match avec le bon ETag : HTTP {s_ok} ; avec un faux : HTTP {s_ko} (412 attendu)")
    else:
        print("  Pas d'ETag : If-Match non testable")
    return body, etag, {"range_status": status, "multi_range_status": status_m, "etag": etag}


def probe_metadata(url, header, style_dir):
    print("\n== 4. Métadonnées et schéma ==")
    raw = read_range(url, header["metadata_offset"], header["metadata_length"])
    meta = json.loads(decompress(raw, header["internal_compression"]))
    print(f"  {header['metadata_length']} octets compressés ; clés : {sorted(meta.keys())}")
    for k in sorted(meta):
        if k == "vector_layers":
            continue
        v = json.dumps(meta[k], ensure_ascii=False)
        print(f"  {k} = {v[:200]}")
    layers = {layer["id"]: sorted(layer.get("fields", {}).keys()) for layer in meta.get("vector_layers", [])}
    print(f"  vector_layers ({len(layers)}) : {', '.join(sorted(layers))}")
    result = {"keys": sorted(meta.keys()), "name": meta.get("name"), "version": meta.get("version"),
              "attribution": meta.get("attribution"), "layers": sorted(layers)}
    if style_dir:
        import glob
        import os

        used = {}
        for f in sorted(glob.glob(os.path.join(style_dir, "*.json"))):
            style = json.load(open(f, encoding="utf-8"))
            used[os.path.basename(f)] = sorted({lay["source-layer"] for lay in style["layers"] if "source-layer" in lay})
        for name, ids in used.items():
            missing = [i for i in ids if i not in layers]
            extra = [i for i in layers if i not in ids]
            print(f"  Style {name} utilise {len(ids)} couches ; absentes de l'édition : {missing or 'aucune'} ; "
                  f"présentes dans l'édition mais pas dans le style : {extra or 'aucune'}")
            result.setdefault("style_missing", {})[name] = missing
    return result


def walk_directories(url, header, root_entries, tile_ids, etag):
    """Descend dans les répertoires et ne lit que les feuilles utiles. Renvoie ({tile_id: (offset, longueur)}, stats)."""
    ic = header["internal_compression"]
    stats = {"leaf_dirs": 0, "leaf_bytes": 0, "levels": 1}
    found = {}
    conditional = {"If-Match": etag} if etag else None

    def visit(entries, wanted, level):
        stats["levels"] = max(stats["levels"], level)
        ids = [e[0] for e in entries]
        leaves = {}
        for tid in wanted:
            i = bisect.bisect_right(ids, tid) - 1
            if i < 0:
                continue
            tile_id_, offset, length, run = entries[i]
            if run == 0:
                leaves.setdefault(i, []).append(tid)
            elif tid < tile_id_ + run:
                found[tid] = (offset, length)
        for i in sorted(leaves):
            _, offset, length, _ = entries[i]
            raw = read_range(url, header["leaf_offset"] + offset, length, conditional)
            stats["leaf_dirs"] += 1
            stats["leaf_bytes"] += length
            visit(decode_directory(decompress(raw, ic)), leaves[i], level + 1)

    visit(root_entries, sorted(tile_ids), 1)
    return found, stats


def merge_ranges(ranges, overfetch):
    """Fusion à excédent borné, comme `pmtiles extract` : on comble d'abord les plus petits trous tant que le budget dure."""
    ranges = sorted(ranges)
    total = sum(length for _, length in ranges)
    if not ranges:
        return [], 0
    merged = [[o, o + n] for o, n in ranges]
    # Les plages contiguës ou qui se chevauchent d'abord.
    out = [merged[0]]
    for s, e in merged[1:]:
        if s <= out[-1][1]:
            out[-1][1] = max(out[-1][1], e)
        else:
            out.append([s, e])
    gaps = sorted((out[i + 1][0] - out[i][1], i) for i in range(len(out) - 1))
    budget = int(total * overfetch)
    closed = set()
    for gap, i in gaps:
        if gap > budget:
            break
        budget -= gap
        closed.add(i)
    final = []
    for i, (s, e) in enumerate(out):
        if final and (i - 1) in closed:
            final[-1][1] = e
        else:
            final.append([s, e])
    fetched = sum(e - s for s, e in final)
    return final, fetched


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", default="https://build.protomaps.com")
    p.add_argument("--lat", type=float, default=45.917)
    p.add_argument("--lon", type=float, default=24.800)
    p.add_argument("--radius-km", type=float, default=30.0)
    p.add_argument("--max-zoom", type=int, default=15)
    p.add_argument("--style-dir", default="androidApp/src/fdroid/assets/offline-map/styles")
    p.add_argument("--today", help="AAAA-MM-JJ (essais) ; par défaut aujourd'hui, UTC")
    p.add_argument("--json-out", default="sonde.json")
    a = p.parse_args()
    started = time.time()
    today = datetime.strptime(a.today, "%Y-%m-%d").date() if a.today else datetime.now(timezone.utc).date()

    summary = {"parameters": vars(a)}
    found = probe_editions(a.base, today, 16, [20, 30, 45, 60, 90])
    if not found:
        print("\nAucune édition ne répond : arrêt.")
        summary["edition"] = None
        json.dump(summary, open(a.json_out, "w"), indent=2)
        return 1
    url, head_hdr = found
    summary["edition"] = url
    prefetch, etag, range_info = probe_range_behaviour(url, head_hdr)
    summary["range"] = range_info

    print("\n== 3. En-tête ==")
    header = parse_header(prefetch[:HEADER_LEN])
    for k, v in header.items():
        shown = {"internal_compression": COMPRESSION, "tile_compression": COMPRESSION, "tile_type": TILE_TYPE}.get(k)
        print(f"  {k}: {v}" + (f" ({shown.get(v, '?')})" if shown else ""))
    summary["header"] = header
    if header["version"] != 3 or not header["clustered"]:
        print("  !! version ≠ 3 ou source non clustered : l'extracteur la refuserait")

    summary["metadata"] = probe_metadata(url, header, a.style_dir)

    print(f"\n== 5. Zone : {a.radius_km} km autour de ({a.lat}, {a.lon}), zoom {a.max_zoom} ==")
    zmax = min(a.max_zoom, header["max_zoom"])
    bbox, per_zoom, circle = region_tiles(a.lat, a.lon, a.radius_km, zmax)
    print(f"  rectangle circonscrit (ouest, sud, est, nord) : {[round(v, 4) for v in bbox]}")
    rect_total = circle_total = 0
    for z, (r, c) in per_zoom.items():
        rect_total += r
        circle_total += c
        if z >= 10:
            print(f"  zoom {z:>2} : rectangle {r:>6} tuiles, cercle {c:>6}")
    print(f"  total zooms 0 à {zmax} : rectangle {rect_total}, cercle {circle_total}")
    ids = {tile_id(z, x, y): z for tiles in circle.values() for (z, x, y) in tiles}

    t0 = time.time()
    before = dict(transferred)
    root_len = header["root_length"]
    root = decode_directory(decompress(prefetch[header["root_offset"]:header["root_offset"] + root_len], header["internal_compression"]))
    print(f"  répertoire racine : {len(root)} entrées, {root_len} octets compressés (déjà dans les 16 Ko lus)")
    entries, stats = walk_directories(url, header, root, ids.keys(), etag)
    dir_seconds = time.time() - t0
    dir_requests = transferred["requests"] - before["requests"]
    print(f"  répertoires feuilles lus : {stats['leaf_dirs']} ({stats['leaf_bytes']} octets compressés), niveaux {stats['levels']}, "
          f"{dir_requests} requêtes, {dir_seconds:.1f} s")
    unique = {}
    per_zoom_bytes = {}
    for tid, (off, length) in entries.items():
        unique[(off, length)] = None
        z = ids[tid]
        per_zoom_bytes[z] = per_zoom_bytes.get(z, 0) + length
    tile_bytes = sum(length for (_, length) in unique)
    print(f"  tuiles présentes : {len(entries)} sur {len(ids)} demandées ; contenus distincts : {len(unique)} ; "
          f"octets de tuiles : {tile_bytes} ({tile_bytes / 1e6:.1f} Mo)")
    for z in sorted(per_zoom_bytes):
        if z >= 12:
            print(f"    zoom {z:>2} : {per_zoom_bytes[z] / 1e6:6.2f} Mo (sans dédoublonnage)")
    merged, fetched = merge_ranges(list(unique.keys()), 0.05)
    print(f"  plages à télécharger après fusion (excédent 5 %) : {len(merged)} requêtes, {fetched / 1e6:.1f} Mo à transférer")
    if entries:
        average = tile_bytes / len(entries)
        print(f"  moyenne : {average / 1000:.2f} Ko par tuile présente")
    summary["region"] = {"tiles_rectangle": rect_total, "tiles_circle": circle_total, "tiles_found": len(entries),
                         "tile_bytes": tile_bytes, "merged_requests": len(merged), "fetched_bytes": fetched,
                         "leaf_dirs": stats["leaf_dirs"], "leaf_bytes": stats["leaf_bytes"], "levels": stats["levels"],
                         "directory_requests": dir_requests, "directory_seconds": round(dir_seconds, 2)}

    print("\n== 6. Tuiles d'essai ==")
    sample = sorted(entries.items(), key=lambda kv: kv[1][1])[-3:]
    for tid, (off, length) in sample:
        data = read_range(url, header["tiledata_offset"] + off, length, {"If-Match": etag} if etag else None)
        ok = data[:2] == b"\x1f\x8b"
        inflated = len(gzip.decompress(data)) if ok else None
        print(f"  tuile id {tid} (zoom {ids[tid]}) : {length} octets, gzip {'oui' if ok else 'NON'}, {inflated} octets décompressée")

    print(f"\n== Bilan réseau : {transferred['requests']} requêtes, {transferred['bytes'] / 1e6:.2f} Mo lus, {time.time() - started:.1f} s ==")
    summary["network"] = dict(transferred)
    json.dump(summary, open(a.json_out, "w"), indent=2, default=str)
    return 0


if __name__ == "__main__":
    sys.exit(main())
