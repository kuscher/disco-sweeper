package io.github.kuscher.discosweeper.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;

/* JADX INFO: loaded from: classes.dex */
public final class Glyphs {
    private Glyphs() {
    }

    public static void ball(Canvas canvas, Paint paint, float f, float f2, float f3, int i, float f4, float f5, int i2) {
        float f6;
        float fSin;
        boolean z;
        boolean z2;
        Paint paint2 = paint;
        paint2.setShader(null);
        paint2.setColor(-16250866);
        canvas.drawCircle(f, f2, f3, paint2);
        float f7 = 2.0f;
        float f8 = (f3 * 2.0f) / i2;
        float fMax = Math.max(0.35f, 0.12f * f8);
        float f9 = f4 * 1.6f;
        boolean z3 = false;
        boolean z4 = f4 == 0.0f;
        int i3 = 0;
        while (i3 < i2) {
            int i4 = 0;
            while (i4 < i2) {
                float f10 = -f3;
                float f11 = i4;
                float f12 = f10 + (f11 * f8);
                float f13 = f7;
                float f14 = i3;
                float f15 = f10 + (f14 * f8);
                float f16 = f8 / f13;
                float f17 = f12 + f16;
                float f18 = f15 + f16;
                int i5 = i3;
                int i6 = i4;
                float fHypot = ((float) Math.hypot(f17, f18)) / f3;
                if (fHypot > 0.94f) {
                    z2 = false;
                } else {
                    float fMax2 = Math.max(0.0f, (((-f17) / f3) * 0.5f) + (((-f18) / f3) * 0.62f) + (((float) Math.sqrt(Math.max(0.0f, 1.0f - Math.min(1.0f, fHypot * fHypot)))) * 0.55f));
                    if (!z4) {
                        f6 = 0.5f;
                        fSin = ((0.7f * fMax2) + 0.16f) * ((((((float) Math.sin(f9 + (f11 * 1.1f) + (f14 * 0.4f))) * 0.5f) + 0.5f) * 0.45f) + 0.75f) * f5;
                    } else {
                        fSin = ((0.75f * fMax2) + 0.18f) * f5;
                        f6 = 0.5f;
                    }
                    int iMix = Ui.mix(i, -1, Ui.clamp(fSin, 0.0f, 1.0f));
                    if (z4) {
                        z = ((i6 * 3) + (i5 * 5)) % 7 == 0 && fMax2 > 0.72f;
                    } else {
                        z = (((float) Math.sin((double) ((f9 + (f11 * 1.1f)) + (f14 * 0.4f)))) * f6) + f6 > 0.93f && fMax2 > 0.55f;
                    }
                    paint2.setColor((!z || f5 <= 0.6f) ? iMix : -1);
                    float f19 = f + f12;
                    float f20 = f2 + f15;
                    z2 = false;
                    canvas.drawRect(f19 + fMax, f20 + fMax, (f19 + f8) - fMax, (f20 + f8) - fMax, paint2);
                }
                i4 = i6 + 1;
                paint2 = paint;
                f7 = f13;
                i3 = i5;
                z3 = z2;
            }
            i3++;
            paint2 = paint;
        }
    }

    public static void ball(Canvas canvas, Paint paint, float f, float f2, float f3, Tokens tokens) {
        ball(canvas, paint, f, f2, f3, Ui.mix(tokens.primary, -15988202, 0.3f), 0.0f, 1.0f, 5);
    }

    public static void shades(Canvas canvas, Paint paint, Paint paint2, RectF rectF, float f, float f2, float f3, int i, int i2, boolean z, int i3) {
        float f4 = 0.4f;
        float f5 = f3 * 0.4f;
        float f6 = f3 * 0.34f;
        float f7 = 0.13f * f3;
        float f8 = (f5 + f7) / 2.0f;
        paint.setShader(null);
        paint.setColor(i);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setColor(i2);
        float f9 = f3 * 0.075f;
        paint2.setStrokeWidth(Math.max(1.0f, f9));
        paint2.setStrokeCap(Paint.Cap.ROUND);
        int i4 = -1;
        while (i4 <= 1) {
            float f10 = f + (i4 * f8);
            float f11 = f5 / 2.0f;
            float f12 = f4;
            float f13 = f6 / 2.0f;
            rectF.set(f10 - f11, f2 - f13, f10 + f11, f2 + f13);
            float f14 = 0.44f * f6;
            canvas.drawRoundRect(rectF, f14, f14, paint);
            canvas.drawRoundRect(rectF, f14, f14, paint2);
            i4 += 2;
            f4 = f12;
        }
        float f15 = f4;
        float f16 = f7 * 0.95f;
        float f17 = f2 - (0.2f * f6);
        canvas.drawLine(f - f16, f17, f + f16, f17, paint2);
        for (int i5 = -1; i5 <= 1; i5 += 2) {
            float f18 = i5;
            float f19 = f + (((f5 / 2.0f) + f8) * f18);
            canvas.drawLine(f19, f2 - (0.26f * f6), (f18 * f3 * 0.09f) + f19, f2 - (f6 * f15), paint2);
        }
        if (z) {
            paint2.setColor(i3);
            paint2.setStrokeWidth(Math.max(2.0f, f9));
            float f20 = 0.38f * f3;
            float f21 = f - f20;
            float f22 = f2 - f20;
            float f23 = f + f20;
            float f24 = f2 + f20;
            canvas.drawLine(f21, f22, f23, f24, paint2);
            canvas.drawLine(f21, f24, f23, f22, paint2);
            return;
        }
        paint2.setColor(-1);
        paint2.setStrokeWidth(Math.max(1.0f, 0.042f * f3));
        float f25 = (f - f8) - (f5 * 0.16f);
        float f26 = f2 - (0.16f * f6);
        float f27 = f5 * 0.08f;
        float f28 = f6 * 0.09f;
        canvas.drawLine(f25 - f27, f26 + f28, f25 + f27, f26 - f28, paint2);
    }
}
