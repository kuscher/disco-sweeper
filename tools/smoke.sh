#!/usr/bin/env bash
# SPDX-License-Identifier: MIT
# Plays Disco Sweeper through its debug hooks (./ds debug) and fails on a crash or a wrong state.
# Needs the debug build installed on a device or emulator that nobody is using at the time:
#   ./ds install && tools/smoke.sh [OUTDIR]
# CI runs it in an emulator (.github/workflows/ci.yml). Renders of the app land in OUTDIR.
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")/.."
OUT=${1:-build/smoke}; mkdir -p "$OUT"
export DBG_WAIT=${DBG_WAIT:-1.2}
fail() { echo "FAIL: $*"; ./ds logs 80 || true; exit 1; }
d() { local r; r=$(./ds debug "$@") || true; echo "    $* -> ${r:-(no answer)}" >&2; printf '%s' "$r"; }
has() { [[ $(d dump) == *"$1"* ]]; }
# Only crashes that happen during this run count (the crash log keeps older ones).
BASE=$(./ds crashes | grep -c "Process:" || true)
nocrash() { if (( $(./ds crashes | grep -c "Process:" || true) > BASE )); then ./ds crashes | tail -30; fail "crashed: $1"; fi; }
# Back has to come from the system; ./ds back sends it only while this app has focus.
back() { ./ds back || fail "Back wasn't sent: Disco Sweeper doesn't have focus"; }
shot() { d shot "$1" >/dev/null; ./ds pull "$1" "$OUT/$1.png" >/dev/null; }

./ds stop || true
./ds launch; sleep 5
has status=READY || fail "no fresh board at launch"
shot start

echo "1. A click just outside the grid does nothing (it used to crash)"
d edge 3 top >/dev/null; d edge 3 bottom >/dev/null; sleep 0.5
nocrash "click outside the grid"; has status=READY || fail "a click outside the grid changed the board"

echo "2. The first click starts the game; a right click marks a tile, again takes it off"
read -r C R <<<"$(d pick covered)"; d cell "$C" "$R" left >/dev/null; sleep 0.6
has status=PLAYING || has status=WON || fail "the first click didn't start the game"
left=$(d dump | sed -n 's/.* left=\([0-9-]*\).*/\1/p')
read -r C R <<<"$(d pick covered)"; d cell "$C" "$R" right >/dev/null; sleep 0.4
has "left=$((left - 1)) " || fail "right click didn't mark"
d cell "$C" "$R" right >/dev/null; d cell "$C" "$R" right >/dev/null; sleep 0.4   # to "?", then clear
has "left=$left " || fail "marking didn't cycle back"
read -r C R <<<"$(d pick covered)"; d cell "$C" "$R" hold >/dev/null; sleep 0.4
has "left=$((left - 1)) " || fail "press and hold didn't mark"
shot playing

echo "3. Recreating the activity (font or display size change) keeps the game"
before=$(d dump | sed 's/ elapsed=[0-9]*ms//'); d recreate >/dev/null; sleep 3
after=$(d dump | sed 's/ elapsed=[0-9]*ms//')
[[ "$before" == "$after" ]] || fail "recreate lost the game: [$before] vs [$after]"
nocrash recreate

echo "4. Back closes the drawer and the About sheet, and nothing else"
d click "Choose a difficulty" >/dev/null; sleep 0.8; has drawer=true || fail "the drawer didn't open"
shot drawer
back; sleep 0.8; has drawer=false || fail "Back didn't close the drawer"
has status=PLAYING || fail "Back closed more than the drawer"
d click "Choose a difficulty" >/dev/null; sleep 0.8; d click "Settings and about" >/dev/null; sleep 0.8
has about=true || fail "the About sheet didn't open"
shot about
back; sleep 0.8; has about=false || fail "Back didn't close the About sheet"

echo "5. Settings: no-guess, ? marks, sound, and Custom's size"
d click "Choose a difficulty" >/dev/null; sleep 0.8; d click "Settings and about" >/dev/null; sleep 0.8
d click "No-guess boards" >/dev/null; sleep 0.3; has noGuess=true || fail "the no-guess toggle"
d click "” marks" >/dev/null; sleep 0.3; has unsure=false || fail "the ? toggle"
d click "Ticks, chimes" >/dev/null || true
d at Columns 0.98 0.5 >/dev/null; d at Rows 0.02 0.5 >/dev/null; sleep 0.4
has "custom=30x9/" || fail "Custom's sliders"
shot settings
back; sleep 0.6

echo "6. Custom deals its own shape"
d click "Choose a difficulty" >/dev/null; sleep 0.8; d click "Custom, " >/dev/null; sleep 1.5
has "board=30x9/" || fail "Custom didn't deal 30x9"
has status=READY || fail "Custom's new board isn't fresh"

echo "7. A whole game: win, then lose"
DBG_WAIT=15 d solve left >/dev/null; sleep 1
has status=WON || fail "couldn't win a no-guess board by clicking"
shot won
d click "New game" >/dev/null; sleep 0.8; has status=READY || fail "New game"
read -r C R <<<"$(d pick covered)"; d cell "$C" "$R" left >/dev/null; sleep 0.5
read -r C R <<<"$(d pick ball)"
if [[ "$C" != none && "$C" != "no" ]]; then d cell "$C" "$R" left >/dev/null; sleep 1; has status=LOST || fail "opening a ball didn't lose"; fi
shot lost
sleep 4; nocrash "win and loss"

echo "8. Back to Intermediate, and one more check that nothing crashed"
d click "Choose a difficulty" >/dev/null; sleep 0.8; d click "Intermediate, " >/dev/null; sleep 3
has level=INTERMEDIATE || fail "picking Intermediate"
nocrash "the whole run"
echo "smoke test passed"
