import io.github.kuscher.discosweeper.game.Fit;
import io.github.kuscher.discosweeper.game.Level;

/**
 * Sweeps every plausible window and checks the fit never produces a bad board.
 *
 * <p>Deliberately outside {@code src/main/java} so it is not part of the APK. The game model is
 * plain Java, so checking a tuning change is a second of arithmetic rather than a build:
 *
 * <pre>
 * cd apps/sweeper
 * javac -d /tmp/sim src/main/java/com/discosweeper/game/{Fit,Level,Board,Solver}.java sim/Sim.java
 * java -cp /tmp/sim Sim
 * </pre>
 */
public final class Sim {
    public static void main(String[] args) {
        int checked = 0, bad = 0;
        double worstDead = 0;
        String worstWhere = "";

        // dp, and the device pixel ratio does not matter because Fit works in one unit.
        for (int w = 320; w <= 3000; w += 7) {
            for (int h = 300; h <= 2000; h += 7) {
                for (Level level : Level.values()) {
                    if (!level.reflows()) continue;
                    float uw = w - 48;                     // window frame + BoardView inset
                    float uh = h - 120;
                    if (uw <= 0 || uh <= 0) continue;
                    Fit fit = Fit.of(uw, uh, level, level.cellMinDp, level.cellMaxDp);
                    checked++;

                    StringBuilder why = new StringBuilder();
                    if (fit.cols < Level.MIN_SIDE || fit.rows < Level.MIN_SIDE)
                        why.append("side<9 ");
                    if (fit.cols * fit.cell > uw + 0.01f || fit.rows * fit.cell > uh + 0.01f)
                        why.append("overflows ");
                    if (fit.mines < Level.MIN_MINES) why.append("too few balls ");
                    if (fit.mines > (fit.cols - 1) * (fit.rows - 1)) why.append("too many balls ");
                    if (fit.mines >= fit.cols * fit.rows) why.append("all balls ");
                    if (fit.cell <= 0 || Float.isNaN(fit.cell)) why.append("bad cell ");
                    if (fit.cols > Level.MAX_COLS || fit.rows > Level.MAX_ROWS) why.append("over cap ");

                    if (why.length() > 0) {
                        if (bad < 8) System.out.println("BAD " + w + "x" + h + " " + level
                                + " -> " + fit.cols + "x" + fit.rows + "/" + fit.mines
                                + " cell " + fit.cell + "  : " + why);
                        bad++;
                    }

                    // Dead space only matters where the board is not floored by the 9-a-side rule.
                    double dead = 1 - (fit.cols * fit.cell * fit.rows * fit.cell) / (uw * uh);
                    boolean floored = fit.cols == Level.MIN_SIDE || fit.rows == Level.MIN_SIDE;
                    if (!floored && dead > worstDead) {
                        worstDead = dead;
                        worstWhere = w + "x" + h + " " + level + " -> " + fit.cols + "x" + fit.rows;
                    }
                }
            }
        }
        System.out.println("checked " + checked + " fits, " + bad + " bad");
        System.out.printf("worst dead space (unfloored): %.2f%%  at %s%n", worstDead * 100, worstWhere);

        // The launch bounds must be fixed points, or picking a difficulty would resize the
        // window and then reflow to a different board.
        System.out.println();
        for (Level level : Level.values()) {
            if (!level.reflows()) continue;
            float uw = level.cols * level.idealCellDp;
            float uh = level.rows * level.idealCellDp;
            Fit fit = Fit.of(uw, uh, level, level.cellMinDp, level.cellMaxDp);
            boolean ok = fit.cols == level.cols && fit.rows == level.rows && fit.mines == level.mines;
            System.out.printf("%-13s ideal %.0fx%.0f -> %dx%d/%d  %s%n", level, uw, uh,
                    fit.cols, fit.rows, fit.mines, ok ? "FIXED POINT" : "DRIFTS");
        }
    }
}
