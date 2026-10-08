#!/usr/bin/env node
// scripts/offline-map-assets.mjs
// Produces what the fdroid flavor ships so an imported .pmtiles map draws with no network at all:
// the Protomaps light and dark styles, their font glyphs and their sprites.
//
//   styles/{light,dark}.json   Protomaps basemap layers (@protomaps/basemaps, BSD-3-Clause), French labels,
//                              wrapped in a style document whose glyphs, sprite and source URL are left empty:
//                              the app fills them in with file:// and pmtiles://file:// paths at run time.
//   fonts/<font>/<range>.pbf   Noto Sans Regular, Medium and Italic (SIL OFL 1.1), from protomaps/basemaps-assets,
//                              limited to the Latin, Greek and Cyrillic ranges and the usual punctuation. A range
//                              that is not shipped is written as an empty glyph set by the app, so a rare character
//                              is left out of a label rather than stalling the label.
//   sprites/{light,dark}*      Protomaps sprites v4 (derived from the MIT-licensed tangrams/icons).
//   OFL.txt, VERSION
//
// Usage (needs Node 18+ and network access, run once by a developer, never by the app):
//   npm install --prefix /tmp/protomaps @protomaps/basemaps@5.7.2
//   node scripts/offline-map-assets.mjs /tmp/protomaps/node_modules [output-dir]
// output-dir defaults to androidApp/src/fdroid/assets/offline-map.

import { mkdir, rm, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

const BASEMAPS_VERSION = "5.7.2";
const ASSETS_BASE = "https://raw.githubusercontent.com/protomaps/basemaps-assets/main";
const FONTS = ["Noto Sans Regular", "Noto Sans Medium", "Noto Sans Italic"];
// Basic Latin to Cyrillic, Latin Extended Additional, general punctuation and letterlike symbols (°, №, ™).
const RANGES = [0, 256, 512, 768, 1024, 7680, 8192, 8448].map((start) => `${start}-${start + 255}`);
const FLAVORS = ["light", "dark"];
const SPRITE_FILES = FLAVORS.flatMap((f) => [`${f}.json`, `${f}.png`, `${f}@2x.json`, `${f}@2x.png`]);
const ATTRIBUTION = "© OpenStreetMap · Protomaps";

const [modulesDir, outArg] = process.argv.slice(2);
if (!modulesDir) {
  console.error("usage: node scripts/offline-map-assets.mjs <node_modules> [output-dir]");
  process.exit(1);
}
const outDir = resolve(outArg ?? "androidApp/src/fdroid/assets/offline-map");
const entry = join(resolve(modulesDir), "@protomaps/basemaps/dist/esm/index.js");
const { layers, namedFlavor } = await import(pathToFileURL(entry).href);

async function download(url) {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`${response.status} ${url}`);
  return Buffer.from(await response.arrayBuffer());
}

await rm(outDir, { recursive: true, force: true });

for (const flavor of FLAVORS) {
  const style = {
    version: 8,
    name: `NetworkTact offline ${flavor}`,
    glyphs: "",
    sprite: "",
    sources: { protomaps: { type: "vector", url: "", attribution: ATTRIBUTION } },
    layers: layers("protomaps", namedFlavor(flavor), { lang: "fr" }),
  };
  await mkdir(join(outDir, "styles"), { recursive: true });
  await writeFile(join(outDir, "styles", `${flavor}.json`), JSON.stringify(style));
}

for (const font of FONTS) {
  await mkdir(join(outDir, "fonts", font), { recursive: true });
  for (const range of RANGES) {
    const url = `${ASSETS_BASE}/fonts/${encodeURIComponent(font)}/${range}.pbf`;
    await writeFile(join(outDir, "fonts", font, `${range}.pbf`), await download(url));
  }
}

await mkdir(join(outDir, "sprites"), { recursive: true });
for (const file of SPRITE_FILES) {
  await writeFile(join(outDir, "sprites", file), await download(`${ASSETS_BASE}/sprites/v4/${file}`));
}

await writeFile(join(outDir, "OFL.txt"), await download(`${ASSETS_BASE}/fonts/OFL.txt`));
// Read by the app: a new value makes it reinstall these files on the next start.
await writeFile(join(outDir, "VERSION"), `protomaps-basemaps-${BASEMAPS_VERSION}-sprites-v4-1\n`);
console.log(`Wrote ${outDir}`);
