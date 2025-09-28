package allen.town.podcast.model.feed;

import android.media.audiofx.AudioEffect;

import androidx.annotation.Nullable;

public enum VolumeAdaptionSetting {
    OFF(0, 1.0f),
    LIGHT_REDUCTION(1, 0.5f),
    HEAVY_REDUCTION(2, 0.2f);

    private final int value;
    private float adaptionFactor;

    VolumeAdaptionSetting(int value, float adaptionFactor) {
        this.value = value;
        this.adaptionFactor = adaptionFactor;
    }

    public static VolumeAdaptionSetting fromInteger(int value) {
        for (VolumeAdaptionSetting setting : values()) {
            if (setting.value == value) {
                return setting;
            }
        }
        throw new IllegalArgumentException("Cannot map value to VolumeAdaptionSetting: " + value);
    }

    @Nullable
    private static Boolean boostSupported = null;

    public static boolean isBoostSupported() {
        if (boostSupported != null) {
            return boostSupported;
        }
        final AudioEffect.Descriptor[] audioEffects = AudioEffect.queryEffects();
        if (audioEffects != null) {
            for (AudioEffect.Descriptor effect : audioEffects) {
                if (effect.type.equals(AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER)) {
                    boostSupported = true;
                    return boostSupported;
                }
            }
        }
        boostSupported = false;
        return boostSupported;
    }

    public int toInteger() {
        return value;
    }

    public float getAdaptionFactor() {
        return adaptionFactor;
    }
}
