package allen.town.podcast.core.pref

import allen.town.core.service.PayService
import allen.town.focus_common.model.CategoryInfo
import allen.town.focus_common.util.BasePreferenceUtil.instance
import allen.town.focus_common.util.BasePreferenceUtil.materialYou
import allen.town.focus_common.util.JsonHelper.parseStringList
import allen.town.focus_common.util.JsonHelper.toJSONString
import allen.town.focus_common.util.PodcastSearchPreferenceUtil
import allen.town.focus_common.util.Timber
import allen.town.podcast.core.R
import allen.town.podcast.core.feed.SubscriptionsFilter
import allen.town.podcast.core.playback.AlbumCoverStyle
import allen.town.podcast.core.playback.NowPlayingScreen
import allen.town.podcast.core.storage.*
import allen.town.podcast.core.util.download.AutoUpdateManager
import allen.town.podcast.core.view.TopAppBarLayout.AppBarMode
import allen.town.podcast.model.download.ProxyConfig
import allen.town.podcast.model.feed.FeedCounter
import allen.town.podcast.model.feed.SortOrder
import allen.town.podcast.model.playback.MediaType
import allen.town.focus_common.util.ThemeUtils.generalThemeValue
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.StyleRes
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.constants.ThemeConstants
import allen.town.focus_common.extensions.getStringOrDefault
import code.name.monkey.retromusic.util.theme.ThemeMode
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.wyjson.router.GoRouter
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.IOException
import java.net.Proxy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Provides access to preferences set by the user in the settings screen. A
 * private instance of this class must first be instantiated via
 * init() or otherwise every public method will throw an Exception
 * when called.
 */
object Prefs {
    private const val TAG = "Prefs"

    // User Interface
    const val PREF_THEME = ThemeConstants.GENERAL_THEME //主题key
    const val PREF_DRAWER_FEED_ORDER_METHOD = "pref_feed_order_method"
    const val PREF_DISABLE_FIREBASE = "prefDisableFirebase"

    //drive
    const val PREF_DROPBOX_REDENTIAL = "pref_dropbox_credential"
    const val PREF_DROPBOX_ACCESS_TOKEN = "pref_dropbox_access_token"
    const val PREF_QUEUE_KEEP_SORTED = "pref_queue_keep_sorted"
    
    //app启动首页
    const val PREF_HOME_PAGE = "pref_homepage"
    const val PREF_DRAWER_FEED_ORDER = "pref_feed_order"
    const val PREF_EXPANDED_NOTIFICATION = "pref_expand_notification"
    private const val PREF_PERSISTENT_NOTIFICATION = "pref_persist_notification"
    const val PREF_SHOW_TIME_LEFT = "pref_show_left_time" //显示剩余时间
    const val PREF_COMPACT_NOTIFICATION_BUTTONS = "pref_compact_noti_buttons"
    const val PREF_LOCKSCREEN_BACKGROUND = "pref_lock_screen_backgound"
    private const val PREF_DRAWER_FEED_COUNTER = "pref_feed_counts"
    private const val PREF_SHOW_DOWNLOAD_REPORT = "pref_show_download_sync_failed"
    const val PREF_COLUMN_IN_LANDSCAPE = "prefColumnDisplayInLandscape"
    const val PREF_BACK_BUTTON_BEHAVIOR = "pref_backbutton_behavior"
    const val PREF_USE_EPISODE_COVER = "pref_use_episode_cover" //使用单集封面
    const val PREF_SHOW_EPISODE_COVER_IN_FEED = "pref_show_episode_cover_in_feed"
    const val PREF_FILTER_FEED = "prefSubscriptionsFilter"
    const val PREF_SUBSCRIPTION_TITLE = "pref_show_sub_title"
    const val APPBAR_MODE = "appbar_mode"
    private const val PREF_SHOW_AUTO_DOWNLOAD_REPORT = "pref_show_auto_downlod_result"


    //播放列表排序
    const val PREF_QUEUE_KEEP_SORTED_ORDER = "pref_playlist_keep_order"


    // Other
    private const val PREF_DATA_FOLDER = "prefDataFolder"
    const val PREF_DELETE_REMOVES_FROM_QUEUE = "prefDeleteRemovesFromQueue"
    const val PREF_USAGE_COUNTING_DATE = "prefUsageCounting"
    const val PREF_ONLINE_PODCAST_SEARCH_HISTORY = "pref_online_podcast_search_history"
    const val PREF_FULL_LOCK_SCREEN = "pref_full_lock_screen"

    // Playback
    const val PREF_PAUSE_ON_HEADSET_DISCONNECT = "pref_pause_when_headset_disconnect"
    const val NEW_BLUR_AMOUNT = "new_blur_amount"
    const val PREF_UNPAUSE_ON_HEADSET_RECONNECT = "pref_play_when_headset_reconnect"
    const val PREF_HARDWARE_FORWARD_BUTTON = "pref_hardware_forward_button"
    private const val PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT = "pref_play_when_bluetooth_reconnect"
    const val PREF_FOLLOW_QUEUE = "pref_follow_playlist"
    const val PREF_SKIP_KEEPS_EPISODE = "pref_keep_episode_when_skip"
    private const val PREF_FAVORITE_KEEPS_EPISODE = "pref_keeps_favorite_episodes"
    private const val PREF_PLAYBACK_SPEED_ARRAY = "pref_playback_speed_list"
    private const val PREF_RESUME_AFTER_CALL = "pref_replay_after_call"
    private const val PREF_AUTO_DELETE = "pref_auto_delete"
    const val PREF_STREAM_OVER_DOWNLOAD = "pref_allow_stream_over_download"
    const val PREF_HARDWARE_PREVIOUS_BUTTON = "pref_hardware_previous_button"
    const val PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS = "pref_pause_when_loss_focus"
    const val PREF_SNOWFALL = "pre_show_snow_fall"
    private const val PREF_TIME_RESPECTS_SPEED = "pref_respects_playbacktime_for_speed"
    const val PREF_TOGGLE_ADD_CONTROLS = "toggle_add_controls"
    const val PREF_ADAPTIVE_COLOR_APP = "pref_adaptive_color_app"
    const val NOW_PLAYING_SCREEN_ID = "now_playing_screen_id"
    const val PREF_SMART_MARK_AS_PLAYED_SECS = "pref_smart_mark_as_played_secs"

    // Network
    private const val PREF_ENQUEUE_DOWNLOADED = "pref_add_to_playlist_when_download"
    const val PREF_UPDATE_INTERVAL = "pref_auto_refresh_interval"
    private const val PREF_MOBILE_UPDATE = "pref_mobile_update_types"
    const val PREF_ENQUEUE_LOCATION = "pref_episode_location_in_playlist"
    const val PREF_PARALLEL_DOWNLOADS = "pref_parallel_downloads"
    const val PREF_EPISODE_CACHE_SIZE = "pref_episodes_cache_size"
    const val PREF_ENABLE_AUTODL = "pref_auto_download_enable"
    const val PREF_ENABLE_AUTODL_ON_BATTERY = "pref_auto_download_enable_on_battery"
    const val PREF_EPISODE_CLEANUP = "pref_episodes_clean_up"
    private const val PREF_PROXY_HOST = "pref_proxy_host"
    private const val PREF_PROXY_PORT = "pref_proxy_port"
    private const val PREF_PROXY_TYPE = "pref_proxy_type"
    private const val PREF_PROXY_USER = "pref_proxy_username"
    private const val PREF_PROXY_PASSWORD = "pref_proxy_pass"
    private const val PREF_REFRESH_ON_START = "pref_refresh_on_start"

    // Services
    public const val PREF_GPODNET_NOTIFICATIONS = "pref_show_gpod_notifications"


    //音量增强
    private const val PREF_AUDIO_LOUDNESS = "pref_audio_loudness"
    const val EPISODE_CLEANUP_QUEUE = -1
    const val EPISODE_CLEANUP_NULL = -2
    const val EPISODE_CLEANUP_EXCEPT_FAVORITE = -3
    const val EPISODE_CLEANUP_DEFAULT = 0
    

    //全局的音频播放速度
    private const val PREF_PLAYBACK_SPEED = "pref_globa_playback_speed"
    private const val PREF_VIDEO_PLAYBACK_SPEED = "pref_global_video_playback_speed"
    const val PREF_PLAYBACK_SKIP_SILENCE = "pref_global_skip_silence"
    private const val PREF_FAST_FORWARD_SECS = "pref_global_fast_forward_secs"
    private const val PREF_REWIND_SECS = "pref_global_rewind_secs"

    //锁定播放列表
    private const val PREF_QUEUE_LOCKED = "pref_queue_Locked"

    // Experimental
    private const val PREF_STEREO_TO_MONO = "pref_stereo_to_mono"


    // Constants
    private const val NOTIFICATION_BUTTON_REWIND = 0
    private const val NOTIFICATION_BUTTON_FAST_FORWARD = 1
    private const val NOTIFICATION_BUTTON_SKIP = 2
    private const val EPISODE_CACHE_SIZE_UNLIMITED = -1
    const val FEED_ORDER_COUNTER = 0
    const val FEED_ORDER_ALPHABETICAL = 1
    const val ORDER_ASC = "asc"
    const val ORDER_DESC = "desc"
    private var context: Context? = null
    private var prefs: SharedPreferences? = null
    private var themePrefs: SharedPreferences? = null

    /**
     * Sets up the Prefs class.
     *
     * @throws IllegalArgumentException if context is null
     */
    @JvmStatic
    fun init(context: Context) {
        Log.d(TAG, "init")
        Prefs.context = context.applicationContext
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        themePrefs = instance(context)
        createNoMediaFile()
    }

    /**
     * 获取当前主题
     *
     * @return R.style.Theme_FocusPodcast_Light or R.style.Theme_FocusPodcast_Dark
     */
    @get:StyleRes
    @JvmStatic
    val theme: Int
        get() = if (materialYou) {
            if (generalThemeValue(context!!) === ThemeMode.BLACK) R.style.Theme_FocusPodcast_MD3_Base_Black else R.style.Theme_FocusPodcast_MD3_Base
        } else {
            val themeMode = generalThemeValue(
                context!!
            )
            if (themeMode === ThemeMode.LIGHT) {
                R.style.Theme_FocusPodcast_Light
            } else if (themeMode === ThemeMode.DARK) {
                R.style.Theme_FocusPodcast_Dark
            } else if (themeMode === ThemeMode.BLACK) {
                R.style.Theme_FocusPodcast_TrueBlack
            } else {
                R.style.Theme_FocusPodcast_Light
            }
        }
    @JvmStatic
    fun getDropboxCredential(context: Context?): String? {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_DROPBOX_REDENTIAL, null)
    }
    @JvmStatic
    fun setDropboxCredential(context: Context?, value: String?) {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_DROPBOX_REDENTIAL, value).commit()
    }
    @JvmStatic
    fun getDropboxAccessToken(context: Context?): String? {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_DROPBOX_ACCESS_TOKEN, null)
    }
    @JvmStatic
    fun setDropboxAccessToken(context: Context?, value: String?) {
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .edit().putString(PREF_DROPBOX_ACCESS_TOKEN, value).commit()
    }
    @JvmStatic
    var compactNotificationButtons: List<Int?>?
        get() {
            val buttons = TextUtils.split(
                prefs!!.getString(
                    PREF_COMPACT_NOTIFICATION_BUTTONS,
                    NOTIFICATION_BUTTON_REWIND.toString() + "," + NOTIFICATION_BUTTON_FAST_FORWARD
                ),
                ","
            )
            val notificationButtons: MutableList<Int> = ArrayList()
            for (button in buttons) {
                notificationButtons.add(button.toInt())
            }
            return notificationButtons
        }
        set(items) {
            val str = TextUtils.join(",", items!!)
            prefs!!.edit()
                .putString(PREF_COMPACT_NOTIFICATION_BUTTONS, str)
                .apply()
        }

    /**
     * Helper function to return whether the specified button should be shown on compact
     * notifications.
     *
     * @param buttonId Either NOTIFICATION_BUTTON_REWIND, NOTIFICATION_BUTTON_FAST_FORWARD or
     * NOTIFICATION_BUTTON_SKIP.
     * @return `true` if button should be shown, `false`  otherwise
     */
    private fun showButtonOnCompactNotification(buttonId: Int): Boolean {
        return compactNotificationButtons!!.contains(buttonId)
    }
    @JvmStatic
    fun showRewindOnCompactNotification(): Boolean {
        return showButtonOnCompactNotification(NOTIFICATION_BUTTON_REWIND)
    }
    @JvmStatic
    fun showFastForwardOnCompactNotification(): Boolean {
        return showButtonOnCompactNotification(NOTIFICATION_BUTTON_FAST_FORWARD)
    }
    @JvmStatic
    fun showSkipOnCompactNotification(): Boolean {
        return showButtonOnCompactNotification(NOTIFICATION_BUTTON_SKIP)
    }
    @JvmStatic
    val feedOrder: Int
        get() {
            val value = prefs!!.getString(PREF_DRAWER_FEED_ORDER, "" + FEED_ORDER_COUNTER)
            return value!!.toInt()
        }
    @JvmStatic
    fun setFeedOrder(selected: String?) {
        prefs!!.edit()
            .putString(PREF_DRAWER_FEED_ORDER, selected)
            .apply()
    }

    @JvmStatic
    var feedOrderMethod: String
        get() = prefs!!.getString(PREF_DRAWER_FEED_ORDER_METHOD, ORDER_ASC)!!
        set(selected) {
            prefs!!.edit()
                .putString(PREF_DRAWER_FEED_ORDER_METHOD, selected)
                .apply()
        }

    @JvmStatic
    val feedCounterSetting: FeedCounter
        get() {
            val value = prefs!!.getString(
                PREF_DRAWER_FEED_COUNTER,
                "" + FeedCounter.SHOW_NEW_UNPLAYED_SUM.id
            )
            return FeedCounter.fromOrdinal(value!!.toInt())
        }

    /**
     * @return `true` if episodes should use their own cover, `false`  otherwise
     */
    @JvmStatic
    val useEpisodeCoverSetting: Boolean
        get() = prefs!!.getBoolean(PREF_USE_EPISODE_COVER, true)

    @JvmStatic
    val showEpisodeCoverInFeed: Boolean
        get() = prefs!!.getBoolean(PREF_SHOW_EPISODE_COVER_IN_FEED, true)


    /**
     * @return `true` if we should show remaining time or the duration
     */
    @JvmStatic
    fun shouldShowRemainingTime(): Boolean {
        return prefs!!.getBoolean(PREF_SHOW_TIME_LEFT, false)
    }

    /**
     * 是否显示最近打开的界面，1是第一个item，0是最近
     * @return
     */
    @JvmStatic
    fun shouldShowLastPageOfHome(): Boolean {
        return prefs!!.getString(PREF_HOME_PAGE, "0") == "0"
    }

    /**
     * Sets the preference for whether we show the remain time, if not show the duration. This will
     * send out events so the current playing screen, queue and the episode list would refresh
     *
     * @return `true` if we should show remaining time or the duration
     */
    @JvmStatic
    fun setShowRemainTimeSetting(showRemain: Boolean?) {
        prefs!!.edit().putBoolean(PREF_SHOW_TIME_LEFT, showRemain!!).apply()
    }

    /**
     * Returns notification priority.
     *
     * @return NotificationCompat.PRIORITY_MAX or NotificationCompat.PRIORITY_DEFAULT
     */
    @JvmStatic
    val notifyPriority: Int
        get() = if (prefs!!.getBoolean(PREF_EXPANDED_NOTIFICATION, false)) {
            NotificationCompat.PRIORITY_MAX
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }
    @JvmStatic
    val isDisableFirebase: Boolean
        get() = prefs!!.getBoolean(PREF_DISABLE_FIREBASE, false)

    /**
     * Returns true if notifications are persistent
     *
     * @return `true` if notifications are persistent, `false`  otherwise
     */
    @JvmStatic
    val isPersistNotify: Boolean
        get() = prefs!!.getBoolean(PREF_PERSISTENT_NOTIFICATION, true)

    /**
     * Returns true if the lockscreen background should be set to the current episode's image
     *
     * @return `true` if the lockscreen background should be set, `false`  otherwise
     */
    @JvmStatic
    fun setLockscreenBackground(): Boolean {
        return prefs!!.getBoolean(PREF_LOCKSCREEN_BACKGROUND, true)
    }

    /**
     * Returns true if download reports are shown
     *
     * @return `true` if download reports are shown, `false`  otherwise
     */
    @JvmStatic
    fun showDownloadReport(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) {
            true // System handles notification preferences
        } else prefs!!.getBoolean(
            PREF_SHOW_DOWNLOAD_REPORT,
            true
        )
    }

    /**
     * Used for migration of the preference to system notification channels.
     */
    @JvmStatic
    val showDownloadReportRaw: Boolean
        get() = prefs!!.getBoolean(PREF_SHOW_DOWNLOAD_REPORT, true)

    @JvmStatic
    fun showAutoDownloadReport(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) {
            true // System handles notification preferences
        } else prefs!!.getBoolean(PREF_SHOW_AUTO_DOWNLOAD_REPORT, false)
    }

    /**
     * Used for migration of the preference to system notification channels.
     */
    @JvmStatic
    val showAutoDownloadReportRaw: Boolean
        get() = prefs!!.getBoolean(PREF_SHOW_AUTO_DOWNLOAD_REPORT, false)

    @JvmStatic
    fun enqueueDownloadedEpisodes(): Boolean {
        return prefs!!.getBoolean(PREF_ENQUEUE_DOWNLOADED, true)
    }

    @VisibleForTesting
    @JvmStatic
    fun setEnqueueDownloadedEpisodes(enqueueDownloadedEpisodes: Boolean) {
        prefs!!.edit()
            .putBoolean(PREF_ENQUEUE_DOWNLOADED, enqueueDownloadedEpisodes)
            .apply()
    }

    // should never happen but just in case
    @JvmStatic
    var enqueueLocation: EnqueueLocation
        get() {
            val valStr = prefs!!.getString(PREF_ENQUEUE_LOCATION, EnqueueLocation.BACK.name)
            return try {
                EnqueueLocation.valueOf(valStr!!)
            } catch (t: Throwable) {
                // should never happen but just in case
                Log.e(TAG, "getEnqueueLocation: invalid value '$valStr' Use default.", t)
                EnqueueLocation.BACK
            }
        }
        set(location) {
            prefs!!.edit()
                .putString(PREF_ENQUEUE_LOCATION, location.name)
                .apply()
        }
    @JvmStatic
    val isPauseOnHeadsetDisconnect: Boolean
        get() = prefs!!.getBoolean(PREF_PAUSE_ON_HEADSET_DISCONNECT, true)
    @JvmStatic
    val isUnpauseOnHeadsetReconnect: Boolean
        get() = prefs!!.getBoolean(PREF_UNPAUSE_ON_HEADSET_RECONNECT, true)
    @JvmStatic
    val isUnpauseOnBluetoothReconnect: Boolean
        get() = prefs!!.getBoolean(PREF_UNPAUSE_ON_BLUETOOTH_RECONNECT, false)
    @JvmStatic
    val hardwareForwardButton: Int
        get() = prefs!!.getString(
            PREF_HARDWARE_FORWARD_BUTTON,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD.toString()
        )!!.toInt()
    @JvmStatic
    val hardwarePreviousButton: Int
        get() = prefs!!.getString(
            PREF_HARDWARE_PREVIOUS_BUTTON,
            KeyEvent.KEYCODE_MEDIA_REWIND.toString()
        )!!.toInt()

    /**
     * Set to true to enable Continuous Playback
     */
    @set:VisibleForTesting
    @JvmStatic
    var isFollowQueue: Boolean
        get() = prefs!!.getBoolean(PREF_FOLLOW_QUEUE, true)
        set(value) {
            prefs!!.edit().putBoolean(PREF_FOLLOW_QUEUE, value).apply()
        }
    @JvmStatic
    fun shouldSkipKeepEpisode(): Boolean {
        return prefs!!.getBoolean(PREF_SKIP_KEEPS_EPISODE, true)
    }
    @JvmStatic
    fun shouldFavoriteKeepEpisode(): Boolean {
        return prefs!!.getBoolean(PREF_FAVORITE_KEEPS_EPISODE, true)
    }

    @JvmStatic
    val isAutoDelete: Boolean
        get() = prefs!!.getBoolean(PREF_AUTO_DELETE, false)
    @JvmStatic
    val smartMarkAsPlayedSecs: Int
        get() = prefs!!.getString(PREF_SMART_MARK_AS_PLAYED_SECS, "30")!!.toInt()
    @JvmStatic
    fun shouldDeleteRemoveFromQueue(): Boolean {
        return prefs!!.getBoolean(PREF_DELETE_REMOVES_FROM_QUEUE, false)
    }
    @JvmStatic
    fun getPlaybackSpeed(mediaType: MediaType?): Float {
        return if (mediaType == MediaType.VIDEO) {
            videoPlaybackSpeed
        } else {
            audioPlaybackSpeed
        }
    }

    private const val PODCAST_SEARCH_ENGINE_LIST = "podcast_search_engine_list"
    @JvmStatic
    var podcastSearchEngineList: List<CategoryInfo>
        get() {
            val gson = Gson()
            val collectionType = object : TypeToken<List<CategoryInfo>>() {}.type

            val data = prefs!!.getStringOrDefault(PODCAST_SEARCH_ENGINE_LIST, gson.toJson(
                PodcastSearchPreferenceUtil.defaultSearchEngine, collectionType))
            return try {
                Gson().fromJson(data, collectionType)
            } catch (e: JsonSyntaxException) {
                Timber.e(e,"podcastSearchEngineList")
                return PodcastSearchPreferenceUtil.defaultSearchEngine!!
            }
        }
        set(value) {
            val collectionType = object : TypeToken<List<CategoryInfo>>() {}.type
            prefs!!.edit()
                .putString(PODCAST_SEARCH_ENGINE_LIST, Gson().toJson(value, collectionType))
                .apply()
        }

    /**
     * 获取全局的音频播放速度
     * @return
     */
    @JvmStatic
    private val audioPlaybackSpeed: Float
        private get() = try {
            prefs!!.getString(PREF_PLAYBACK_SPEED, "1.00")!!.toFloat()
        } catch (e: NumberFormatException) {
            Log.e(TAG, Log.getStackTraceString(e))
            setPlaybackSpeed(1.0f)
            1.0f
        }

    /**
     * 获取全局的视频播放速度
     * @return
     */
    @JvmStatic
    var videoPlaybackSpeed: Float
        get() = try {
            prefs!!.getString(PREF_VIDEO_PLAYBACK_SPEED, "1.00")!!.toFloat()
        } catch (e: NumberFormatException) {
            Log.e(TAG, Log.getStackTraceString(e))
            videoPlaybackSpeed = 1.0f
            1.0f
        }
        set(speed) {
            prefs!!.edit()
                .putString(PREF_VIDEO_PLAYBACK_SPEED, speed.toString())
                .apply()
        }
    @JvmStatic
    var isSkipSilence: Boolean
        get() = prefs!!.getBoolean(PREF_PLAYBACK_SKIP_SILENCE, false)
        set(skipSilence) {
            prefs!!.edit()
                .putBoolean(PREF_PLAYBACK_SKIP_SILENCE, skipSilence)
                .apply()
        }
    @JvmStatic
    var playbackSpeedArray: List<Float>
        get() = readPlaybackSpeedArray(prefs!!.getString(PREF_PLAYBACK_SPEED_ARRAY, null))
        set(speeds) {
            val format = DecimalFormatSymbols(Locale.US)
            format.decimalSeparator = '.'
            val speedFormat = DecimalFormat("0.0", format)
            val jsonArray = JSONArray()
            for (speed in speeds) {
                jsonArray.put(speedFormat.format(speed.toDouble()))
            }
            prefs!!.edit()
                .putString(PREF_PLAYBACK_SPEED_ARRAY, jsonArray.toString())
                .apply()
        }
    @JvmStatic
    fun shouldPauseForFocusLoss(): Boolean {
        return prefs!!.getBoolean(PREF_PAUSE_PLAYBACK_FOR_FOCUS_LOSS, true)
    }
    /*
     * Returns update interval in milliseconds; value 0 means that auto update is disabled
     * or feeds are updated at a certain time of day
     */// when updating with an interval, we assume the user wants
    // to update *now* and then every 'hours' interval thereafter.
    /**
     * Sets the update interval value.
     */
    @JvmStatic
    var updateInterval: Long
        get() {
            val updateInterval = prefs!!.getString(PREF_UPDATE_INTERVAL, "0")
            return if (!updateInterval!!.contains(":")) {
                readUpdateInterval(updateInterval)
            } else {
                0
            }
        }
        set(hours) {
            prefs!!.edit()
                .putString(PREF_UPDATE_INTERVAL, hours.toString())
                .apply()
            // when updating with an interval, we assume the user wants
            // to update *now* and then every 'hours' interval thereafter.
            AutoUpdateManager.restartUpdateAlarm(context)
        }
    @JvmStatic
    val updateTimeOfDay: IntArray
        get() {
            val datetime = prefs!!.getString(PREF_UPDATE_INTERVAL, "")
            return if (datetime!!.length >= 3 && datetime.contains(":")) {
                val parts = datetime.split(":".toRegex()).toTypedArray()
                val hourOfDay = parts[0].toInt()
                val minute = parts[1].toInt()
                intArrayOf(hourOfDay, minute)
            } else {
                IntArray(0)
            }
        }
    @JvmStatic
    val isAutoUpdateDisabled: Boolean
        get() = prefs!!.getString(PREF_UPDATE_INTERVAL, "") == "0"

    private fun isAllowMobileFor(type: String): Boolean {
        val defaultValue = HashSet<String>()
        defaultValue.add("images")
        val allowed = prefs!!.getStringSet(PREF_MOBILE_UPDATE, defaultValue)
        return allowed!!.contains(type)
    }

    @JvmStatic
    var isAllowMobileFeedRefresh: Boolean
        get() = isAllowMobileFor("feed_refresh")
        set(allow) {
            setAllowMobileFor("feed_refresh", allow)
        }

    @JvmStatic
    var isAllowMobileEpisodeDownload: Boolean
        get() = isAllowMobileFor("episode_download")
        set(allow) {
            setAllowMobileFor("episode_download", allow)
        }

    @JvmStatic
    var isAllowMobileAutoDownload: Boolean
        get() = isAllowMobileFor("auto_download")
        set(allow) {
            setAllowMobileFor("auto_download", allow)
        }

    @JvmStatic
    var isAllowMobileStreaming: Boolean
        get() = isAllowMobileFor("streaming")
        set(allow) {
            setAllowMobileFor("streaming", allow)
        }

    @JvmStatic
    var isAllowMobileImages: Boolean
        get() = isAllowMobileFor("images")
        set(allow) {
            setAllowMobileFor("images", allow)
        }

    private fun setAllowMobileFor(type: String, allow: Boolean) {
        val defaultValue = HashSet<String>()
        defaultValue.add("images")
        val getValueStringSet = prefs!!.getStringSet(PREF_MOBILE_UPDATE, defaultValue)
        val allowed: MutableSet<String> = HashSet(getValueStringSet)
        if (allow) {
            allowed.add(type)
        } else {
            allowed.remove(type)
        }
        prefs!!.edit().putStringSet(PREF_MOBILE_UPDATE, allowed).apply()
    }

    @JvmStatic
    val parallelDownloads: Int
        get() = prefs!!.getString(PREF_PARALLEL_DOWNLOADS, "4")!!.toInt()
    @JvmStatic
    val episodeCacheSizeUnlimited: Int
        get() = context!!.resources.getInteger(R.integer.episode_cache_size_unlimited)

    /**
     * Returns the capacity of the episode cache. This method will return the
     * negative integer EPISODE_CACHE_SIZE_UNLIMITED if the cache size is set to
     * 'unlimited'.
     */
    @JvmStatic
    val episodeCacheSize: Int
        get() = readEpisodeCacheSizeInternal(prefs!!.getString(PREF_EPISODE_CACHE_SIZE, "20"))

    @set:VisibleForTesting
    @JvmStatic
    var isEnableAutodownload: Boolean
        get() = prefs!!.getBoolean(PREF_ENABLE_AUTODL, false)
        set(enabled) {
            prefs!!.edit().putBoolean(PREF_ENABLE_AUTODL, enabled).apply()
        }
    @JvmStatic
    val isEnableAutodownloadOnBattery: Boolean
        get() = prefs!!.getBoolean(PREF_ENABLE_AUTODL_ON_BATTERY, true)

    @JvmStatic
    var fastForwardSecs: Int
        get() = prefs!!.getInt(PREF_FAST_FORWARD_SECS, 30)
        set(secs) {
            prefs!!.edit()
                .putInt(PREF_FAST_FORWARD_SECS, secs)
                .apply()
        }
    @JvmStatic
    var rewindSecs: Int
        get() = prefs!!.getInt(PREF_REWIND_SECS, 10)
        set(secs) {
            prefs!!.edit()
                .putInt(PREF_REWIND_SECS, secs)
                .apply()
        }

    @JvmStatic
    var proxyConfig: ProxyConfig
        get() {
            val type =
                Proxy.Type.valueOf(prefs!!.getString(PREF_PROXY_TYPE, Proxy.Type.DIRECT.name)!!)
            val host = prefs!!.getString(PREF_PROXY_HOST, null)
            val port = prefs!!.getInt(PREF_PROXY_PORT, 0)
            val username = prefs!!.getString(PREF_PROXY_USER, null)
            val password = prefs!!.getString(PREF_PROXY_PASSWORD, null)
            return ProxyConfig(type, host, port, username, password)
        }
        set(config) {
            val editor = prefs!!.edit()
            editor.putString(PREF_PROXY_TYPE, config.type.name)
            if (TextUtils.isEmpty(config.host)) {
                editor.remove(PREF_PROXY_HOST)
            } else {
                editor.putString(PREF_PROXY_HOST, config.host)
            }
            if (config.port <= 0 || config.port > 65535) {
                editor.remove(PREF_PROXY_PORT)
            } else {
                editor.putInt(PREF_PROXY_PORT, config.port)
            }
            if (TextUtils.isEmpty(config.username)) {
                editor.remove(PREF_PROXY_USER)
            } else {
                editor.putString(PREF_PROXY_USER, config.username)
            }
            if (TextUtils.isEmpty(config.password)) {
                editor.remove(PREF_PROXY_PASSWORD)
            } else {
                editor.putString(PREF_PROXY_PASSWORD, config.password)
            }
            editor.apply()
        }

    @JvmStatic
    fun shouldResumeAfterCall(): Boolean {
        return prefs!!.getBoolean(PREF_RESUME_AFTER_CALL, true)
    }

    @JvmStatic
    fun showSnowFall(): Boolean {
        return prefs!!.getBoolean(PREF_SNOWFALL, false)
    }

    @JvmStatic
    fun showExtraMiniButtons(): Boolean {
        return prefs!!.getBoolean(PREF_TOGGLE_ADD_CONTROLS, false)
    }

    @JvmStatic
    val isAdapterColor: Boolean
        get() = prefs!!.getBoolean(PREF_ADAPTIVE_COLOR_APP, true)

    /**
     * 收费播放主题又不是付费用户
     */
    fun isNowPlayingThemesNotAvalibale(screen: NowPlayingScreen): Boolean {
        return (screen.isCharge) && !GoRouter.getInstance().getService(PayService::class.java)!!.isPurchase(null,false)
    }
    // Also set a cover theme for that now playing
    @JvmStatic
    var nowPlayingScreen: NowPlayingScreen
        get() {
            val id = prefs!!.getInt(NOW_PLAYING_SCREEN_ID, 0)
            for (nowPlayingScreen in NowPlayingScreen.values()) {
                if (nowPlayingScreen.id == id) {
                    if (isNowPlayingThemesNotAvalibale(nowPlayingScreen)) {
                        Timber.i("is not pro use default playing theme")
                    } else {
                        return nowPlayingScreen
                    }
                }
            }
            return NowPlayingScreen.Normal
        }
        set(nowPlayingScreen) {
            prefs!!.edit()
                .putInt(NOW_PLAYING_SCREEN_ID, nowPlayingScreen.id)
                .apply()
            // Also set a cover theme for that now playing
            albumCoverStyle = nowPlayingScreen.defaultCoverTheme
        }

    @JvmStatic
    var ALBUM_COVER_STYLE = "album_cover_style_id"
    @JvmStatic
    var albumCoverStyle: AlbumCoverStyle?
        get() {
            val id = prefs!!.getInt(ALBUM_COVER_STYLE, 0)
            for (albumCoverStyle in AlbumCoverStyle.values()) {
                if (albumCoverStyle.id == id) {
                    val screenId = prefs!!.getInt(NOW_PLAYING_SCREEN_ID, 0)
                    var isNowPlayingThemesNotAvalibale = false
                    for (nowPlayingScreen in NowPlayingScreen.values()) {
                        if (nowPlayingScreen.id == screenId) {
                            isNowPlayingThemesNotAvalibale = isNowPlayingThemesNotAvalibale(nowPlayingScreen)
                        }
                    }
                    //coverStyle和playingScreen匹配，如果需要用默认的都使用默认
                    if (isNowPlayingThemesNotAvalibale) {
                        Timber.i("is not pro use default album cover")
                    } else {
                        return albumCoverStyle
                    }
                }
            }
            return AlbumCoverStyle.Normal
        }
        set(albumCoverStyle) {
            prefs!!.edit()
                .putInt(ALBUM_COVER_STYLE, albumCoverStyle!!.id)
                .apply()
        }
    @JvmStatic
    val blurAmount: Int
        get() = prefs!!.getInt(NEW_BLUR_AMOUNT, 12)

    /**
     * 播放列表是否锁定了
     * @return
     */
    @JvmStatic
    var isPlaylistLocked: Boolean
        get() = prefs!!.getBoolean(PREF_QUEUE_LOCKED, false)
        set(locked) {
            prefs!!.edit()
                .putBoolean(PREF_QUEUE_LOCKED, locked)
                .apply()
        }

    @JvmStatic
    fun setPlaybackSpeed(speed: Float) {
        prefs!!.edit()
            .putString(PREF_PLAYBACK_SPEED, speed.toString())
            .apply()
    }

    private const val LAST_CHECKED_APP_VERSION = "last_checked_app_version"
    private const val LAST_CHECKED_NOTIFY_VERSION = "last_checked_notify_version"

    @JvmStatic
    var versionCode: Int
        get() = prefs!!.getInt(LAST_CHECKED_APP_VERSION, 0)
        set(versionCode) {
            prefs!!.edit().putInt(LAST_CHECKED_APP_VERSION, versionCode).apply()
        }

    @JvmStatic
    var notifyVersionCode: Int
        get() = prefs!!.getInt(LAST_CHECKED_NOTIFY_VERSION, 0)
        set(versionCode) {
            prefs!!.edit().putInt(LAST_CHECKED_NOTIFY_VERSION, versionCode).apply()
        }

    /**
     * 检查该版本是否被用户取消过更新
     *
     * @param newVersion
     * @return
     */
    @JvmStatic
    fun lastVersionChecked(newVersion: Int): Boolean {
        return versionCode == newVersion
    }

    @JvmStatic
    fun lastNotifyVersionChecked(newVersion: Int): Boolean {
        return notifyVersionCode == newVersion
    }


    /**
     * Sets the update interval value.
     */
    @JvmStatic
    fun setUpdateTimeOfDay(hourOfDay: Int, minute: Int) {
        prefs!!.edit()
            .putString(PREF_UPDATE_INTERVAL, "$hourOfDay:$minute")
            .apply()
        AutoUpdateManager.restartUpdateAlarm(context)
    }

    @JvmStatic
    fun disableAutoUpdate(context: Context?) {
        prefs!!.edit()
            .putString(PREF_UPDATE_INTERVAL, "0")
            .apply()
        AutoUpdateManager.disableAutoUpdate(context)
    }

    @JvmStatic
    fun gpodnetNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= 26) {
            true // System handles notification preferences
        } else prefs!!.getBoolean(PREF_GPODNET_NOTIFICATIONS, true)
    }

    /**
     * Used for migration of the preference to system notification channels.
     */
    @JvmStatic
    val gpodnetNotificationsEnabledRaw: Boolean
        get() = prefs!!.getBoolean(PREF_GPODNET_NOTIFICATIONS, true)

    @JvmStatic
    fun setGpodnetNotificationsEnabled() {
        prefs!!.edit()
            .putBoolean(PREF_GPODNET_NOTIFICATIONS, true)
            .apply()
    }

    private fun readUpdateInterval(valueFromPrefs: String?): Long {
        val hours = valueFromPrefs!!.toInt()
        return TimeUnit.HOURS.toMillis(hours.toLong())
    }

    private fun readEpisodeCacheSizeInternal(valueFromPrefs: String?): Int {
        return if (valueFromPrefs == context!!.getString(R.string.pref_episode_cache_unlimited)) {
            EPISODE_CACHE_SIZE_UNLIMITED
        } else {
            valueFromPrefs!!.toInt()
        }
    }

    private fun readPlaybackSpeedArray(valueFromPrefs: String?): List<Float> {
        if (valueFromPrefs != null) {
            try {
                val jsonArray = JSONArray(valueFromPrefs)
                val selectedSpeeds: MutableList<Float> = ArrayList()
                for (i in 0 until jsonArray.length()) {
                    selectedSpeeds.add(jsonArray.getDouble(i).toFloat())
                }
                return selectedSpeeds
            } catch (e: JSONException) {
                Log.e(TAG, "Got JSON error when trying to get speeds from JSONArray")
                e.printStackTrace()
            }
        }
        // If this preference hasn't been set yet, return the default options
        return Arrays.asList(0.8f, 1.0f, 1.2f, 1.5f, 2.0f)
    }

    @JvmStatic
    fun useExoplayer(): Boolean {
        return true
    }

    @JvmStatic
    fun stereoToMono(): Boolean {
        return prefs!!.getBoolean(PREF_STEREO_TO_MONO, false)
    }

    @JvmStatic
    fun stereoToMono(enable: Boolean) {
        prefs!!.edit()
            .putBoolean(PREF_STEREO_TO_MONO, enable)
            .apply()
    }

    @JvmStatic
    fun audioLoudness(): Boolean {
        return prefs!!.getBoolean(PREF_AUDIO_LOUDNESS, false)
    }

    @JvmStatic
    fun setAudioLoudness(enable: Boolean) {
        prefs!!.edit()
            .putBoolean(PREF_AUDIO_LOUDNESS, enable)
            .apply()
    }

    @JvmStatic
    val episodeCleanupAlgorithm: EpisodeCleanupAlgorithm
        get() {
            if (!isEnableAutodownload) {
                return APNullCleanupAlgorithm()
            }
            val cleanupValue = episodeCleanupValue
            return if (cleanupValue == EPISODE_CLEANUP_EXCEPT_FAVORITE) {
                ExceptFavoriteCleanupAlgorithm()
            } else if (cleanupValue == EPISODE_CLEANUP_QUEUE) {
                APQueueCleanupAlgorithm()
            } else if (cleanupValue == EPISODE_CLEANUP_NULL) {
                APNullCleanupAlgorithm()
            } else {
                APCleanupAlgorithm(cleanupValue)
            }
        }
    @JvmStatic
    var episodeCleanupValue: Int
        get() = prefs!!.getString(PREF_EPISODE_CLEANUP, "" + EPISODE_CLEANUP_NULL)!!
            .toInt()
        set(episodeCleanupValue) {
            prefs!!.edit()
                .putString(PREF_EPISODE_CLEANUP, Integer.toString(episodeCleanupValue))
                .apply()
        }

    /**
     * 返回应用程序存储其所有数据的文件夹。这个方法将
     * 返回标准数据文件夹，如果用户没有设置的话。
     * @param type 数据文件夹内的文件夹的名称。当访问数据文件夹的根时为空
     * @return 被请求的数据文件夹，如果文件夹不能被创建，则为空。
     */
    @JvmStatic
    fun getDataFolder(type: String?): File? {
        var dataFolder = getTypeDir(prefs!!.getString(PREF_DATA_FOLDER, null), type)
        if (dataFolder == null || !dataFolder.canWrite()) {
            dataFolder = context!!.getExternalFilesDir(type)
        }
        if (dataFolder == null || !dataFolder.canWrite()) {
            dataFolder = getTypeDir(context!!.filesDir.absolutePath, type)
        }
        return dataFolder
    }

    private fun getTypeDir(baseDirPath: String?, type: String?): File? {
        if (baseDirPath == null) {
            return null
        }
        val baseDir = File(baseDirPath)
        val typeDir = if (type == null) baseDir else File(baseDir, type)
        if (!typeDir.exists()) {
            if (!baseDir.canWrite()) {
                Log.e(TAG, "Base dir is not writable " + baseDir.absolutePath)
                return null
            }
            if (!typeDir.mkdirs()) {
                Log.e(TAG, "Could not create type dir " + typeDir.absolutePath)
                return null
            }
        }
        return typeDir
    }

    @JvmStatic
    fun setDataFolder(dir: String) {
        Log.d(TAG, "set storage folder $dir")
        prefs!!.edit()
            .putString(PREF_DATA_FOLDER, dir)
            .apply()
    }

    /**
     * Create a .nomedia file to prevent scanning by the media scanner.
     */
    private fun createNoMediaFile() {
        val f = File(context!!.getExternalFilesDir(null), ".nomedia")
        if (!f.exists()) {
            try {
                f.createNewFile()
            } catch (e: IOException) {
                Log.e(TAG, "could not create .nomedia file")
                e.printStackTrace()
            }
            Log.d(TAG, ".nomedia file created")
        }
    }

    /**
     *
     * @return true if auto update is set to a specific time
     * false if auto update is set to interval
     */
    @JvmStatic
    val isAutoUpdateTimeOfDay: Boolean
        get() = updateTimeOfDay.size == 2

    @JvmStatic
    val backButtonBehavior: BackButtonBehavior
        get() = when (prefs!!.getString(PREF_BACK_BUTTON_BEHAVIOR, "default")) {
            "drawer" -> BackButtonBehavior.OPEN_DRAWER
            "doubletap" -> BackButtonBehavior.DOUBLE_TAP
            "prompt" -> BackButtonBehavior.SHOW_PROMPT
            "default" -> BackButtonBehavior.DEFAULT
            else -> BackButtonBehavior.DEFAULT
        }

    @JvmStatic
    fun timeRespectsSpeed(): Boolean {
        return prefs!!.getBoolean(PREF_TIME_RESPECTS_SPEED, false)
    }

    @JvmStatic
    var isStreamOverDownload: Boolean
        get() = true
        set(stream) {
            prefs!!.edit().putBoolean(PREF_STREAM_OVER_DOWNLOAD, stream).apply()
        }
    /**
     * Returns if the queue is in keep sorted mode.
     *
     * @see .getQueueKeepSortedOrder
     */
    /**
     * Enables/disables the keep sorted mode of the queue.
     *
     * @see .setQueueKeepSortedOrder
     */
    @JvmStatic
    var isPlaylistKeepSorted: Boolean
        get() = prefs!!.getBoolean(PREF_QUEUE_KEEP_SORTED, false)
        set(keepSorted) {
            prefs!!.edit()
                .putBoolean(PREF_QUEUE_KEEP_SORTED, keepSorted)
                .apply()
        }
    /**
     * Returns the sort order for the queue keep sorted mode.
     * Note: This value is stored independently from the keep sorted state.
     *
     * @see .isQueueKeepSorted
     */
    /**
     * Sets the sort order for the queue keep sorted mode.
     *
     * @see .setQueueKeepSorted
     */
    @JvmStatic
    var queueKeepSortedOrder: SortOrder?
        get() {
            val sortOrderStr = prefs!!.getString(PREF_QUEUE_KEEP_SORTED_ORDER, "use-default")
            return SortOrder.parseWithDefault(sortOrderStr, SortOrder.DATE_NEW_OLD)
        }
        set(sortOrder) {
            if (sortOrder == null) {
                return
            }
            prefs!!.edit()
                .putString(PREF_QUEUE_KEEP_SORTED_ORDER, sortOrder.name)
                .apply()
        }

    @JvmStatic
    var subscriptionsFilter: SubscriptionsFilter
        get() {
            val value = prefs!!.getString(PREF_FILTER_FEED, "")
            return SubscriptionsFilter(value)
        }
        set(value) {
            prefs!!.edit()
                .putString(PREF_FILTER_FEED, value.serialize())
                .apply()
        }

    @JvmStatic
    fun shouldShowSubscriptionTitle(): Boolean {
        return prefs!!.getBoolean(PREF_SUBSCRIPTION_TITLE, false)
    }

    @JvmStatic
    var onlinePodcastSearchHistory: List<String?>
        get() = parseStringList(prefs!!.getString(PREF_ONLINE_PODCAST_SEARCH_HISTORY, ""))
        set(keywords) {
            prefs!!.edit()
                .putString(PREF_ONLINE_PODCAST_SEARCH_HISTORY, toJSONString(keywords))
                .apply()
        }

    @JvmStatic
    fun clearOnlinePodcastSearchHistory() {
        prefs!!.edit()
            .putString(PREF_ONLINE_PODCAST_SEARCH_HISTORY, "")
            .apply()
    }

    @JvmStatic
    val isFullLockScreen: Boolean
        get() = prefs!!.getBoolean(PREF_FULL_LOCK_SCREEN, false)

    @JvmStatic
    val appBarMode: AppBarMode
        get() {
            val value = prefs!!.getString(APPBAR_MODE, "1")
            return if (value == "0") {
                AppBarMode.COLLAPSING
            } else if (value == "2") {
                AppBarMode.FIXED
            } else {
                AppBarMode.SIMPLE
            }
        }

    /**
     * 是否横屏时分两栏显示
     * @return
     */
    fun shouldShowColumnInLandscape(): Boolean {
        return prefs!!.getBoolean(PREF_COLUMN_IN_LANDSCAPE, true)
    }

    fun shouldSyncOnStart(): Boolean {
        return prefs!!.getBoolean(PREF_REFRESH_ON_START, true)
    }


    enum class EnqueueLocation {
        BACK, FRONT, AFTER_CURRENTLY_PLAYING
    }

    enum class BackButtonBehavior {
        DEFAULT, OPEN_DRAWER, DOUBLE_TAP, SHOW_PROMPT
    }
}