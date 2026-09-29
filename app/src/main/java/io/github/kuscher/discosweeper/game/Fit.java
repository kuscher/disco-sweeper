package io.github.kuscher.discosweeper.game;

/**
 * Solves the board shape for the window instead of the other way round.
 *
 * <p>A desktop window cannot be pinned to the board — the platform ignores
 * {@code resizeableActivity} and the aspect-ratio attributes on large screens, and there is no
 * API to resize your own window — so the board has to follow. A level supplies a cell-size band
 * and a ball density; the space available supplies everything else.
 *
 * <p>Everything here is one unit. The app passes pixels; a simulation can pass dp, or anything
 * else, and get the same answer. No Android types on purpose: this is the part worth checking by
 * running it over a few thousand window sizes rather than by building an APK.
 */
public final class Fit {
    /** Nine a side is the smallest board that still plays like the game. */
    public static final int MIN_SIDE = 9;

    private static final int SETTLE_PASSES = 3;

    public final int cols;
    public final int rows;
    public final int mines;
    public final float cell;

    private Fit(int cols, int rows, int mines, float cell) {
        this.cols = cols;
        this.rows = rows;
        this.mines = mines;
        this.cell = cell;
    }

    public static Fit of(float width, float height, Level level, float cellMin, float cellMax) {
        return of(width, height, level.nominalCells(), level.density(), cellMin, cellMax);
    }

    public static Fit of(float width, float height, int nominalCells, float density,
                         float cellMin, float cellMax) {
        if (width <= 0.0f || height <= 0.0f || nominalCells <= 0) {
            return new Fit(MIN_SIDE, MIN_SIDE, Level.capMines(MIN_SIDE, MIN_SIDE, 10), cellMin);
        }
        // Start from the cell size that would give this level its usual number of cells in this
        // much room, then let the band decide whether a bigger window grows the cells or the grid.
        float cell = clamp((float) Math.sqrt((width * height) / nominalCells), cellMin, cellMax);
        int cols = MIN_SIDE;
        int rows = MIN_SIDE;
        // The nine-a-side floor moves the cell, and the cell moves the counts, so settle rather
        // than solve once: a single pass leaves a wide window with too few columns.
        for (int pass = 0; pass < SETTLE_PASSES; pass++) {
            cols = clamp(Math.round(width / cell), MIN_SIDE, Level.MAX_COLS);
            rows = clamp(Math.round(height / cell), MIN_SIDE, Level.MAX_ROWS);
            cell = Math.min(width / cols, height / rows);
        }
        int mines = Level.capMines(cols, rows, Math.round(density * cols * rows));
        return new Fit(cols, rows, mines, cell);
    }

    public boolean matches(Board board) {
        return board != null && board.cols == cols && board.rows == rows;
    }

    private static float clamp(float value, float lo, float hi) {
        return value < lo ? lo : (value > hi ? hi : value);
    }

    private static int clamp(int value, int lo, int hi) {
        return value < lo ? lo : (value > hi ? hi : value);
    }
}
