#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Builds and checks the release files for the version in app/build.gradle.kts, in
# executables/release-<version>/ (gitignored):
#
#   DiscoSweeper.apk   signed with the release key in ~/.config/discosweeper (never in git);
#                      a stable name, so releases/latest/download/DiscoSweeper.apk always works
#   SHA256SUMS
#   notes.md           the release notes: the version's CHANGELOG section, install, verify
#
#   tools/release.sh [--publish]
#
# --publish tags v<version>, pushes the tag and creates the GitHub release with those files.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
REPO=kuscher/disco-sweeper
# The release key's certificate. Every release must carry it, or updates won't install over it.
RELEASE_CERT=ba4d93aad05548d8025ede97b4c2d63e1cd6cb314a99f5c10696229d4b9a7917
die() { echo "release: $*" >&2; exit 1; }

V=$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)
CODE=$(sed -n 's/^ *versionCode = \([0-9]*\)/\1/p' app/build.gradle.kts)
[[ -n $V && -n $CODE ]] || die "no versionName/versionCode in app/build.gradle.kts"
git diff --quiet HEAD || die "the repo has uncommitted changes"
[[ -f $HOME/.config/discosweeper/keystore.jks ]] || die "no release key in ~/.config/discosweeper"

./gradlew :app:assembleRelease --console=plain -q
apk=app/build/outputs/apk/release/app-release.apk
[[ -f $apk ]] || die "no signed $apk (is the key's password in ~/.config/discosweeper/keystore.pass?)"

BT=$(ls -d "$HOME"/Android/Sdk/build-tools/* | sort -V | tail -1)
cert=$("$BT/apksigner" verify --print-certs "$apk" | sed -n 's/.*SHA-256 digest: //p' | head -1)
[[ $cert == "$RELEASE_CERT" ]] || die "the APK isn't signed with the release key ($cert)"
badging=$("$BT/aapt2" dump badging "$apk" 2>/dev/null | head -1)
[[ $badging == *"name='io.github.kuscher.discosweeper'"*"versionCode='$CODE'"*"versionName='$V'"* ]] ||
  die "unexpected package or version: $badging"
perms=$("$BT/aapt2" dump permissions "$apk" 2>/dev/null | grep -c "uses-permission" || true)
[[ $perms == 0 ]] || die "the release asks for permissions; it's meant to ask for none"
unzip -p "$apk" 'classes*.dex' | grep -aq DebugHooks && die "the debug test hooks are in the release APK"

OUT=executables/release-$V
rm -rf "$OUT"
mkdir -p "$OUT"
cp "$apk" "$OUT/DiscoSweeper.apk"
(cd "$OUT" && sha256sum DiscoSweeper.apk > SHA256SUMS)

notes=$OUT/notes.md
{
  echo "**Disco Sweeper $V**: Minesweeper with disco balls, made for Android laptops and desktop windows."
  echo
  awk -v v="$V" '$0 ~ "^## " v "( |$)" {on=1; next} /^## / {on=0} on' CHANGELOG.md | sed '/./,$!d'
  cat <<EOF

### Install
1. On your device (Android 12 or later), download **DiscoSweeper.apk** below and open it from your
   browser's downloads or your files app. If Android asks, allow that app to install apps, then tap
   **Install**.
2. Open **Disco Sweeper** from your apps.

It asks for no permissions at all. The full guide is in the
[README](https://github.com/$REPO#readme).

### Verify
- SHA-256 of \`DiscoSweeper.apk\`: see \`SHA256SUMS\`.
- Signing certificate SHA-256: \`$(echo "$cert" | tr a-f A-F | sed 's/../&:/g; s/:$//')\`
  (\`apksigner verify --print-certs DiscoSweeper.apk\`). Every future release is signed with the
  same key, so updates install over this one.
EOF
} > "$notes"
grep -q "^### " "$notes" && [[ $(awk -v v="$V" '$0 ~ "^## " v "( |$)"' CHANGELOG.md) ]] || die "CHANGELOG.md has no section for $V"
ls -la "$OUT"
cat "$OUT/SHA256SUMS"

[[ ${1:-} == --publish ]] || exit 0
git tag -a "v$V" -m "Disco Sweeper $V"
git push -q origin "v$V"
gh release create "v$V" --repo "$REPO" --title "Disco Sweeper $V" --notes-file "$notes" \
  "$OUT/DiscoSweeper.apk" "$OUT/SHA256SUMS"
