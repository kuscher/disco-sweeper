package io.github.kuscher.discosweeper.game;

/**
 * A difficulty is a cell-size band and a ball density, not a fixed grid.
 *
 * <p>The classic {@code cols}/{@code rows}/{@code mines} survive as the nominal shape: they set
 * the density, they are what {@link #idealCellDp} is chosen to reproduce, and Custom still uses
 * them literally. Everywhere else the window decides the grid — see {@link Fit}.
 *
 * <p>{@code idealCellDp} is picked so that a board view of {@code cols * idealCellDp} by
 * {@code rows * idealCellDp} is a fixed point of the fit: ask for that much room and the solver
 * hands back exactly the classic board. That is what makes resizing the window to a difficulty
 * well defined.
 */
public enum Level {
    BEGINNER("Beginner", 9, 9, 10, -5090017, 28.0f, 76.0f, 68.0f),
    INTERMEDIATE("Intermediate", 16, 16, 40, -8758536, 24.0f, 52.0f, 38.0f),
    EXPERT("Expert", 30, 16, 99, -2614406, 20.0f, 40.0f, 28.0f),
    CUSTOM("Custom", 20, 14, 55, -14708870, 18.0f, 64.0f, 34.0f);

    /** A ceiling for reflowed boards, well above any real window; see {@link Fit}. */
    public static final int MAX_COLS = 200;
    public static final int MAX_ROWS = 160;
    public static final int MIN_MINES = 10;
    public static final int MIN_SIDE = 9;

    /** Custom keeps hand-set dimensions, so its sliders still mean what they always did. */
    public static final int CUSTOM_MAX_COLS = 30;
    public static final int CUSTOM_MAX_ROWS = 24;

    public final int cols;
    public final int mines;
    public final int rows;
    public final int seedColor;
    public final String title;
    public final float cellMinDp;
    public final float cellMaxDp;
    public final float idealCellDp;

    Level(String title, int cols, int rows, int mines, int seedColor,
          float cellMinDp, float cellMaxDp, float idealCellDp) {
        this.title = title;
        this.cols = cols;
        this.rows = rows;
        this.mines = mines;
        this.seedColor = seedColor;
        this.cellMinDp = cellMinDp;
        this.cellMaxDp = cellMaxDp;
        this.idealCellDp = idealCellDp;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    /** Custom is the one level that keeps its shape and lets the window letterbox it. */
    public boolean reflows() {
        return this != CUSTOM;
    }

    public int nominalCells() {
        return this.cols * this.rows;
    }

    public float density() {
        return this.mines / (float) nominalCells();
    }

    public String subtitle(int cols, int rows, int mines) {
        return cols + " × " + rows + " · " + mines + (mines == 1 ? " ball" : " balls");
    }

    public static int clampCols(int cols) {
        return Math.max(MIN_SIDE, Math.min(CUSTOM_MAX_COLS, cols));
    }

    public static int clampRows(int rows) {
        return Math.max(MIN_SIDE, Math.min(CUSTOM_MAX_ROWS, rows));
    }

    public static int capMines(int cols, int rows, int mines) {
        return Math.max(MIN_MINES, Math.min(Math.min(999, (cols - 1) * (rows - 1)), mines));
    }
}
