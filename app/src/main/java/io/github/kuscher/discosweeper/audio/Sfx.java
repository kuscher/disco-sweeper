package io.github.kuscher.discosweeper.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * The game's sounds, synthesised once into static AudioTracks.
 *
 * <p>{@link #prepare()} runs on a background thread while the others run on the main thread, so
 * every method that touches the tracks holds the lock, and a release that lands first stops a
 * late prepare from building tracks nobody would free.
 */
public final class Sfx {
    private static final float BPM = 116.0f;
    public static final int BUST = 5;
    public static final int CHORD = 2;
    private static final int COUNT = 6;
    public static final int DISCO = 4;
    public static final int MARK = 1;
    private static final int RATE = 22050;
    public static final int TICK = 0;
    public static final int WIN = 3;
    private volatile boolean enabled;
    private boolean ready;
    private boolean released;
    private final AudioTrack[] tracks = new AudioTrack[6];

    public void setEnabled(boolean z) {
        this.enabled = z;
    }

    public void prepare() {
        short[][] pcm;
        try {
            pcm = new short[][] {tick(), mark(), chord(), win(), disco(3.25f), bust()};
        } catch (Throwable th) {
            return;
        }
        synchronized (this) {
            if (this.ready || this.released) {
                return;
            }
            build(pcm);
        }
    }

    private void build(short[][] pcm) {
        try {
            for (int i = 0; i < COUNT; i++) {
                this.tracks[i] = build(pcm[i]);
            }
            this.ready = true;
        } catch (Throwable th) {
            releaseTracks();
        }
    }

    public synchronized void play(int i) {
        AudioTrack audioTrack;
        if (!this.enabled || !this.ready || i < 0 || i >= 6 || (audioTrack = this.tracks[i]) == null) {
            return;
        }
        try {
            if (audioTrack.getPlayState() == 3) {
                audioTrack.pause();
            }
            audioTrack.stop();
            audioTrack.reloadStaticData();
            audioTrack.play();
        } catch (Throwable th) {
        }
    }

    public synchronized void stop(int i) {
        if (!this.ready || i < 0 || i >= COUNT || this.tracks[i] == null) {
            return;
        }
        try {
            this.tracks[i].pause();
            this.tracks[i].flush();
        } catch (Throwable th) {
        }
    }

    public synchronized void release() {
        this.released = true;
        releaseTracks();
    }

    private void releaseTracks() {
        for (int i = 0; i < COUNT; i++) {
            if (this.tracks[i] != null) {
                try {
                    this.tracks[i].release();
                } catch (Throwable th) {
                }
            }
            this.tracks[i] = null;
        }
        this.ready = false;
    }

    private static AudioTrack build(short[] sArr) {
        AudioTrack audioTrackBuild = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(14).setContentType(4).build()).setAudioFormat(new AudioFormat.Builder().setEncoding(2).setSampleRate(22050).setChannelMask(4).build()).setBufferSizeInBytes(sArr.length * 2).setTransferMode(0).build();
        audioTrackBuild.write(sArr, 0, sArr.length);
        return audioTrackBuild;
    }

    private static int n(float f) {
        return Math.max(1, (int) (f * 22050.0f));
    }

    private static short clip(float f) {
        if (f > 1.0f) {
            f = 1.0f;
        }
        if (f < -1.0f) {
            f = -1.0f;
        }
        return (short) (f * 32000.0f);
    }

    private static float sine(float f) {
        return (float) Math.sin(((double) (f * 2.0f)) * 3.141592653589793d);
    }

    private static float noise(int i) {
        int i2 = (i * 1103515245) + 12345;
        return (((i2 ^ (i2 >>> 15)) & 65535) / 32768.0f) - 1.0f;
    }

    private static short[] tick() {
        int iN = n(0.045f);
        short[] sArr = new short[iN];
        for (int i = 0; i < iN; i++) {
            float f = i / 22050.0f;
            sArr[i] = clip(((sine(1350.0f * f) * 0.5f) + (sine(f * 2020.0f) * 0.22f)) * ((float) Math.exp((-f) * 90.0f)) * 0.16f);
        }
        return sArr;
    }

    private static short[] mark() {
        int iN = n(0.08f);
        short[] sArr = new short[iN];
        for (int i = 0; i < iN; i++) {
            float f = i / 22050.0f;
            sArr[i] = clip(sine(((Math.min(1.0f, 22.0f * f) * 900.0f) + 620.0f) * f) * ((float) Math.exp((-f) * 40.0f)) * 0.2f);
        }
        return sArr;
    }

    private static short[] chord() {
        int iN = n(0.26f);
        short[] sArr = new short[iN];
        float[] fArr = {587.0f, 784.0f, 1047.0f};
        for (int i = 0; i < 3; i++) {
            int i2 = (int) (i * 0.055f * 22050.0f);
            int i3 = 0;
            while (true) {
                int i4 = i2 + i3;
                if (i4 >= iN || i3 >= n(0.2f)) {
                    break;
                }
                float f = i3 / 22050.0f;
                sArr[i4] = clip((sArr[i4] / 32000.0f) + (sine(fArr[i] * f) * ((float) Math.exp((-f) * 16.0f)) * 0.17f));
                i3++;
            }
        }
        return sArr;
    }

    private static short[] win() {
        int iN = n(1.15f);
        short[] sArr = new short[iN];
        float[] fArr = {523.0f, 659.0f, 784.0f, 1047.0f, 1319.0f};
        for (int i = 0; i < 5; i++) {
            int i2 = (int) (i * 0.085f * 22050.0f);
            // Each note rings on to the end of the buffer. (This loop once had no exit, so the
            // thread preparing the sounds spun a core forever and no sound was ever ready.)
            for (int i3 = 0; i2 + i3 < iN; i3++) {
                int i4 = i2 + i3;
                float f = i3 / 22050.0f;
                sArr[i4] = clip((sArr[i4] / 32000.0f) + (((sine(fArr[i] * f) * 0.6f) + (sine(fArr[i] * 2.0f * f) * 0.18f)) * ((float) Math.exp((-f) * 4.2f)) * 0.13f));
            }
        }
        return sArr;
    }

    private static short[] bust() {
        int iN = n(0.6f);
        short[] sArr = new short[iN];
        for (int i = 0; i < iN; i++) {
            float f = i / 22050.0f;
            float f2 = -f;
            sArr[i] = clip(((sine(((((float) Math.exp(9.0f * f2)) * 120.0f) + 42.0f) * f) * 0.9f) + (noise(i) * 0.25f * ((float) Math.exp(f2 * 30.0f)))) * ((float) Math.exp(7.0f * f2)) * 0.34f);
        }
        return sArr;
    }

    private static short[] disco(float f) {
        float fNoise;
        int iN = n(f);
        short[] sArr = new short[iN];
        for (int i = 0; i < iN; i++) {
            float f2 = i / 22050.0f;
            float f3 = f2 / 0.51724136f;
            float fFloor = (f3 - ((float) Math.floor(f3))) * 0.51724136f;
            if (f3 < 0.5f) {
                fNoise = 0.0f;
            } else {
                float f4 = -fFloor;
                fNoise = (sine(((((float) Math.exp(f4 * 24.0f)) * 130.0f) + 46.0f) * fFloor) * ((float) Math.exp(26.0f * f4)) * 0.55f) + 0.0f;
            }
            float f5 = f2 / 0.25862068f;
            float fFloor2 = (f5 - ((float) Math.floor(f5))) * 0.51724136f * 0.5f;
            float fMax = 1.0f;
            if (((int) f5) % 2 == 1) {
                fNoise += noise(i) * ((float) Math.exp((-fFloor2) * 60.0f)) * 0.13f * Math.min(1.0f, 1.6f * f2);
            }
            float fMin = Math.min(1.0f, f2 / 0.9f);
            float fMax2 = Math.max(0.0f, (1.0f - Math.max(0.0f, (f2 - 2.2f) / 1.0f)) * fMin);
            float fSine = fNoise + (sine(((900.0f * fMin) + 220.0f) * f2) * 0.1f * fMax2) + (sine(((fMin * 1350.0f) + 330.0f) * f2) * 0.06f * fMax2);
            if (f2 > f - 0.35f) {
                fMax = Math.max(0.0f, (f - f2) / 0.35f);
            }
            sArr[i] = clip(fSine * 0.55f * fMax);
        }
        return sArr;
    }
}
