package io.github.kuscher.discosweeper.game;

import android.content.Context;
import android.content.SharedPreferences;

/* JADX INFO: loaded from: classes.dex */
public final class Settings {
    private static final String BEST = "best_";
    private static final String C_COLS = "custom_cols";
    private static final String C_MINES = "custom_mines";
    private static final String C_ROWS = "custom_rows";
    private static final String FILE = "disco_sweeper";
    private static final String LEVEL = "level";
    private static final String NO_GUESS = "no_guess";
    private static final String SOUND = "sound";
    private static final String UNSURE = "allow_unsure";

    private Settings() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("disco_sweeper", 0);
    }

    public static Level level(Context context) {
        try {
            return Level.valueOf(prefs(context).getString("level", Level.INTERMEDIATE.name()));
        } catch (IllegalArgumentException e) {
            return Level.INTERMEDIATE;
        }
    }

    public static void setLevel(Context context, Level level) {
        prefs(context).edit().putString("level", level.name()).apply();
    }

    public static int customCols(Context context) {
        return Level.clampCols(prefs(context).getInt("custom_cols", 20));
    }

    public static int customRows(Context context) {
        return Level.clampRows(prefs(context).getInt("custom_rows", 14));
    }

    public static int customMines(Context context) {
        return Level.capMines(customCols(context), customRows(context), prefs(context).getInt("custom_mines", 55));
    }

    public static void setCustom(Context context, int i, int i2, int i3) {
        int iClampCols = Level.clampCols(i);
        int iClampRows = Level.clampRows(i2);
        prefs(context).edit().putInt("custom_cols", iClampCols).putInt("custom_rows", iClampRows).putInt("custom_mines", Level.capMines(iClampCols, iClampRows, i3)).apply();
    }

    public static boolean allowUnsure(Context context) {
        return prefs(context).getBoolean("allow_unsure", true);
    }

    public static void setAllowUnsure(Context context, boolean z) {
        prefs(context).edit().putBoolean("allow_unsure", z).apply();
    }

    public static boolean noGuess(Context context) {
        return prefs(context).getBoolean("no_guess", false);
    }

    public static void setNoGuess(Context context, boolean z) {
        prefs(context).edit().putBoolean("no_guess", z).apply();
    }

    public static boolean sound(Context context) {
        return prefs(context).getBoolean("sound", false);
    }

    public static void setSound(Context context, boolean z) {
        prefs(context).edit().putBoolean("sound", z).apply();
    }

    /**
     * Keyed by the board actually played, not just the difficulty. Now that the window picks the
     * grid, a time is only comparable against another time on the same shape — Custom already
     * worked this way, so the other levels simply adopt its convention.
     */
    private static String bestKey(Level level, int cols, int rows, int mines) {
        return "best_" + level.name() + "_" + cols + "x" + rows + "x" + mines;
    }

    public static int best(Context context, Level level, int cols, int rows, int mines) {
        return prefs(context).getInt(bestKey(level, cols, rows, mines), 0);
    }

    public static boolean recordWin(Context context, Level level, int cols, int rows, int mines, int seconds) {
        int best = best(context, level, cols, rows, mines);
        if (best != 0 && seconds >= best) {
            return false;
        }
        prefs(context).edit().putInt(bestKey(level, cols, rows, mines), seconds).apply();
        return true;
    }
}
