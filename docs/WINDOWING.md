# Disco Sweeper in a desktop window

Disco Sweeper used to draw a fixed grid (9×9, 16×16, 30×16) at one square cell size,
centred in whatever rectangle it was given. On a phone that is invisible. In a desktop
window it was most of the screen: maximised on a 1706×1066dp display, an Intermediate
board was a square island with **half the window empty**.

The board now follows the window, and picking a difficulty by hand resizes the window to
suit. This is what that means and why it is built the way it is.

## Why the board follows the window and not the other way round

The obvious fix — pin the window to the board — is the one the platform has deliberately
removed. Measured on an Android 17 (API 37) desktop device, 2560×1600 at 240dpi =
1706×1066dp, with `app` at `targetSdk 36`:

| Lever | Status |
|---|---|
| `android:resizeableActivity` | **Ignored** on sw≥600dp for targetSdk 36+ |
| `android:screenOrientation`, `setRequestedOrientation()` | **Ignored**, same conditions |
| `android:minAspectRatio` / `maxAspectRatio` | **Ignored**, same conditions |
| `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` | Opt-out, **removed at API 37** — this device |
| `android:appCategory="game"` | Still exempts, but buys system letterboxing — the thing we are removing |
| `<layout defaultWidth/defaultHeight/gravity>` | **Honoured** at freeform launch |
| `<layout minWidth/minHeight>` | **Honoured here**, though documented as ignored |
| Resize your own *running* window | **No public API** |

`ActivityOptions.setLaunchBounds()` is the one thing left, and it applies only to a
launch. That is the whole shape of the design: the window belongs to the user, the board
adapts to it, and the only moment the app may size its own window is when it is starting.

### The minimum size is real, but do not lean on it

The docs say `<layout minWidth>` is ignored on large screens for targetSdk 36+. On this
device it is not: `am task resize` to 600×500px comes back as

```
mBounds=Rect(300, 200 - 1260, 860)      # 960x660px
mLastReportedConfiguration={ ... sw440dp w640dp h440dp 240dpi ... }
```

960×660px at 1.5× is exactly the declared `minWidth="640dp" minHeight="440dp"`. Requests
above the floor pass through untouched. Read the *window's* configuration, not the Task
dump — the Task records the requested bounds and will happily show you a size the window
never took.

The reflow does not depend on that floor, which is the point: `CELL_MIN` and the
nine-a-side rule mean the smallest board is 9×9 at 28dp = 252dp, inside even the
386×352dp system minimum. That also fixes a latent clipping bug — `BoardView` used to
clamp the cell *up* to 14dp, so a 30-column board in a small window rendered 420dp wide
inside a 338dp view and was cut off rather than letterboxed.

## The model

`Level` is now a cell-size band and a ball density. The classic dimensions survive as the
nominal shape: they set the density, Custom still uses them literally, and `idealCellDp`
is chosen to reproduce them.

| Level | Classic | Density | Cell band | Ideal cell |
|---|---|---|---|---|
| Beginner | 9×9 / 10 | 12.3% | 28–76dp | 68dp |
| Intermediate | 16×16 / 40 | 15.6% | 24–52dp | 38dp |
| Expert | 30×16 / 99 | 20.6% | 20–40dp | 28dp |
| Custom | hand-set | — | — | — |

`Fit` does the arithmetic, in whatever unit the caller passes:

```java
float cell = clamp(sqrt(uw * uh / N), CELL_MIN, CELL_MAX);
for (int pass = 0; pass < 3; pass++) {
    cols = clamp(round(uw / cell), MIN_SIDE, MAX_COLS);
    rows = clamp(round(uh / cell), MIN_SIDE, MAX_ROWS);
    cell = min(uw / cols, uh / rows);      // re-fit: span one axis exactly
}
mines = capMines(cols, rows, round(density * cols * rows));
```

The three-pass settle is not decoration. Without it a wide window drives `rows` down to
the nine-row floor, the re-fit shrinks the cell to suit, and `cols` is left too small —
Beginner at 1000×533dp lands on 14×9 and wastes 32%. Re-deriving the counts from the
settled cell closes it to 1.2%.

Maximised needs no special case, only a clamp. Left unbounded, a maximised Beginner wants
130dp cells for its 81 squares; `CELL_MAX` is where the window stops growing the cells and
starts adding grid. Making that cap *per difficulty* is also what keeps the levels
distinct when maximised — under one shared cap, Beginner and Intermediate collapse onto
the same 23×12 grid and differ only in ball count.

`Fit`, `Level`, `Board` and `Solver` touch nothing but `java.util`, so the model is worth
checking by running it rather than by building an APK. `app/sim/Sim.java` — kept
outside `src/main/java` so it never reaches the APK — sweeps 320–3000dp by
300–2000dp across the three reflowing levels — **279,207 fits, none bad**: nothing
overflows its view, no board is smaller than nine a side or over the caps, no ball count
is degenerate. Worst dead space away from the nine-a-side floor is 4.76%, at an extreme
2357×538dp window.

## Picking a difficulty resizes the window

The one moment the app may size its own window is a launch, so selecting a difficulty by
hand relaunches into bounds cut for that level. The target is chosen to be a **fixed point
of `Fit`**: land in that window and the solver hands back exactly the classic board, so
the resize and the reflow agree instead of fighting.

| Level | Window | Board it reflows to |
|---|---|---|
| Beginner | 660×772dp | 9×9 / 10 |
| Intermediate | 656×768dp | 16×16 / 40 |
| Expert | 888×608dp | 30×16 / 99 |

The window frame is measured rather than assumed — in desktop windowing the caption bar sits inside
the window bounds and its height is not ours to guess, so the target is
`grid + BoardView.gridInset() + (window − boardView)`.

Two cases are deliberately left alone. **Maximised or fullscreen** is the user having
already said what size they want, so the board reflows into it instead — a maximised
Beginner is a big 22×13 board, which is the right answer. And a window already within
8dp of the target is not disturbed.

### The relaunch hits an OS bug

Relaunching into new launch bounds lays the activity out at the new size — uiautomator
reports the full rect — but the window keeps the **surface** it had. The bottom of the
board is simply never composited, and you get a board with its last rows missing over
whatever is behind the window.

It is the platform, not the app. Any window-manager-driven bounds change knocks it loose:
a 1px `am task resize`, or just grabbing the window header and nudging it. Neither
`Window.setAttributes`, `requestApplyInsets`, `requestLayout`, nor
`setLayout(MATCH_PARENT, MATCH_PARENT)` fixes it — an app cannot re-issue its own surface.

What does work is making the relaunch a genuine cold start, which the platform sizes
correctly: tear the task down with `finishAndRemoveTask()`, then start from the
application context a beat later. Verified at 0px shortfall for all three levels, against
443px unpainted for the in-task relaunch.

If a future platform release fixes the surface bug, `RELAUNCH_DELAY_MS` and the
`finishAndRemoveTask()` dance can collapse back to a plain `startActivity` with
`FLAG_ACTIVITY_CLEAR_TASK`.

## Reflow only while the board is uncommitted

Once a cell is open or a ball is marked, the shape belongs to that game: resizing then
only re-fits the cell, exactly as it always did. Dragging a window edge never throws
progress away.

Before the first move it is free to reshape, and free in the literal sense —
`new Board(...)` only allocates, and `place()` (with `Solver.solvableFrom` inside it) runs
on the first click, so a board discarded before it is touched has consumed no solver time.
Resizing fires continuously through a drag, so the rebuild settles for
`REFLOW_SETTLE_MS` first rather than generating a board per frame.

## What changed

- **`Fit`** (new) — the solver above. Pure arithmetic, no Android types.
- **`Level`** — cell band, density, `idealCellDp`, `reflows()`. `MAX_COLS`/`MAX_ROWS` are
  now a ceiling for reflowed boards (200×160); the Custom slider range is unchanged at
  30×24.
- **`BoardView`** — `usableWidth()`/`usableHeight()`/`gridInset()` expose the geometry;
  `setCellCeiling()` replaces the hard 14–64dp clamp, and the floor is gone so an existing
  board always fits its view.
- **`MainActivity`** — `newGame()` builds from the fit, `reflowIfUncommitted()` on a
  settled resize, `resizeWindowTo()` on manual selection.
- **`Settings`** — best times are keyed by the board played, not the level, following the
  convention Custom already used. Times recorded before this change were all set on the
  level's classic grid, so `migrateLegacyBest` files them under it rather than discarding
  them.
- The drawer advertises the live shape each difficulty would take in the current window,
  and the About sheet shows each best time against the board it belongs to.

## Still open

**How far Expert should scale.** Maximised Expert is 882 cells and 182 balls, roughly
twice classic. That is either the point of maximising or far too long a game; capping
nominal cells would trade dead space back for a bounded session.

**Custom** stays the one explicit-dimensions mode and the only level that letterboxes,
which keeps a route to a literal classic 30×16 and gives the sliders something to mean.

**`minWidth`/`minHeight`** at 640×440dp costs nothing on desktop, but on a phone —
sw<600dp, where the attribute is definitely honoured — it makes the app ineligible for
split-screen entirely. Now that the board reflows, that restriction has no purpose left.

## Sources

- [App orientation, aspect ratio, and resizability](https://developer.android.com/develop/adaptive-apps/guides/app-orientation-aspect-ratio-resizability)
- [Support desktop windowing](https://developer.android.com/develop/adaptive-apps/guides/support-desktop-windowing)
- [Support multi-window mode](https://developer.android.com/develop/adaptive-apps/guides/support-multi-window-mode)
- [`<layout>` manifest element](https://developer.android.com/guide/topics/manifest/layout-element)
- [Behavior changes: apps targeting Android 16](https://developer.android.com/about/versions/16/behavior-changes-16#adaptive-layouts)
- [Behavior changes: apps targeting Android 17](https://developer.android.com/about/versions/17/behavior-changes-17)
