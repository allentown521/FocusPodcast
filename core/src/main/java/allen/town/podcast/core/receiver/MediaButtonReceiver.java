package allen.town.podcast.core.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;

import allen.town.podcast.core.ClientConfig;
import allen.town.podcast.core.service.playback.PlaybackService;

/** Receives media button events. */
public class MediaButtonReceiver extends BroadcastReceiver {
	private static final String TAG = "MediaButtonReceiver";
	public static final String EXTRA_KEYCODE = "allen.town.podcast.core.service.extra.MediaButtonReceiver.KEYCODE";
	public static final String EXTRA_SOURCE = "allen.town.podcast.core.service.extra.MediaButtonReceiver.SOURCE";
	public static final String EXTRA_HARDWAREBUTTON = "allen.town.podcast.core.service.extra.MediaButtonReceiver.HARDWAREBUTTON";

	public static final String NOTIFY_BUTTON_RECEIVER = "allen.town.podcast.NOTIFY_BUTTON_RECEIVER";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
		if (intent == null || intent.getExtras() == null) {
			return;
		}
		KeyEvent event = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
		if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount()==0) {
			ClientConfig.initialize(context);
			Intent serviceIntent = new Intent(context, PlaybackService.class);
			serviceIntent.putExtra(EXTRA_KEYCODE, event.getKeyCode());
			serviceIntent.putExtra(EXTRA_SOURCE, event.getSource());
			//detect if this is a hardware button press
			if (event.getEventTime() > 0 || event.getDownTime() > 0) {
				serviceIntent.putExtra(EXTRA_HARDWAREBUTTON, true);
			} else {
				serviceIntent.putExtra(EXTRA_HARDWAREBUTTON, false);
			}
			ContextCompat.startForegroundService(context, serviceIntent);
		}

	}

}
