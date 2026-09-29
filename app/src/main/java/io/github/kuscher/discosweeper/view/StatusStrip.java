package io.github.kuscher.discosweeper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;
import io.github.kuscher.discosweeper.game.Board;

/* JADX INFO: loaded from: classes.dex */
public final class StatusStrip extends View {
    private static final float BUTTON = 48.0f;
    private static final float HEIGHT = 62.0f;
    private static final float RADIUS = 21.0f;
    private Board board;
    private final Paint fill;
    private boolean hoverButton;
    private float insetLeft;
    private float insetRight;
    private long lastFrame;
    private OnReset onReset;
    private boolean pressingButton;
    private final RectF r;
    private float spin;
    private boolean spinning;
    private final Paint stroke;
    private final Paint text;
    private final Runnable tick;
    private boolean ticking;
    /** How long the ball keeps spinning after a win, so a finished game doesn't draw forever. */
    private static final long WIN_SPIN_MS = 8000L;
    private Board wonBoard;
    private long wonAt;
    private Tokens tokens;

    public interface OnReset {
        void onReset();
    }

    public StatusStrip(Context context) {
        super(context);
        this.fill = new Paint(1);
        this.text = new Paint(129);
        this.stroke = new Paint(1);
        this.r = new RectF();
        this.tick = new Runnable() { // from class: io.github.kuscher.discosweeper.view.StatusStrip.1
            @Override // java.lang.Runnable
            public void run() {
                // onDraw books the next tick, and only while a game's clock is running.
                StatusStrip.this.ticking = false;
                StatusStrip.this.invalidate();
            }
        };
        setClickable(true);
        this.text.setTypeface(Ui.weight(700));
        this.stroke.setStyle(Paint.Style.STROKE);
        setTooltipText("New game");
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
        invalidate();
    }

    public void setBoard(Board board) {
        this.board = board;
        invalidate();
    }

    public void setOnReset(OnReset onReset) {
        this.onReset = onReset;
    }

    public void setInsets(float f, float f2) {
        this.insetLeft = Ui.dpf(f);
        this.insetRight = Ui.dpf(f2);
        invalidate();
    }

    public void setSpinning(boolean z) {
        if (this.spinning != z) {
            this.spinning = z;
            this.lastFrame = SystemClock.uptimeMillis();
            invalidate();
        }
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.tick);
        this.ticking = false;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(resolveSize(Ui.dp(400.0f), i), Ui.dp(62.0f));
    }

    public float countersNeedDp() {
        this.text.setTextSize(Ui.spf(21.0f));
        return (Math.max(this.text.measureText("10:00"), this.text.measureText("-99")) / Ui.dpf(1.0f)) + 56.0f;
    }

    private float ballCentreX() {
        float fDpf = Ui.dpf(48.0f) / 2.0f;
        float fDpf2 = Ui.dpf(countersNeedDp());
        float f = this.insetLeft + fDpf2 + fDpf;
        float width = ((getWidth() - this.insetRight) - fDpf2) - fDpf;
        return width < f ? (f + width) / 2.0f : Ui.clamp(getWidth() / 2.0f, f, width);
    }

    private boolean overButton(float f, float f2) {
        return Math.hypot((double) (f - ballCentreX()), (double) (f2 - (((float) getHeight()) / 2.0f))) <= ((double) (Ui.dpf(48.0f) / 2.0f));
    }

    @Override // android.view.View
    public boolean onHoverEvent(MotionEvent motionEvent) {
        boolean z = this.hoverButton;
        this.hoverButton = motionEvent.getActionMasked() != 10 && overButton(motionEvent.getX(), motionEvent.getY());
        if (z != this.hoverButton) {
            invalidate();
        }
        return true;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.pressingButton = overButton(motionEvent.getX(), motionEvent.getY());
                invalidate();
                return this.pressingButton || super.onTouchEvent(motionEvent);
            case 1:
                if (this.pressingButton && overButton(motionEvent.getX(), motionEvent.getY()) && this.onReset != null) {
                    this.onReset.onReset();
                }
                this.pressingButton = false;
                invalidate();
                return true;
            case 2:
            default:
                return super.onTouchEvent(motionEvent);
            case 3:
                this.pressingButton = false;
                invalidate();
                return true;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float f;
        float f2;
        boolean z;
        if (this.tokens == null) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        float fMin = this.lastFrame == 0 ? 0.0f : Math.min(0.1f, (jUptimeMillis - this.lastFrame) / 1000.0f);
        this.lastFrame = jUptimeMillis;
        boolean z2 = this.board != null && this.board.status() == 2;
        boolean z3 = this.board != null && this.board.status() == 3;
        if (this.board != null && this.board.status() == Board.PLAYING && !this.ticking) {
            this.ticking = true;
            postDelayed(this.tick, 200L);
        }
        if (z2 && this.wonBoard != this.board) {
            this.wonBoard = this.board;
            this.wonAt = jUptimeMillis;
        }
        boolean celebrating = z2 && jUptimeMillis - this.wonAt < WIN_SPIN_MS;
        if (this.spinning || celebrating) {
            this.spin += fMin * (z2 ? 2.6f : 1.7f);
            postInvalidateOnAnimation();
        }
        this.r.set(0.0f, 0.0f, getWidth(), getHeight());
        this.fill.setColor(this.tokens.surfaceContainer);
        canvas.drawRoundRect(this.r, Ui.dpf(21.0f), Ui.dpf(21.0f), this.fill);
        float height = getHeight() / 2.0f;
        float fDpf = Ui.dpf(13.0f);
        float fDpf2 = this.insetLeft + Ui.dpf(18.0f);
        drawBall(canvas, fDpf2, height, fDpf, 0.0f, 1.0f);
        Canvas canvas2 = canvas;
        this.text.setColor(this.tokens.onSurface);
        this.text.setTextSize(Ui.spf(21.0f));
        this.text.setTextAlign(Paint.Align.LEFT);
        float f3 = 22.0f;
        canvas2.drawText(fmtCount(this.board == null ? 0 : this.board.ballsLeft()), Ui.dpf(22.0f) + fDpf2, Ui.spf(7.5f) + height, this.text);
        float fDpf3 = Ui.dpf(48.0f) / 2.0f;
        float fBallCentreX = ballCentreX();
        float f4 = 1.0f;
        float f5 = this.pressingButton ? 0.94f : 1.0f;
        this.fill.setColor(this.tokens.surfaceContainerHighest);
        float f6 = fDpf3 * f5;
        canvas2.drawCircle(fBallCentreX, height, f6, this.fill);
        if (this.hoverButton || this.pressingButton) {
            this.fill.setColor(Ui.alpha(this.tokens.onSurface, this.pressingButton ? 0.12f : 0.08f));
            canvas2.drawCircle(fBallCentreX, height, f6, this.fill);
        }
        if (z2) {
            this.stroke.setColor(Ui.alpha(this.tokens.primary, 0.85f));
            this.stroke.setStrokeWidth(Ui.dpf(2.2f));
            this.stroke.setStrokeCap(Paint.Cap.ROUND);
            int i = 0;
            while (i < 8) {
                double d = (((double) this.spin) * 0.7d) + ((((double) i) * 3.141592653589793d) / 4.0d);
                float f7 = 0.98f * fDpf3;
                float f8 = fDpf3;
                float fSin = f8 * ((((float) Math.sin((this.spin * 3.0f) + i)) * 0.12f) + 1.16f);
                canvas2.drawLine((((float) Math.cos(d)) * f7) + fBallCentreX, height + (((float) Math.sin(d)) * f7), (((float) Math.cos(d)) * fSin) + fBallCentreX, (((float) Math.sin(d)) * fSin) + height, this.stroke);
                i++;
                canvas2 = canvas;
                z2 = z2;
                fDpf3 = f8;
                f3 = f3;
            }
            f = f3;
            f2 = fDpf3;
            z = z2;
        } else {
            f = 22.0f;
            f2 = fDpf3;
            z = z2;
        }
        float fDpf4 = (f2 - Ui.dpf(6.0f)) * f5;
        float f9 = this.spin;
        if (z3) {
            f4 = 0.42f;
        } else if (z) {
            f4 = 1.35f;
        }
        drawBall(canvas, fBallCentreX, height, fDpf4, f9, f4);
        float width = (getWidth() - this.insetRight) - Ui.dpf(18.0f);
        this.text.setColor(this.tokens.onSurface);
        this.text.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(fmtTime(this.board == null ? 0 : this.board.elapsedSeconds()), width - Ui.dpf(f), Ui.spf(7.5f) + height, this.text);
        this.stroke.setColor(this.tokens.onSurfaceVariant);
        this.stroke.setStrokeWidth(Ui.dpf(1.4f));
        float fDpf5 = width - Ui.dpf(8.0f);
        float fDpf6 = Ui.dpf(8.0f);
        canvas.drawCircle(fDpf5, height, fDpf6, this.stroke);
        canvas.drawLine(fDpf5, height, fDpf5, height - (0.55f * fDpf6), this.stroke);
        canvas.drawLine(fDpf5, height, fDpf5 + (fDpf6 * 0.45f), height, this.stroke);
    }

    private static String fmtCount(int i) {
        String str = "0";
        if (i < 0) {
            int i2 = -i;
            return "-" + (Math.min(99, i2) >= 10 ? "" : "0") + Math.min(99, i2);
        }
        if (i < 10) {
            str = "00";
        } else if (i >= 100) {
            str = "";
        }
        return str + Math.min(999, i);
    }

    private static String fmtTime(int i) {
        int i2 = i / 60;
        int i3 = i % 60;
        return i2 + ":" + (i3 < 10 ? "0" : "") + i3;
    }

    private void drawBall(Canvas canvas, float f, float f2, float f3, float f4, float f5) {
        Glyphs.ball(canvas, this.fill, f, f2, f3, Ui.mix(this.tokens.primary, -15988202, 0.28f), f4, f5, 6);
    }
}
