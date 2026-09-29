package io.github.kuscher.discosweeper.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;

/* JADX INFO: loaded from: classes.dex */
public final class Toggle extends View {
    private static final float H = 32.0f;
    private static final float THUMB_OFF = 16.0f;
    private static final float THUMB_ON = 24.0f;
    private static final float THUMB_PRESS = 28.0f;
    private static final float W = 52.0f;
    private float hover;
    private ValueAnimator hoverAnim;
    private OnChange listener;
    private boolean on;
    private final Paint p;
    private float pos;
    private ValueAnimator posAnim;
    private float pressAmt;
    private ValueAnimator pressAnim;
    private final RectF rect;
    private Tokens tokens;

    public interface OnChange {
        void onChange(boolean z);
    }

    public Toggle(Context context) {
        super(context);
        this.p = new Paint(1);
        this.rect = new RectF();
        setClickable(true);
        setFocusable(true);
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
        invalidate();
    }

    public void setOnChange(OnChange onChange) {
        this.listener = onChange;
    }

    public boolean isOn() {
        return this.on;
    }

    public void setOn(boolean z, boolean z2) {
        this.on = z;
        if (this.posAnim != null) {
            this.posAnim.cancel();
        }
        if (!z2) {
            this.pos = z ? 1.0f : 0.0f;
            invalidate();
            return;
        }
        this.posAnim = ValueAnimator.ofFloat(this.pos, z ? 1.0f : 0.0f);
        this.posAnim.setDuration(280L);
        this.posAnim.setInterpolator(new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f));
        this.posAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.Toggle.2
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                Toggle.this.lambda$setOn$0(valueAnimator);
            }
        });
        this.posAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setOn$0(ValueAnimator valueAnimator) {
        this.pos = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    @Override // android.view.View
    public boolean performClick() {
        setOn(!this.on, true);
        if (this.listener != null) {
            this.listener.onChange(this.on);
        }
        return super.performClick();
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(Ui.dp(52.0f), Ui.dp(32.0f));
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.tokens == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float f = height / 2.0f;
        int iMix = Ui.mix(this.tokens.surfaceContainerHighest, this.tokens.primary, this.pos);
        this.p.setStyle(Paint.Style.FILL);
        this.p.setColor(iMix);
        this.rect.set(0.0f, 0.0f, width, height);
        canvas.drawRoundRect(this.rect, f, f, this.p);
        if (this.pos < 0.999f) {
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(Ui.dpf(2.0f));
            this.p.setColor(Ui.alpha(this.tokens.outline, 1.0f - this.pos));
            this.rect.set(Ui.dpf(1.0f), Ui.dpf(1.0f), width - Ui.dpf(1.0f), height - Ui.dpf(1.0f));
            canvas.drawRoundRect(this.rect, f, f, this.p);
            this.p.setStyle(Paint.Style.FILL);
        }
        float fLerp = Ui.lerp(Ui.lerp(Ui.dpf(16.0f), Ui.dpf(24.0f), this.pos), Ui.dpf(28.0f), this.pressAmt);
        float fDpf = Ui.dpf(4.0f);
        float f2 = fLerp / 2.0f;
        float fClamp = Ui.clamp(Ui.lerp((Ui.dpf(16.0f) / 2.0f) + fDpf, (width - fDpf) - (Ui.dpf(24.0f) / 2.0f), this.pos), Ui.dpf(2.0f) + f2, (width - f2) - Ui.dpf(2.0f));
        float fMax = Math.max(this.hover * 0.08f, this.pressAmt * 0.12f);
        if (fMax > 0.001f) {
            Paint paint = this.p;
            float f3 = this.pos;
            Tokens tokens = this.tokens;
            paint.setColor(Ui.alpha(f3 > 0.5f ? tokens.primary : tokens.onSurface, fMax));
            canvas.drawCircle(fClamp, f, Ui.dpf(20.0f), this.p);
        }
        this.p.setColor(Ui.mix(this.tokens.outline, this.tokens.onPrimary, this.pos));
        canvas.drawCircle(fClamp, f, f2, this.p);
        if (this.pos > 0.02f) {
            Icons.draw(canvas, 0, fClamp, f, fLerp * 0.62f * this.pos, Ui.alpha(this.tokens.primary, this.pos), this.p);
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                animatePress(1.0f);
                break;
            case 1:
            case 3:
                animatePress(0.0f);
                break;
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override // android.view.View
    public boolean onHoverEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 9:
                animateHover(1.0f);
                break;
            case 10:
                animateHover(0.0f);
                break;
        }
        return super.onHoverEvent(motionEvent);
    }

    private void animatePress(float f) {
        if (this.pressAnim != null) {
            this.pressAnim.cancel();
        }
        this.pressAnim = ValueAnimator.ofFloat(this.pressAmt, f);
        this.pressAnim.setDuration(140L);
        this.pressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.Toggle.0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                Toggle.this.lambda$animatePress$1(valueAnimator);
            }
        });
        this.pressAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animatePress$1(ValueAnimator valueAnimator) {
        this.pressAmt = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }

    private void animateHover(float f) {
        if (this.hoverAnim != null) {
            this.hoverAnim.cancel();
        }
        this.hoverAnim = ValueAnimator.ofFloat(this.hover, f);
        this.hoverAnim.setDuration(150L);
        this.hoverAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.Toggle.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                Toggle.this.lambda$animateHover$2(valueAnimator);
            }
        });
        this.hoverAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animateHover$2(ValueAnimator valueAnimator) {
        this.hover = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }
}
