#!/usr/bin/env bash
# Télécharge le compilateur Kotlin et les bibliothèques (Maven Central), puis compile l'extracteur de
# feature/map/.../offline/extract avec ExtractCli.kt dans extract-cli.jar. Usage : build.sh DOSSIER_DE_TRAVAIL
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
repo=$(cd "$here/../.." && pwd)
work=$1
mkdir -p "$work/lib"
kotlin_version=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$repo/gradle/libs.versions.toml")
if [ ! -x "$work/kotlinc/bin/kotlinc" ]; then
  curl -sSfL --retry 3 -o "$work/kotlinc.zip" \
    "https://github.com/JetBrains/kotlin/releases/download/v$kotlin_version/kotlin-compiler-$kotlin_version.zip"
  unzip -q -o "$work/kotlinc.zip" -d "$work"
fi
grep -v '^#' "$here/jars.txt" | while read -r path; do
  [ -f "$work/lib/$(basename "$path")" ] || curl -sSf --retry 5 --retry-delay 5 -o "$work/lib/$(basename "$path")" \
    "https://repo1.maven.org/maven2/$path"
done
classpath=$(ls "$work"/lib/*.jar | tr '\n' ':')
src="$repo/feature/map/src/commonMain/kotlin/org/meshtastic/feature/map/offline"
"$work/kotlinc/bin/kotlinc" -nowarn \
  -Xplugin="$work/kotlinc/lib/kotlinx-serialization-compiler-plugin.jar" \
  -cp "$classpath" -d "$work/extract-cli.jar" \
  "$src"/extract/*.kt "$src/PmTilesHeader.kt" "$here/ExtractCli.kt"
echo "$classpath$work/extract-cli.jar:$work/kotlinc/lib/kotlin-stdlib.jar" > "$work/classpath.txt"
