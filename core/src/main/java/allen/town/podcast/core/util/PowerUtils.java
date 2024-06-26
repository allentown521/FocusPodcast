package allen.town.podcast.core.util;

import static android.content.Context.RECEIVER_EXPORTED;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

/**
 * Created by Tom on 1/5/15.
 */
public class PowerUtils {

    private PowerUtils() {

    }

    /**
     * @return true if the device is charging
     */
    public static boolean deviceCharging(Context context) {
        // from http://developer.android.com/training/monitoring-device-state/battery-monitoring.html
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            batteryStatus = context.registerReceiver(null, iFilter,RECEIVER_EXPORTED);
        } else {
            batteryStatus = context.registerReceiver(null, iFilter);
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL);

    }
}
