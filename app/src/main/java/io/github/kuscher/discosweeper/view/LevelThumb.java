package io.github.kuscher.discosweeper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import io.github.kuscher.discosweeper.Tokens;
import io.github.kuscher.discosweeper.Ui;
import io.github.kuscher.discosweeper.game.Level;

/* JADX INFO: loaded from: classes.dex */
public final class LevelThumb extends View {
    private int cols;
    private final Paint fill;
    private final Level level;
    private int mines;
    private Tokens own;
    private final RectF r;
    private int rows;
    private final Paint text;
    private Tokens tokens;

    public LevelThumb(Context context, Level level) {
        super(context);
        this.fill = new Paint(1);
        this.text = new Paint(129);
        this.r = new RectF();
        this.level = level;
        this.cols = level.cols;
        this.rows = level.rows;
        this.mines = level.mines;
        this.text.setTextAlign(Paint.Align.CENTER);
        this.text.setTypeface(Ui.weight(700));
    }

    public void setShape(int i, int i2, int i3) {
        this.cols = i;
        this.rows = i2;
        this.mines = i3;
        invalidate();
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
        this.own = new Tokens(tokens.dark, this.level.seedColor);
        invalidate();
    }

    /**
     * A little board for the level: a few tiles still covered, a few balls, the rest open. The
     * pattern is a fixed hash of the tile and the level, so each thumb always looks the same.
     */
    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.own == null) {
            return;
        }
        float width = getWidth();
        float height = getHeight();
        this.fill.setShader(null);
        this.fill.setColor(Ui.mix(this.own.surfaceContainerHighest, this.own.primary, this.own.dark ? 0.26f : 0.34f));
        this.r.set(0.0f, 0.0f, width, height);
        float corner = 0.28f * width;
        canvas.drawRoundRect(this.r, corner, corner, this.fill);
        int side = Math.max(4, Math.min(8, Math.round(((float) Math.sqrt(this.cols * this.rows)) / 2.6f)));
        float tile = width / side;
        float density = this.mines / (float) (this.cols * this.rows);
        float gap = tile * 0.1f;
        int ballsWanted = Math.max(1, Math.round(side * side * density));
        int ballColour = Ui.mix(this.own.primary, -15988202, 0.3f);
        int coveredColour = Ui.alpha(this.own.surface, this.own.dark ? 0.55f : 0.85f);
        int balls = 0;
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int hash = (((73856093 * x) ^ (19349663 * y) ^ (this.level.ordinal() * 83492791)) >>> 1) % 100;
                float left = x * tile;
                float top = y * tile;
                if (balls < ballsWanted && hash < 190.0f * density) {
                    float half = tile / 2.0f;
                    Glyphs.ball(canvas, this.fill, left + half, top + half, tile * 0.34f, ballColour, 0.0f, 1.0f, 3);
                    balls++;
                } else if (hash > 62) {
                    this.fill.setColor(coveredColour);
                    this.r.set(left + gap, top + gap, (left + tile) - gap, (top + tile) - gap);
                    float round = 0.16f * tile;
                    canvas.drawRoundRect(this.r, round, round, this.fill);
                }
            }
        }
    }
}
