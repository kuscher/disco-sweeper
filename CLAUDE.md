# CLAUDE.md

Notes for working on Disco Sweeper. [README.md](README.md) says what the app does; this is what to
know before changing code. Status and next steps: [docs/PICKING-UP.md](docs/PICKING-UP.md).

## House style: framework Java only

No Kotlin, no Compose, no AndroidX, no third-party libraries, no layout XML. Every surface is drawn
in code against the small design system in `Tokens` (a tonal palette generated from one seed
colour, light and dark) and `Ui` (dp/sp, rounded drawables, ripples, typefaces, clamp/lerp/mix).
Don't add a dependency to solve something the framework already does. The release APK is ~100 KB.

The code was recovered from a release APK by decompiling it, so older code has decompiler names
(`var3`, `n2`, `iArr`, `lambda$buildUi$3`) and few comments. Rename as you touch things, as part of
a change you're making anyway; a repo-wide rename would be a big unreviewable diff.

## Layout

```
app/src/main/java/io/github/kuscher/discosweeper/
  MainActivity      the whole UI: header, board, drawer, Settings and about sheet
  game/             the model, plain Java (java.util + java.io only): Board, Solver, Fit, Level, Settings
  view/             BoardView (the grid and input), StatusStrip (counter, reset ball, clock),
                    DiscoOverlay (win/loss party), LevelThumb, Glyphs
  ui/               PillButton, NavItem, Toggle, StepSlider, Icons
  audio/Sfx         sounds synthesised into static AudioTracks on a background thread
app/src/debug/      test hooks for ./ds debug (never in a release)
app/sim/            headless checks: Sim (board fit), SolverSim (generator, hints, save/restore)
tools/              icon.py (README icon from the launcher vectors), smoke.sh, release.sh
```

## Build and check

```sh
./ds build [release]    # release is signed from ~/.config/discosweeper when the key is there
./ds sim                # ~30 s, no device and no Gradle: run it after touching game/
./ds run                # build, install, launch on the test device (VSCodeBook's adb socket)
tools/smoke.sh          # plays a whole game through the test hooks; CI runs it in an emulator
```

The model is plain Java on purpose: `Fit`, `Level`, `Board` and `Solver` compile with `javac` alone,
so a tuning change is checked in seconds, not by a build-install-squint loop.

## Driving the device

The owner uses the same test device. Before launching or clicking anything, check they aren't
mid-task (`dumpsys power` → `lastUserActivityTime`, `dumpsys window` → `mCurrentFocus`).

- `./ds debug …` (debug builds) injects input **as the app's own uid**, so Android refuses any event
  whose target is another app's window: a hook can only ever click Disco Sweeper. It also renders
  screenshots from the app's own views (`./ds debug shot NAME`, then `./ds pull NAME`).
- `./ds shot` is a real screen capture cropped to the window, so it also captures whatever covers
  the window. Only use it when the device is free.
- Never inject raw `adb shell input` taps or keys while someone is using the device.

## The board follows the window

Read [docs/WINDOWING.md](docs/WINDOWING.md) before changing any of it. In short: the platform
ignores `resizeableActivity` and the aspect-ratio attributes on large screens from targetSdk 36,
and no API resizes a running window. So a `Level` is a cell-size band and a ball density, and `Fit`
turns the space available into `cols × rows × mines`. Its three-pass settle is load-bearing.
`Level.idealCellDp` makes picking a difficulty relaunch into a window that is a **fixed point of
`Fit`** (the classic 9×9, 16×16, 30×16); `Sim` checks that, plus a quarter-million window sizes.

The difficulty relaunch tears its own task down (`finishAndRemoveTask`, then a start from the
application context 220 ms later) because an in-task relaunch keeps the old surface and leaves the
bottom rows uncomposited. The board only reshapes while nothing has been committed to it.

targetSdk stays 36: the windowing behaviour was measured against it.

## The model

- `Board.flood` opens a cell **when it is queued**, so each cell is queued once and the stack never
  exceeds the board. Counting at pop time overflowed (a crash on ~4% of Beginner first clicks).
  `Solver.open` is the same flood on the solver's copy.
- The clock is `System.nanoTime()` banked across `pause()`/`resume()` (onStop/onStart).
- `Board.save()`/`restore()` carry a game across recreation. Bump `SAVE_VERSION` if the format
  changes; an unreadable save just means a new game.
- `Solver.deduce` is single-constraint plus subset deduction. `hint()` ignores the player's marks
  and feeds back only balls the numbers prove, and zeros count as constraints (a zero next to a
  mark proves the mark wrong). `solvableFrom` powers no-guess generation: up to 500 boards or
  600 ms on the first click, then `noGuessMissed()` tells the player. Past about 22% balls no-guess
  boards get rare; that's the Custom slider's 35% end.

## UI notes

- Back: from Android 16, apps targeting 36 don't get `onBackPressed`, so an
  `OnBackInvokedCallback` is registered only while the drawer or the About sheet is open.
- `density` is deliberately not in `configChanges`: every size is fixed in px when the UI is built,
  so a density change recreates the activity (the game is saved) instead of keeping stale sizes.
- Drawn controls (`PillButton`, `NavItem`, `Toggle`, `StepSlider`) set content descriptions; the
  test hooks find views by them too.
- `StatusStrip` ticks every 200 ms only while a game's clock runs, and the reset ball stops
  spinning 8 s after a win.

## Releasing

`tools/release.sh` builds the signed APK and checks the certificate, package, version, that there
are no permissions and no test hooks; `--publish` tags and creates the GitHub release with
`DiscoSweeper.apk` (a stable name for `releases/latest/download/`) and `SHA256SUMS`. Bump
`versionCode`/`versionName` in `app/build.gradle.kts` and add a CHANGELOG section first. The key
is in `~/.config/discosweeper` (never in git) and backed up privately; see docs/PICKING-UP.md.

Commit as the global git identity (the noreply address); never pass `-c user.email`.
