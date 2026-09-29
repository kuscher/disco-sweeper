package io.github.kuscher.discosweeper.view;

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;

/* JADX INFO: loaded from: classes.dex */
public final class DiscoOverlay extends View implements Choreographer.FrameCallback {
    private static final float BPM = 116.0f;
    private static final int CONFETTI = 80;
    private static final float END = 3.25f;
    private static final int FACET_COLS = 22;
    private static final int FACET_ROWS = 11;
    private static final int FAN_A = 9;
    private static final int FAN_B = 13;
    private static final int SPARKS = 54;
    private float anim;
    private final Paint ballPaint;
    private Shader ballShader;
    private Shader[] beamA;
    private Shader[] beamB;
    private final Paint beamPaint;
    private String caption;
    private final Paint confPaint;
    private Flake[] confetti;
    private final Paint facetPaint;
    private float[] facetShade;
    private float fromR;
    private float fromX;
    private float fromY;
    private Shader haloShader;
    private OnDone onDone;
    private boolean playing;
    private final RectF rect;
    private final Paint ringPaint;
    private long rnd;
    private final Paint scrimPaint;
    private boolean skipped;
    private float[] sparkF;
    private float[] sparkP;
    private final Paint sparkPaint;
    private float[] sparkS;
    private float[] sparkX;
    private float[] sparkY;
    private long startedAt;
    private final Paint textPaint;
    private Tokens tokens;
    private final Path wedgeA;
    private final Path wedgeB;
    private boolean win;

    public interface OnDone {
        void onDone(boolean z);
    }

    private static final class Flake {
        float amp;
        int color;
        float freq;
        float phase;
        float rot;
        float size;
        float spin;
        float vy;
        float x;
        float y;

        private Flake() {
        }
    }

    public DiscoOverlay(Context context) {
        super(context);
        this.scrimPaint = new Paint(1);
        this.beamPaint = new Paint(1);
        this.facetPaint = new Paint(1);
        this.ballPaint = new Paint(1);
        this.sparkPaint = new Paint(1);
        this.confPaint = new Paint(1);
        this.ringPaint = new Paint(1);
        this.textPaint = new Paint(129);
        this.wedgeA = new Path();
        this.wedgeB = new Path();
        this.rect = new RectF();
        this.rnd = 942871602402086407L;
        this.caption = "";
        setVisibility(8);
        this.beamPaint.setBlendMode(BlendMode.PLUS);
        this.beamPaint.setDither(true);
        this.sparkPaint.setBlendMode(BlendMode.PLUS);
        this.ringPaint.setBlendMode(BlendMode.PLUS);
        this.ringPaint.setStyle(Paint.Style.STROKE);
        this.scrimPaint.setDither(true);
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.textPaint.setTypeface(Ui.weight(700));
        wedge(this.wedgeA, 0.105f);
        wedge(this.wedgeB, 0.062f);
    }

    public void play(float f, float f2, float f3, Tokens tokens, boolean z, String str, OnDone onDone) {
        this.tokens = tokens;
        this.win = z;
        this.caption = str;
        this.onDone = onDone;
        this.fromX = f;
        this.fromY = f2;
        this.fromR = f3;
        this.startedAt = SystemClock.uptimeMillis();
        this.anim = 0.0f;
        this.playing = true;
        this.skipped = false;
        setVisibility(0);
        build();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.playing && motionEvent.getActionMasked() == 0) {
            this.skipped = true;
            finish();
            return true;
        }
        return this.playing;
    }

    private void finish() {
        this.playing = false;
        setVisibility(8);
        if (this.onDone != null) {
            this.onDone.onDone(this.skipped);
        }
    }

    @Override // android.view.Choreographer.FrameCallback
    public void doFrame(long j) {
        if (this.playing) {
            this.anim = (SystemClock.uptimeMillis() - this.startedAt) / 1000.0f;
            if (this.anim >= 3.25f) {
                finish();
            } else {
                invalidate();
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        build();
    }

    private float rnd() {
        this.rnd ^= this.rnd << 13;
        this.rnd ^= this.rnd >>> 7;
        this.rnd ^= this.rnd << 17;
        return ((this.rnd >>> 11) & 16777215) / 1.6777215E7f;
    }

    private float rnd(float f, float f2) {
        return f + (rnd() * (f2 - f));
    }

    private void build() {
        if (getWidth() == 0 || this.tokens == null) {
            return;
        }
        this.rnd = 942871602402086407L;
        float fMax = Math.max(getWidth(), getHeight());
        if (this.beamA == null) {
            this.beamA = new Shader[9];
            this.beamB = new Shader[13];
            for (int i = 0; i < 9; i++) {
                this.beamA[i] = beamShader(Tokens.hsl((i * 360.0f) / 9.0f, 0.95f, 0.58f));
            }
            for (int i2 = 0; i2 < 13; i2++) {
                this.beamB[i2] = beamShader(Tokens.hsl(((i2 * 360.0f) / 13.0f) + 28.0f, 0.88f, 0.64f));
            }
            this.facetShade = new float[242];
            for (int i3 = 0; i3 < this.facetShade.length; i3++) {
                this.facetShade[i3] = rnd(0.45f, 1.0f);
            }
        }
        this.ballShader = new RadialGradient(-0.35f, -0.4f, 1.6f, new int[]{-9537391, -14012865, -15723744}, (float[]) null, Shader.TileMode.CLAMP);
        this.haloShader = new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{1720688895, 779914480, 0}, new float[]{0.0f, 0.45f, 1.0f}, Shader.TileMode.CLAMP);
        this.sparkX = new float[54];
        this.sparkY = new float[54];
        this.sparkF = new float[54];
        this.sparkP = new float[54];
        this.sparkS = new float[54];
        for (int i4 = 0; i4 < 54; i4++) {
            this.sparkX[i4] = rnd(0.0f, 1.0f);
            this.sparkY[i4] = rnd(0.0f, 1.0f);
            this.sparkF[i4] = rnd(0.6f, 2.4f);
            this.sparkP[i4] = rnd(0.0f, 6.283f);
            this.sparkS[i4] = rnd(0.004f, 0.011f);
        }
        this.confetti = new Flake[80];
        for (int i5 = 0; i5 < 80; i5++) {
            Flake flake = new Flake();
            respawn(flake, rnd(-1.2f, 0.0f));
            this.confetti[i5] = flake;
        }
        this.textPaint.setTextSize(Math.max(Ui.spf(20.0f), fMax * 0.026f));
    }

    private void respawn(Flake flake, float f) {
        flake.x = rnd(-0.05f, 1.05f);
        flake.y = f;
        flake.vy = rnd(0.3f, 0.85f);
        flake.amp = rnd(0.01f, 0.045f);
        flake.freq = rnd(0.25f, 0.9f);
        flake.phase = rnd(0.0f, 6.283f);
        flake.size = rnd(0.006f, 0.015f);
        flake.rot = rnd(0.0f, 360.0f);
        flake.spin = rnd(-160.0f, 160.0f);
        flake.color = Tokens.hsl(rnd(0.0f, 360.0f), 0.9f, 0.62f);
    }

    private static void wedge(Path path, float f) {
        path.rewind();
        path.moveTo(0.0f, 0.0f);
        double d = -f;
        path.lineTo((float) Math.cos(d), (float) Math.sin(d));
        double d2 = f;
        path.lineTo((float) Math.cos(d2), (float) Math.sin(d2));
        path.close();
    }

    private static Shader beamShader(int i) {
        int i2 = i & 16777215;
        return new RadialGradient(0.0f, 0.0f, 1.0f, new int[]{(-1073741824) | i2, 1879048192 | i2, 536870912 | i2, i2}, new float[]{0.0f, 0.35f, 0.7f, 1.0f}, Shader.TileMode.CLAMP);
    }

    private float beat() {
        float f = this.anim * 1.9333333f;
        float fFloor = 1.0f - (f - ((float) Math.floor(f)));
        return fFloor * fFloor * fFloor;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float f;
        if (this.tokens == null || this.beamA == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float fMax = Math.max(width, height);
        float f2 = this.anim;
        float fBeat = beat();
        float f3 = 0.0f;
        float fSmoothstep = Ui.smoothstep(0.0f, 0.22f, f2) * (1.0f - Ui.smoothstep(2.75f, 3.25f, f2));
        float fSmoothstep2 = Ui.smoothstep(0.14f, 0.78f, f2);
        float fSmoothstep3 = Ui.smoothstep(0.5f, 0.95f, f2) * (1.0f - Ui.smoothstep(2.3f, 3.0f, f2));
        float fSmoothstep4 = Ui.smoothstep(2.0f, 2.4f, f2) * (1.0f - Ui.smoothstep(2.85f, 3.15f, f2));
        this.scrimPaint.setColor(Ui.alpha(-16317170, 0.88f * fSmoothstep));
        canvas.drawRect(0.0f, 0.0f, width, height, this.scrimPaint);
        float fSmoothstep5 = Ui.smoothstep(0.0f, 0.55f, f2);
        if (fSmoothstep5 > 0.0f && fSmoothstep5 < 1.0f) {
            float f4 = 1.0f - fSmoothstep5;
            this.ringPaint.setStrokeWidth(0.012f * fMax * f4);
            this.ringPaint.setColor(Ui.alpha(this.win ? -8589120 : -45152, f4 * 0.85f));
            canvas.drawCircle(this.fromX, this.fromY, fMax * 0.42f * fSmoothstep5, this.ringPaint);
        }
        float fLerp = Ui.lerp(this.fromX, width * 0.5f, fSmoothstep2);
        float fLerp2 = Ui.lerp(this.fromY, height * 0.42f, fSmoothstep2);
        float fLerp3 = Ui.lerp(this.fromR, 0.085f * fMax * ((0.06f * fBeat) + 1.0f), fSmoothstep2);
        if (fSmoothstep3 <= 0.01f) {
            f = 0.55f;
        } else {
            float f5 = fMax * 1.35f;
            f = 0.55f;
            drawFan(canvas, this.beamA, this.wedgeA, fLerp, fLerp2, f5, this.anim * 26.0f, ((fBeat * 0.45f) + 0.55f) * fSmoothstep3);
            drawFan(canvas, this.beamB, this.wedgeB, fLerp, fLerp2, f5, ((-this.anim) * 17.0f) + 12.0f, fSmoothstep3 * ((fBeat * 0.35f) + 0.42f));
        }
        Canvas canvas2 = canvas;
        drawBall(canvas2, fLerp, fLerp2, fLerp3, fSmoothstep);
        if (fSmoothstep > 0.05f) {
            int i = 0;
            while (i < 54) {
                float fSin = (float) Math.sin((this.anim * this.sparkF[i] * 2.2f) + this.sparkP[i]);
                if (fSin > f3) {
                    float fPow = ((float) Math.pow(fSin, 7.0d)) * ((fBeat * 0.45f) + f) * fSmoothstep;
                    if (fPow >= 0.02f) {
                        float f6 = this.sparkX[i] * width;
                        float f7 = this.sparkY[i] * height;
                        float f8 = this.sparkS[i] * fMax * ((fBeat * 0.5f) + 1.0f);
                        this.sparkPaint.setColor(Ui.alpha(-1, fPow));
                        this.sparkPaint.setStyle(Paint.Style.FILL);
                        canvas2.drawCircle(f6, f7, f8, this.sparkPaint);
                        this.sparkPaint.setStyle(Paint.Style.STROKE);
                        this.sparkPaint.setStrokeWidth(f8 * 0.5f);
                        float f9 = f8 * 3.2f;
                        canvas.drawLine(f6 - f9, f7, f6 + f9, f7, this.sparkPaint);
                        canvas.drawLine(f6, f7 - f9, f6, f7 + f9, this.sparkPaint);
                        canvas2 = canvas;
                    }
                }
                i++;
                f3 = 0.0f;
            }
        }
        if (fSmoothstep2 > 0.4f) {
            for (Flake flake : this.confetti) {
                flake.y += flake.vy * 0.016666668f;
                flake.rot += flake.spin * 0.016666668f;
                if (flake.y > 1.12f) {
                    respawn(flake, -0.12f);
                }
                float fSin2 = flake.x + (flake.amp * ((float) Math.sin((this.anim * flake.freq * 6.283f) + flake.phase)));
                float f10 = flake.size * fMax;
                this.confPaint.setColor(Ui.alpha(flake.color, fSmoothstep));
                canvas2.save();
                canvas2.translate(fSin2 * width, flake.y * height);
                canvas2.rotate(flake.rot);
                canvas2.scale(1.0f, (Math.abs((float) Math.cos(flake.rot * 0.0175f)) * 0.85f) + 0.15f);
                float f11 = -f10;
                this.rect.set(f11, f11 * 1.5f, f10, 1.5f * f10);
                float f12 = f10 * 0.35f;
                canvas2.drawRoundRect(this.rect, f12, f12, this.confPaint);
                canvas2.restore();
            }
        }
        if (fSmoothstep4 > 0.01f) {
            this.textPaint.setColor(Ui.alpha(-1, fSmoothstep4));
            float f13 = width / 2.0f;
            float f14 = height * 0.8f;
            canvas2.drawText(this.caption, f13, f14, this.textPaint);
            this.textPaint.setTextSize(Ui.spf(12.0f));
            this.textPaint.setColor(Ui.alpha(-4278068, 0.9f * fSmoothstep4));
            canvas2.drawText("Click anywhere to skip", f13, f14 + Ui.dpf(30.0f), this.textPaint);
            this.textPaint.setTextSize(Math.max(Ui.spf(20.0f), fMax * 0.026f));
        }
    }

    private void drawFan(Canvas canvas, Shader[] shaderArr, Path path, float f, float f2, float f3, float f4, float f5) {
        int length = shaderArr.length;
        for (int i = 0; i < length; i++) {
            canvas.save();
            canvas.translate(f, f2);
            canvas.rotate(((i * 360.0f) / length) + f4);
            canvas.scale(f3, f3);
            this.beamPaint.setShader(shaderArr[i]);
            this.beamPaint.setAlpha(Math.round(Ui.clamp(f5, 0.0f, 1.0f) * 255.0f));
            canvas.drawPath(path, this.beamPaint);
            canvas.restore();
        }
        this.beamPaint.setShader(null);
    }

    private void drawBall(Canvas canvas, float f, float f2, float f3, float f4) {
        float f5;
        float f6;
        float f7;
        char c;
        float f8 = this.anim * 0.85f;
        canvas.save();
        canvas.translate(f, f2);
        float f9 = 3.1f * f3;
        canvas.scale(f9, f9);
        this.ballPaint.setShader(this.haloShader);
        this.ballPaint.setAlpha(Math.round(216.75f * f4));
        float f10 = 0.0f;
        canvas.drawCircle(0.0f, 0.0f, 1.0f, this.ballPaint);
        canvas.restore();
        this.ballPaint.setShader(null);
        char c2 = 255;
        this.ballPaint.setAlpha(255);
        canvas.save();
        canvas.translate(f, f2);
        canvas.scale(f3, f3);
        this.ballPaint.setShader(this.ballShader);
        float f11 = 255.0f;
        this.ballPaint.setAlpha(Math.round(f4 * 255.0f));
        canvas.drawCircle(0.0f, 0.0f, 1.0f, this.ballPaint);
        this.ballPaint.setShader(null);
        int i = 0;
        while (i < 11) {
            double d = (float) (((((double) (i + 0.5f)) * 3.141592653589793d) / 11.0d) - 1.5707963267948966d);
            float fCos = (float) Math.cos(d);
            float fSin = (float) Math.sin(d);
            int i2 = 0;
            while (i2 < 22) {
                double d2 = ((float) ((((double) i2) * 6.283185307179586d) / 22.0d)) + f8;
                float fCos2 = ((float) Math.cos(d2)) * fCos;
                if (fCos2 <= 0.04f) {
                    f6 = f11;
                    f7 = f10;
                    c = c2;
                    f5 = fSin;
                } else {
                    float fSin2 = ((float) Math.sin(d2)) * fCos;
                    float f12 = this.facetShade[(i * 22) + i2];
                    f5 = fSin;
                    f6 = f11;
                    float fPow = (fCos2 * fCos2 * f12 * 0.42f) + 0.07f + (((float) Math.pow(Math.max(f10, fCos2), 14.0d)) * f12 * 0.95f);
                    f7 = 0.0f;
                    int iRound = Math.round(Ui.clamp(fPow, 0.0f, 1.0f) * f6);
                    c = 255;
                    this.facetPaint.setColor(Ui.alpha(Math.min(255, iRound + 12) | (iRound << 16) | (-16777216) | (iRound << 8), f4));
                    float f13 = (fCos * 0.047727272f) + 0.012f;
                    this.rect.set(fSin2 - f13, f5 - 0.047727272f, fSin2 + f13, f5 + 0.047727272f);
                    canvas.drawRect(this.rect, this.facetPaint);
                }
                i2++;
                f11 = f6;
                f10 = f7;
                c2 = c;
                fSin = f5;
            }
            i++;
            c2 = c2;
        }
        canvas.restore();
    }
}
