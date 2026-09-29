package io.github.kuscher.discosweeper;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewOutlineProvider;

/* JADX INFO: loaded from: classes.dex */
public final class Ui {
    private static float density = 2.0f;
    private static float scaled = 2.0f;

    private Ui() {
    }

    public static void init(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        density = displayMetrics.density;
        scaled = displayMetrics.scaledDensity;
    }

    public static int dp(float f) {
        return Math.round(f * density);
    }

    public static float dpf(float f) {
        return f * density;
    }

    public static int sp(float f) {
        return Math.round(f * scaled);
    }

    public static float spf(float f) {
        return f * scaled;
    }

    public static GradientDrawable round(int i, float f) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        gradientDrawable.setCornerRadius(dpf(f));
        return gradientDrawable;
    }

    public static GradientDrawable round(int i, float f, float f2, float f3, float f4) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i);
        float fDpf = dpf(f);
        float fDpf2 = dpf(f2);
        float fDpf3 = dpf(f3);
        float fDpf4 = dpf(f4);
        gradientDrawable.setCornerRadii(new float[]{fDpf, fDpf, fDpf2, fDpf2, fDpf3, fDpf3, fDpf4, fDpf4});
        return gradientDrawable;
    }

    public static GradientDrawable stroked(int i, int i2, float f, float f2) {
        GradientDrawable gradientDrawableRound = round(i, f2);
        gradientDrawableRound.setStroke(dp(f), i2);
        return gradientDrawableRound;
    }

    public static Drawable ripple(Drawable drawable, int i) {
        return new RippleDrawable(ColorStateList.valueOf(i), drawable, null);
    }

    public static Drawable rippleOnly(int i, float f) {
        return new RippleDrawable(ColorStateList.valueOf(i), null, round(-1, f));
    }

    public static void clipRound(View view, final float f) {
        view.setOutlineProvider(new ViewOutlineProvider() { // from class: io.github.kuscher.discosweeper.Ui.1
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view2, Outline outline) {
                outline.setRoundRect(0, 0, view2.getWidth(), view2.getHeight(), Ui.dpf(f));
            }
        });
        view.setClipToOutline(true);
    }

    public static Typeface weight(int i) {
        return Typeface.create(Typeface.SANS_SERIF, i, false);
    }

    public static Paint textPaint(float f, int i, int i2) {
        Paint paint = new Paint(1);
        paint.setSubpixelText(true);
        paint.setTextSize(spf(f));
        paint.setTypeface(weight(i));
        paint.setColor(i2);
        return paint;
    }

    public static float clamp(float f, float f2, float f3) {
        if (f < f2) {
            return f2;
        }
        return f > f3 ? f3 : f;
    }

    public static float lerp(float f, float f2, float f3) {
        return f + ((f2 - f) * f3);
    }

    public static float approach(float f, float f2, float f3, float f4) {
        return f + ((f2 - f) * (1.0f - ((float) Math.exp((-f3) * f4))));
    }

    public static float smoothstep(float f, float f2, float f3) {
        float fClamp = clamp((f3 - f) / (f2 - f), 0.0f, 1.0f);
        return fClamp * fClamp * (3.0f - (fClamp * 2.0f));
    }

    public static int alpha(int i, float f) {
        return (i & 16777215) | (Math.round(Color.alpha(i) * clamp(f, 0.0f, 1.0f)) << 24);
    }

    public static int mix(int i, int i2, float f) {
        float fClamp = clamp(f, 0.0f, 1.0f);
        return Color.argb(Math.round(Color.alpha(i) + ((Color.alpha(i2) - Color.alpha(i)) * fClamp)), Math.round(Color.red(i) + ((Color.red(i2) - Color.red(i)) * fClamp)), Math.round(Color.green(i) + ((Color.green(i2) - Color.green(i)) * fClamp)), Math.round(Color.blue(i) + ((Color.blue(i2) - Color.blue(i)) * fClamp)));
    }
}
