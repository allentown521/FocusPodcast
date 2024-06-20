package allen.town.podcast.core.widget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.receiver.MediaButtonReceiver;
import allen.town.podcast.core.util.Converter;
import allen.town.podcast.core.util.TimeSpeedConverter;
import allen.town.podcast.core.widget.appwidgets.AppWidgetBig;
import allen.town.podcast.core.widget.appwidgets.AppWidgetCircle;
import allen.town.podcast.core.widget.appwidgets.AppWidgetClassic;
import allen.town.podcast.core.widget.appwidgets.AppWidgetMD;
import allen.town.podcast.core.widget.appwidgets.AppWidgetMD3;
import allen.town.podcast.core.widget.appwidgets.AppWidgetText;
import allen.town.podcast.core.widget.base.BaseAppWidget;
import allen.town.podcast.model.playback.Playable;
import allen.town.podcast.playback.base.PlayerStatus;

/**
 * Updates the state of the player widget.
 */
public abstract class WidgetUpdater {
    private static final String TAG = "WidgetUpdater";

    public static class WidgetState {
        public final Playable media;
        public final PlayerStatus status;
        public final int position;
        public final int duration;
        public final float playbackSpeed;

        public WidgetState(Playable media, PlayerStatus status, int position, int duration, float playbackSpeed) {
            this.media = media;
            this.status = status;
            this.position = position;
            this.duration = duration;
            this.playbackSpeed = playbackSpeed;
        }

        public WidgetState(PlayerStatus status) {
            this(null, status, Playable.INVALID_TIME, Playable.INVALID_TIME, 1.0f);
        }
    }

    /**
     * Update the widgets with the given parameters. Must be called in a background thread.
     */
    public static void updateWidget(Context context, WidgetState widgetState) {
        if (widgetState == null) {
            Log.e(TAG,"updateWidget widgetState is null");
            return;
        }

        BaseAppWidget appWidgetClassic = AppWidgetClassic.getInstance();
        BaseAppWidget appWidgetMd = AppWidgetMD.getInstance();
        BaseAppWidget appWidgetMd3 = AppWidgetMD3.getInstance();
        BaseAppWidget appWidgetBig = AppWidgetBig.getInstance();
        BaseAppWidget appWidgetCircle = AppWidgetCircle.getInstance();
        BaseAppWidget appWidgetText = AppWidgetText.getInstance();


        appWidgetClassic.start(context, widgetState);
        appWidgetMd.start(context, widgetState);
        appWidgetMd3.start(context, widgetState);
        appWidgetBig.start(context, widgetState);
        appWidgetCircle.start(context, widgetState);
        appWidgetText.start(context, widgetState);

    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    public static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }

    /**
     * Creates an intent which fakes a mediabutton press.
     */
    public static PendingIntent createMediaButtonIntent(Context context, int eventCode) {
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, eventCode);
        Intent startingIntent = new Intent(context, MediaButtonReceiver.class);
        startingIntent.setAction(MediaButtonReceiver.NOTIFY_BUTTON_RECEIVER);
        startingIntent.putExtra(Intent.EXTRA_KEY_EVENT, event);

        return PendingIntent.getBroadcast(context, eventCode, startingIntent,
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    }

    public static String getProgressString(int position, int duration, float speed) {
        if (position < 0 || duration <= 0) {
            return null;
        }
        TimeSpeedConverter converter = new TimeSpeedConverter(speed);
        if (Prefs.shouldShowRemainingTime()) {
            return Converter.getDurationStringLong(converter.convert(position)) + " / -"
                    + Converter.getDurationStringLong(converter.convert(Math.max(0, duration - position)));
        } else {
            return Converter.getDurationStringLong(converter.convert(position)) + " / "
                    + Converter.getDurationStringLong(converter.convert(duration));
        }
    }
}
