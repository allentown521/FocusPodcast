package allen.town.podcast.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.google.android.material.slider.Slider;

import allen.town.podcast.R;

public class PlaybackSpeedSlider extends FrameLayout {
    private Slider seekBar;
    private Consumer<Float> progressChangedListener;

    public PlaybackSpeedSlider(@NonNull Context context) {
        super(context);
        setup();
    }

    public PlaybackSpeedSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public PlaybackSpeedSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setup();
    }

    private void setup() {
        View.inflate(getContext(), R.layout.playback_speed_seek_bar, this);
        seekBar = findViewById(R.id.playback_speed);
        findViewById(R.id.butDecSpeed).setOnClickListener(v -> seekBar.setValue(seekBar.getValue() - 2));
        findViewById(R.id.butIncSpeed).setOnClickListener(v -> seekBar.setValue(seekBar.getValue() + 2));

        seekBar.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                float playbackSpeed = (value + 10) / 20.0f;
                if (progressChangedListener != null) {
                    progressChangedListener.accept(playbackSpeed);
                }
            }
        });
    }

    public void updateSpeed(float speedMultiplier) {
        seekBar.setValue(Math.round((20 * speedMultiplier) - 10));
    }

    public void setProgressChangedListener(Consumer<Float> progressChangedListener) {
        this.progressChangedListener = progressChangedListener;
    }

    public float getCurrentSpeed() {
        return (seekBar.getValue() + 10) / 20.0f;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        seekBar.setEnabled(enabled);
        findViewById(R.id.butDecSpeed).setEnabled(enabled);
        findViewById(R.id.butIncSpeed).setEnabled(enabled);
    }
}
