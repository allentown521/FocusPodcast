package allen.town.podcast.core.util;

import allen.town.podcast.core.pref.Prefs;

public class TimeSpeedConverter {
    private final float speed;

    public TimeSpeedConverter(float speed) {
        this.speed = speed;
    }

    /** Convert millisecond according to the current playback speed
     * @param time time to convert
     * @return converted time (can be < 0 if time is < 0)
     */
    public int convert(int time) {
        boolean timeRespectsSpeed = Prefs.timeRespectsSpeed();
        if (time > 0 && timeRespectsSpeed) {
            return (int)(time / speed);
        }
        return time;
    }
}
