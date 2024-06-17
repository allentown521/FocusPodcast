package allen.town.podcast.view;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import allen.town.focus_common.util.ImageUtils;
import code.name.monkey.appthemehelper.ThemeStore;
import code.name.monkey.appthemehelper.util.ColorUtil;
import allen.town.podcast.R;

/* renamed from: fm.player.ui.customviews.SubscribeButton */
/* loaded from: classes4.dex */
public class SubscribeButton extends View {
    private static final boolean API_17 = true;
    private Bitmap mBackgroundBitmap;
    private Paint mBackgroundBitmapOverlayPaint;
    private Paint mBackgroundBitmapPaint;
    private Paint mCircleCheckedFillPaint;
    private Paint mCircleFillPaint;
    private Paint mCircleRingPaint;
    private int mCircleRingWidth;
    private int mDrawableSize;
    private int mInnerSize;
    private boolean mIsSubscribed;
    private Paint mSelectorPaint;
    private Paint mShadowPaint;
    private boolean mShowCircleShadow;
    @Nullable
    private Drawable mSubscribeDrawable;
    private Drawable mSubscribeIcon;
    @Nullable
    private Drawable mSubscribedDrawable;
    private Drawable mUnsubscribeIcon;
    private Paint mTempPaint = new Paint();
    private Rect mTempRect = new Rect();
    private RectF mTempRectF = new RectF();
    private boolean mDrawSelector = true;
    private int mCircleRingColor = 0;

    public SubscribeButton(Context context) {
        super(context);
        init(null);
    }

    private void drawIt(Canvas canvas) {
        RectF rectF = this.mTempRectF;
        int i = this.mInnerSize;
        rectF.set(-0.5f, -0.5f, i + 0.5f, i + 0.5f);
        this.mTempRectF.offset((getWidth() - this.mInnerSize) / 2, (getHeight() - this.mInnerSize) / 2);
        Paint paint = this.mCircleCheckedFillPaint;
        if (paint != null) {
            RectF rectF2 = this.mTempRectF;
            if (!this.mIsSubscribed) {
                paint = this.mCircleFillPaint;
            }
            canvas.drawArc(rectF2, 0.0f, 360.0f, true, paint);
        } else {
            canvas.drawArc(this.mTempRectF, 0.0f, 360.0f, true, this.mCircleFillPaint);
        }
        Drawable drawable = this.mSubscribeDrawable;
        if (drawable != null) {
            drawable.setAlpha(255);
        }
        if (this.mCircleRingWidth > 0) {
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, this.mInnerSize / 2, this.mCircleRingPaint);
        }
    }

    /* JADX WARN: Finally extract failed */
    private void init(AttributeSet attributeSet) {
        Resources resources = getResources();
        TypedArray obtainStyledAttributes = getContext().getTheme().obtainStyledAttributes(attributeSet, R.styleable.PlayPauseProgressButton, 0, 0);
        int color = resources.getColor(R.color.primary_color_5);
        this.mCircleRingColor = 0;
        int themedColor = getResources().getColor(R.color.fullscreen_player_play_pause_current_progress_neutral);
        int themedColor2 = getResources().getColor(R.color.fullscreen_player_play_pause_pause_throb);
        int accentColor = ThemeStore.accentColor(getContext());
        int color2 = resources.getColor(R.color.primary_color_5);
        try {
            this.mSubscribeDrawable = obtainStyledAttributes.getDrawable(18);
            this.mSubscribedDrawable = obtainStyledAttributes.getDrawable(16);
            this.mInnerSize = obtainStyledAttributes.getDimensionPixelSize(12, resources.getDimensionPixelOffset(R.dimen.episode_item_play_pause_inner_size));
            this.mCircleRingWidth = obtainStyledAttributes.getDimensionPixelSize(4, 0);
            int color3 = obtainStyledAttributes.getColor(1, color);
            this.mShowCircleShadow = obtainStyledAttributes.getBoolean(23, false);
            float f = obtainStyledAttributes.getFloat(6, 20.0f);
            int integer = obtainStyledAttributes.getInteger(5, 180);
            float dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(7, 0);
            this.mCircleRingColor = obtainStyledAttributes.getColor(3, this.mCircleRingColor);
            obtainStyledAttributes.getColor(9, themedColor);
            obtainStyledAttributes.getColor(27, themedColor2);
            obtainStyledAttributes.getColor(21, accentColor);
            obtainStyledAttributes.getColor(17, color2);
            obtainStyledAttributes.recycle();
            Drawable drawable = this.mSubscribeDrawable;
            if (drawable != null) {
                drawable.setCallback(this);
            }
            Drawable drawable2 = this.mSubscribedDrawable;
            if (drawable2 != null) {
                drawable2.setCallback(this);
                this.mDrawableSize = this.mSubscribeDrawable.getIntrinsicWidth();
            }
            Paint paint = new Paint();
            this.mCircleFillPaint = paint;
            paint.setColor(color3);
            this.mCircleFillPaint.setAntiAlias(true);
            Paint paint2 = new Paint();
            this.mSelectorPaint = paint2;
            paint2.setColor(520093696);
            this.mSelectorPaint.setAntiAlias(true);
            Paint paint3 = new Paint();
            this.mCircleRingPaint = paint3;
            paint3.setColor(this.mCircleRingColor);
            this.mCircleRingPaint.setAntiAlias(true);
            this.mCircleRingPaint.setStrokeWidth(this.mCircleRingWidth);
            this.mCircleRingPaint.setStyle(Paint.Style.STROKE);
            if (this.mShowCircleShadow) {
                this.mCircleRingPaint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
                Paint paint4 = new Paint();
                this.mShadowPaint = paint4;
                paint4.setStyle(Paint.Style.FILL);
                this.mShadowPaint.setColor(0);
                this.mShadowPaint.setShadowLayer(f, 0.0f, dimensionPixelSize, Color.argb(integer, 0, 0, 0));
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            this.mTempPaint.setAntiAlias(true);
            setClickable(true);
            setFocusable(true);
        } catch (Throwable th) {
            obtainStyledAttributes.recycle();
            throw th;
        }
    }

    @Override // android.view.View
    public void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable drawable = this.mSubscribeDrawable;
        if (drawable != null && drawable.isStateful()) {
            this.mSubscribeDrawable.setState(getDrawableState());
        }
        Drawable drawable2 = this.mSubscribedDrawable;
        if (drawable2 != null && drawable2.isStateful()) {
            this.mSubscribedDrawable.setState(getDrawableState());
        }
        invalidate();
    }

    public int getCircleRingColor() {
        return this.mCircleRingColor;
    }

    public Bitmap getRoundedShape(Bitmap bitmap) {
        int i = this.mInnerSize;
        Bitmap createBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        Path path = new Path();
        float f = i;
        float f2 = i;
        path.addCircle((f - 1.0f) / 2.0f, (f2 - 1.0f) / 2.0f, Math.min(f, f2) / 2.0f, Path.Direction.CCW);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, new Rect((bitmap.getWidth() / 4) + 0, (bitmap.getHeight() / 4) + 0, bitmap.getWidth() - (bitmap.getWidth() / 4), bitmap.getHeight() - (bitmap.getHeight() / 4)), new Rect(0, 0, i, i), (Paint) null);
        return createBitmap;
    }

    public boolean isSubscribed() {
        return this.mIsSubscribed;
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mShowCircleShadow) {
            RectF rectF = this.mTempRectF;
            int i = this.mInnerSize;
            rectF.set(-0.5f, -0.5f, i + 0.5f, i + 0.5f);
            this.mTempRectF.offset((getWidth() - this.mInnerSize) / 2, (getHeight() - this.mInnerSize) / 2);
            canvas.drawArc(this.mTempRectF, 0.0f, 360.0f, true, this.mShadowPaint);
        }
        Rect rect = this.mTempRect;
        int i2 = this.mDrawableSize;
        rect.set(0, 0, i2, i2);
        this.mTempRect.offset((getWidth() - this.mDrawableSize) / 2, (getHeight() - this.mDrawableSize) / 2);
        Bitmap bitmap = this.mBackgroundBitmap;
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, (getWidth() / 2) - (this.mInnerSize / 2), (getHeight() / 2) - (this.mInnerSize / 2), this.mBackgroundBitmapPaint);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, this.mInnerSize / 2, this.mBackgroundBitmapOverlayPaint);
        }
        drawIt(canvas);
        if (this.mDrawSelector && (isSelected() || isPressed() || isFocused())) {
            RectF rectF2 = this.mTempRectF;
            int i3 = this.mInnerSize;
            rectF2.set(-0.5f, -0.5f, i3 + 0.5f, i3 + 0.5f);
            this.mTempRectF.offset((getWidth() - this.mInnerSize) / 2, (getHeight() - this.mInnerSize) / 2);
            canvas.drawArc(this.mTempRectF, 0.0f, 360.0f, true, this.mSelectorPaint);
        }
        Drawable drawable = this.mIsSubscribed ? this.mSubscribedDrawable : this.mSubscribeDrawable;
        if (drawable != null) {
            drawable.setBounds(this.mTempRect);
            drawable.draw(canvas);
        }
    }

    @Override // android.view.View
    public void onMeasure(int i, int i2) {
        setMeasuredDimension(View.resolveSize(this.mDrawableSize, i), View.resolveSize(this.mDrawableSize, i2));
    }

    public void setBackgroundImage(Bitmap bitmap) {
        this.mBackgroundBitmap = getRoundedShape(bitmap);
        Paint paint = new Paint();
        this.mBackgroundBitmapPaint = paint;
        paint.setAntiAlias(true);
        this.mBackgroundBitmapPaint.setFilterBitmap(true);
        this.mBackgroundBitmapPaint.setDither(true);
        Paint paint2 = new Paint();
        this.mBackgroundBitmapOverlayPaint = paint2;
        paint2.setColor(Color.parseColor("#99444444"));
        this.mBackgroundBitmapOverlayPaint.setAntiAlias(true);
        invalidate();
    }

    public void setButtonColors(int i, int i2, int i3, int i4, int i5) {
        this.mCircleFillPaint.setColor(i);
        if (this.mCircleCheckedFillPaint == null) {
            Paint paint = new Paint();
            this.mCircleCheckedFillPaint = paint;
            paint.setAntiAlias(true);
        }
        this.mCircleCheckedFillPaint.setColor(i2);
        if (i3 == 0) {
            i3 = Color.parseColor("#eeeeee");
        }
        if (i4 == 0) {
            i4 = Color.parseColor("#eeeeee");
        }
        Drawable drawable = this.mSubscribeIcon;
        this.mSubscribeDrawable = drawable != null ? ImageUtils.getColoredDrawable(drawable, i3) : ImageUtils.getColoredDrawable(getContext(), R.drawable.ic_add_white, i3);
        Drawable drawable2 = this.mUnsubscribeIcon;
        this.mSubscribedDrawable = drawable2 != null ? ImageUtils.getColoredDrawable(drawable2, i4) : ImageUtils.getColoredDrawable(getContext(), R.drawable.ic_round_check_24, i4);
        Drawable drawable3 = this.mSubscribeDrawable;
        if (drawable3 != null) {
            this.mDrawableSize = drawable3.getIntrinsicWidth();
        }
        this.mCircleRingColor = i5;
        this.mCircleRingPaint.setColor(i5);
        invalidate();
    }

    public void setButtonColorsValue(int i, int i2) {
        setButtonColorsValue(i, i2, false);
    }

    public void setCircleFillColor(int i) {
        if (this.mCircleFillPaint == null) {
            Paint paint = new Paint();
            this.mCircleFillPaint = paint;
            paint.setAntiAlias(true);
        }
        this.mCircleFillPaint.setColor(i);
        invalidate();
    }

    public void setCircleRingColor(int i) {
        this.mCircleRingColor = i;
        this.mCircleRingPaint.setColor(i);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setDrawSelector(boolean z) {
        this.mDrawSelector = z;
    }

    public void setSubscribeIcon(Drawable drawable) {
        this.mSubscribeIcon = drawable;
    }

    public void setSubscribed(boolean z) {
        if (this.mIsSubscribed != z) {
            this.mIsSubscribed = z;
            invalidate();
        }
        setContentDescription(getResources().getString(this.mIsSubscribed ? R.string.remove_feed_label : R.string.subscribe_label));
    }

    public void setSubscribedIconColor(int i) {
        if (i == 0) {
            i = Color.parseColor("#eeeeee");
        }
        Drawable drawable = this.mUnsubscribeIcon;
        this.mSubscribedDrawable = drawable != null ? ImageUtils.getColoredDrawable(drawable, i) : ImageUtils.getColoredDrawable(getContext(), R.drawable.ic_round_check_24, i);
        invalidate();
    }

    public void setTickSize(int i) {
        this.mDrawableSize = i;
        invalidate();
    }

    public void setUnsubscribeIcon(Drawable drawable) {
        this.mUnsubscribeIcon = drawable;
    }

    public void showShadow(int i, float f, float f2, float f3, boolean z) {
        this.mShowCircleShadow = true;
        if (z) {
            this.mCircleRingPaint.setShadowLayer(2.0f, 0.0f, 0.0f, i);
        }
        Paint paint = new Paint();
        this.mShadowPaint = paint;
        paint.setStyle(Paint.Style.FILL);
        this.mShadowPaint.setColor(0);
        this.mShadowPaint.setShadowLayer(f2, 0.0f, f3, ColorUtil.adjustAlpha(i, f));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setButtonColorsValue(int i, int i2, boolean z) {
        Drawable drawable;
        this.mCircleFillPaint.setColor(i);
        if (i2 == 0) {
            i2 = Color.parseColor("#eeeeee");
        }
        Drawable drawable2 = this.mSubscribeIcon;
        this.mSubscribeDrawable = drawable2 != null ? ImageUtils.getColoredDrawable(drawable2, i2) : ImageUtils.getColoredDrawable(getContext(), R.drawable.ic_add_white, i2);
        Drawable drawable3 = this.mUnsubscribeIcon;
        this.mSubscribedDrawable = drawable3 != null ? ImageUtils.getColoredDrawable(drawable3, i2) : ImageUtils.getColoredDrawable(getContext(), R.drawable.ic_round_check_24, i2);
        if (z && (drawable = this.mSubscribeDrawable) != null) {
            this.mDrawableSize = drawable.getIntrinsicWidth();
        }
        invalidate();
    }

    public void setSubscribeButtonColorsValue(int i2) {
        Drawable drawable;
        Drawable drawable2 = this.mSubscribeIcon;
        this.mSubscribeDrawable = drawable2 != null ? ImageUtils.getColoredDrawable(drawable2, i2) : ImageUtils.getColoredDrawable(getContext(), R.drawable.ic_add_white, i2);
        invalidate();
    }

    public SubscribeButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(attributeSet);
    }

    public void setButtonColorsValue(int i) {
        setButtonColorsValue(i, 0);
    }

    public SubscribeButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(attributeSet);
    }
}
