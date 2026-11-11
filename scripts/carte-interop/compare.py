#!/usr/bin/env python3
"""Compare deux .pmtiles tuile par tuile (essai d'interopérabilité, tâche 7) : celui de l'extracteur Kotlin de
NetworkTact et celui de `pmtiles extract` sur la même édition et la même zone. Sort en erreur si l'ensemble des
tuiles ou le contenu d'une tuile diffère. Usage : compare.py notre.pmtiles reference.pmtiles"""
import hashlib
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "carte-sonde"))
import sonde  # noqa: E402


def tiles(path):
    data = open(path, "rb").read()
    header = sonde.parse_header(data[: sonde.HEADER_LEN])
    ic = header["internal_compression"]

    def directory(offset, length):
        return sonde.decode_directory(sonde.decompress(data[offset : offset + length], ic))

    result = {}
    stack = [directory(header["root_offset"], header["root_length"])]
    while stack:
        for tile_id, offset, length, run in stack.pop():
            if run == 0:
                stack.append(directory(header["leaf_offset"] + offset, length))
                continue
            blob = data[header["tiledata_offset"] + offset : header["tiledata_offset"] + offset + length]
            digest = hashlib.sha256(blob).hexdigest()
            for i in range(run):
                result[tile_id + i] = digest
    return header, result, len(data)


def main():
    ours_path, ref_path = sys.argv[1], sys.argv[2]
    ours_h, ours, ours_size = tiles(ours_path)
    ref_h, ref, ref_size = tiles(ref_path)
    print(f"{'':28} {'NetworkTact':>14} {'pmtiles extract':>16}")
    for key in ["addressed_tiles", "tile_entries", "tile_contents", "tiledata_length", "min_zoom", "max_zoom",
                "tile_type", "tile_compression", "internal_compression", "clustered", "leaf_length"]:
        print(f"{key:28} {ours_h[key]:>14} {ref_h[key]:>16}")
    print(f"{'taille du fichier':28} {ours_size:>14} {ref_size:>16}")
    print(f"{'bornes':28} {ours_h['bounds']} {ref_h['bounds']}")
    only_ours = sorted(set(ours) - set(ref))
    only_ref = sorted(set(ref) - set(ours))
    differ = sorted(t for t in set(ours) & set(ref) if ours[t] != ref[t])
    print(f"tuiles : {len(ours)} contre {len(ref)} ; seulement chez nous : {len(only_ours)} ; "
          f"seulement dans la référence : {len(only_ref)} ; contenu différent : {len(differ)}")
    for name, ids in [("seulement chez nous", only_ours), ("seulement dans la référence", only_ref), ("différentes", differ)]:
        if ids:
            print(f"  {name} (10 premières) : {ids[:10]}")
    if only_ours or only_ref or differ:
        print("DIFFÉRENCE : à examiner avant de continuer")
        return 1
    print("IDENTIQUES : même ensemble de tuiles, même contenu tuile par tuile")
    return 0


if __name__ == "__main__":
    sys.exit(main())
