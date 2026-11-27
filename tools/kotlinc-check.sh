#!/bin/bash
# kotlinc-check.sh — local type-check of the launcher's Kotlin sources.
#
# Why this exists: local Gradle builds are impossible in this sandbox
# (the other_tcp permission breaks Java TCP, including Gradle's daemon
# handshake — see BUILD_LOG.md). This script is the local verification
# lever instead: kotlinc 2.2.20 runs in-process (no daemon, no network)
# and type-checks every main source against android.jar plus the
# androidx classes extracted from the offline Gradle cache.
#
# What it proves: Kotlin syntax, types, and Compose usage in main
# sources. What it does NOT prove: resource merging, manifest merging,
# the Robolectric startup-gate test, or the dex guard — those run on CI
# (.github/workflows/android.yml), which is the authority. Note the
# version skew: kotlinc here is 2.2.20, the project builds with Kotlin
# 2.4.20 on CI; a clean check here is strong evidence, not a build.
#
# Convention this check enforces: Kotlin code must not reference
# app.launcher.R (no generated R class exists for kotlinc). Keep
# strings/constants in Kotlin; resources live in XML and the manifest.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="${GRADLE_USER_HOME:-$HOME/workspace/.gradle-launcher}/caches/modules-2/files-2.1"
LIBS="$HOME/workspace/.gradle-launcher/check-libs"
ANDROID_JAR="$HOME/workspace/android-sdk/platforms/android-37/android.jar"
KOT="$HOME/workspace/tools/kotlinc-2.2.20/kotlinc"

if [ ! -d "$CACHE" ]; then
  echo "ERROR: Gradle cache not found at $CACHE" >&2
  echo "Copy the Automap cache first: cp -r ~/workspace/automap/.gradle-home/caches ~/workspace/.gradle-launcher/caches" >&2
  exit 1
fi

# 1. Extract each cached AAR's classes.jar (highest version per artifact).
python3 - "$CACHE" "$LIBS" <<'EOF'
import glob, os, subprocess, sys
cache, libs = sys.argv[1], sys.argv[2]
best = {}
for aar in glob.glob(os.path.join(cache, "**", "*.aar"), recursive=True):
    parts = aar.split(os.sep)
    version, artifact = parts[-3], parts[-4]
    if artifact not in best or version > best[artifact][0]:
        best[artifact] = (version, aar)
for artifact, (version, aar) in sorted(best.items()):
    d = os.path.join(libs, f"{artifact}-{version}")
    if os.path.exists(os.path.join(d, "classes.jar")):
        continue
    os.makedirs(d, exist_ok=True)
    subprocess.run(["unzip", "-o", "-q", aar, "classes.jar", "-d", d],
                   check=False)
print(f"check-libs: {len(best)} artifacts")
EOF

# 2. Classpath: extracted classes + android.jar + the cache's plain jars
#    (lifecycle-common etc. ship as jars, not AARs). Kotlin jars are
#    excluded — kotlinc supplies its own stdlib/compiler — but only the
#    exact org.jetbrains.kotlin group: org.jetbrains.kotlinx (coroutines,
#    a declared dependency) must stay on the classpath. An earlier glob
#    ('*org.jetbrains.kotlin*') matched kotlinx too and hid coroutines;
#    fixed 2026-11-26 (Phase 1).
CP=$(find "$LIBS" -name classes.jar | tr '\n' ':')
CP="${CP}${ANDROID_JAR}:"
CP="${CP}$(find "$CACHE" -name '*.jar' ! -path '*org.jetbrains.kotlin/*' \
  ! -name '*sources*' ! -name '*javadoc*' | tr '\n' ':')"

# 3. Type-check every main Kotlin source.
SOURCES=$(find "$REPO/app/src/main/java" -name '*.kt')
if [ -z "$SOURCES" ]; then
  echo "ERROR: no Kotlin sources under $REPO/app/src/main/java" >&2
  exit 1
fi
mkdir -p "$REPO/build/kotlinc-check-out"
# shellcheck disable=SC2086
"$KOT/bin/kotlinc" -cp "$CP" -Xplugin="$KOT/lib/compose-compiler-plugin.jar" \
  -d "$REPO/build/kotlinc-check-out" $SOURCES
echo "kotlinc-check passed: $(echo "$SOURCES" | wc -l) source file(s)"
