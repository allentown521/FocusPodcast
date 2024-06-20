package allen.town.podcast.fragment

import allen.town.focus_common.common.prefs.supportv7.ATESwitchPreference
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.MyApp.Companion.instance
import allen.town.podcast.R
import allen.town.podcast.core.pref.Prefs.isEnableAutodownload
import allen.town.podcast.core.storage.DBReader
import allen.town.podcast.core.storage.DBTasks
import allen.town.podcast.core.storage.DBWriter
import allen.town.podcast.databinding.PlaybackSpeedFeedSettingDialogBinding
import allen.town.podcast.dialog.AuthenticationDialog
import allen.town.podcast.dialog.EpisodeFilterDialog
import allen.town.podcast.dialog.FeedSkipPreDialog
import allen.town.podcast.dialog.TagEditDialog
import allen.town.podcast.event.playback.TitleChangeEvent
import allen.town.podcast.event.settings.SkipIntroEndingChangedEvent
import allen.town.podcast.event.settings.SpeedPresetChangedEvent
import allen.town.podcast.event.settings.VolumeAdaptionChangedEvent
import allen.town.podcast.fragment.pref.AbsSettingsFragment
import allen.town.podcast.fragment.pref.AudioEffectFragment
import allen.town.podcast.model.feed.*
import allen.town.podcast.model.feed.FeedPreferences.AutoDeleteAction
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.MaybeOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import java.util.concurrent.ExecutionException

class FeedSettingsFragment : Fragment() {
    private var disposable: Disposable? = null
    fun setTitle(@StringRes titleId: Int) {
        collapsingToolbarLayout!!.title = getString(titleId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun titleChange(titleChangeEvent: TitleChangeEvent) {
        setTitle(titleChangeEvent.title)
    }

    private var collapsingToolbarLayout: CollapsingToolbarLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.feedsettings, container, false)
        val feedId = requireArguments().getLong(EXTRA_FEED_ID)
        val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
        collapsingToolbarLayout = root.findViewById(R.id.collapsingToolbarLayout)
        toolbar.setNavigationOnClickListener { v: View? -> parentFragmentManager.popBackStack() }
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.settings_fragment_container,
                FeedSettingsPreferenceFragment.newInstance(feedId), "settings_fragment"
            )
            .commitAllowingStateLoss()
        disposable = Maybe.create(MaybeOnSubscribe { emitter: MaybeEmitter<Feed> ->
            val feed = DBReader.getFeed(feedId)
            if (feed != null) {
                emitter.onSuccess(feed)
            } else {
                emitter.onComplete()
            }
        } as MaybeOnSubscribe<Feed>)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: Feed -> toolbar.subtitle = result.title },
                { error: Throwable? -> Log.d(TAG, Log.getStackTraceString(error)) }
            ) {}
        return root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (disposable != null) {
            disposable!!.dispose()
        }
        EventBus.getDefault().unregister(this)
    }

    class FeedSettingsPreferenceFragment : AbsSettingsFragment() {
        private var feed: Feed? = null
        private var disposable: Disposable? = null
        private var feedPreferences: FeedPreferences? = null
        override fun onResume() {
            super.onResume()
            EventBus.getDefault().post(TitleChangeEvent(R.string.feed_settings_label))
        }

        override fun onCreateRecyclerView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            state: Bundle?
        ): RecyclerView {
            val view = super.onCreateRecyclerView(inflater, parent, state)
            // To prevent transition animation because of summary update
            view.itemAnimator = null
            view.layoutAnimation = null
            return view
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.feed_settings)
            // To prevent displaying partially loaded data
            findPreference<Preference>(PREF_SCREEN)!!.isVisible = false
            val feedId = requireArguments().getLong(EXTRA_FEED_ID)
            disposable = Maybe.create(
                MaybeOnSubscribe { emitter: MaybeEmitter<Feed?> ->
                    val feed = DBReader.getFeed(feedId)
                    if (feed != null) {
                        emitter.onSuccess(feed)
                    } else {
                        emitter.onComplete()
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result: Feed? ->
                    feed = result
                    feedPreferences = feed!!.preferences
                    setupAutoDownloadGlobalPreference()
                    setupAutoDownloadPreference()
                    setupKeepUpdatedPreference()
                    setupAutoDeletePreference()
                    setupVolumeReductionPreferences()
                    setupAuthentificationPreference()
                    setupEpisodeFilterPreference()
                    setupPlaybackSpeedPreference()
                    setupFeedAutoSkipPreference()
                    setupEpisodeNotificationPreference()
                    setupTags()
                    updateAutoDeleteSummary()
                    updateVolumeReductionValue()
                    updateAutoDownloadEnabled()
                    setupAudioEffectPreference(feedPreferences, feed)
                    if (feed!!.isLocalFeed) {
                        findPreference<Preference>(PREF_AUTHENTICATION)!!.isVisible = false
                        findPreference<Preference>(PREF_AUTO_DELETE)!!.isVisible = false
                        findPreference<Preference>(PREF_CATEGORY_AUTO_DOWNLOAD)!!.isVisible = false
                    }
                    findPreference<Preference>(PREF_SCREEN)!!.isVisible = true
                }, { error: Throwable? -> Log.d(TAG, Log.getStackTraceString(error)) }) {}
        }

        override fun onDestroy() {
            super.onDestroy()
            if (disposable != null) {
                disposable!!.dispose()
            }
        }

        private fun setupFeedAutoSkipPreference() {
            findPreference<Preference>(PREF_AUTO_SKIP)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    object : FeedSkipPreDialog(
                        context,
                        feedPreferences!!.feedSkipIntro,
                        feedPreferences!!.feedSkipEnding
                    ) {
                        override fun onConfirmed(skipIntro: Int, skipEnding: Int) {
                            if (!instance.checkSupporter(context, true)) {
                                return
                            }
                            feedPreferences!!.feedSkipIntro = skipIntro
                            feedPreferences!!.feedSkipEnding = skipEnding
                            DBWriter.setFeedPreferences(feedPreferences)
                            EventBus.getDefault().post(
                                SkipIntroEndingChangedEvent(
                                    feedPreferences!!.feedSkipIntro,
                                    feedPreferences!!.feedSkipEnding,
                                    feed!!.id
                                )
                            )
                        }
                    }.show()
                    false
                }
        }

        private fun setupPlaybackSpeedPreference() {
            val feedPlaybackSpeedPreference = findPreference<Preference>(PREF_FEED_PLAYBACK_SPEED)
            feedPlaybackSpeedPreference!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    val viewBinding = PlaybackSpeedFeedSettingDialogBinding.inflate(
                        layoutInflater
                    )
                    viewBinding.seekBar.setProgressChangedListener { speed: Float? ->
                        viewBinding.currentSpeedLabel.text = String.format(
                            Locale.getDefault(), "%.1fx", speed
                        )
                    }
                    val speed = feedPreferences!!.feedPlaybackSpeed
                    viewBinding.useGlobalCheckbox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                        viewBinding.seekBar.isEnabled = !isChecked
                        viewBinding.seekBar.alpha = if (isChecked) 0.4f else 1f
                        viewBinding.currentSpeedLabel.alpha = if (isChecked) 0.4f else 1f
                    }
                    viewBinding.useGlobalCheckbox.isChecked =
                        speed == FeedPreferences.SPEED_USE_GLOBAL
                    viewBinding.seekBar.updateSpeed(if (speed == FeedPreferences.SPEED_USE_GLOBAL) 1f else speed)
                    AccentMaterialDialog(
                        requireContext(),
                        R.style.MaterialAlertDialogTheme
                    )
                        .setTitle(R.string.playback_speed)
                        .setView(viewBinding.root)
                        .setPositiveButton(android.R.string.ok) { dialog: DialogInterface?, which: Int ->
                            if (!viewBinding.useGlobalCheckbox.isChecked && !instance.checkSupporter(
                                    requireContext(), true
                                )
                            ) {
                                return@setPositiveButton
                            }
                            val newSpeed =
                                if (viewBinding.useGlobalCheckbox.isChecked) FeedPreferences.SPEED_USE_GLOBAL else viewBinding.seekBar.currentSpeed
                            feedPreferences!!.feedPlaybackSpeed = newSpeed
                            DBWriter.setFeedPreferences(feedPreferences)
                            EventBus.getDefault().post(
                                SpeedPresetChangedEvent(
                                    feedPreferences!!.feedPlaybackSpeed,
                                    feed!!.id
                                )
                            )
                        }
                        .setNegativeButton(R.string.cancel_label, null)
                        .show()
                    true
                }
        }

        private fun setupEpisodeFilterPreference() {
            findPreference<Preference>(PREF_EPISODE_FILTER)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    object : EpisodeFilterDialog(context, feedPreferences!!.filter) {
                        override fun onConfirmed(filter: FeedFilter?) {
                            feedPreferences!!.filter = filter!!
                            DBWriter.setFeedPreferences(feedPreferences)
                        }
                    }.show()
                    false
                }
        }

        private fun setupAuthentificationPreference() {
            findPreference<Preference>(PREF_AUTHENTICATION)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    object : AuthenticationDialog(
                        context,
                        R.string.authentication_label, true,
                        feedPreferences!!.username, feedPreferences!!.password
                    ) {
                        override fun onConfirmed(username: String?, password: String?) {
                            feedPreferences!!.username = username
                            feedPreferences!!.password = password
                            val setPreferencesFuture = DBWriter.setFeedPreferences(feedPreferences)

                            Thread({
                                try {
                                    setPreferencesFuture.get()
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                } catch (e: ExecutionException) {
                                    e.printStackTrace()
                                }
                                DBTasks.forceRefreshFeed(requireContext(), feed, true)
                            }, "RefreshAfterCredentialChange").start()
                        }
                    }.show()
                    false
                }
        }

        private fun setupAutoDeletePreference() {
            findPreference<Preference>(PREF_AUTO_DELETE)!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    when (newValue as String?) {
                        "global" -> feedPreferences!!.autoDeleteAction = AutoDeleteAction.GLOBAL
                        "always" -> feedPreferences!!.autoDeleteAction = AutoDeleteAction.YES
                        "never" -> feedPreferences!!.autoDeleteAction = AutoDeleteAction.NO
                    }
                    DBWriter.setFeedPreferences(feedPreferences)
                    updateAutoDeleteSummary()
                    false
                }
        }

        private fun updateAutoDeleteSummary() {
            val autoDeletePreference = findPreference<ListPreference>(PREF_AUTO_DELETE)
            when (feedPreferences!!.autoDeleteAction) {
                AutoDeleteAction.GLOBAL -> {
                    autoDeletePreference!!.setSummary(R.string.feed_auto_download_global)
                    autoDeletePreference.value = "global"
                }
                AutoDeleteAction.YES -> {
                    autoDeletePreference!!.setSummary(R.string.feed_auto_download_always)
                    autoDeletePreference.value = "always"
                }
                AutoDeleteAction.NO -> {
                    autoDeletePreference!!.setSummary(R.string.feed_auto_download_never)
                    autoDeletePreference.value = "never"
                }
            }
        }

        private fun setupVolumeReductionPreferences() {
            val volumeReductionPreference = findPreference<ListPreference>("volumeReduction")
            volumeReductionPreference!!.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    when (newValue as String?) {
                        "off" -> feedPreferences!!.volumeAdaptionSetting = VolumeAdaptionSetting.OFF
                        "light" -> feedPreferences!!.volumeAdaptionSetting =
                            VolumeAdaptionSetting.LIGHT_REDUCTION
                        "heavy" -> feedPreferences!!.volumeAdaptionSetting =
                            VolumeAdaptionSetting.HEAVY_REDUCTION
                    }
                    DBWriter.setFeedPreferences(feedPreferences)
                    updateVolumeReductionValue()
                    EventBus.getDefault().post(
                        VolumeAdaptionChangedEvent(
                            feedPreferences!!.volumeAdaptionSetting,
                            feed!!.id
                        )
                    )
                    false
                }
        }

        private fun updateVolumeReductionValue() {
            val volumeReductionPreference = findPreference<ListPreference>("volumeReduction")
            when (feedPreferences!!.volumeAdaptionSetting) {
                VolumeAdaptionSetting.OFF -> volumeReductionPreference!!.value = "off"
                VolumeAdaptionSetting.LIGHT_REDUCTION -> volumeReductionPreference!!.value = "light"
                VolumeAdaptionSetting.HEAVY_REDUCTION -> volumeReductionPreference!!.value = "heavy"
            }
        }

        private fun setupKeepUpdatedPreference() {
            val pref = findPreference<ATESwitchPreference>("keepUpdated")
            pref!!.isChecked = feedPreferences!!.keepUpdated
            pref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val checked = newValue === java.lang.Boolean.TRUE
                    feedPreferences!!.keepUpdated = checked
                    DBWriter.setFeedPreferences(feedPreferences)
                    pref.isChecked = checked
                    false
                }
        }

        private fun setupAudioEffectPreference(feedPreferences: FeedPreferences?, feed: Feed?) {
            findPreference<Preference>(PREF_AUDIO_EFFECT)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(
                            R.anim.retro_fragment_open_enter,
                            R.anim.retro_fragment_open_exit,
                            R.anim.retro_fragment_close_enter,
                            R.anim.retro_fragment_close_exit
                        )
                        .replace(
                            R.id.settings_fragment_container,
                            AudioEffectFragment(feedPreferences, feed)
                        )
                        .addToBackStack(getString(R.string.audio_effects)).commit()
                    true
                }
        }

        private fun setupAutoDownloadGlobalPreference() {
            if (!isEnableAutodownload) {
                val autodl = findPreference<ATESwitchPreference>("autoDownload")
                autodl!!.isChecked = false
                autodl.isEnabled = false
                autodl.setSummary(R.string.auto_download_disabled_globally)
                findPreference<Preference>(PREF_EPISODE_FILTER)!!.isEnabled = false
            }
        }

        private fun setupAutoDownloadPreference() {
            val pref = findPreference<ATESwitchPreference>("autoDownload")
            pref!!.isEnabled = isEnableAutodownload
            if (isEnableAutodownload) {
                pref.isChecked = feedPreferences!!.autoDownload
            } else {
                pref.isChecked = false
                pref.setSummary(R.string.auto_download_disabled_globally)
            }
            pref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val checked = newValue === java.lang.Boolean.TRUE
                    feedPreferences!!.autoDownload = checked
                    DBWriter.setFeedPreferences(feedPreferences)
                    updateAutoDownloadEnabled()
                    pref.isChecked = checked
                    false
                }
        }

        private fun updateAutoDownloadEnabled() {
            if (feed != null && feed!!.preferences != null) {
                val enabled = feed!!.preferences.autoDownload && isEnableAutodownload
                findPreference<Preference>(PREF_EPISODE_FILTER)!!.isEnabled =
                    enabled
            }
        }

        private fun setupTags() {
            findPreference<Preference>(PREF_TAGS)!!.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference ->
                    TagEditDialog.newInstance(listOf(feedPreferences!!))
                        .show(childFragmentManager, TagEditDialog.TAG)
                    true
                }
        }

        private fun setupEpisodeNotificationPreference() {
            val pref = findPreference<ATESwitchPreference>("episodeNotification")
            pref!!.isChecked = feedPreferences!!.showEpisodeNotification
            pref.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    val checked = newValue === java.lang.Boolean.TRUE
                    feedPreferences!!.showEpisodeNotification = checked
                    DBWriter.setFeedPreferences(feedPreferences)
                    pref.isChecked = checked
                    false
                }
        }

        override fun invalidateSettings() {}

        companion object {
            private val PREF_EPISODE_FILTER: CharSequence = "episodeFilter"
            private val PREF_SCREEN: CharSequence = "feedSettingsScreen"
            private val PREF_AUTHENTICATION: CharSequence = "authentication"
            private val PREF_AUTO_DELETE: CharSequence = "autoDelete"
            private val PREF_CATEGORY_AUTO_DOWNLOAD: CharSequence = "autoDownloadCategory"
            private const val PREF_FEED_PLAYBACK_SPEED = "feedPlaybackSpeed"
            private const val PREF_AUTO_SKIP = "feedAutoSkip"
            private const val PREF_AUDIO_EFFECT = "feed_audio_effect_pref"
            private const val PREF_TAGS = "tags"
            fun newInstance(feedId: Long): FeedSettingsPreferenceFragment {
                val fragment = FeedSettingsPreferenceFragment()
                val arguments = Bundle()
                arguments.putLong(EXTRA_FEED_ID, feedId)
                fragment.arguments = arguments
                return fragment
            }
        }
    }

    companion object {
        private const val TAG = "FeedSettingsFragment"
        private const val EXTRA_FEED_ID = "allen.town.podcast.extra.feedId"
        fun newInstance(feed: Feed): FeedSettingsFragment {
            val fragment = FeedSettingsFragment()
            val arguments = Bundle()
            arguments.putLong(EXTRA_FEED_ID, feed.id)
            fragment.arguments = arguments
            return fragment
        }
    }
}