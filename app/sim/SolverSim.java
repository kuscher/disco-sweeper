import io.github.kuscher.discosweeper.game.Board;
import io.github.kuscher.discosweeper.game.Fit;
import io.github.kuscher.discosweeper.game.Level;
import io.github.kuscher.discosweeper.game.Solver;

/**
 * Plays boards headless: how long the no-guess generator takes on the first click, how often it
 * gives up, and whether a whole game played by the hint solver ever breaks the model.
 *
 * <p>Outside {@code src/main/java}, like {@code Sim}; {@code ./ds sim} runs both.
 */
public final class SolverSim {
    public static void main(String[] args) {
        // Classic boards, then each level maximised on two laptop displays, 1706x1066dp and
        // 1772x1107dp, minus the window frame and inset as in Sim.
        int[][] windows = { {0, 0}, {1706 - 48, 1066 - 120}, {1772 - 48, 1107 - 120} };
        boolean ok = true;
        for (Level level : Level.values()) {
            if (!level.reflows()) continue;
            for (int[] w : windows) {
                int cols = level.cols, rows = level.rows, mines = level.mines;
                if (w[0] > 0) {
                    Fit fit = Fit.of(w[0], w[1], level, level.cellMinDp, level.cellMaxDp);
                    cols = fit.cols; rows = fit.rows; mines = fit.mines;
                }
                ok &= run(level + " " + cols + "x" + rows + "/" + mines, cols, rows, mines, 200);
            }
        }
        // Custom's extremes, and sparse boards whose flood fill opens almost everything.
        // Custom at the sliders' ends: 9x9 to 30x24, 8% to 35% balls. Dense no-guess boards are
        // often impossible, so the generator may give up; what matters is that it does so in time.
        ok &= run("custom 30x24 at 35%", 30, 24, Math.round(0.35f * 30 * 24), 100);
        ok &= run("custom 9x9 at 35%", 9, 9, Math.round(0.35f * 81), 100);
        ok &= run("custom 30x24 at 8%", 30, 24, Math.round(0.08f * 30 * 24), 100);
        ok &= run("sparse 200x160/10", 200, 160, 10, 20);
        ok &= run("sparse 58x10/10", 58, 10, 10, 200);
        // The player's marks are not evidence: three wrong ones must never make a hint lose.
        ok &= run("INTERMEDIATE, 3 wrong marks", 16, 16, 40, 200, 3);
        ok &= run("EXPERT, 3 wrong marks", 30, 16, 99, 200, 3);
        System.out.println(ok ? "solver checks passed" : "SOLVER CHECKS FAILED");
        if (!ok) System.exit(1);
    }

    private static boolean run(String name, int cols, int rows, int mines, int games) {
        return run(name, cols, rows, mines, games, 0);
    }

    private static boolean run(String name, int cols, int rows, int mines, int games, int wrongMarks) {
        long worstFirst = 0, totalFirst = 0;
        int gaveUp = 0, won = 0, broken = 0;
        for (int g = 0; g < games; g++) {
            Board board = new Board(cols, rows, mines, 1000L * g + cols * 31 + rows, true);
            board.setNoGuess(true);
            int cx = cols / 2, cy = rows / 2;
            long t0 = System.nanoTime();
            try {
                board.open(cx, cy);
            } catch (RuntimeException e) {
                if (broken++ < 3) System.out.println("  " + name + ": first open threw " + e);
                continue;
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            worstFirst = Math.max(worstFirst, ms);
            totalFirst += ms;

            boolean[] layout = new boolean[cols * rows];
            for (int y = 0; y < rows; y++)
                for (int x = 0; x < cols; x++) layout[y * cols + x] = board.isMine(x, y);
            if (!Solver.solvableFrom(layout, cols, rows, cx, cy)) gaveUp++;

            java.util.Random rng = new java.util.Random(g);
            for (int placed = 0, tries = 0; placed < wrongMarks && tries < 10_000; tries++) {
                int x = rng.nextInt(cols), y = rng.nextInt(rows);
                if (board.cell(x, y) == Board.COVERED && !board.isMine(x, y)) { board.mark(x, y); placed++; }
            }

            // Play it out with hints only; a no-guess board the solver accepted must be won.
            try {
                int moves = 0;
                while (!board.over() && moves++ < cols * rows * 2) {
                    Solver.Move m = Solver.hint(board);
                    if (m == null) break;
                    if (moves % 7 == 0) board = roundTrip(board);   // a restored game must play on identically
                    if (m.kind == Solver.SAFE && board.cell(m.x, m.y) == Board.MARKED) board.mark(m.x, m.y);
                    if (m.kind == Solver.SAFE) board.open(m.x, m.y);
                    else board.mark(m.x, m.y);
                }
                board = roundTrip(board);
                if (board.status() == Board.WON) won++;
                else if (board.status() == Board.LOST) { if (broken++ < 3) System.out.println("  " + name + ": a hint lost the game"); }
            } catch (RuntimeException e) {
                if (broken++ < 3) System.out.println("  " + name + ": play threw " + e);
            }
        }
        System.out.printf("%-28s first click avg %4d ms, worst %4d ms; generator gave up %3d/%d; hints won %3d/%d%s%n",
                name, totalFirst / Math.max(1, games), worstFirst, gaveUp, games, won, games,
                broken > 0 ? "  BROKEN " + broken : "");
        return broken == 0;
    }

    /** Saves and restores the board, and fails loudly if anything visible changed. */
    private static Board roundTrip(Board board) {
        Board back = Board.restore(board.save(), 7L);
        if (back == null) throw new IllegalStateException("restore returned null");
        if (back.cols != board.cols || back.rows != board.rows || back.mines != board.mines
                || back.status() != board.status() || back.ballsLeft() != board.ballsLeft()
                || back.hitIndex() != board.hitIndex()
                || Math.abs(back.elapsedMs() - board.elapsedMs()) > 50)
            throw new IllegalStateException("restore changed the game");
        for (int y = 0; y < board.rows; y++)
            for (int x = 0; x < board.cols; x++)
                if (back.cell(x, y) != board.cell(x, y) || back.isMine(x, y) != board.isMine(x, y)
                        || back.number(x, y) != board.number(x, y) || back.isExploded(x, y) != board.isExploded(x, y)
                        || back.wasWrongMark(x, y) != board.wasWrongMark(x, y))
                    throw new IllegalStateException("restore changed tile " + x + "," + y);
        back.resume();
        return back;
    }
}
