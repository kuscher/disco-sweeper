package io.github.kuscher.discosweeper.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;

/* JADX INFO: loaded from: classes.dex */
public final class StepSlider extends View {
    private static final float GAP = 5.0f;
    private static final float HEIGHT = 52.0f;
    private static final float LABEL_GAP = 12.0f;
    private static final float MAX_TRACK = 224.0f;
    private static final float MIN_TRACK = 116.0f;
    private static final float PAD = 18.0f;
    private static final float RADIUS = 26.0f;
    private static final float THUMB_H = 26.0f;
    private static final float THUMB_W = 4.0f;
    private static final float TRACK_H = 10.0f;
    private final Paint bg;
    private float drag;
    private String highLabel;
    private float hover;
    private final Paint labelP;
    private boolean labelsVisible;
    private OnChange listener;
    private String lowLabel;
    private float pos;
    private ValueAnimator posAnim;
    private final RectF rect;
    private int steps;
    private Tokens tokens;
    private int value;
    private final Paint valueP;

    public interface OnChange {
        void onChange(int i);
    }

    public StepSlider(Context context) {
        super(context);
        this.bg = new Paint(1);
        this.labelP = new Paint(1);
        this.valueP = new Paint(1);
        this.rect = new RectF();
        this.lowLabel = "";
        this.highLabel = "";
        this.steps = 5;
        this.value = 0;
        this.labelsVisible = true;
        setClickable(true);
        setFocusable(true);
        this.labelP.setTypeface(Ui.weight(500));
        this.labelP.setTextSize(Ui.spf(11.5f));
        this.labelP.setSubpixelText(true);
        this.valueP.setTypeface(Ui.weight(500));
        this.valueP.setTextSize(Ui.spf(11.5f));
        this.valueP.setSubpixelText(true);
    }

    public StepSlider config(int i, String str, String str2) {
        this.steps = Math.max(2, i);
        this.lowLabel = str;
        this.highLabel = str2;
        invalidate();
        return this;
    }

    public void setOnChange(OnChange onChange) {
        this.listener = onChange;
    }

    public void setLabelsVisible(boolean z) {
        if (this.labelsVisible == z) {
            return;
        }
        this.labelsVisible = z;
        requestLayout();
        invalidate();
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
        invalidate();
    }

    public int value() {
        return this.value;
    }

    public void setValue(int i, boolean z) {
        int iMax = Math.max(0, Math.min(this.steps - 1, i));
        this.value = iMax;
        if (this.posAnim != null) {
            this.posAnim.cancel();
        }
        if (!z) {
            this.pos = iMax;
            invalidate();
            return;
        }
        this.posAnim = ValueAnimator.ofFloat(this.pos, iMax);
        this.posAnim.setDuration(260L);
        this.posAnim.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f));
        this.posAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.StepSlider.0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                StepSlider.this.lambda$setValue$0(valueAnimator);
            }
        });
        this.posAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setValue$0(ValueAnimator valueAnimator) {
        this.pos = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    private void commit(int i, boolean z) {
        int iMax = Math.max(0, Math.min(this.steps - 1, i));
        boolean z2 = iMax != this.value;
        setValue(iMax, z);
        if (!z2 || this.listener == null) {
            return;
        }
        this.listener.onChange(iMax);
    }

    private float sideWidth() {
        if (this.labelsVisible) {
            return this.labelP.measureText(this.lowLabel) + this.valueP.measureText(this.highLabel) + (Ui.dpf(12.0f) * 2.0f);
        }
        return 0.0f;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        float fDpf = (Ui.dpf(18.0f) * 2.0f) + sideWidth();
        int iRound = Math.round(Ui.dpf(224.0f) + fDpf);
        int iRound2 = Math.round(fDpf + Ui.dpf(116.0f));
        if (View.MeasureSpec.getMode(i) == View.MeasureSpec.EXACTLY) {
            iRound = View.MeasureSpec.getSize(i);   // laid out full width: fill it
        } else if (View.MeasureSpec.getMode(i) != 0) {
            iRound = Math.max(Math.min(iRound2, View.MeasureSpec.getSize(i)), Math.min(View.MeasureSpec.getSize(i), iRound));
        }
        setMeasuredDimension(iRound, Ui.dp(52.0f));
    }

    private float trackLeft() {
        return Ui.dpf(18.0f) + (this.labelsVisible ? this.labelP.measureText(this.lowLabel) + Ui.dpf(12.0f) : 0.0f) + (Ui.dpf(4.0f) / 2.0f);
    }

    private float trackRight() {
        return ((getWidth() - Ui.dpf(18.0f)) - (this.labelsVisible ? this.valueP.measureText(this.highLabel) + Ui.dpf(12.0f) : 0.0f)) - (Ui.dpf(4.0f) / 2.0f);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.tokens == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float fDpf = Ui.dpf(26.0f);
        this.bg.setColor(this.tokens.surfaceContainer);
        this.rect.set(0.0f, 0.0f, width, height);
        canvas.drawRoundRect(this.rect, fDpf, fDpf, this.bg);
        float fMax = Math.max(this.hover * 0.05f, this.drag * 0.09f);
        if (fMax > 0.001f) {
            this.bg.setColor(Ui.alpha(this.tokens.onSurface, fMax));
            canvas.drawRoundRect(this.rect, fDpf, fDpf, this.bg);
        }
        float f = height / 2.0f;
        if (this.labelsVisible) {
            Paint.FontMetrics fontMetrics = this.labelP.getFontMetrics();
            float f2 = f - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f);
            this.labelP.setColor(this.tokens.onSurfaceVariant);
            canvas.drawText(this.lowLabel, Ui.dpf(18.0f), f2, this.labelP);
            this.valueP.setColor(this.tokens.onSurfaceVariant);
            canvas.drawText(this.highLabel, (width - Ui.dpf(18.0f)) - this.valueP.measureText(this.highLabel), f2, this.valueP);
        }
        float fTrackLeft = trackLeft();
        float fTrackRight = trackRight();
        float fDpf2 = Ui.dpf(10.0f) / 2.0f;
        float f3 = fTrackRight - fTrackLeft;
        float f4 = ((this.pos / (this.steps - 1)) * f3) + fTrackLeft;
        float fDpf3 = Ui.dpf(5.0f);
        this.bg.setColor(this.tokens.surfaceContainerHighest);
        float f5 = f4 + fDpf3;
        if (fTrackRight > f5) {
            this.rect.set(f5, f - fDpf2, fTrackRight + fDpf2, f + fDpf2);
            canvas.drawRoundRect(this.rect, fDpf2, fDpf2, this.bg);
        }
        this.bg.setColor(this.tokens.primary);
        float f6 = f4 - fDpf3;
        if (f6 > fTrackLeft) {
            this.rect.set(fTrackLeft - fDpf2, f - fDpf2, f6, f + fDpf2);
            canvas.drawRoundRect(this.rect, fDpf2, fDpf2, this.bg);
        }
        float fDpf4 = Ui.dpf(1.6f);
        for (int i = 0; i < this.steps; i++) {
            float f7 = ((i * f3) / (this.steps - 1)) + fTrackLeft;
            if (Math.abs(f7 - f4) >= Ui.dpf(4.0f) + fDpf3) {
                Paint paint = this.bg;
                Tokens tokens = this.tokens;
                paint.setColor(f7 < f4 ? Ui.alpha(tokens.onPrimary, 0.55f) : Ui.alpha(tokens.onSurfaceVariant, 0.45f));
                canvas.drawCircle(f7, f, fDpf4, this.bg);
            }
        }
        float fDpf5 = (Ui.dpf(4.0f) + (Ui.dpf(2.0f) * this.drag)) / 2.0f;
        float fDpf6 = Ui.dpf(26.0f) / 2.0f;
        this.bg.setColor(this.tokens.primary);
        this.rect.set(f4 - fDpf5, f - fDpf6, f4 + fDpf5, f + fDpf6);
        canvas.drawRoundRect(this.rect, fDpf5, fDpf5, this.bg);
    }

    private int stepAt(float f) {
        float fTrackLeft = trackLeft();
        return Math.round(Ui.clamp((f - fTrackLeft) / Math.max(1.0f, trackRight() - fTrackLeft), 0.0f, 1.0f) * (this.steps - 1));
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.drag = 1.0f;
                getParent().requestDisallowInterceptTouchEvent(true);
                commit(stepAt(motionEvent.getX()), true);
                invalidate();
                return true;
            case 1:
            case 3:
                this.drag = 0.0f;
                invalidate();
                return true;
            case 2:
                commit(stepAt(motionEvent.getX()), true);
                return true;
            default:
                return super.onTouchEvent(motionEvent);
        }
    }

    @Override // android.view.View
    public boolean onHoverEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 9:
                this.hover = 1.0f;
                invalidate();
                break;
            case 10:
                this.hover = 0.0f;
                invalidate();
                break;
        }
        return super.onHoverEvent(motionEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 21) {
            commit(this.value - 1, true);
            return true;
        }
        if (i != 22) {
            return super.onKeyDown(i, keyEvent);
        }
        commit(this.value + 1, true);
        return true;
    }
}
