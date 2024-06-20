package allen.town.podcast.util;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

public class AnimationsUtils {
    public static final int DETAIL_SCREENS_ANIMATION_TYPE = 2;
    public static final int EPISODE_SERIES_DETAIL_ANIMATION_IMAGE_SHARED_ELEMENT_TRANSITION = 0;
    public static final int EPISODE_SERIES_DETAIL_ANIMATION_SLIDE_IN_FROM_RIGHT = 2;
    public static final int EPISODE_SERIES_DETAIL_ANIMATION_SLIDE_UP_FROM_BOTTOM = 1;
    private static final String TAG = "AnimationsUtils";


    public static void fadeIn(View view, int i) {
        fadeIn(view, i, 0);
    }

    public static Animation fadeOut(final View view, final int i, int i2, int i3) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setInterpolator(new AccelerateInterpolator());
        alphaAnimation.setDuration(i2);
        alphaAnimation.setStartOffset(i3);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() { // from class: fm.player.ui.animations.AnimationsUtils.1
            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationEnd(Animation animation) {
                View view2 = view;
                if (view2 != null) {
                    view2.setVisibility(i);
                }
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationRepeat(Animation animation) {
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationStart(Animation animation) {
            }
        });
        view.startAnimation(alphaAnimation);
        return alphaAnimation;
    }

    public static ValueAnimator getChangeImageColorOverTimeAnimation(@NonNull final ImageView imageView, final int i, final int i2, long j, boolean z) {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        ofFloat.setDuration(j);
        if (z) {
            ofFloat.setRepeatCount(-1);
        }
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: fm.player.ui.animations.a
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                AnimationsUtils.lambda$getChangeImageColorOverTimeAnimation$0(imageView, i, i2, valueAnimator);
            }
        });
        return ofFloat;
    }

    public static Animation getFadeAnimation(int i, boolean z) {
        AlphaAnimation alphaAnimation;
        if (z) {
            alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
            alphaAnimation.setInterpolator(new DecelerateInterpolator());
        } else {
            alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setInterpolator(new AccelerateInterpolator());
        }
        alphaAnimation.setDuration(i);
        return alphaAnimation;
    }

    public static Animation getScaleAnimation(int i, boolean z) {
        ScaleAnimation scaleAnimation;
        if (z) {
            scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, 1, 0.5f, 1, 0.5f);
            scaleAnimation.setInterpolator(new DecelerateInterpolator());
        } else {
            scaleAnimation = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f, 1, 0.5f, 1, 0.5f);
            scaleAnimation.setInterpolator(new AccelerateInterpolator());
        }
        scaleAnimation.setDuration(i);
        return scaleAnimation;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$getChangeImageColorOverTimeAnimation$0(ImageView imageView, int i, int i2, ValueAnimator valueAnimator) {
        imageView.setImageTintList(ColorStateList.valueOf(ColorUtils.blendARGB(i, i2, ((Float) valueAnimator.getAnimatedValue()).floatValue())));
    }

    public static void scaleDown(final View view, int i, final int i2) {
        Animation scaleAnimation = getScaleAnimation(i, false);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() { // from class: fm.player.ui.animations.AnimationsUtils.4
            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationEnd(Animation animation) {
                View view2 = view;
                if (view2 != null) {
                    view2.setVisibility(i2);
                }
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationRepeat(Animation animation) {
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationStart(Animation animation) {
            }
        });
        view.startAnimation(scaleAnimation);
    }

    public static void scaleUp(final View view, int i) {
        Animation scaleAnimation = getScaleAnimation(i, true);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() { // from class: fm.player.ui.animations.AnimationsUtils.3
            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationEnd(Animation animation) {
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationRepeat(Animation animation) {
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationStart(Animation animation) {
                View view2 = view;
                if (view2 != null) {
                    view2.setVisibility(View.VISIBLE);
                }
            }
        });
        view.startAnimation(scaleAnimation);
    }


    public static void fadeIn(final View view, int i, int i2) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setInterpolator(new DecelerateInterpolator());
        alphaAnimation.setDuration(i);
        alphaAnimation.setStartOffset(i2);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() { // from class: fm.player.ui.animations.AnimationsUtils.2
            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationEnd(Animation animation) {
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationRepeat(Animation animation) {
            }

            @Override // android.view.animation.Animation.AnimationListener
            public void onAnimationStart(Animation animation) {
                View view2 = view;
                if (view2 != null) {
                    view2.setVisibility(View.VISIBLE);
                }
            }
        });
        view.startAnimation(alphaAnimation);
    }


    public static Animation getScaleAnimation(int i, boolean z, float f, float f2, float f3, float f4) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(f, f2, f3, f4, 1, 0.5f, 1, 0.5f);
        scaleAnimation.setInterpolator(z ? new DecelerateInterpolator() : new AccelerateInterpolator());
        scaleAnimation.setDuration(i);
        return scaleAnimation;
    }
}

