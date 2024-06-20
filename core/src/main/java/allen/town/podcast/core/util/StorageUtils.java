package allen.town.podcast.core.util;

import android.app.Activity;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.podcast.core.R;
import allen.town.podcast.core.pref.Prefs;

/**
 * Utility functions for handling storage errors
 */
public class StorageUtils {
    private StorageUtils(){}

    private static final String TAG = "StorageUtils";

    public static boolean storageAvailable() {
        File dir = Prefs.getDataFolder(null);
        if (dir != null) {
            return dir.exists() && dir.canRead() && dir.canWrite();
        } else {
            Log.d(TAG, "Storage not available: data folder is null");
            return false;
        }
    }

    /**
     * Checks if external storage is available. If external storage isn't
     * available, the current activity is finsished an an error activity is
     * launched.
     *
     * @param activity the activity which would be finished if no storage is
     *                 available
     * @return true if external storage is available
     */
    public static boolean checkStorageAvailability(Activity activity) {
        boolean storageAvailable = storageAvailable();
        if (!storageAvailable) {
            TopSnackbarUtil.showSnack(activity, R.string.external_storage_error_msg, Toast.LENGTH_LONG);
        }
        return storageAvailable;
    }

    /**
     * Get the number of free bytes that are available on the external storage.
     */
    public static long getFreeSpaceAvailable() {
        File dataFolder = Prefs.getDataFolder(null);
        if (dataFolder != null) {
            return getFreeSpaceAvailable(dataFolder.getAbsolutePath());
        } else {
            return 0;
        }
    }

    /**
     * Get the number of free bytes that are available on the external storage.
     */
    public static long getFreeSpaceAvailable(String path) {
        StatFs stat = new StatFs(path);
        long availableBlocks = stat.getAvailableBlocksLong();
        long blockSize = stat.getBlockSizeLong();
        return availableBlocks * blockSize;
    }

    public static long getTotalSpaceAvailable(String path) {
        StatFs stat = new StatFs(path);
        long blockCount = stat.getBlockCountLong();
        long blockSize = stat.getBlockSizeLong();
        return blockCount * blockSize;
    }
}
