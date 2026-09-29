# Changelog

## 1.0 (2026-09-29)

The first public release of Disco Sweeper, as its own app. Compared with earlier builds:

- **New settings** in *Settings and about*: **no-guess boards** (every board can be cleared by
  logic alone; the game tells you when a Custom board is too crowded for one), **“?” marks** on or
  off, and **Custom's size**: 9 to 30 columns, 9 to 24 rows, 8% to 35% disco balls.
- **Hint never lies:** it uses only what the numbers prove and never trusts your marks, so a wrong
  mark can't send you onto a ball. It even points out a mark that's wrong.
- **Your game survives** a change of font or display size, and Android closing the app in the
  background. The clock stops while the window is minimised.
- **Back** closes the difficulty drawer and the About sheet (it used to leave them open).
- Fixed: a crash when the first click opened a big empty area (about 1 game in 25 on Beginner),
  and a crash when clicking just above or below the board.
- Fixed: sound never played, and a background thread kept one CPU core busy the whole time the app
  was open.
- Left+right clicks clear around a number however the two buttons go down; the first board is dealt
  for your window straight away; the level pictures show their disco balls; the themed icon shows
  the mirrorball's facets; buttons are labelled for screen readers.
- About 100 KB, no permissions. New package `io.github.kuscher.discosweeper`.
