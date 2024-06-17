package allen.town.podcast.view

import allen.town.focus_common.util.ImageUtils.getColoredDrawable
import allen.town.focus_common.util.ImageUtils.getColoredVectorDrawable
import allen.town.focus_common.util.Timber
import allen.town.focus_common.util.Util.dp2Px
import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.annotation.Keep
import androidx.core.view.ViewCompat
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import code.name.monkey.appthemehelper.util.ATHUtil.isWindowBackgroundDark
import code.name.monkey.appthemehelper.util.ATHUtil.resolveColor
import code.name.monkey.appthemehelper.util.ColorUtil.adjustAlpha
import com.leinardi.android.speeddial.UiUtils
import allen.town.podcast.R
import allen.town.podcast.actionbuttons.ItemActionButton
import allen.town.podcast.core.pref.UsageStatistics
import allen.town.podcast.core.service.playback.PlaybackService
import allen.town.podcast.core.storage.DBTasks
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.core.util.FeedItemUtil
import allen.town.podcast.core.util.IntentUtils.sendLocalBroadcast
import allen.town.podcast.core.util.NetworkUtils
import allen.town.podcast.core.util.playback.PlaybackServiceStarter
import allen.town.podcast.dialog.UseStreamConfirmDialog
import allen.town.podcast.model.feed.FeedItem
import allen.town.podcast.model.feed.FeedMedia
import allen.town.podcast.model.playback.MediaType
import android.app.Activity
import java.util.*

/* renamed from: fm.player.ui.customviews.PlayPauseProgressButton */ /* loaded from: classes2.dex */
class PlayPauseProgressButton : View, ItemActionButton {
    private var mBackgroundBitmap: Bitmap? = null
    private var mBackgroundBitmapOverlayPaint: Paint? = null
    private var mBackgroundBitmapPaint: Paint? = null
    var mBufferingPaint: Paint? = null
    private var mCircleFillPaint: Paint? = null
    private var mCircleProgressObjectAnimator: ObjectAnimator? = null
    private var mCircleRingPaint: Paint? = null
    private var mCircleRingProgressPaint: Paint? = null
    private var mCircleRingWidth = 0
    private var mDrawableSize = 0
    private var mInnerPaint: Paint? = null
    private var mInnerSize = 0
    private var mIsAnimatedPlayPauseDrawable = false
    private var mIsBuffering = false
    private val mIsDownloaded = false
    private var mIsDownloading = false
    private var mIsPaused = false
    var isPlayed = false
        private set
    var isPlaying = false
        private set
    private var mLastTime: Long = 0
    private var mPauseDrawable: Drawable? = null
    var mPercenateTextSize = 0
    var mPercenateTextSize100 = 0
    private var mPercentageTextPaint: Paint? = null
    private var mPlayDrawable: Drawable? = null
    private var mPlayPauseAnimator: Animator? = null
    private var mPlayPauseDrawable: PlayPauseDrawable? = null
    private var mPlayedDrawable: Drawable? = null
    private var mProgressPaint: Paint? = null
    private var mProgressRingPaint: Paint? = null
    private var mPulseRingPaint: Paint? = null
    private var mSelectorPaint: Paint? = null
    private var mShadowPaint: Paint? = null
    private var mShowCircleShadow = false
    private var mShowInnerBorder = false
    private var mStopDrawable: Drawable? = null
    private var mThrobPaint: Paint? = null
    private val mMax = 100
    private var mProgress = 0
    private var mAnimationFrame = 0.0f
    private var mPulsingAnimation = true
    private val mTempPaint = Paint()
    private val mTempRect = Rect()
    private val mTempRectF = RectF()
    private var mCircleRingColor = 0
    private val mInnerStrokeWitdh = 4
    private var mCircleProgress = 100.0f
    private val mStartAngle = -90
    private var mPlayPauseRedrawsEnabled = true
    private var mIsShowProgressOnRing = true
    private var mIsDrawFullCircleFill = false
    private var mForceDrawPause = false
    private var mIsShowCircleRingWithProgressOnly = false

    constructor(context: Context?) : super(context) {
        init(null)
    }

    private fun drawBufferingPulseAnimation(canvas: Canvas) {
        val d: Double
        val width = width.toFloat()
        val height = height.toFloat()
        Math.min(width, height)
        val f = mInnerSize * 0.55f
        canvas.save()
        canvas.translate(width / 2.0f, height / 2.0f)
        val f2 = mAnimationFrame % 122.0f
        d = if (f2 <= 60.0f) {
            Math.cos(f2 * 3.141592653589793 / 60.0f)
        } else {
            Math.cos((1.0f - (f2 - 60.0f) / 60.0f) * 3.141592653589793)
        }
        canvas.drawCircle(
            0.0f,
            0.0f,
            ((0.5f - d.toFloat() * 0.5f) * 0.23000002f + 1.0f) * f,
            mThrobPaint!!
        )
        val cos =
            0.5f - Math.cos(mAnimationFrame % 80.0f * 3.141592653589793 / 79.0).toFloat() * 0.5f
        mPulseRingPaint!!.alpha = ((1.0f - cos) * 255.0f).toInt()
        canvas.drawCircle(0.0f, 0.0f, (0.43f * cos + 0.9f) * f, mPulseRingPaint!!)
        canvas.restore()
        val rectF = mTempRectF
        val i = mInnerSize
        rectF[-0.5f, -0.5f, i + 0.5f] = i + 0.5f
        mTempRectF.offset(
            ((getWidth() - mInnerSize) / 2).toFloat(),
            ((getHeight() - mInnerSize) / 2).toFloat()
        )
        var f3 = (mProgress * 360 / mMax).toFloat()
        if (mCircleFillPaint != null) {
            drawCircleFill(canvas, f3)
        }
        if (f3 < 5.0f) {
            f3 = 5.0f
        }
        drawProgress(canvas, f3)
    }

    private fun drawCircleFill(canvas: Canvas, f: Float) {
        if (mIsShowProgressOnRing || mIsDrawFullCircleFill) {
            canvas.drawArc(mTempRectF, -90.0f, 360.0f, true, mCircleFillPaint!!)
        } else {
            canvas.drawArc(mTempRectF, f - 90.0f, 360.0f - f, true, mCircleFillPaint!!)
        }
    }

    private fun drawDownloading(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val rectF = mTempRectF
        val i = mInnerSize
        rectF[-0.5f, -0.5f, i + 0.5f] = i + 0.5f
        mTempRectF.offset(
            ((getWidth() - mInnerSize) / 2).toFloat(),
            ((getHeight() - mInnerSize) / 2).toFloat()
        )
        val f = (mProgress * 360 / mMax).toFloat()
        if (mCircleFillPaint != null) {
            drawCircleFill(canvas, f)
        }
        drawProgress(canvas, f)
        if (mCircleRingWidth > 0 && !mIsShowProgressOnRing) {
            canvas.drawCircle(
                (getWidth() / 2).toFloat(),
                (getHeight() / 2).toFloat(),
                (mInnerSize / 2).toFloat(),
                mCircleRingPaint!!
            )
        }
        canvas.save()
        canvas.translate(width / 2.0f, height / 2.0f)
        val rect = Rect()
        val l = m30854l(StringBuilder(), mProgress, "%")
        mPercentageTextPaint!!.setTextSize((if (mProgress < 100) mPercenateTextSize else mPercenateTextSize100.toFloat()) as Float)
        mPercentageTextPaint!!.getTextBounds(l, 0, l.length, rect)
        canvas.drawText(
            l,
            (0 - (rect.right - rect.left) / 2).toFloat(),
            ((mPercentageTextPaint!!.ascent() + mPercentageTextPaint!!.descent()) * -1.0f / 2.0f).toInt()
                .toFloat(),
            mPercentageTextPaint!!
        )
        canvas.restore()
    }

    private fun drawPaused(canvas: Canvas) {
        val rectF = mTempRectF
        val i = mInnerSize
        rectF[-0.5f, -0.5f, i + 0.5f] = i + 0.5f
        mTempRectF.offset(
            ((width - mInnerSize) / 2).toFloat(),
            ((height - mInnerSize) / 2).toFloat()
        )
        val f = (mProgress * 360 / mMax).toFloat()
        if (mCircleFillPaint != null) {
            drawCircleFill(canvas, f)
        }
        drawProgress(canvas, f)
    }

    private fun drawPlaying(canvas: Canvas) {
        val d: Double
        val width = width.toFloat()
        val height = height.toFloat()
        Math.min(width, height)
        val f = mInnerSize * 0.55f
        canvas.save()
        canvas.translate(width / 2.0f, height / 2.0f)
        val f2 = mAnimationFrame % 122.0f
        d = if (f2 <= 60.0f) {
            Math.cos(f2 * 3.141592653589793 / 60.0f)
        } else {
            Math.cos((1.0f - (f2 - 60.0f) / 60.0f) * 3.141592653589793)
        }
        canvas.drawCircle(
            0.0f,
            0.0f,
            ((0.5f - d.toFloat() * 0.5f) * 0.23000002f + 1.0f) * f,
            mThrobPaint!!
        )
        val cos =
            0.5f - Math.cos(mAnimationFrame % 80.0f * 3.141592653589793 / 79.0).toFloat() * 0.5f
        mPulseRingPaint!!.alpha = ((1.0f - cos) * 255.0f).toInt()
        canvas.drawCircle(0.0f, 0.0f, (0.43f * cos + 0.9f) * f, mPulseRingPaint!!)
        canvas.restore()
        val rectF = mTempRectF
        val i = mInnerSize
        rectF[-0.5f, -0.5f, i + 0.5f] = i + 0.5f
        mTempRectF.offset(
            ((getWidth() - mInnerSize) / 2).toFloat(),
            ((getHeight() - mInnerSize) / 2).toFloat()
        )
        var f3 = (mProgress * 360 / mMax).toFloat()
        if (mCircleFillPaint != null) {
            drawCircleFill(canvas, f3)
        }
        if (f3 < 5.0f) {
            f3 = 5.0f
        }
        drawProgress(canvas, f3)
    }

    private fun drawPlayingNoAnimation(canvas: Canvas) {
        val rectF = mTempRectF
        val i = mInnerSize
        rectF[-0.5f, -0.5f, i + 0.5f] = i + 0.5f
        mTempRectF.offset(
            ((width - mInnerSize) / 2).toFloat(),
            ((height - mInnerSize) / 2).toFloat()
        )
        val f = (mProgress * 360 / mMax).toFloat()
        if (mCircleFillPaint != null) {
            drawCircleFill(canvas, f)
        }
        drawProgress(canvas, f)
    }

    private fun drawProgress(canvas: Canvas, f: Float) {
        if (mIsShowProgressOnRing) {
            if (f > 0.0f) {
                val iArr = intArrayOf(0, mProgressRingPaint!!.color)
                val f2 = f / 360.0f
                var f3 = 0.25f
                if (f2 < 0.25f) {
                    f3 = f2 * 0.25f / 0.25f
                }
                val sweepGradient = SweepGradient(
                    mTempRectF.centerX(),
                    mTempRectF.centerY(),
                    iArr,
                    floatArrayOf(f2, f2 + f3)
                )
                val matrix = Matrix()
                matrix.setRotate(-90.0f, mTempRectF.centerX(), mTempRectF.centerY())
                sweepGradient.setLocalMatrix(matrix)
                mProgressRingPaint!!.shader = sweepGradient
            } else {
                mProgressRingPaint!!.shader = null
            }
            canvas.drawArc(mTempRectF, -90.0f, -(360.0f - f), false, mProgressRingPaint!!)
            return
        }
        canvas.drawArc(mTempRectF, -90.0f, f, true, mProgressPaint!!)
    }

    /* JADX WARN: Finally extract failed */
    private fun init(attributeSet: AttributeSet?) {
        val i: Int
        if (!isInEditMode) {
            val resources = resources
            val obtainStyledAttributes = context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.PlayPauseProgressButton,
                0,
                0
            )
            val dimensionPixelSize = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.PlayPauseProgressButton_pauseBarWidth, dp2Px(
                    context, 8f
                )
            )
            val dimensionPixelSize2 = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.PlayPauseProgressButton_pauseBarHeight, dp2Px(
                    context, 26f
                )
            )
            val dimensionPixelSize3 = obtainStyledAttributes.getDimensionPixelSize(
                R.styleable.PlayPauseProgressButton_pauseBarDistance, dp2Px(
                    context, 6f
                )
            )
            mIsShowProgressOnRing = obtainStyledAttributes.getBoolean(
                R.styleable.PlayPauseProgressButton_showProgressOnRing,
                mIsShowProgressOnRing
            )
            mIsDrawFullCircleFill = obtainStyledAttributes.getBoolean(
                R.styleable.PlayPauseProgressButton_circleFillDrawFullCircle,
                mIsDrawFullCircleFill
            )
            val playPauseDrawable = PlayPauseDrawable(
                context,
                dimensionPixelSize,
                dimensionPixelSize2,
                dimensionPixelSize3
            )
            mPlayPauseDrawable = playPauseDrawable
            playPauseDrawable.callback = this
            mPlayPauseDrawable!!.setSource(TAG)
            val color = resources.getColor(R.color.gray_3)
            val accentColor = accentColor(context)
            val themedColor =
                getResources().getColor(R.color.fullscreen_player_play_pause_pause_throb)
            val accentColor2 = accentColor(context)
            val themedColor2 =
                getResources().getColor(R.color.play_pause_progress_button_selector_dark)
            try {
                mPlayDrawable =
                    obtainStyledAttributes.getDrawable(R.styleable.PlayPauseProgressButton_playIconSrc)
                val color2 = obtainStyledAttributes.getColor(
                    R.styleable.PlayPauseProgressButton_playPauseIconColor,
                    -1
                )
                mPauseDrawable =
                    obtainStyledAttributes.getDrawable(R.styleable.PlayPauseProgressButton_pauseIconSrc)
                if (color2 != -1) {
                    mPlayDrawable = getColoredDrawable(
                        mPlayDrawable!!, color2
                    )
                    mPauseDrawable = getColoredDrawable(
                        mPauseDrawable!!, color2
                    )
                }
                var dimensionPixelSize4 = obtainStyledAttributes.getDimensionPixelSize(
                    R.styleable.PlayPauseProgressButton_iconSize,
                    0
                )
                mInnerSize = obtainStyledAttributes.getDimensionPixelSize(
                    R.styleable.PlayPauseProgressButton_innerSize,
                    resources.getDimensionPixelOffset(R.dimen.episode_item_play_pause_inner_size)
                )
                mCircleRingWidth = obtainStyledAttributes.getDimensionPixelSize(
                    R.styleable.PlayPauseProgressButton_circleRingWidth,
                    0
                )
                val color3 = obtainStyledAttributes.getColor(
                    R.styleable.PlayPauseProgressButton_progressColorPaused,
                    color
                )
                mShowCircleShadow = obtainStyledAttributes.getBoolean(
                    R.styleable.PlayPauseProgressButton_showCircleShadow,
                    false
                )
                val f = obtainStyledAttributes.getFloat(
                    R.styleable.PlayPauseProgressButton_circleShadowRadius,
                    20.0f
                )
                val integer = obtainStyledAttributes.getInteger(
                    R.styleable.PlayPauseProgressButton_circleShadowAlpha,
                    180
                )
                mShowInnerBorder = obtainStyledAttributes.getBoolean(
                    R.styleable.PlayPauseProgressButton_showInnerBorder,
                    false
                )
                mCircleRingColor = obtainStyledAttributes.getColor(
                    R.styleable.PlayPauseProgressButton_circleRingColor,
                    mCircleRingColor
                )
                val color4 = obtainStyledAttributes.getColor(
                    R.styleable.PlayPauseProgressButton_throbColor,
                    themedColor
                )
                val color5 = obtainStyledAttributes.getColor(
                    R.styleable.PlayPauseProgressButton_pulseRingColor,
                    accentColor2
                )
                val color6 = obtainStyledAttributes.getColor(
                    R.styleable.PlayPauseProgressButton_selectorColor,
                    themedColor2
                )
                mPulsingAnimation = obtainStyledAttributes.getBoolean(
                    R.styleable.PlayPauseProgressButton_showPulsingAnimation,
                    true
                )
                val z = obtainStyledAttributes.getBoolean(
                    R.styleable.PlayPauseProgressButton_clickable,
                    true
                )
                val z2 = obtainStyledAttributes.getBoolean(
                    R.styleable.PlayPauseProgressButton_focusable,
                    true
                )
                obtainStyledAttributes.recycle()
                mAnimationFrame = 0.0f
                if (API_17) {
                    i = accentColor
                    mLastTime = SystemClock.elapsedRealtimeNanos()
                } else {
                    i = accentColor
                    mLastTime = SystemClock.elapsedRealtime() * 1000000
                }
                mPlayDrawable!!.callback = this
                mPauseDrawable!!.callback = this
                val drawable = resources.getDrawable(R.drawable.ic_played)
                mPlayedDrawable = drawable
                drawable.callback = this
                mStopDrawable = mPauseDrawable
                if (dimensionPixelSize4 <= 0) {
                    dimensionPixelSize4 = mPlayDrawable!!.intrinsicWidth
                }
                mDrawableSize = dimensionPixelSize4
                val paint = Paint()
                mSelectorPaint = paint
                paint.color = color6
                mSelectorPaint!!.isAntiAlias = true
                val paint2 = Paint()
                mCircleRingPaint = paint2
                paint2.color = mCircleRingColor
                mCircleRingPaint!!.isAntiAlias = true
                mCircleRingPaint!!.strokeWidth = mCircleRingWidth.toFloat()
                mCircleRingPaint!!.style = Paint.Style.STROKE
                val paint3 = Paint()
                mCircleRingProgressPaint = paint3
                paint3.color = adjustAlpha(
                    mCircleRingColor, if (isWindowBackgroundDark(
                            context
                        )
                    ) 0.5f else 0.3f
                )
                mCircleRingProgressPaint!!.isAntiAlias = true
                mCircleRingProgressPaint!!.strokeWidth = mCircleRingWidth.toFloat()
                mCircleRingProgressPaint!!.style = Paint.Style.STROKE
                if (mShowCircleShadow) {
                    mCircleRingPaint!!.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216)
                    val paint4 = Paint()
                    mShadowPaint = paint4
                    paint4.style = Paint.Style.FILL
                    mShadowPaint!!.color = 0
                    mShadowPaint!!.setShadowLayer(f, 0.0f, 0.0f, Color.argb(integer, 0, 0, 0))
                    setLayerType(LAYER_TYPE_SOFTWARE, null)
                }
                val paint5 = Paint()
                mBufferingPaint = paint5
                paint5.isAntiAlias = true
                mBufferingPaint!!.color = i
                val paint6 = Paint()
                mProgressPaint = paint6
                paint6.color = color3
                mProgressPaint!!.isAntiAlias = true
                mProgressRingPaint = Paint()
                setProgressRingColor(mCircleRingColor)
                mProgressRingPaint!!.isAntiAlias = true
                mProgressRingPaint!!.strokeWidth = mCircleRingWidth.toFloat()
                mProgressRingPaint!!.strokeCap = Paint.Cap.SQUARE
                mProgressRingPaint!!.style = Paint.Style.STROKE
                val paint7 = Paint()
                mThrobPaint = paint7
                paint7.color = color4
                mThrobPaint!!.isAntiAlias = true
                val paint8 = Paint()
                mPulseRingPaint = paint8
                paint8.color = color5
                mPulseRingPaint!!.isAntiAlias = true
                mPulseRingPaint!!.strokeWidth = 6.0f
                mPulseRingPaint!!.style = Paint.Style.STROKE
                mTempPaint.isAntiAlias = true
                mPercentageTextPaint = Paint()
                mPercenateTextSize = resources.getDimensionPixelSize(R.dimen.text_size_micro)
                mPercenateTextSize100 =
                    resources.getDimensionPixelSize(R.dimen.text_size_xsuper_micro)
                mPercentageTextPaint!!.textSize = mPercenateTextSize.toFloat()
                mPercentageTextPaint!!.isFakeBoldText = true
                mPercentageTextPaint!!.isAntiAlias = true
                mPercentageTextPaint!!.isFakeBoldText = true
                val paint9 = Paint()
                mInnerPaint = paint9
                paint9.style = Paint.Style.STROKE
                mInnerPaint!!.isDither = true
                mInnerPaint!!.isAntiAlias = true
                mInnerPaint!!.color = Color.parseColor("#44222222")
                mInnerPaint!!.strokeWidth = mInnerStrokeWitdh.toFloat()
                isClickable = z
                isFocusable = z2
                setButtonContentDescription()
            } catch (th: Throwable) {
                obtainStyledAttributes.recycle()
                throw th
            }
        }
    }

    private fun setButtonContentDescription() {
        contentDescription =
            resources.getString(if (isPlaying) R.string.pause_label else R.string.play_label)
    }

    private fun setProgressRingColor(i: Int) {
        mProgressRingPaint!!.color = i
    }

    fun cancelCircleProgressAnimation() {
        val objectAnimator = mCircleProgressObjectAnimator
        objectAnimator?.cancel()
    }

    fun drawPause() {
        val playPauseDrawable = mPlayPauseDrawable
        if (playPauseDrawable != null) {
            playPauseDrawable.drawPause()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    // android.view.View
    public override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (!isInEditMode) {
            if (mPlayDrawable!!.isStateful) {
                mPlayDrawable!!.state = drawableState
            }
            if (mPauseDrawable!!.isStateful) {
                mPauseDrawable!!.state = drawableState
            }
            if (mPlayedDrawable!!.isStateful) {
                mPlayedDrawable!!.state = drawableState
            }
            if (mStopDrawable!!.isStateful) {
                mStopDrawable!!.state = drawableState
            }
            invalidate()
        }
    }

    fun forceDrawPauseIcon(z: Boolean) {
        mForceDrawPause = z
        invalidate()
    }

    var progress: Int
        get() = mProgress
        set(i) {
            if (i > mMax || i < 0) {
                val locale = Locale.US
                Timber.d(
                    String.format(
                        locale,
                        "PlayPauseButton setProgress Progress (%d) must be between %d and %d",
                        Integer.valueOf(i),
                        0,
                        Integer.valueOf(
                            mMax
                        )
                    ), IllegalArgumentException(
                        String.format(
                            locale,
                            "PlayPauseButton Progress (%d) must be between %d and %d",
                            Integer.valueOf(i),
                            0,
                            Integer.valueOf(
                                mMax
                            )
                        )
                    ), true
                )
            } else if (mProgress != i) {
                mProgress = i
                invalidate()
            }
        }

    fun getRoundedShape(bitmap: Bitmap): Bitmap {
        val i = mInnerSize
        val createBitmap = Bitmap.createBitmap(i, i, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(createBitmap)
        val path = Path()
        val f = i.toFloat()
        val f2 = i.toFloat()
        path.addCircle(
            (f - 1.0f) / 2.0f,
            (f2 - 1.0f) / 2.0f,
            Math.min(f, f2) / 2.0f,
            Path.Direction.CCW
        )
        canvas.clipPath(path)
        canvas.drawBitmap(
            bitmap,
            Rect(
                bitmap.width / 4 + 0,
                bitmap.height / 4 + 0,
                bitmap.width - bitmap.width / 4,
                bitmap.height - bitmap.height / 4
            ),
            Rect(0, 0, i, i),
            null as Paint?
        )
        return createBitmap
    }

    // android.view.View
    public override fun onDraw(canvas: Canvas) {
        val drawable: Drawable?
        val j: Long
        super.onDraw(canvas)
        if (!isInEditMode) {
            if (mShowCircleShadow) {
                if (mIsShowProgressOnRing) {
                    val rectF = mTempRectF
                    val i = mInnerSize
                    rectF[6.5f, -0.5f, i - 6.5f] = i + 0.5f
                } else {
                    val rectF2 = mTempRectF
                    val i2 = mInnerSize
                    rectF2[-0.5f, -0.5f, i2 + 0.5f] = i2 + 0.5f
                }
                mTempRectF.offset(
                    ((width - mInnerSize) / 2).toFloat(),
                    ((height - mInnerSize) / 2).toFloat()
                )
                canvas.drawArc(mTempRectF, 0.0f, 360.0f, true, mShadowPaint!!)
            }
            if (mIsDownloading) {
                drawDownloading(canvas)
                return
            }
            val rect = mTempRect
            val i3 = mDrawableSize
            var z = false
            rect[0, 0, i3] = i3
            mTempRect.offset((width - mDrawableSize) / 2, (height - mDrawableSize) / 2)
            val bitmap = mBackgroundBitmap
            if (bitmap != null) {
                canvas.drawBitmap(
                    bitmap,
                    (width / 2 - mInnerSize / 2).toFloat(),
                    (height / 2 - mInnerSize / 2).toFloat(),
                    mBackgroundBitmapPaint
                )
                canvas.drawCircle(
                    (width / 2).toFloat(),
                    (height / 2).toFloat(),
                    (mInnerSize / 2).toFloat(),
                    mBackgroundBitmapOverlayPaint!!
                )
            }
            if (isPlaying) {
                drawPlayingNoAnimation(canvas)
            } else {
                drawPaused(canvas)
            }
            if (mCircleRingWidth > 0) {
                if (mCircleProgress < 100.0f) {
                    val rectF3 = mTempRectF
                    val i4 = mInnerSize
                    rectF3[-0.5f, -0.5f, i4 + 0.5f] = i4 + 0.5f
                    mTempRectF.offset(
                        ((width - mInnerSize) / 2).toFloat(),
                        ((height - mInnerSize) / 2).toFloat()
                    )
                    val f = mCircleProgress * 360.0f / 100.0f
                    if (mIsShowCircleRingWithProgressOnly) {
                        canvas.drawArc(
                            mTempRectF,
                            mStartAngle.toFloat(),
                            -(360.0f - f),
                            false,
                            mCircleRingProgressPaint!!
                        )
                    } else {
                        canvas.drawCircle(
                            (width / 2).toFloat(),
                            (height / 2).toFloat(),
                            (mInnerSize / 2).toFloat(),
                            mCircleRingProgressPaint!!
                        )
                        canvas.drawArc(
                            mTempRectF,
                            mStartAngle.toFloat(),
                            f,
                            false,
                            mCircleRingPaint!!
                        )
                    }
                } else if (!mIsShowProgressOnRing) {
                    canvas.drawCircle(
                        (width / 2).toFloat(),
                        (height / 2).toFloat(),
                        (mInnerSize / 2).toFloat(),
                        mCircleRingPaint!!
                    )
                }
            }
            if (isSelected || isPressed || isFocused) {
                val rectF4 = mTempRectF
                val i5 = mInnerSize
                rectF4[-0.5f, -0.5f, i5 + 0.5f] = i5 + 0.5f
                mTempRectF.offset(
                    ((width - mInnerSize) / 2).toFloat(),
                    ((height - mInnerSize) / 2).toFloat()
                )
                canvas.drawArc(mTempRectF, 0.0f, 360.0f, true, mSelectorPaint!!)
            }
            if (mShowInnerBorder) {
                canvas.drawCircle(
                    (width / 2).toFloat(),
                    (height / 2).toFloat(),
                    (mInnerSize / 2 - mInnerStrokeWitdh / 2).toFloat(),
                    mInnerPaint!!
                )
            }
            if (mPulsingAnimation && mIsBuffering) {
                z = true
            }
            if (z) {
                drawBufferingPulseAnimation(canvas)
            }
            if (z) {
                j = if (API_17) {
                    SystemClock.elapsedRealtimeNanos()
                } else {
                    SystemClock.elapsedRealtime() * 1000000
                }
                mAnimationFrame = ((mLastTime - j) * 6.0E-8 + mAnimationFrame).toFloat()
                mLastTime = j
                ViewCompat.postInvalidateOnAnimation(this)
            }
            if (mIsAnimatedPlayPauseDrawable) {
                mPlayPauseDrawable!!.draw(canvas)
                return
            }
            drawable = if (isPlaying || mForceDrawPause) {
                mPauseDrawable
            } else {
                val z2 = isPlayed
                mPlayDrawable
            }
            drawable!!.bounds = mTempRect
            drawable.draw(canvas)
        }
    }

    // android.view.View
    public override fun onSizeChanged(i: Int, i2: Int, i3: Int, i4: Int) {
        super.onSizeChanged(i, i2, i3, i4)
        if (!isInEditMode) {
            mPlayPauseDrawable!!.setBounds(0, 0, i, i2)
        }
    }

    fun scalePlayPauseDrawable(f: Float) {
        val i = mDrawableSize
        if (i != 0) {
            mDrawableSize = (i * f).toInt()
            invalidate()
        }
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        mBackgroundBitmap = getRoundedShape(bitmap)
        val paint = Paint()
        mBackgroundBitmapPaint = paint
        paint.isAntiAlias = true
        mBackgroundBitmapPaint!!.isFilterBitmap = true
        mBackgroundBitmapPaint!!.isDither = true
        val paint2 = Paint()
        mBackgroundBitmapOverlayPaint = paint2
        paint2.color = Color.parseColor("#99444444")
        mBackgroundBitmapOverlayPaint!!.isAntiAlias = true
        invalidate()
    }

    fun setButtonColorsValue(i: Int, i2: Int) {
        if (mCircleFillPaint == null) {
            val paint = Paint()
            mCircleFillPaint = paint
            paint.isAntiAlias = true
        }
        mCircleFillPaint!!.color = i
        mProgressPaint!!.color = i2
        val parseColor = Color.parseColor("#eeeeee")
        mPlayDrawable = getColoredDrawable(mPlayDrawable!!, parseColor)
        mPauseDrawable = getColoredDrawable(mPauseDrawable!!, parseColor)
        invalidate()
    }

    fun setCircleFillAndRingColor(i: Int, i2: Int) {
        if (mCircleFillPaint == null) {
            val paint = Paint()
            mCircleFillPaint = paint
            paint.isAntiAlias = true
        }
        mCircleFillPaint!!.color = i
        if (mCircleRingColor != i2) {
            mCircleRingColor = i2
            setProgressRingColor(i2)
            mCircleRingPaint!!.color = mCircleRingColor
            mCircleRingProgressPaint!!.color = adjustAlpha(
                mCircleRingColor,
                if (isWindowBackgroundDark(context)) 0.5f else 0.3f
            )
            invalidate()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setCircleFillColor(i: Int) {
        if (mCircleFillPaint == null) {
            val paint = Paint()
            mCircleFillPaint = paint
            paint.isAntiAlias = true
        }
        mCircleFillPaint!!.color = i
        invalidate()
    }

    fun setCircleFillPlusRingAndPlayPauseColor(i: Int, i2: Int) {
        if (mCircleFillPaint == null) {
            val paint = Paint()
            mCircleFillPaint = paint
            paint.isAntiAlias = true
        }
        mCircleFillPaint!!.color = i
        if (mCircleRingColor != i) {
            mCircleRingColor = i
            setProgressRingColor(i)
            mCircleRingPaint!!.color = mCircleRingColor
            mCircleRingProgressPaint!!.color = adjustAlpha(
                mCircleRingColor,
                if (isWindowBackgroundDark(context)) 0.5f else 0.3f
            )
        }
        mPlayPauseDrawable!!.setColorFilter(i2, PorterDuff.Mode.SRC_IN)
        invalidate()
    }

    @Keep
    fun setCircleProgress(f: Float) {
        if (f != mCircleProgress) {
            mCircleProgress = f
            invalidate()
        }
    }

    fun setCircleProgressWithAnimation(f: Float, j: Long) {
        val ofFloat = ObjectAnimator.ofFloat(this, "circleProgress", f)
        mCircleProgressObjectAnimator = ofFloat
        ofFloat.duration = j
        mCircleProgressObjectAnimator!!.interpolator = DecelerateInterpolator()
        mCircleProgressObjectAnimator!!.start()
    }

    fun setCircleRingColor(i: Int) {
        if (mCircleRingColor != i) {
            mCircleRingColor = i
            setProgressRingColor(i)
            mCircleRingPaint!!.color = mCircleRingColor
            mCircleRingProgressPaint!!.color =
                adjustAlpha(
                    mCircleRingColor,
                    if (isWindowBackgroundDark(context)) 0.5f else 0.3f
                )
            invalidate()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setCircleRingWidth(i: Int) {
        mCircleRingWidth = i
        mCircleRingPaint!!.strokeWidth = i.toFloat()
        mCircleRingProgressPaint!!.strokeWidth = mCircleRingWidth.toFloat()
        mProgressRingPaint!!.strokeWidth = mCircleRingWidth.toFloat()
        ViewCompat.postInvalidateOnAnimation(this)
    }

    fun setIconSize(i: Int) {
        mDrawableSize = i
        invalidate()
    }

    fun setInnerSize(i: Int) {
        mInnerSize = i
    }

    fun setIsAnimatedPlayPauseDrawable(z: Boolean) {
        mIsAnimatedPlayPauseDrawable = z
    }

    fun setIsBuffering(z: Boolean) {
        if (mIsBuffering != z) {
            mIsBuffering = z
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setIsDownloading(z: Boolean) {
        if (mIsDownloading != z) {
            mIsDownloading = z
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setIsPaused(z: Boolean) {
        if (mIsPaused != z) {
            mIsPaused = z
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    fun setPercentageTextColor(i: Int) {
        mPercentageTextPaint!!.color = i
    }

    fun setPlayPauseColor(i: Int) {
        mPlayPauseDrawable!!.setColorFilter(i, PorterDuff.Mode.SRC_IN)
        invalidate()
    }

    fun setPlayPauseDrawable(drawable: Drawable?, drawable2: Drawable?) {
        if (drawable != null) {
            mPlayDrawable = drawable
        }
        if (drawable2 != null) {
            mPauseDrawable = drawable2
        }
        ViewCompat.postInvalidateOnAnimation(this)
    }

    fun setPlayPauseRedrawsEnabled(z: Boolean) {
        mPlayPauseRedrawsEnabled = z
    }

    fun setPlayingAndPlayed(z: Boolean, z2: Boolean, z3: Boolean) {
        val z4 = isPlaying
        if (!(z4 == z && isPlayed == z2)) {
            val z5 = z4 != z
            isPlaying = z
            if (z5) {
                mPlayPauseDrawable!!.setIsPlay(z)
                val animator = mPlayPauseAnimator
                if ((animator == null || !animator.isStarted || z3) && mPlayPauseRedrawsEnabled) {
                    if (z3) {
                        animatePlayPause()
                    } else {
                        val animator2 = mPlayPauseAnimator
                        if (animator2 == null || !animator2.isStarted) {
                            if (isPlaying) {
                                mPlayPauseDrawable!!.drawPause()
                            } else {
                                mPlayPauseDrawable!!.drawPlay()
                            }
                        }
                    }
                }
            }
            isPlayed = z2
            ViewCompat.postInvalidateOnAnimation(this)
        }
        setButtonContentDescription()
    }

    fun setProgressColor(i: Int) {
        mProgressPaint!!.color = i
        invalidate()
    }

    fun setPulsingAnimation(z: Boolean) {
        mPulsingAnimation = z
    }

    fun setSelectorColor(i: Int) {
        mSelectorPaint!!.color = i
    }

    fun setSelectorColorAutoAdjust(i: Int) {
        if (!isInEditMode) {
            mSelectorPaint!!.color = adjustAlpha(i, 0.12f)
        }
    }

    fun setShowInnerBorder(z: Boolean) {
        mShowInnerBorder = z
        invalidate()
    }

    fun showCircleRingWithProgressOnly(i: Int) {
        mIsShowCircleRingWithProgressOnly = true
        mCircleRingProgressPaint!!.color = i
        invalidate()
    }

    fun showShadow(i: Int, f: Float, f2: Float, f3: Float, z: Boolean) {
        mShowCircleShadow = true
        if (z) {
            mCircleRingPaint!!.setShadowLayer(2.0f, 0.0f, 0.0f, i)
        }
        val paint = Paint()
        mShadowPaint = paint
        paint.style = Paint.Style.FILL
        mShadowPaint!!.color = 0
        mShadowPaint!!.setShadowLayer(f2, 0.0f, f3, adjustAlpha(i, f))
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // android.view.View
    public override fun verifyDrawable(drawable: Drawable): Boolean {
        return drawable === mPlayPauseDrawable || super.verifyDrawable(drawable)
    }

    @JvmOverloads
    fun animatePlayPause(j: Long = 180L) {
        val animator = mPlayPauseAnimator
        if (animator != null) {
            animator.removeAllListeners()
            mPlayPauseAnimator!!.end()
            mPlayPauseAnimator!!.cancel()
        }
        val pausePlayAnimator = mPlayPauseDrawable!!.pausePlayAnimator
        mPlayPauseAnimator = pausePlayAnimator
        pausePlayAnimator.interpolator = LinearInterpolator()
        mPlayPauseAnimator!!.duration = j
        mPlayPauseAnimator!!.start()
    }

    fun setCircleProgress(f: Float, z: Boolean) {
        if (z) {
            cancelCircleProgressAnimation()
        }
        if (f != mCircleProgress) {
            mCircleProgress = f
            invalidate()
        }
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        init(attributeSet)
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        init(attributeSet)
    }

    override val label: Int
        get() = if (noMedia(feedItem)) {
            R.string.mark_read_no_media_label
        } else if (isPlaying) {
            R.string.pause_label
        } else {
            R.string.play_label
        }
    override val drawable: Int
        get() = 0

    fun setFeedItem(feedItem: FeedItem) {
        this.feedItem = feedItem
    }

    private var feedItem: FeedItem? = null

    override fun configure(button: View, icon: ImageView?, context: Activity) {
        super.configure(button, icon, context)
        if (noMedia(feedItem)) {
            setPlayPauseDrawable(
                getColoredVectorDrawable(
                    context,
                    R.drawable.ic_round_check_24,
                    accentColor(context)
                ),
                getColoredVectorDrawable(
                    context,
                    R.drawable.ic_round_check_24,
                    accentColor(context)
                )
            )
        } else {
            setPlayPauseDrawable(
                getColoredVectorDrawable(
                    context,
                    R.drawable.ic_play_48dp,
                    accentColor(context)
                ),
                getColoredVectorDrawable(
                    context,
                    R.drawable.ic_pause,
                    accentColor(context)
                )
            )
        }
    }

    override val isVisibility: Int
        get() = if (noMedia(feedItem)) {
            if (feedItem!!.isPlayed) {
                GONE
            } else {
                VISIBLE
            }
        } else VISIBLE

    override fun onClick(context: Activity?) {
        if(feedItem == null){
            return
        }
        val media: FeedMedia? = feedItem!!.media

        if (noMedia(feedItem)) {
            if (!feedItem!!.isPlayed()) {
                DBWriter.markItemPlayed(feedItem, FeedItem.PLAYED, true)
            }
        } else if (isPlaying) {
            if (FeedItemUtil.isCurrentlyPlaying(media)) {
                sendLocalBroadcast(context!!, PlaybackService.ACTION_PAUSE_PLAY_CURRENT_EPISODE)
            }
        } else {
            //需要这行，考虑这种情况app备份数据（item文件已下载），恢复后数据库显示已下载但是文件实际不存在
            //这种情况太少见了，所以稳定起见，只是继续往下执行
            if (!media!!.fileExists()) {
                //TODO:本地文件需要验证下
                DBTasks.notifyMissingFeedMediaFile(context, media)

                //使用流式播放弹窗提醒
                UsageStatistics.logAction(UsageStatistics.ACTION_STREAM)
                if (!NetworkUtils.isStreamingAllowed()) {
                    UseStreamConfirmDialog(context!!, media).show()
                    return
                }
            }


            PlaybackServiceStarter(context, media)
                .callEvenIfRunning(true)
                .start()
            if (media!!.mediaType == MediaType.VIDEO) {
                context!!.startActivity(PlaybackService.getPlayerActivityIntent(context, media))
            }
        }
    }

    override fun getDrawableTintColor(context: Context?): Int {
        return -1
    }


    companion object {
        const val ANIMATION_PLAY_PAUSE_DURATION: Long = 180
        const val ANIMATION_PLAY_PAUSE_DURATION_ACTION_DELAY: Long = 210
        private const val API_17 = true
        const val CIRCLE_PROGRESS_BG_ALPHA_DARK_BACKGROUND = 0.5f
        const val CIRCLE_PROGRESS_BG_ALPHA_LIGHT_BACKGROUND = 0.3f
        private const val MAX_PROGRESS_CICRCLE = 100
        private const val MIN_PROGRESS_CICRCLE = 0
        private const val TAG = "PlayPauseProgressButton"
        fun m30854l(sb2: StringBuilder, i: Int, str: String?): String {
            sb2.append(i)
            sb2.append(str)
            return sb2.toString()
        }
    }
    
    fun setAccentDefaultTheme(){
        setCircleFillAndRingColor(
            resolveColor(
                context,
                R.attr.colorSurface
            ), accentColor(context)
        )
        showShadow(
            accentColor(context),
            0.2f,
            UiUtils.dpToPx(context, 4.0f).toFloat(),
            UiUtils.dpToPx(context, 3.0f).toFloat(),
            false
        )
    }
}