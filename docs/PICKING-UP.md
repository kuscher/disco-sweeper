# Picking up Disco Sweeper

The dev VM can restart mid-task. This file is the status and the next steps; keep it current and
commit at every milestone.

Disco Sweeper is a private, personal passion project, published as Fika Labs: its maker made it and owns it.

## Where things live
- Repo `~/disco-sweeper`, GitHub github.com/kuscher/disco-sweeper (public). Releases carry
  `DiscoSweeper.apk` (stable name for `releases/latest/download/DiscoSweeper.apk`) and
  `SHA256SUMS`.
- Releasing: push a tag `v<version>` and GitHub Actions builds, signs and publishes it: the APK as
  a GitHub release, the bundle as a draft on Google Play's closed-testing track ("Send for review"
  stays a button in the Play Console). No machine needs the key. Steps, the dry run and the route
  by hand (`tools/release.sh`, files in the gitignored `executables/release-<version>/`):
  [RELEASING.md](RELEASING.md).
- Release key: secrets of the GitHub environment `release` (the Play key: environment `play`),
  alias `discosweeper`, certificate SHA-256 `BA:4D:93:AA:…:4B:9A:79:17`. A new key since 2026-09-30 (the one Google Play uses too;
  GitHub installs of 1.0 must be uninstalled once), backed up privately, outside the repo. A machine that has it keeps
  it in `~/.config/discosweeper/keystore.jks` + `keystore.pass` (not in git).
- Where the code came from: the Sweeper module of the owner's private Disco app collection, moved
  here on its own with a new package, `io.github.kuscher.discosweeper`.

## 1.0 (2026-09-29): its own repo, reviewed and fixed
Found and fixed (details in CHANGELOG):
- crashes: the flood fill's stack overflowed (~4% of Beginner first clicks), and a click just
  outside the grid read row -1;
- the sound thread never finished (one core at 100% for the app's whole life), so sound never
  played;
- Back didn't close the drawer or the About sheet (Android 16 + targetSdk 36);
- the game was lost on recreation; the clock ran while minimised and used wall time;
- Hint trusted the player's marks and could point at a ball;
- no-guess, "?" and Custom's size had no controls (now in Settings and about);
- the first board was a 9×9 placeholder for 150 ms; left+right chords; hint carry-over; the
  status strip redrew forever after a win; level thumbnails; the themed icon; a11y labels;
  unused code removed.

Checks: `./ds sim` (board fit, generator, hints with wrong marks, save/restore); `tools/smoke.sh`
passed on an Android 17 laptop (real system Back included) and in CI's x86_64 Android 16 emulator,
where CI also launches the minified release build twice.

## Open
- Real-mouse checks by hand: right-click, middle-click, left+right chord, hover.
- Custom's LevelThumb shows a density-based pattern, not the exact grid.
- Split-screen on a phone or tablet: picking a difficulty relaunches the app (it can't tell
  split-screen from a desktop window), which leaves split-screen.
