package io.github.kuscher.discosweeper.debug;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import io.github.kuscher.discosweeper.game.Board;
import io.github.kuscher.discosweeper.game.Settings;
import io.github.kuscher.discosweeper.view.BoardView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test hooks for debug builds, driven by {@code ./ds debug}. Never compiled into a release.
 *
 * <p>Input is dispatched straight into this app's own view tree, never injected into the system,
 * so a hook can only ever click this app, whatever else is on the screen. Screenshots render this
 * app's own views, never the screen. Back goes through the system, so tools/smoke.sh sends it
 * with adb only while this app has focus.
 *
 * <pre>
 * dump                       the board, the settings, and which of the drawer and the About sheet is open
 * pick covered|ball|frontier|number   a tile of that kind (frontier: a ball next to an open tile), as "C R" (it peeks at the balls: tests only)
 * solve [left|touch]         click every tile that isn't a ball until the game is won
 * hints N                    play N moves the way Hint shows them (open safe tiles, mark balls)
 * cell C R [left|right|middle|both|touch|hold]   click a tile
 * edge C top|bottom          a left click just outside the grid, in column C
 * click TEXT                 tap the view whose label or text contains TEXT
 * at TEXT FX FY              tap at a fraction of that view's bounds (0..1)
 * shown TEXT                 whether such a view is on screen
 * recreate                   recreate the activity, as a font or display-size change would
 * shot NAME                  render the window into cache/NAME.png
 * </pre>
 */
public final class DebugHooks extends ContentProvider {
    static final String TAG = "DiscoSweeper";
    static WeakReference<Activity> live = new WeakReference<>(null);

    @Override
    public boolean onCreate() {
        Application app = (Application) getContext().getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) { live = new WeakReference<>(a); }
            @Override public void onActivityStarted(Activity a) { live = new WeakReference<>(a); }
            @Override public void onActivityResumed(Activity a) { live = new WeakReference<>(a); }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) {
                if (live.get() == a) live = new WeakReference<>(null);
            }
        });
        return true;
    }

    public static final class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra("c");
            if (command == null) return;
            PendingResult pending = goAsync();
            new Thread(() -> {
                String result;
                try {
                    result = run(command.trim().split("\\s+"));
                } catch (Throwable t) {
                    result = "error " + t;
                }
                Log.i(TAG, "debug " + command + " -> " + result);
                pending.finish();
            }, "debug-hook").start();
        }
    }

    private static String run(String[] a) throws Exception {
        Activity activity = live.get();
        if (activity == null) return "no activity";
        switch (a[0]) {
            case "dump": return onUi(() -> dump(activity));
            case "cell": return cell(activity, Integer.parseInt(a[1]), Integer.parseInt(a[2]), a.length > 3 ? a[3] : "left");
            case "edge": return edge(activity, Integer.parseInt(a[1]), a.length > 2 ? a[2] : "top");
            case "click": return at(activity, join(a, 1, a.length), 0.5f, 0.5f);
            case "at": return at(activity, join(a, 1, a.length - 2), Float.parseFloat(a[a.length - 2]), Float.parseFloat(a[a.length - 1]));
            case "shown": return onUi(() -> String.valueOf(find(activity.getWindow().getDecorView(), join(a, 1, a.length)) != null));
            case "recreate": return onUi(() -> { activity.recreate(); return "ok"; });
            case "shot": return onUi(() -> shot(activity, a[1]));
            case "pick": return onUi(() -> pick(activity, a[1]));
            case "solve": return solve(activity, a.length > 1 ? a[1] : "left");
            case "hints": return hints(activity, Integer.parseInt(a[1]));
            default: return "unknown command";
        }
    }

    private static String dump(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        BoardView view = findBoard(decor);
        Board b = view == null ? null : view.board();
        String board = b == null ? "no board" : String.format("status=%s board=%dx%d/%d left=%d elapsed=%dms markMode=%s",
                new String[] {"READY", "PLAYING", "WON", "LOST"}[b.status()], b.cols, b.rows, b.mines,
                b.ballsLeft(), b.elapsedMs(), view.markMode());
        return board + " drawer=" + (find(decor, "Expert, ") != null) + " about=" + (find(decor, "Close") != null)
                + " cell=" + (view == null ? 0 : Math.round(view.cellSize())) + "px"
                + " level=" + Settings.level(activity) + " noGuess=" + Settings.noGuess(activity)
                + " unsure=" + Settings.allowUnsure(activity) + " sound=" + Settings.sound(activity)
                + " custom=" + Settings.customCols(activity) + "x" + Settings.customRows(activity) + "/" + Settings.customMines(activity);
    }

    private static String pick(Activity activity, String kind) {
        BoardView view = findBoard(activity.getWindow().getDecorView());
        Board b = view == null ? null : view.board();
        if (b == null) return "no board";
        boolean placed = b.started();
        for (int r = 0; r < b.rows; r++) {
            for (int c = 0; c < b.cols; c++) {
                int cell = b.cell(c, r);
                boolean match;
                switch (kind) {
                    case "covered": match = cell == Board.COVERED && (!placed || !b.isMine(c, r)); break;
                    case "ball": match = placed && cell == Board.COVERED && b.isMine(c, r); break;
                    case "frontier": match = placed && cell == Board.COVERED && b.isMine(c, r) && nextToOpen(b, c, r); break;
                    case "number": match = cell == Board.OPEN && b.number(c, r) > 0; break;
                    default: return "unknown kind";
                }
                if (match) return c + " " + r;
            }
        }
        return "none";
    }

    private static String hints(Activity activity, int moves) throws Exception {
        for (int i = 0; i < moves; i++) {
            String move = onUi(() -> {
                BoardView view = findBoard(activity.getWindow().getDecorView());
                Board b = view == null ? null : view.board();
                if (b == null || b.over()) return "over";
                io.github.kuscher.discosweeper.game.Solver.Move m = io.github.kuscher.discosweeper.game.Solver.hint(b);
                return m == null ? "none" : m.x + " " + m.y + " " + (m.kind == io.github.kuscher.discosweeper.game.Solver.SAFE ? "left" : "right");
            });
            String[] at = move.split(" ");
            if (at.length != 3) return "stopped after " + i + ": " + move;
            cell(activity, Integer.parseInt(at[0]), Integer.parseInt(at[1]), at[2]);
        }
        return "played " + moves;
    }

    private static boolean nextToOpen(Board b, int c, int r) {
        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++)
                if (b.inBounds(c + dx, r + dy) && b.cell(c + dx, r + dy) == Board.OPEN) return true;
        return false;
    }

    private static String solve(Activity activity, String how) throws Exception {
        for (int i = 0; i < 2000; i++) {
            String status = onUi(() -> {
                BoardView view = findBoard(activity.getWindow().getDecorView());
                Board b = view == null ? null : view.board();
                return b == null ? "none" : String.valueOf(b.status());
            });
            if (status.equals(String.valueOf(Board.WON))) return "won in " + i + " clicks";
            if (!status.equals(String.valueOf(Board.READY)) && !status.equals(String.valueOf(Board.PLAYING))) return "status " + status;
            String[] at = onUi(() -> pick(activity, "covered")).split(" ");
            if (at.length != 2) return "stuck: " + at[0];
            String r = cell(activity, Integer.parseInt(at[0]), Integer.parseInt(at[1]), how);
            if (!r.startsWith("ok")) return r;
        }
        return "gave up";
    }

    /** A click on a tile, as a mouse (with buttons) or a finger (tap, or hold to mark). */
    private static String cell(Activity activity, int c, int r, String how) throws Exception {
        float[] xy = new float[2];
        String err = onUi(() -> {
            BoardView view = findBoard(activity.getWindow().getDecorView());
            if (view == null || view.board() == null) return "no board";
            int[] loc = new int[2];
            view.getLocationInWindow(loc);
            xy[0] = loc[0] + view.cellCentreX(c);
            xy[1] = loc[1] + view.cellCentreY(r);
            return null;
        });
        if (err != null) return err;
        return click(activity, xy[0], xy[1], how);
    }

    private static String edge(Activity activity, int c, String side) throws Exception {
        float[] xy = new float[2];
        String err = onUi(() -> {
            BoardView view = findBoard(activity.getWindow().getDecorView());
            if (view == null || view.board() == null) return "no board";
            int[] loc = new int[2];
            view.getLocationInWindow(loc);
            float half = view.cellSize() / 2.0f;
            float outside = 3.0f * view.getResources().getDisplayMetrics().density;
            xy[0] = loc[0] + view.cellCentreX(c);
            xy[1] = loc[1] + (side.equals("top")
                    ? view.cellCentreY(0) - half - outside
                    : view.cellCentreY(view.board().rows - 1) + half + outside);
            return null;
        });
        if (err != null) return err;
        return click(activity, xy[0], xy[1], "left");
    }

    private static String at(Activity activity, String text, float fx, float fy) throws Exception {
        float[] xy = new float[2];
        String err = onUi(() -> {
            View v = find(activity.getWindow().getDecorView(), text);
            if (v == null) return "not found: " + text;
            v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()), true);
            return null;
        });
        if (err != null) return err;
        SystemClock.sleep(150);
        err = onUi(() -> {
            View v = find(activity.getWindow().getDecorView(), text);
            if (v == null) return "not found: " + text;
            int[] loc = new int[2];
            v.getLocationInWindow(loc);
            xy[0] = loc[0] + v.getWidth() * fx;
            xy[1] = loc[1] + v.getHeight() * fy;
            return null;
        });
        if (err != null) return err;
        return click(activity, xy[0], xy[1], "touch");
    }

    /** A press and release at window coordinates, dispatched into this app's views only. */
    private static String click(Activity activity, float x, float y, String how) throws Exception {
        boolean touch = how.equals("touch") || how.equals("hold");
        int buttons = touch ? 0 : how.equals("right") ? MotionEvent.BUTTON_SECONDARY
                : how.equals("middle") ? MotionEvent.BUTTON_TERTIARY : MotionEvent.BUTTON_PRIMARY;
        long down = SystemClock.uptimeMillis();
        View decor = activity.getWindow().getDecorView();
        onUi(() -> { decor.dispatchTouchEvent(event(down, down, MotionEvent.ACTION_DOWN, x, y, buttons, touch)); return null; });
        if (how.equals("both")) {
            onUi(() -> { decor.dispatchGenericMotionEvent(event(down, down + 30, MotionEvent.ACTION_BUTTON_PRESS, x, y,
                    MotionEvent.BUTTON_PRIMARY | MotionEvent.BUTTON_SECONDARY, false)); return null; });
        }
        if (how.equals("hold")) SystemClock.sleep(600);   // past BoardView's 380 ms long press
        onUi(() -> { decor.dispatchTouchEvent(event(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0, touch)); return null; });
        return "ok " + Math.round(x) + "," + Math.round(y);
    }

    private static MotionEvent event(long down, long time, int action, float x, float y, int buttons, boolean touch) {
        MotionEvent.PointerProperties p = new MotionEvent.PointerProperties();
        p.id = 0;
        p.toolType = touch ? MotionEvent.TOOL_TYPE_FINGER : MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords c = new MotionEvent.PointerCoords();
        c.x = x;
        c.y = y;
        c.pressure = 1.0f;
        c.size = 1.0f;
        int source = touch ? InputDevice.SOURCE_TOUCHSCREEN : InputDevice.SOURCE_MOUSE;
        MotionEvent e = MotionEvent.obtain(down, time, action, 1, new MotionEvent.PointerProperties[] {p},
                new MotionEvent.PointerCoords[] {c}, 0, buttons, 1.0f, 1.0f, 0, 0, source, 0);
        return e;
    }

    private static String shot(Activity activity, String name) {
        View decor = activity.getWindow().getDecorView();
        Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        decor.draw(new android.graphics.Canvas(bitmap));
        File out = new File(activity.getCacheDir(), name + ".png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (Exception e) {
            return "error " + e;
        }
        return out.getAbsolutePath();
    }

    private static BoardView findBoard(View v) {
        if (v instanceof BoardView) return (BoardView) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                BoardView found = findBoard(g.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    /** The topmost shown view whose content description or text contains the given words. */
    private static View find(View v, String text) {
        if (!v.isShown()) return null;
        CharSequence label = v.getContentDescription();
        if (label == null && v instanceof TextView) label = ((TextView) v).getText();
        if (label != null && label.toString().contains(text)) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = g.getChildCount() - 1; i >= 0; i--) {
                View found = find(g.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String join(String[] a, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) sb.append(i > from ? " " : "").append(a[i]);
        return sb.toString();
    }

    private interface UiCall { String call() throws Exception; }

    private static String onUi(UiCall call) throws Exception {
        String[] out = new String[1];
        Exception[] err = new Exception[1];
        CountDownLatch done = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                out[0] = call.call();
            } catch (Exception e) {
                err[0] = e;
            }
            done.countDown();
        });
        if (!done.await(5, TimeUnit.SECONDS)) return "timeout";
        if (err[0] != null) throw err[0];
        return out[0];
    }

    // A ContentProvider only for its early start; it serves nothing.
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
