# Picking up Disco Sweeper

The dev VM can restart mid-task. This file is the status and the next steps; keep it current and
commit at every milestone.

Disco Sweeper is Alexander Kuscher's private, personal passion project: he made it and owns it.

## Where things live
- Repo `~/disco-sweeper`, GitHub github.com/kuscher/disco-sweeper (public). Releases carry
  `DiscoSweeper.apk` (stable name for `releases/latest/download/DiscoSweeper.apk`) and
  `SHA256SUMS`, built by `tools/release.sh`. Release files are kept in
  `executables/release-<version>/` (gitignored).
- Release key `~/.config/discosweeper/keystore.jks` + `keystore.pass` (not in git), alias
  `discosweeper`, certificate SHA-256 `A5:32:63:72:…:8F:D7:58:60`. Backed up to the owner's a private folder, with a README; the password is for the owner's password
  manager.
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
