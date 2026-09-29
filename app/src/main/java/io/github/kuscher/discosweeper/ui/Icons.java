package io.github.kuscher.discosweeper.ui;

import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/* JADX INFO: loaded from: classes.dex */
public final class Icons {
    public static final int BULB = 9;
    public static final int CHECK = 0;
    public static final int COLLAPSE = 3;
    public static final int CORNERS = 7;
    public static final int EXPAND = 2;
    public static final int GRID = 6;
    public static final int INFO = 5;
    public static final int SHADES = 8;
    public static final int SHUFFLE = 4;
    public static final int SPARKLE = 1;
    private static final Path P = new Path();
    private static final int[] MARK_COLORS = {-7709697, -13246224, -41048};
    private static final float[][] MARK_POS = {new float[]{-0.22f, -0.16f}, new float[]{0.24f, -0.12f}, new float[]{0.02f, 0.26f}};

    private Icons() {
    }

    public static void draw(Canvas canvas, int i, float f, float f2, float f3, int i2, Paint paint) {
        Paint paint2 = paint;
        paint2.setColor(i2);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(0.115f * f3);
        paint2.setStrokeCap(Paint.Cap.ROUND);
        paint2.setStrokeJoin(Paint.Join.ROUND);
        paint2.setAntiAlias(true);
        float f4 = f3 / 2.0f;
        P.rewind();
        switch (i) {
            case 0:
                P.moveTo(f - (0.62f * f4), (0.05f * f4) + f2);
                P.lineTo(f - (0.18f * f4), f2 + (0.52f * f4));
                P.lineTo(f + (0.66f * f4), f2 - (f4 * 0.5f));
                canvas.drawPath(P, paint2);
                break;
            case 1:
                star(P, f - (0.12f * f4), f2 - (0.06f * f4), 0.86f * f4, 0.3f * f4);
                paint2.setStyle(Paint.Style.FILL);
                canvas.drawPath(P, paint2);
                P.rewind();
                star(P, (0.6f * f4) + f, (0.58f * f4) + f2, 0.4f * f4, f4 * 0.14f);
                canvas.drawPath(P, paint2);
                break;
            case 2:
                float f5 = f4 * 0.75f;
                float f6 = f4 * 0.62f;
                paint2 = paint;
                corner(canvas, paint2, f - f5, f2 - f5, f6, 1, 1);
                corner(canvas, paint2, f + f5, f2 + f5, f6, -1, -1);
                break;
            case 3:
                float f7 = 0.1f * f4;
                float f8 = f4 * 0.62f;
                corner(canvas, paint2, f - f7, f2 - f7, f8, -1, -1);
                paint2 = paint;
                corner(canvas, paint2, f + f7, f2 + f7, f8, 1, 1);
                break;
            case 4:
                float f9 = 0.72f * f4;
                float f10 = f - f9;
                float f11 = 0.42f * f4;
                float f12 = f2 - f11;
                P.moveTo(f10, f12);
                float f13 = f - (0.24f * f4);
                P.lineTo(f13, f12);
                float f14 = (0.34f * f4) + f;
                float f15 = f11 + f2;
                P.lineTo(f14, f15);
                float f16 = f9 + f;
                P.lineTo(f16, f15);
                P.moveTo(f10, f15);
                P.lineTo(f13, f15);
                P.lineTo(f + (0.1f * f4), f2 - (0.06f * f4));
                P.moveTo(f14, f12);
                P.lineTo(f16, f12);
                canvas.drawPath(P, paint2);
                P.rewind();
                float f17 = (0.44f * f4) + f;
                float f18 = 0.7f * f4;
                P.moveTo(f17, f2 - f18);
                float f19 = (0.74f * f4) + f;
                P.lineTo(f19, f12);
                float f20 = f4 * 0.14f;
                P.lineTo(f17, f2 - f20);
                P.moveTo(f17, f20 + f2);
                P.lineTo(f19, f15);
                P.lineTo(f17, f2 + f18);
                canvas.drawPath(P, paint2);
                break;
            case 5:
                paint2.setStrokeWidth(0.095f * f3);
                canvas.drawCircle(f, f2, 0.86f * f4, paint2);
                paint2.setStyle(Paint.Style.FILL);
                canvas.drawCircle(f, f2 - (0.4f * f4), 0.062f * f3, paint2);
                paint2.setStyle(Paint.Style.STROKE);
                P.moveTo(f, f2 - (0.1f * f4));
                P.lineTo(f, f2 + (f4 * 0.46f));
                canvas.drawPath(P, paint2);
                break;
            case 6:
                float f21 = 0.62f * f4;
                float f22 = f4 * 0.14f;
                int i3 = 0;
                while (i3 < 4) {
                    float f23 = f21 / 2.0f;
                    float f24 = (f22 / 2.0f) + f23;
                    float f25 = (i3 % 2 == 0 ? -1 : 1) * f24;
                    float f26 = (i3 < 2 ? -1 : 1) * f24;
                    P.rewind();
                    float f27 = f25 + f;
                    float f28 = f2 + f26;
                    float f29 = f3 * 0.07f;
                    P.addRoundRect(f27 - f23, f28 - f23, f27 + f23, f28 + f23, f29, f29, Path.Direction.CW);
                    canvas.drawPath(P, paint2);
                    i3++;
                }
                break;
            case 7:
                float f30 = f4 * 0.78f;
                float f31 = f - f30;
                float f32 = (0.55f * f30) + f2;
                P.moveTo(f31, f32);
                float f33 = 0.3f * f30;
                float f34 = f2 - f33;
                P.lineTo(f31, f34);
                float f35 = f2 - f30;
                P.quadTo(f31, f35, f - f33, f35);
                P.lineTo(f + f33, f35);
                float f36 = f + f30;
                P.quadTo(f36, f35, f36, f34);
                P.lineTo(f36, f32);
                canvas.drawPath(P, paint2);
                break;
            case 8:
                float f37 = 0.62f * f4;
                float f38 = 0.56f * f4;
                float f39 = f4 * 0.2f;
                float f40 = (f37 + f39) / 2.0f;
                for (int i4 = -1; i4 <= 1; i4 += 2) {
                    float f41 = (i4 * f40) + f;
                    P.rewind();
                    float f42 = f37 / 2.0f;
                    float f43 = f38 / 2.0f;
                    float f44 = f38 * 0.42f;
                    P.addRoundRect(f41 - f42, f2 - f43, f41 + f42, f2 + f43, f44, f44, Path.Direction.CW);
                    canvas.drawPath(P, paint2);
                }
                P.rewind();
                float f45 = f39 * 0.9f;
                float f46 = f2 - (f38 * 0.18f);
                P.moveTo(f - f45, f46);
                P.lineTo(f + f45, f46);
                canvas.drawPath(P, paint2);
                break;
            case 9:
                float f47 = f4 * 0.52f;
                float f48 = 0.7f * f4;
                P.addArc(f - f47, f2 - f48, f + f47, ((f2 + f47) - f48) + f47, 200.0f, 320.0f);
                canvas.drawPath(P, paint2);
                P.rewind();
                float f49 = 0.52f * f47;
                float f50 = (0.32f * f4) + f2;
                P.moveTo(f - f49, f50);
                P.lineTo(f + f49, f50);
                float f51 = f47 * 0.34f;
                float f52 = (f4 * 0.66f) + f2;
                P.moveTo(f - f51, f52);
                P.lineTo(f51 + f, f52);
                canvas.drawPath(P, paint2);
                break;
        }
        paint2.setStyle(Paint.Style.FILL);
    }

    private static void star(Path path, float f, float f2, float f3, float f4) {
        float f5 = f2 - f3;
        path.moveTo(f, f5);
        float f6 = f4 * 0.4f;
        float f7 = f + f6;
        float f8 = f2 - f4;
        float f9 = f + f4;
        float f10 = f2 - f6;
        path.cubicTo(f7, f8, f9, f10, f + f3, f2);
        float f11 = f2 + f6;
        float f12 = f2 + f4;
        path.cubicTo(f9, f11, f7, f12, f, f2 + f3);
        float f13 = f - f6;
        float f14 = f - f4;
        path.cubicTo(f13, f12, f14, f11, f - f3, f2);
        path.cubicTo(f14, f10, f13, f8, f, f5);
        path.close();
    }

    private static void corner(Canvas canvas, Paint paint, float f, float f2, float f3, int i, int i2) {
        Path path = new Path();
        float f4 = i2 * f3;
        path.moveTo(f, (f2 - (0.05f * f4)) + f4);
        path.lineTo(f, f2);
        path.lineTo(f + (i * f3), f2);
        canvas.drawPath(path, paint);
    }

    public static void appMark(Canvas canvas, float f, float f2, float f3, Paint paint) {
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setBlendMode(BlendMode.SCREEN);
        float f4 = f3 * 0.4f;
        for (int i = 0; i < 3; i++) {
            float f5 = (MARK_POS[i][0] * f3) + f;
            float f6 = (MARK_POS[i][1] * f3) + f2;
            int i2 = MARK_COLORS[i] & 16777215;
            paint.setShader(new RadialGradient(f5, f6, f4, new int[]{(-16777216) | i2, (-872415232) | i2, i2}, new float[]{0.0f, 0.55f, 1.0f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(f5, f6, f4, paint);
        }
        paint.setShader(null);
        paint.setBlendMode(null);
    }
}
