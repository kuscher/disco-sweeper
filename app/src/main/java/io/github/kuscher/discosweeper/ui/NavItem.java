package io.github.kuscher.discosweeper.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;

/* JADX INFO: loaded from: classes.dex */
public final class NavItem extends FrameLayout {
    private static final float H = 68.0f;
    private static final float ICON = 46.0f;
    private static final float PAD = 12.0f;
    private static final float R_CIRCLE = 23.0f;
    private static final float R_SQUARE = 14.0f;
    private static final float TEXT_GAP = 14.0f;
    private boolean active;
    private final Paint badge;
    private final Paint bg;
    private float expand;
    private float hover;
    private ValueAnimator hoverAnim;
    private final View icon;
    private float iconRadiusDp;
    private String kicker;
    private final Paint kickerP;
    private float pressAmt;
    private final RectF rect;
    private float sel;
    private ValueAnimator selAnim;
    private String title;
    private final Paint titleP;
    private Tokens tokens;

    public NavItem(Context context, View view) {
        super(context);
        this.bg = new Paint(1);
        this.titleP = new Paint(1);
        this.kickerP = new Paint(1);
        this.badge = new Paint(1);
        this.rect = new RectF();
        this.title = "";
        this.kicker = "";
        this.iconRadiusDp = 14.0f;
        this.expand = 1.0f;
        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);
        this.titleP.setTypeface(Ui.weight(600));
        this.titleP.setTextSize(Ui.spf(15.0f));
        this.titleP.setSubpixelText(true);
        this.kickerP.setTypeface(Ui.weight(400));
        this.kickerP.setTextSize(Ui.spf(12.5f));
        this.kickerP.setSubpixelText(true);
        this.icon = view;
        view.setOutlineProvider(new ViewOutlineProvider() { // from class: io.github.kuscher.discosweeper.ui.NavItem.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view2, Outline outline) {
                outline.setRoundRect(0, 0, view2.getWidth(), view2.getHeight(), Ui.dpf(NavItem.this.iconRadiusDp));
            }
        });
        view.setClipToOutline(true);
        addView(view, new FrameLayout.LayoutParams(Ui.dp(46.0f), Ui.dp(46.0f)));
    }

    public NavItem text(String str, String str2) {
        this.title = str;
        this.kicker = str2;
        setContentDescription(str + ", " + str2);
        invalidate();
        return this;
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
        invalidate();
    }

    private void applyShape() {
        float fClamp = Ui.clamp(this.sel, 0.0f, 1.3f);
        float f = (0.07f * fClamp) + 1.0f;
        this.icon.setScaleX(f);
        this.icon.setScaleY(f);
        this.iconRadiusDp = Ui.lerp(23.0f, (fClamp * 3.0f) + 14.0f, this.expand);
        this.icon.invalidateOutline();
    }

    public void setSelected(boolean z, boolean z2) {
        float f = z ? 1.0f : 0.0f;
        if (this.selAnim != null) {
            this.selAnim.cancel();
        }
        if (!z2) {
            this.sel = f;
            applyShape();
            invalidate();
        } else {
            this.selAnim = ValueAnimator.ofFloat(this.sel, f);
            this.selAnim.setDuration(z ? 460L : 240L);
            if (z) {
                this.selAnim.setInterpolator(new OvershootInterpolator(1.6f));
            }
            this.selAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.NavItem.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    NavItem.this.lambda$setSelected$0(valueAnimator);
                }
            });
            this.selAnim.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setSelected$0(ValueAnimator valueAnimator) {
        this.sel = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        applyShape();
        invalidate();
    }

    @Override // android.widget.FrameLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(View.MeasureSpec.getSize(i), Ui.dp(68.0f));
        measureChildren(i, i2);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int iDp = Ui.dp(46.0f);
        int iRound = Math.round(Ui.lerp(((i3 - i) - iDp) / 2.0f, Ui.dpf(12.0f), this.expand));
        int height = (getHeight() - iDp) / 2;
        this.icon.layout(iRound, height, iRound + iDp, iDp + height);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.tokens == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float fDpf = Ui.dpf(Ui.lerp(22.0f, 26.0f, Ui.clamp(this.sel, 0.0f, 1.0f)));
        this.rect.set(0.0f, 0.0f, width, height);
        if (this.sel > 0.001f) {
            this.bg.setColor(Ui.alpha(this.tokens.secondaryContainer, Ui.clamp(this.sel, 0.0f, 1.0f)));
            canvas.drawRoundRect(this.rect, fDpf, fDpf, this.bg);
        }
        float fMax = Math.max(this.hover * 0.07f, this.pressAmt * 0.11f);
        if (fMax > 0.001f) {
            this.bg.setColor(Ui.alpha(this.tokens.onSurface, fMax));
            canvas.drawRoundRect(this.rect, fDpf, fDpf, this.bg);
        }
        float fSmoothstep = Ui.smoothstep(0.42f, 1.0f, this.expand);
        if (fSmoothstep > 0.01f) {
            float fDpf2 = Ui.dpf(12.0f) + Ui.dpf(46.0f) + Ui.dpf(14.0f);
            this.titleP.setColor(Ui.alpha(Ui.mix(this.tokens.onSurface, this.tokens.onSecondaryContainer, Ui.clamp(this.sel, 0.0f, 1.0f)), fSmoothstep));
            this.kickerP.setColor(Ui.alpha(this.tokens.onSurfaceVariant, fSmoothstep));
            Paint.FontMetrics fontMetrics = this.titleP.getFontMetrics();
            float fDpf3 = ((height / 2.0f) - Ui.dpf(2.0f)) - ((fontMetrics.ascent + fontMetrics.descent) / 2.0f);
            canvas.drawText(this.title, fDpf2, fDpf3, this.titleP);
            canvas.drawText(this.kicker, fDpf2, fDpf3 + Ui.dpf(17.0f), this.kickerP);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!this.active || this.tokens == null) {
            return;
        }
        float width = this.icon.getWidth() / 2.0f;
        float left = this.icon.getLeft() + width;
        float top = this.icon.getTop() + width;
        float fLerp = Ui.lerp(0.72f * width, width - Ui.dpf(2.0f), this.expand);
        float f = left + fLerp;
        float f2 = top - fLerp;
        float fDpf = Ui.dpf(9.0f);
        this.badge.setStyle(Paint.Style.FILL);
        this.badge.setColor(this.tokens.surfaceContainer);
        canvas.drawCircle(f, f2, Ui.dpf(1.5f) + fDpf, this.badge);
        this.badge.setColor(this.tokens.primary);
        canvas.drawCircle(f, f2, fDpf, this.badge);
        Icons.draw(canvas, 0, f, f2, fDpf * 1.25f, this.tokens.onPrimary, this.badge);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.pressAmt = 1.0f;
                invalidate();
                break;
            case 1:
            case 3:
                this.pressAmt = 0.0f;
                invalidate();
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

    private void animateHover(float f) {
        if (this.hoverAnim != null) {
            this.hoverAnim.cancel();
        }
        this.hoverAnim = ValueAnimator.ofFloat(this.hover, f);
        this.hoverAnim.setDuration(150L);
        this.hoverAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: io.github.kuscher.discosweeper.ui.NavItem.0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                NavItem.this.lambda$animateHover$1(valueAnimator);
            }
        });
        this.hoverAnim.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$animateHover$1(ValueAnimator valueAnimator) {
        this.hover = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        invalidate();
    }
}
