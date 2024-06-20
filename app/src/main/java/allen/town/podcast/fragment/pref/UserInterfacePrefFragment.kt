package allen.town.podcast.fragment.pref

import allen.town.focus_common.extensions.installLanguageAndRecreate
import allen.town.podcast.MyApp.Companion.instance
import allen.town.podcast.R
import allen.town.podcast.activity.SettingsActivity
import allen.town.podcast.core.pref.Prefs
import allen.town.podcast.core.pref.Prefs.PREF_HOME_PAGE
import allen.town.podcast.core.pref.Prefs.setShowRemainTimeSetting
import allen.town.podcast.dialog.FeedsSortDialog.Companion.newInstance
import allen.town.podcast.dialog.SubsFilterDialog.showDialog
import allen.town.podcast.event.PlayerStatusEvent
import allen.town.podcast.event.UnreadItemsUpdateEvent
import android.os.Bundle
import androidx.preference.Preference
import code.name.monkey.appthemehelper.constants.ThemeConstants.LANGUAGE_NAME
import org.greenrobot.eventbus.EventBus

class UserInterfacePrefFragment : AbsSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_user_interface)
        setupInterfaceScreen()
    }

    override fun onStart() {
        super.onStart()
        (activity as SettingsActivity?)!!.setTitle(R.string.user_interface_label)
    }

    private fun setupInterfaceScreen() {

        //播放界面显示剩余时间还是时长
        findPreference<Preference>(Prefs.PREF_SHOW_TIME_LEFT)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                setShowRemainTimeSetting(newValue as Boolean?)
                EventBus.getDefault().post(UnreadItemsUpdateEvent())
                EventBus.getDefault().post(PlayerStatusEvent())
                true
            }
        findPreference<Preference>(Prefs.PREF_FILTER_FEED)?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                showDialog(requireContext())
                true
            }
        findPreference<Preference>(Prefs.PREF_DRAWER_FEED_ORDER)?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                newInstance().show(
                    childFragmentManager, null
                )
                true
            }
        findPreference<Preference>(PREF_THEME_INTERFACE)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                (activity as SettingsActivity?)!!.openScreen(R.xml.pref_theme)
                true
            }
        findPreference<Preference>(LANGUAGE_NAME)!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                setSummary(preference, newValue.toString())
                requireActivity().installLanguageAndRecreate(newValue.toString())
                true
            }
        findPreference<Preference>(PREF_HOME_PAGE)!!.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (!instance.checkSupporter(context, true)) {
                    return@OnPreferenceChangeListener false
                }
                restartActivity()
                true
            }
    }

    override fun invalidateSettings() {}

    companion object {
        private const val PREF_THEME_INTERFACE = "pref_theme"
    }
}