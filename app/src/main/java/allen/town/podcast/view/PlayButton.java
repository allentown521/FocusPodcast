package allen.town.podcast.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import allen.town.focus_common.util.Timber;
import allen.town.podcast.R;
import allen.town.podcast.activity.LockScreenActivity;

public class PlayButton extends AppCompatImageButton {
    private boolean isShowPlay = true;
    private boolean isVideoScreen = false;

    public PlayButton(@NonNull Context context) {
        super(context);
    }

    public PlayButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setIsVideoScreen(boolean isVideoScreen) {
        this.isVideoScreen = isVideoScreen;
    }

    public void setIsShowPlay(boolean showPlay) {
        Context context = getContext();
        if (this.isShowPlay != showPlay) {
            this.isShowPlay = showPlay;
            setContentDescription(getContext().getString(showPlay ? R.string.play_label : R.string.pause_label));
            if (isVideoScreen) {
                setImageResource(showPlay ? R.drawable.ic_play_video_white : R.drawable.ic_pause_video_white);
            }else if (/*!isShown()*/!getGlobalVisibleRect(new Rect()) || context instanceof LockScreenActivity) {
                //ishown 对于在屏幕外的没有用，着色图标会导致动画执行完后显示异常（切换adapter app color必现），同时锁屏界面无法规避，确实是可见的时候执行了
                Timber.v("showPlay" + showPlay);
                setImageResource(showPlay ? R.drawable.ic_play_48dp : R.drawable.ic_pause);
            } else if (showPlay) {
                Timber.v("showPlayDrawbale to play");
                AnimatedVectorDrawableCompat drawable = AnimatedVectorDrawableCompat.create(
                        getContext(), R.drawable.ic_animate_pause_play);
                setImageDrawable(drawable);
                drawable.start();
            } else {
                Timber.v("showPlayDrawbale to pause");
                AnimatedVectorDrawableCompat drawable = AnimatedVectorDrawableCompat.create(
                        getContext(), R.drawable.ic_animate_play_pause);
                setImageDrawable(drawable);
                drawable.start();
            }
        }
    }
}
