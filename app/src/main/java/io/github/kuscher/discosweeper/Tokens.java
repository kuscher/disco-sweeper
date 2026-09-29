package io.github.kuscher.discosweeper;

import android.content.Context;
import android.graphics.Color;

/* JADX INFO: loaded from: classes.dex */
public final class Tokens {
    private static final float[] TMP = new float[3];
    public final boolean dark;
    public final int onPrimary;
    public final int onPrimaryContainer;
    public final int onSecondaryContainer;
    public final int onSurface;
    public final int onSurfaceVariant;
    public final int outline;
    public final int outlineVariant;
    public final int primary;
    public final int primaryContainer;
    public final int ripple;
    public final int scrim;
    public final int secondaryContainer;
    public final int surface;
    public final int surfaceContainer;
    public final int surfaceContainerHigh;
    public final int surfaceContainerHighest;
    public final int surfaceContainerLow;

    public static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static Tokens of(Context context, int i) {
        return new Tokens(isNight(context), i);
    }

    public Tokens(boolean z, int i) {
        this.dark = z;
        float fHue = hue(i);
        float fSat = sat(i);
        if (z) {
            this.surface = hsl(fHue, 0.1f, 0.07f);
            this.surfaceContainerLow = hsl(fHue, 0.1f, 0.093f);
            this.surfaceContainer = hsl(fHue, 0.1f, 0.115f);
            this.surfaceContainerHigh = hsl(fHue, 0.09f, 0.155f);
            this.surfaceContainerHighest = hsl(fHue, 0.08f, 0.2f);
            this.onSurface = hsl(fHue, 0.06f, 0.91f);
            this.onSurfaceVariant = hsl(fHue, 0.08f, 0.76f);
            this.outline = hsl(fHue, 0.08f, 0.56f);
            this.outlineVariant = hsl(fHue, 0.08f, 0.28f);
            this.primary = hsl(fHue, Math.max(0.62f, fSat * 0.9f), 0.78f);
            this.onPrimary = hsl(fHue, 0.55f, 0.15f);
            this.primaryContainer = hsl(fHue, Math.max(0.5f, fSat * 0.8f), 0.3f);
            this.onPrimaryContainer = hsl(fHue, 0.55f, 0.9f);
            this.secondaryContainer = hsl(fHue, 0.22f, 0.23f);
            this.onSecondaryContainer = hsl(fHue, 0.3f, 0.9f);
            this.scrim = -872415232;
            this.ripple = 587202559;
            return;
        }
        this.surface = hsl(fHue, 0.3f, 0.985f);
        this.surfaceContainerLow = hsl(fHue, 0.28f, 0.965f);
        this.surfaceContainer = hsl(fHue, 0.26f, 0.94f);
        this.surfaceContainerHigh = hsl(fHue, 0.24f, 0.91f);
        this.surfaceContainerHighest = hsl(fHue, 0.22f, 0.88f);
        this.onSurface = hsl(fHue, 0.25f, 0.11f);
        this.onSurfaceVariant = hsl(fHue, 0.18f, 0.32f);
        this.outline = hsl(fHue, 0.12f, 0.52f);
        this.outlineVariant = hsl(fHue, 0.16f, 0.8f);
        this.primary = hsl(fHue, Math.max(0.55f, 0.95f * fSat), 0.4f);
        this.onPrimary = -1;
        this.primaryContainer = hsl(fHue, Math.max(0.6f, fSat), 0.87f);
        this.onPrimaryContainer = hsl(fHue, 0.6f, 0.18f);
        this.secondaryContainer = hsl(fHue, 0.35f, 0.895f);
        this.onSecondaryContainer = hsl(fHue, 0.4f, 0.17f);
        this.scrim = -1728053248;
        this.ripple = 436207616;
    }

    private static float hue(int i) {
        float f;
        synchronized (TMP) {
            Color.colorToHSV(i, TMP);
            f = TMP[0];
        }
        return f;
    }

    private static float sat(int i) {
        float f;
        synchronized (TMP) {
            Color.colorToHSV(i, TMP);
            f = TMP[1];
        }
        return f;
    }

    public static int hsl(float f, float f2, float f3) {
        float f4 = ((f % 360.0f) + 360.0f) % 360.0f;
        float fAbs = (1.0f - Math.abs((f3 * 2.0f) - 1.0f)) * f2;
        float fAbs2 = (1.0f - Math.abs(((f4 / 60.0f) % 2.0f) - 1.0f)) * fAbs;
        float f5 = f3 - (fAbs / 2.0f);
        float f6 = 0.0f;
        if (f4 < 60.0f) {
            fAbs2 = 0.0f;
            f6 = fAbs2;
        } else if (f4 < 120.0f) {
            fAbs2 = 0.0f;
            f6 = fAbs;
            fAbs = fAbs2;
        } else if (f4 < 180.0f) {
            fAbs = 0.0f;
            f6 = fAbs;
        } else if (f4 < 240.0f) {
            fAbs = 0.0f;
            f6 = fAbs2;
            fAbs2 = fAbs;
        } else if (f4 < 300.0f) {
            fAbs2 = fAbs;
            fAbs = fAbs2;
        }
        return Color.argb(255, Math.round((fAbs + f5) * 255.0f), Math.round((f6 + f5) * 255.0f), Math.round((fAbs2 + f5) * 255.0f));
    }
}
