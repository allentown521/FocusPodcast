package allen.town.podcast.core;

import android.content.Context;

import allen.town.podcast.net.ssl.SslProviderInstaller;

import allen.town.podcast.core.pref.PlaybackPreferences;
import allen.town.podcast.core.pref.SleepTimerPreferences;
import allen.town.podcast.core.pref.UsageStatistics;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.service.download.PodcastHttpClient;
import allen.town.podcast.core.util.NetworkUtils;
import allen.town.podcast.core.util.ui.NotificationUtils;
import allen.town.podcast.storage.db.Db;

import java.io.File;

/**
 * Stores callbacks for core classes like Services, DB classes etc. and other configuration variables.
 * Apps using the core module should register implementations of all interfaces here.
 */
public class ClientConfig {

    /**
     * Should be used when setting User-Agent header for HTTP-requests.
     */
    public static String USER_AGENT;

    public static ApplicationCallbacks applicationCallbacks;

    public static DownloadServiceCallbacks downloadServiceCallbacks;

    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }
        Db.init(context);
        Prefs.init(context);
        UsageStatistics.init(context);
        PlaybackPreferences.init(context);
        SslProviderInstaller.install(context);
        NetworkUtils.init(context);
        PodcastHttpClient.setCacheDirectory(new File(context.getCacheDir(), "okhttp"));
        PodcastHttpClient.setProxyConfig(Prefs.getProxyConfig());
        SleepTimerPreferences.init(context);
        NotificationUtils.createChannels(context);
        initialized = true;
    }
}
