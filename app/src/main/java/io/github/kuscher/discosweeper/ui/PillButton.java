package io.github.kuscher.discosweeper.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import io.github.kuscher.discosweeper.Ui;

/* JADX INFO: loaded from: classes.dex */
public final class PillButton extends View {
    public static final int CHIP = 3;
    public static final int FILLED = 0;
    public static final int GHOST = 2;
    public static final int TONAL = 1;
    private final Paint bg;
    private int contentColor;
    private int fillColor;
    private float gapDp;
    private final Paint glyph;
    private float heightDp;
    private float hoverAmt;
    private ValueAnimator hoverAnim;
    private int icon;
    private float iconDp;
    private String label;
    private float labelAlpha;
    private int outlineColor;
    private float padHDp;
    private float pressAmt;
    private ValueAnimator pressAnim;
    private float radiusDp;
    private final RectF rect;
    private int style;
    private final Paint text;

    public PillButton(Context context) {
        super(context);
        this.bg = new Paint(1);
        this.text = new Paint(1);
        this.glyph = new Paint(1);
        this.rect = new RectF();
        this.icon = -1;
        this.style = 0;
        this.heightDp = 52.0f;
        this.radiusDp = 26.0f;
        this.padHDp = 24.0f;
        this.iconDp = 19.0f;
        this.gapDp = 9.0f;
        this.labelAlpha = 1.0f;
        setClickable(true);
        setFocusable(true);
        this.text.setTypeface(Ui.weight(600));
        this.text.setTextSize(Ui.spf(15.0f));
        this.text.setSubpixelText(true);
    }

    public PillButton label(String str) {
        this.label = str;
        setContentDescription(str);   // drawn, not a TextView, so screen readers need telling
        requestLayout();
        invalidate();
        return this;
    }

    public PillButton icon(int i) {
        this.icon = i;
        requestLayout();
        invalidate();
        return this;
    }

    public PillButton style(int i) {
        this.style = i;
        invalidate();
        return this;
    }

    public void setLabelAlpha(float f) {
        float fClamp = Ui.clamp(f, 0.0f, 1.0f);
        if (fClamp == this.labelAlpha) {
            return;
        }
        this.labelAlpha = fClamp;
        requestLayout();
        invalidate();
    }

    public PillButton colors(int i, int i2, int i3) {
        this.fillColor = i;
        this.contentColor = i2;
        this.outlineColor = i3;
        invalidate();
        return this;
    }

    public PillButton sizing(float f, float f2, float f3, float f4) {
        this.heightDp = f;
        this.radiusDp = f2;
        this.padHDp = f3;
        this.text.setTextSize(Ui.spf(f4));
        requestLayout();
        return this;
    }

    public int widthWithLabel() {
        float fDpf = Ui.dpf(this.padHDp) * 2.0f;
        if (this.icon >= 0) {
            fDpf += Ui.dpf(this.iconDp) + (this.label != null ? Ui.dpf(this.gapDp) : 0.0f);
        }
        if (this.label != null) {
            fDpf += this.text.measureText(this.label);
        }
        return Math.round(fDpf);
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        float fDpf = Ui.dpf(this.padHDp) * 2.0f;
        if (this.icon >= 0) {
            fDpf += Ui.dpf(this.iconDp) + (this.label != null ? Ui.dpf(this.gapDp) * this.labelAlpha : 0.0f);
        }
        if (this.label != null) {
            fDpf += this.text.measureText(this.label) * this.labelAlpha;
        }
        setMeasuredDimension(resolveSize(Math.round(fDpf), i), resolveSize(Ui.dp(this.heightDp), i2));
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float fMeasureText;
        Canvas canvas2;
        float width = getWidth();
        float height = getHeight();
        if (width <= 0.0f) {
            return;
        }
        float fLerp = Ui.lerp(Ui.dpf(this.radiusDp), Ui.dpf(this.radiusDp) * 0.4f, this.pressAmt);
        float f = 1.0f - (this.pressAmt * 0.035f);
        canvas.save();
        float f2 = height / 2.0f;
        canvas.scale(f, f, width / 2.0f, f2);
        this.rect.set(0.0f, 0.0f, width, height);
        if (this.style == 2) {
            if (this.outlineColor != 0) {
                this.bg.setStyle(Paint.Style.STROKE);
                this.bg.setStrokeWidth(Ui.dpf(1.2f));
                this.bg.setColor(this.outlineColor);
                float fDpf = Ui.dpf(0.6f);
                this.rect.inset(fDpf, fDpf);
                canvas.drawRoundRect(this.rect, fLerp, fLerp, this.bg);
                float f3 = -fDpf;
                this.rect.inset(f3, f3);
                this.bg.setStyle(Paint.Style.FILL);
            }
        } else {
            this.bg.setStyle(Paint.Style.FILL);
            this.bg.setColor(this.fillColor);
            canvas.drawRoundRect(this.rect, fLerp, fLerp, this.bg);
        }
        float fMax = Math.max(this.hoverAmt * 0.08f, this.pressAmt * 0.12f);
        if (fMax > 0.001f) {
            this.bg.setColor(Ui.alpha(this.contentColor, fMax));
            canvas.drawRoundRect(this.rect, fLerp, fLerp, this.bg);
        }
        float fDpf2 = Ui.dpf(this.iconDp);
        if (this.icon >= 0) {
            fMeasureText = (this.label != null ? Ui.dpf(this.gapDp) * this.labelAlpha : 0.0f) + fDpf2 + 0.0f;
        } else {
            fMeasureText = 0.0f;
        }
        if (this.label != null) {
            fMeasureText += this.text.measureText(this.label) * this.labelAlpha;
        }
        float fDpf3 = (width - fMeasureText) / 2.0f;
        if (this.icon >= 0) {
            canvas2 = canvas;
            Icons.draw(canvas2, this.icon, fDpf3 + (fDpf2 / 2.0f), f2, fDpf2, this.contentColor, this.glyph);
            fDpf3 += fDpf2 + (this.label != null ? Ui.dpf(this.gapDp) * this.labelAlpha : 0.0f);
        } else {
            canvas2 = canvas;
        }
        if (this.label != null && this.labelAlpha > 0.01f) {
            Paint.FontMetrics fontMetrics = this.text.getFontMetrics();
            float f4 = f2 - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f);
            this.text.setColor(Ui.alpha(this.contentColor, this.labelAlpha));
            canvas2.drawText(this.label, fDpf3, f4, this.text);
        }
        canvas2.restore();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                animatePress(1.0f, false);
                break;
            case 1:
                animatePress(0.0f, true);
                break;
            case 3:
                animatePress(0.0f, true);
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

    private void animatePress(float f, boolean z) {
        if (this.pressAnim != null) {
            this.pressAnim.cancel();
        }
        this.pressAnim = ValueAnimator.ofFloat(this.pressAmt, f);
        this.pressAnim.setDuration(z ? 320L : 110L);
        if (z) {
            this.pressAnim.setInterpolator(new OvershootInterpolator(2.2f));
        }
        this.pressAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.PillButton.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                PillButton.this.lambda$animatePress$0(valueAnimator);
            }
        });
        this.pressAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animatePress$0(ValueAnimator valueAnimator) {
        this.pressAmt = Ui.clamp(((Float) valueAnimator.getAnimatedValue()).floatValue(), 0.0f, 1.4f);
        invalidate();
    }

    private void animateHover(float f) {
        if (this.hoverAnim != null) {
            this.hoverAnim.cancel();
        }
        this.hoverAnim = ValueAnimator.ofFloat(this.hoverAmt, f);
        this.hoverAnim.setDuration(160L);
        this.hoverAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.PillButton.0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                PillButton.this.lambda$animateHover$1(valueAnimator);
            }
        });
        this.hoverAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animateHover$1(ValueAnimator valueAnimator) {
        this.hoverAmt = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }
}
