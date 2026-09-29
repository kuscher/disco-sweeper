package io.github.kuscher.discosweeper.game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/* JADX INFO: loaded from: classes.dex */
public final class Board {
    public static final int COVERED = 0;
    public static final int LOST = 3;
    public static final int MARKED = 2;
    public static final int OPEN = 1;
    public static final int PLAYING = 1;
    public static final int READY = 0;
    public static final int UNSURE = 3;
    public static final int WON = 2;
    private boolean allowUnsure;
    private final byte[] cell;
    public final int cols;
    /** Play time before the current stretch, in ns; the clock only runs while the game is shown. */
    private long banked;
    private boolean paused;
    private final boolean[] exploded;
    private int marks;
    private final boolean[] mine;
    public final int mines;
    private boolean noGuess;
    private boolean noGuessMissed;
    private final byte[] num;
    private int opened;
    private final Random rng;
    public final int rows;
    private int[] stack;
    /** System.nanoTime() when the current stretch began: monotonic, so a clock change can't skew a time. */
    private long runningSince;
    private boolean[] wrongMark;
    private int status = 0;
    private int hit = -1;

    public Board(int i, int i2, int i3, long j, boolean z) {
        this.cols = i;
        this.rows = i2;
        int i4 = i * i2;
        this.mines = Math.max(1, Math.min(i3, i4 - 1));
        this.allowUnsure = z;
        this.mine = new boolean[i4];
        this.num = new byte[i4];
        this.cell = new byte[i4];
        this.exploded = new boolean[i4];
        this.rng = new Random(j);
    }

    public int idx(int i, int i2) {
        return (i2 * this.cols) + i;
    }

    public boolean inBounds(int i, int i2) {
        return i >= 0 && i2 >= 0 && i < this.cols && i2 < this.rows;
    }

    public int cell(int i, int i2) {
        return this.cell[idx(i, i2)];
    }

    public int number(int i, int i2) {
        return this.num[idx(i, i2)];
    }

    public boolean isMine(int i, int i2) {
        return this.mine[idx(i, i2)];
    }

    public void setNoGuess(boolean z) {
        this.noGuess = z;
    }

    /** Whether marking cycles through "?"; takes effect on the next mark. */
    public void setAllowUnsure(boolean z) {
        this.allowUnsure = z;
    }

    /**
     * True when a no-guess board was asked for and none turned up in time. Past about a fifth of
     * the tiles as balls, boards that logic alone can clear get rare, so the game says so rather
     * than quietly dealing one that needs a guess.
     */
    public boolean noGuessMissed() {
        return this.noGuessMissed;
    }

    public boolean wasWrongMark(int i, int i2) {
        return this.wrongMark != null && this.wrongMark[idx(i, i2)];
    }

    public boolean isExploded(int i, int i2) {
        return this.exploded[idx(i, i2)];
    }

    public int status() {
        return this.status;
    }

    public boolean over() {
        return this.status == 2 || this.status == 3;
    }

    public boolean started() {
        return this.status != 0;
    }

    public int hitIndex() {
        return this.hit;
    }

    public int ballsLeft() {
        return this.mines - this.marks;
    }

    public long elapsedMs() {
        if (this.status == 0) {
            return 0L;
        }
        long ns = this.banked;
        if (this.status == PLAYING && !this.paused) {
            ns += System.nanoTime() - this.runningSince;
        }
        return ns / 1_000_000L;
    }

    /** Stops the clock while the game can't be seen, such as a minimised window. */
    public void pause() {
        if (this.status == PLAYING && !this.paused) {
            this.banked += System.nanoTime() - this.runningSince;
        }
        this.paused = true;
    }

    public void resume() {
        if (this.paused) {
            this.paused = false;
            this.runningSince = System.nanoTime();
        }
    }

    private void stopClock() {
        if (!this.paused) {
            this.banked += System.nanoTime() - this.runningSince;
        }
        this.paused = true;
    }

    public int elapsedSeconds() {
        if (this.status == 0) {
            return 0;
        }
        return (int) Math.max(1L, Math.min(999L, elapsedMs() / 1000));
    }

    public boolean open(int i, int i2) {
        if (over() || !inBounds(i, i2)) {
            return false;
        }
        int iIdx = idx(i, i2);
        if (this.cell[iIdx] == 1 || this.cell[iIdx] == 2) {
            return false;
        }
        if (this.status == 0) {
            place(i, i2);
            this.status = 1;
            this.banked = 0L;
            this.paused = false;
            this.runningSince = System.nanoTime();
        }
        if (this.mine[iIdx]) {
            this.hit = iIdx;
            this.exploded[iIdx] = true;
            lose();
            return true;
        }
        flood(i, i2);
        checkWin();
        return true;
    }

    public boolean mark(int i, int i2) {
        if (over() || !inBounds(i, i2)) {
            return false;
        }
        int iIdx = idx(i, i2);
        switch (this.cell[iIdx]) {
            case 0:
                this.cell[iIdx] = 2;
                this.marks++;
                return true;
            case 1:
            default:
                return false;
            case 2:
                this.marks--;
                this.cell[iIdx] = (byte) (this.allowUnsure ? 3 : 0);
                return true;
            case 3:
                this.cell[iIdx] = 0;
                return true;
        }
    }

    public boolean chord(int i, int i2) {
        boolean z = false;
        if (over() || !inBounds(i, i2)) {
            return false;
        }
        int iIdx = idx(i, i2);
        if (this.cell[iIdx] != 1 || this.num[iIdx] == 0 || countAdjacent(i, i2, 2) != this.num[iIdx]) {
            return false;
        }
        boolean z2 = false;
        for (int i3 = -1; i3 <= 1; i3++) {
            for (int i4 = -1; i4 <= 1; i4++) {
                if (i4 != 0 || i3 != 0) {
                    int i5 = i + i4;
                    int i6 = i2 + i3;
                    if (inBounds(i5, i6)) {
                        int iIdx2 = idx(i5, i6);
                        if (this.cell[iIdx2] == 0 || this.cell[iIdx2] == 3) {
                            if (this.mine[iIdx2]) {
                                if (!z) {
                                    this.hit = iIdx2;
                                }
                                this.exploded[iIdx2] = true;
                                z = true;
                                z2 = true;
                            } else {
                                flood(i5, i6);
                                z2 = true;
                            }
                        }
                    }
                }
            }
        }
        if (z) {
            lose();
            return true;
        }
        if (z2) {
            checkWin();
        }
        return z2;
    }

    public boolean canChord(int i, int i2) {
        if (over() || !inBounds(i, i2)) {
            return false;
        }
        int iIdx = idx(i, i2);
        return this.cell[iIdx] == 1 && this.num[iIdx] > 0 && countAdjacent(i, i2, 2) == this.num[iIdx];
    }

    public int countAdjacent(int i, int i2, int i3) {
        int i4 = 0;
        for (int i5 = -1; i5 <= 1; i5++) {
            for (int i6 = -1; i6 <= 1; i6++) {
                if (i6 != 0 || i5 != 0) {
                    int i7 = i + i6;
                    int i8 = i2 + i5;
                    if (inBounds(i7, i8) && this.cell[idx(i7, i8)] == i3) {
                        i4++;
                    }
                }
            }
        }
        return i4;
    }

    private void place(int i, int i2) {
        if (!this.noGuess) {
            scatter(i, i2);
            countNumbers();
            return;
        }
        // Give up after 600 ms and keep the last board rather than stall the first click.
        long deadline = System.nanoTime() + 600_000_000L;
        boolean solvable = false;
        for (int i3 = 0; i3 < 500 && !solvable; i3++) {
            Arrays.fill(this.mine, false);
            scatter(i, i2);
            solvable = Solver.solvableFrom(this.mine, this.cols, this.rows, i, i2);
            if (System.nanoTime() > deadline) {
                break;
            }
        }
        this.noGuessMissed = !solvable;
        countNumbers();
    }

    private void scatter(int i, int i2) {
        int i3 = this.cols * this.rows;
        int[] iArr = new int[i3];
        int i4 = 0;
        for (int i5 = 0; i5 < i3; i5++) {
            int i6 = i5 % this.cols;
            int i7 = i5 / this.cols;
            if (Math.abs(i6 - i) > 1 || Math.abs(i7 - i2) > 1) {
                iArr[i4] = i5;
                i4++;
            }
        }
        if (i4 < this.mines) {
            i4 = 0;
            for (int i8 = 0; i8 < i3; i8++) {
                if (i8 != idx(i, i2)) {
                    iArr[i4] = i8;
                    i4++;
                }
            }
        }
        for (int i9 = 0; i9 < this.mines && i9 < i4; i9++) {
            int iNextInt = this.rng.nextInt(i4 - i9) + i9;
            int i10 = iArr[i9];
            iArr[i9] = iArr[iNextInt];
            iArr[iNextInt] = i10;
            this.mine[iArr[i9]] = true;
        }
    }

    private void countNumbers() {
        for (int i = 0; i < this.rows; i++) {
            for (int i2 = 0; i2 < this.cols; i2++) {
                if (this.mine[idx(i2, i)]) {
                    this.num[idx(i2, i)] = 0;
                } else {
                    int i3 = 0;
                    for (int i4 = -1; i4 <= 1; i4++) {
                        for (int i5 = -1; i5 <= 1; i5++) {
                            if (i5 != 0 || i4 != 0) {
                                int i6 = i2 + i5;
                                int i7 = i + i4;
                                if (inBounds(i6, i7) && this.mine[idx(i6, i7)]) {
                                    i3++;
                                }
                            }
                        }
                    }
                    this.num[idx(i2, i)] = (byte) i3;
                }
            }
        }
    }

    /**
     * Opens a cell and, while it has no ball next to it, everything around it. A cell is opened
     * the moment it is queued, so it can be queued only once and the stack never holds more than
     * the board: counting it at pop time let an open area queue the same cell from several sides
     * and run past the end of the stack.
     */
    private void flood(int x, int y) {
        int start = idx(x, y);
        if (!openable(start)) {
            return;
        }
        if (this.stack == null) {
            this.stack = new int[this.cols * this.rows];
        }
        this.cell[start] = OPEN;
        this.opened++;
        this.stack[0] = start;
        int top = 1;
        while (top > 0) {
            int at = this.stack[--top];
            if (this.num[at] != 0) {
                continue;
            }
            int ax = at % this.cols;
            int ay = at / this.cols;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = ax + dx;
                    int ny = ay + dy;
                    if ((dx != 0 || dy != 0) && inBounds(nx, ny)) {
                        int next = idx(nx, ny);
                        if (openable(next)) {
                            this.cell[next] = OPEN;
                            this.opened++;
                            this.stack[top++] = next;
                        }
                    }
                }
            }
        }
    }

    /** Covered or "?", and not a ball: marked cells stay marked through a flood. */
    private boolean openable(int at) {
        return (this.cell[at] == COVERED || this.cell[at] == UNSURE) && !this.mine[at];
    }

    private void checkWin() {
        if (this.opened < (this.cols * this.rows) - this.mines) {
            return;
        }
        stopClock();
        this.status = 2;
        this.marks = this.mines;
        for (int i = 0; i < this.cell.length; i++) {
            if (this.mine[i]) {
                this.cell[i] = 2;
            }
        }
    }

    private void lose() {
        stopClock();
        this.status = 3;
        this.wrongMark = new boolean[this.cell.length];
        for (int i = 0; i < this.cell.length; i++) {
            if (this.cell[i] == 2 && !this.mine[i]) {
                this.wrongMark[i] = true;
            }
            if (this.mine[i] && this.cell[i] != 2) {
                this.cell[i] = 1;
            }
        }
    }

    private static final int SAVE_VERSION = 1;

    /**
     * The game as bytes, so it survives the activity being recreated (a font or display-size
     * change, or the process being reclaimed in the background). An untouched board saves its
     * shape only: its balls are placed on the first click.
     */
    public byte[] save() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(SAVE_VERSION);
            out.writeInt(this.cols);
            out.writeInt(this.rows);
            out.writeInt(this.mines);
            out.writeBoolean(this.allowUnsure);
            out.writeBoolean(this.noGuess);
            out.writeInt(this.status);
            if (this.status != READY) {
                out.writeLong(elapsedMs());
                out.writeInt(this.hit);
                out.writeInt(this.marks);
                out.writeInt(this.opened);
                out.write(this.cell);
                for (int i = 0; i < this.cell.length; i++) {
                    out.writeByte((this.mine[i] ? 1 : 0) | (this.exploded[i] ? 2 : 0)
                            | (this.wrongMark != null && this.wrongMark[i] ? 4 : 0));
                }
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** The board {@link #save()} wrote, paused; or null if the bytes aren't a board. */
    public static Board restore(byte[] saved, long seed) {
        if (saved == null) {
            return null;
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(saved));
            if (in.readInt() != SAVE_VERSION) {
                return null;
            }
            int cols = in.readInt();
            int rows = in.readInt();
            int mines = in.readInt();
            if (cols < 1 || rows < 1 || cols > Level.MAX_COLS || rows > Level.MAX_ROWS) {
                return null;
            }
            Board board = new Board(cols, rows, mines, seed, in.readBoolean());
            board.noGuess = in.readBoolean();
            int status = in.readInt();
            if (status == READY) {
                return board;
            }
            board.status = status;
            board.banked = in.readLong() * 1_000_000L;
            board.paused = true;
            board.hit = in.readInt();
            board.marks = in.readInt();
            board.opened = in.readInt();
            in.readFully(board.cell);
            boolean lost = status == LOST;
            if (lost) {
                board.wrongMark = new boolean[board.cell.length];
            }
            for (int i = 0; i < board.cell.length; i++) {
                int bits = in.readUnsignedByte();
                board.mine[i] = (bits & 1) != 0;
                board.exploded[i] = (bits & 2) != 0;
                if (lost) {
                    board.wrongMark[i] = (bits & 4) != 0;
                }
            }
            board.countNumbers();
            return board;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
