#!/usr/bin/env bash
# Compiles DbCopyPlugin.java against the JVM fakes in tests/android/fakes and runs DbCopyTest.
#   PLUGIN_SRC=/path/to/src/android tests/android/run.sh    # an older copy, to show a BEFORE
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
SRC="${PLUGIN_SRC:-$ROOT/src/android}"
CACHE="$HERE/.cache"; OUT="$CACHE/classes"; mkdir -p "$CACHE"
JSON_JAR="${ORG_JSON_JAR:-$(find "$HOME/.gradle/caches/modules-2/files-2.1/org.json" -name 'json-*.jar' 2>/dev/null | grep -v sources | head -1 || true)}"
if [ -z "$JSON_JAR" ]; then
  JSON_JAR="$CACHE/json-20240303.jar"
  [ -f "$JSON_JAR" ] || curl -sSfL -o "$JSON_JAR" https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar
fi
rm -rf "$OUT"; mkdir -p "$OUT"
javac -nowarn -encoding UTF-8 -cp "$JSON_JAR" -d "$OUT" $(find "$HERE/fakes" -name '*.java') "$SRC/DbCopyPlugin.java" \
  "$HERE"/src/CommunityPlugins/DbCopy/android/DbCopyTest.java
java -cp "$OUT:$JSON_JAR" CommunityPlugins.DbCopy.android.DbCopyTest
