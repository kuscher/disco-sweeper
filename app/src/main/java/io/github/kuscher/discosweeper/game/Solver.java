package io.github.kuscher.discosweeper.game;

import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public final class Solver {
    public static final int BALL = 2;
    private static final byte FLAG = 2;
    private static final byte HIDDEN = 0;
    public static final int NONE = 0;
    private static final byte OPEN = 1;
    public static final int SAFE = 1;

    public static final class Move {
        public final int kind;
        public final int x;
        public final int y;

        Move(int i, int i2, int i3) {
            this.x = i;
            this.y = i2;
            this.kind = i3;
        }
    }

    private Solver() {
    }

    /**
     * A move the open numbers prove: a safe tile if there is one, otherwise a ball to mark.
     *
     * <p>The player's own marks are not trusted. One wrong mark would make every deduction around
     * it wrong, and a hint could send the player onto a ball. Balls count as known only once the
     * numbers prove them, and each round feeds the proven ones back in until a safe tile turns up.
     * A safe tile the player has marked means that mark is wrong, so it is still a fair hint.
     */
    public static Move hint(Board board) {
        int cols = board.cols;
        int size = cols * board.rows;
        byte[] state = new byte[size];
        int[] numbers = new int[size];
        boolean[] marked = new boolean[size];
        for (int y = 0; y < board.rows; y++) {
            for (int x = 0; x < cols; x++) {
                int at = board.idx(x, y);
                int cell = board.cell(x, y);
                if (cell == Board.OPEN) {
                    state[at] = OPEN;
                    numbers[at] = board.number(x, y);
                }
                marked[at] = cell == Board.MARKED;
            }
        }
        Move ball = null;
        // Every round flags at least one hidden tile or stops, so this ends within the board.
        for (int round = 0; round < size; round++) {
            Deductions found = deduce(cols, board.rows, numbers, state);
            for (int i = 0; i < found.safeCount; i++) {
                if (!marked[found.safe[i]]) {
                    return new Move(found.safe[i] % cols, found.safe[i] / cols, SAFE);
                }
            }
            if (found.safeCount > 0) {
                return new Move(found.safe[0] % cols, found.safe[0] / cols, SAFE);
            }
            if (found.ballCount == 0) {
                break;
            }
            for (int i = 0; i < found.ballCount; i++) {
                int at = found.balls[i];
                state[at] = FLAG;
                if (ball == null && !marked[at]) {
                    ball = new Move(at % cols, at / cols, BALL);
                }
            }
        }
        return ball;
    }

    private static final class Deductions {
        int ballCount;
        int[] balls;
        int[] safe;
        int safeCount;

        private Deductions() {
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r11v18 */
    /* JADX WARN: Type inference failed for: r11v23 */
    /* JADX WARN: Type inference failed for: r11v28 */
    /* JADX WARN: Type inference failed for: r11v29 */
    /* JADX WARN: Type inference failed for: r11v30 */
    /* JADX WARN: Type inference failed for: r11v6 */
    private static Deductions deduce(int n, int n2, int[] nArray, byte[] objectArray) {
        // The decompiler reused the byte[] parameter's slot for an int[] in the
        // subset-deduction phase below. Give that use its own variable; the board
        // array is still needed as a byte[] earlier in the method.
        int[] subset;
        int n3;
        int n4;
        Deductions deductions = new Deductions();
        int n5 = n * n2;
        deductions.safe = new int[n5];
        deductions.balls = new int[n5];
        boolean[] blArray = new boolean[n5];
        boolean[] blArray2 = new boolean[n5];
        int[][] nArrayArray = new int[n5][];
        int[] nArray2 = new int[n5];
        int[] nArray3 = new int[8];
        int n6 = 0;
        int n7 = 0;
        while (true) {
            n5 = 1;
            if (n6 >= n2) break;
            for (n4 = 0; n4 < n; ++n4) {
                int[] nArray4;
                int n8;
                int n9 = n6 * n + n4;
                // Zeros count too. A flood opens everything around a zero except a tile the player
                // marked, and a zero is the proof that such a mark is wrong.
                if (objectArray[n9] != n5) continue;
                int n10 = 0;
                int n11 = 0;
                for (n3 = -1; n3 <= n5; ++n3) {
                    for (n8 = -1; n8 <= n5; ++n8) {
                        int n12;
                        int n13;
                        if (n8 == 0 && n3 == 0) {
                            n13 = n10;
                            n12 = n11;
                        } else {
                            int n14 = n4 + n8;
                            int n15 = n6 + n3;
                            n13 = n10;
                            n12 = n11;
                            if (n14 >= 0) {
                                n13 = n10;
                                n12 = n11;
                                if (n15 >= 0) {
                                    n13 = n10;
                                    n12 = n11;
                                    if (n14 < n) {
                                        if (n15 >= n2) {
                                            n13 = n10;
                                            n12 = n11;
                                        } else if (objectArray[n14 = n15 * n + n14] == 0) {
                                            nArray3[n10] = n14;
                                            n13 = n10 + 1;
                                            n12 = n11;
                                        } else {
                                            n13 = n10;
                                            n12 = n11;
                                            if (objectArray[n14] == 2) {
                                                n12 = n11 + 1;
                                                n13 = n10;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        n10 = n13;
                        n11 = n12;
                    }
                }
                if (n10 == 0 || (n11 = nArray[n9] - n11) < 0) continue;
                if (n11 == 0) {
                    for (n3 = 0; n3 < n10; ++n3) {
                        if (blArray[nArray3[n3]]) continue;
                        blArray[nArray3[n3]] = true;
                        nArray4 = deductions.safe;
                        n8 = deductions.safeCount;
                        deductions.safeCount = n8 + 1;
                        nArray4[n8] = nArray3[n3];
                    }
                } else if (n11 == n10) {
                    for (n3 = 0; n3 < n10; ++n3) {
                        if (blArray2[nArray3[n3]]) continue;
                        blArray2[nArray3[n3]] = true;
                        nArray4 = deductions.balls;
                        n8 = deductions.ballCount;
                        deductions.ballCount = n8 + 1;
                        nArray4[n8] = nArray3[n3];
                    }
                }
                nArray4 = new int[n10];
                System.arraycopy(nArray3, 0, nArray4, 0, n10);
                Arrays.sort(nArray4);
                nArrayArray[n7] = nArray4;
                nArray2[n7] = n11;
                ++n7;
            }
            ++n6;
        }
        if (deductions.safeCount <= 0 && deductions.ballCount <= 0) {
            for (n = 0; n < n7; ++n) {
                for (n2 = 0; n2 < n7; ++n2) {
                    if (n == n2 || (subset = nArrayArray[n2]).length >= (nArray = nArrayArray[n]).length || !Solver.isSubset(subset, nArray)) continue;
                    n5 = nArray2[n] - nArray2[n2];
                    n6 = nArray.length;
                    n4 = subset.length;
                    if (n5 == 0) {
                        n6 = nArray.length;
                        for (n5 = 0; n5 < n6; ++n5) {
                            n3 = nArray[n5];
                            if (Solver.contains(subset, n3) || blArray[n3]) continue;
                            blArray[n3] = true;
                            nArray3 = deductions.safe;
                            n4 = deductions.safeCount;
                            deductions.safeCount = n4 + 1;
                            nArray3[n4] = n3;
                        }
                        continue;
                    }
                    if (n5 != n6 - n4) continue;
                    n6 = nArray.length;
                    for (n5 = 0; n5 < n6; ++n5) {
                        n3 = nArray[n5];
                        if (Solver.contains(subset, n3) || blArray2[n3]) continue;
                        blArray2[n3] = true;
                        nArray3 = deductions.balls;
                        n4 = deductions.ballCount;
                        deductions.ballCount = n4 + 1;
                        nArray3[n4] = n3;
                    }
                }
                if (deductions.safeCount <= 0 && deductions.ballCount <= 0) {
                    continue;
                }
                return deductions;
            }
            return deductions;
        }
        return deductions;
    }

    private static boolean isSubset(int[] iArr, int[] iArr2) {
        int i = 0;
        for (int i2 : iArr) {
            while (i < iArr2.length && iArr2[i] < i2) {
                i++;
            }
            if (i >= iArr2.length || iArr2[i] != i2) {
                return false;
            }
        }
        return true;
    }

    private static boolean contains(int[] iArr, int i) {
        for (int i2 : iArr) {
            if (i2 == i) {
                return true;
            }
        }
        return false;
    }

    public static boolean solvableFrom(boolean[] zArr, int i, int i2, int i3, int i4) {
        int iOpen;
        int i5 = i;
        int i6 = i2;
        int i7 = i5 * i6;
        int[] iArr = new int[i7];
        for (int i8 = 0; i8 < i6; i8++) {
            for (int i9 = 0; i9 < i5; i9++) {
                int i10 = (i8 * i5) + i9;
                if (!zArr[i10]) {
                    int i11 = 0;
                    for (int i12 = -1; i12 <= 1; i12++) {
                        for (int i13 = -1; i13 <= 1; i13++) {
                            if (i13 != 0 || i12 != 0) {
                                int i14 = i9 + i13;
                                int i15 = i8 + i12;
                                if (i14 >= 0 && i15 >= 0 && i14 < i5 && i15 < i6 && zArr[(i15 * i5) + i14]) {
                                    i11++;
                                }
                            }
                        }
                    }
                    iArr[i10] = i11;
                }
            }
        }
        byte[] bArr = new byte[i7];
        int[] iArr2 = new int[i7];
        int iCountTrue = i7 - countTrue(zArr);
        for (int iOpen2 = open(i5, i6, iArr, zArr, bArr, iArr2, i3, i4); iOpen2 < iCountTrue; iOpen2 = iOpen) {
            Deductions deductionsDeduce = deduce(i5, i6, iArr, bArr);
            if (deductionsDeduce.safeCount == 0 && deductionsDeduce.ballCount == 0) {
                return false;
            }
            for (int i16 = 0; i16 < deductionsDeduce.ballCount; i16++) {
                bArr[deductionsDeduce.balls[i16]] = 2;
            }
            iOpen = iOpen2;
            int i17 = 0;
            while (i17 < deductionsDeduce.safeCount) {
                int i18 = deductionsDeduce.safe[i17];
                if (bArr[i18] == 0) {
                    iOpen += open(i5, i6, iArr, zArr, bArr, iArr2, i18 % i5, i18 / i5);
                }
                i17++;
                i5 = i;
                i6 = i2;
            }
            i5 = i;
            i6 = i2;
        }
        return true;
    }

    private static int countTrue(boolean[] zArr) {
        int i = 0;
        for (boolean z : zArr) {
            if (z) {
                i++;
            }
        }
        return i;
    }

    /** Board.flood on the solver's copy: a cell is opened as it is queued, so at most once. */
    private static int open(int cols, int rows, int[] numbers, boolean[] balls, byte[] state, int[] stack, int x, int y) {
        int start = (y * cols) + x;
        if (state[start] != HIDDEN || balls[start]) {
            return 0;
        }
        state[start] = OPEN;
        int opened = 1;
        stack[0] = start;
        int top = 1;
        while (top > 0) {
            int at = stack[--top];
            if (numbers[at] != 0) {
                continue;
            }
            int ax = at % cols;
            int ay = at / cols;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = ax + dx;
                    int ny = ay + dy;
                    if ((dx != 0 || dy != 0) && nx >= 0 && ny >= 0 && nx < cols && ny < rows) {
                        int next = (ny * cols) + nx;
                        if (state[next] == HIDDEN && !balls[next]) {
                            state[next] = OPEN;
                            opened++;
                            stack[top++] = next;
                        }
                    }
                }
            }
        }
        return opened;
    }
}
