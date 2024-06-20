package allen.town.podcast.view;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Property;

import allen.town.focus_common.util.Util;

/* renamed from: fm.player.ui.drawable.PlayPauseDrawable */
/* loaded from: classes3.dex */
public class PlayPauseDrawable extends Drawable {
    private static final Property<PlayPauseDrawable, Float> PROGRESS = new Property<PlayPauseDrawable, Float>(Float.class, "progress") { // from class: fm.player.ui.drawable.PlayPauseDrawable.1
        public Float get(PlayPauseDrawable playPauseDrawable) {
            return Float.valueOf(playPauseDrawable.getProgress());
        }

        public void set(PlayPauseDrawable playPauseDrawable, Float f) {
            playPauseDrawable.setProgress(f.floatValue());
        }
    };
    private static final String TAG = "PlayPauseDrawable";
    private int m2dp;
    private float mHeight;
    private boolean mIsPlay;
    private final Paint mPaint;
    private final float mPauseBarDistance;
    private final float mPauseBarHeight;
    private final float mPauseBarWidth;
    private boolean mTmpIsPlay;
    private float mWidth;
    private final Path mLeftPauseBar = new Path();
    private final Path mRightPauseBar = new Path();
    private final RectF mBounds = new RectF();
    private String mSource = "";
    private float mProgress = 1.0f;

    public PlayPauseDrawable(Context context, int i, int i2, int i3) {
        Paint paint = new Paint();
        this.mPaint = paint;
        this.m2dp = Util.dp2Px(context, 2);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(-1);
        i = i % 2 != 0 ? i + 1 : i;
        i2 = i2 % 2 != 0 ? i2 + 1 : i2;
        i3 = i3 % 2 != 0 ? i3 + 1 : i3;
        this.mPauseBarWidth = i;
        this.mPauseBarHeight = i2;
        this.mPauseBarDistance = i3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getProgress() {
        return this.mProgress;
    }

    public static float m30864b(float f, float f2, float f3, float f4) {
        return ((f - f2) * f3) + f4;
    }

    private static float lerp(float f, float f2, float f3) {
        return m30864b(f2, f, f3, f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setProgress(float f) {
        this.mProgress = f;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        this.mLeftPauseBar.rewind();
        this.mRightPauseBar.rewind();
        float f = 0.0f;
        float lerp = lerp(this.mPauseBarDistance, 0.0f, this.mProgress);
        float lerp2 = lerp(this.mPauseBarWidth, this.mPauseBarHeight / 2.0f, this.mProgress);
        float lerp3 = lerp(0.0f, lerp2, this.mProgress);
        float f2 = (lerp2 * 2.0f) + lerp;
        float f3 = lerp + lerp2;
        float lerp4 = lerp(f2, f3, this.mProgress);
        float f4 = this.mProgress;
        float f5 = this.mPauseBarHeight;
        int i = (int) ((-f5) + (0.267f * f5 * f4));
        if (i % 2 != 0) {
            i++;
        }
        float f6 = i;
        float f7 = (this.mIsPlay || f4 != 1.0f) ? 0.0f : -1.0f;
        this.mLeftPauseBar.moveTo(0.0f, 0.0f);
        float f8 = f6 + f7;
        this.mLeftPauseBar.lineTo(lerp3, f8);
        this.mLeftPauseBar.lineTo(lerp2, f8);
        this.mLeftPauseBar.lineTo(lerp2, 0.0f);
        this.mLeftPauseBar.close();
        float f9 = f3 + f7;
        this.mRightPauseBar.moveTo(f9, 0.0f);
        this.mRightPauseBar.lineTo(f9, f6);
        this.mRightPauseBar.lineTo(lerp4, f6);
        this.mRightPauseBar.lineTo(f2, 0.0f);
        this.mRightPauseBar.close();
        canvas.save();
        canvas.translate(lerp(0.0f, this.mPauseBarHeight / 5.0f, this.mProgress), 0.0f);
        boolean z = this.mIsPlay;
        float f10 = z ? 1.0f - this.mProgress : this.mProgress;
        if (z) {
            f = 90.0f;
        }
        canvas.rotate(lerp(f, 90.0f + f, f10), this.mWidth / 2.0f, this.mHeight / 2.0f);
        canvas.translate((this.mWidth / 2.0f) - (f2 / 2.0f), (this.mPauseBarHeight / 2.0f) + (this.mHeight / 2.0f));
        canvas.drawPath(this.mLeftPauseBar, this.mPaint);
        canvas.drawPath(this.mRightPauseBar, this.mPaint);
        canvas.restore();
    }

    public void drawPause() {
        this.mProgress = 0.0f;
        invalidateSelf();
    }

    public void drawPlay() {
        this.mProgress = 1.0f;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public Animator getPausePlayAnimator() {
        Property<PlayPauseDrawable, Float> property = PROGRESS;
        float[] fArr = new float[2];
        boolean z = this.mIsPlay;
        float f = 1.0f;
        fArr[0] = z ? 1.0f : 0.0f;
        if (z) {
            f = 0.0f;
        }
        fArr[1] = f;
        return ObjectAnimator.ofFloat(this, property, fArr);
    }

    public boolean isPlay() {
        return this.mIsPlay;
    }

    @Override // android.graphics.drawable.Drawable
    public void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        this.mBounds.set(rect);
        this.mWidth = this.mBounds.width();
        this.mHeight = this.mBounds.height();
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    public void setIsPlay(boolean z) {
        this.mIsPlay = z;
    }

    public void setSource(String str) {
        this.mSource = str;
    }
}