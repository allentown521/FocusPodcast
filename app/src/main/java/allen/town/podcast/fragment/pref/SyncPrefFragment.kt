package allen.town.podcast.fragment.pref

import allen.town.focus_common.util.TopSnackbarUtil.showSnack
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.MyApp
import allen.town.podcast.R
import allen.town.podcast.activity.DriveBackupActivity
import allen.town.podcast.activity.SettingsActivity
import allen.town.podcast.core.sync.SyncService
import allen.town.podcast.core.sync.SynchronizationCredentials
import allen.town.podcast.core.sync.SynchronizationProviderViewData
import allen.town.podcast.core.sync.SynchronizationSettings
import allen.town.podcast.dialog.AuthenticationDialog
import allen.town.podcast.event.SyncServiceEvent
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.text.HtmlCompat
import androidx.preference.Preference
import code.name.monkey.appthemehelper.ThemeStore.Companion.accentColor
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SyncPrefFragment : AbsSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_sync)
        setupScreen()
        updateScreen()
    }

    override fun onStart() {
        super.onStart()
        (activity as? SettingsActivity)?.setTitle(R.string.synchronization_pref)
        updateScreen()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
        (activity as? SettingsActivity)?.setSubtitle("")
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun syncStatusChanged(event: SyncServiceEvent) {
        if (!SynchronizationSettings.isProviderConnected) {
            return
        }
        updateScreen()
        if (event.messageResId == R.string.sync_status_error
            || event.messageResId == R.string.sync_status_success
        ) {
            updateLastSyncReport(
                SynchronizationSettings.isLastSyncSuccessful,
                SynchronizationSettings.lastSyncAttempt
            )
        } else {
            (activity as? SettingsActivity)?.setSubtitle(event.messageResId)
        }
    }

    private fun setupScreen() {
        val activity: Activity? = activity
        findPreference<Preference>(PREFERENCE_SYNC_ALL_DATA)!!.isVisible = !(MyApp.instance.isDroid || MyApp.instance.isAlipay)
        findPreference<Preference>(PREFERENCE_SYNC_ALL_DATA)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                activity!!.startActivity(Intent(activity, DriveBackupActivity::class.java))
                true
            }
        findPreference<Preference>(PREFERENCE_GPODNET_SETLOGIN_INFORMATION)
            ?.setOnPreferenceClickListener(Preference.OnPreferenceClickListener { preference: Preference? ->
                val dialog: AuthenticationDialog = object : AuthenticationDialog(
                    activity,
                    R.string.pref_gpodnet_setlogin_information_title,
                    false, SynchronizationCredentials.getUsername(), null
                ) {
                    override fun onConfirmed(username: String?, password: String?) {
                        SynchronizationCredentials.setPassword(password)
                    }
                }
                dialog.show()
                true
            })
        findPreference<Preference>(PREFERENCE_SYNC)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                SyncService.syncImmediately(context)
                true
            }
        findPreference<Preference>(PREFERENCE_FORCE_FULL_SYNC)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                SyncService.fullSync(context)
                true
            }
        findPreference<Preference>(PREFERENCE_LOGOUT)!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { preference: Preference? ->
                SynchronizationCredentials.clear(context)
                showSnack(
                    getActivity(),
                    R.string.pref_synchronization_logout_toast,
                    Toast.LENGTH_LONG
                )
                SynchronizationSettings.setSelectedSyncProvider(null)
                updateScreen()
                true
            }
    }

    private fun updateScreen() {
        val loggedIn = SynchronizationSettings.isProviderConnected
        val preferenceHeader = findPreference<Preference>(PREFERENCE_SYNCHRONIZATION_DESCRIPTION)
        if (loggedIn) {
            val selectedProvider = SynchronizationProviderViewData.fromIdentifier(
                selectedSyncProviderKey
            )
            preferenceHeader!!.title = ""
            preferenceHeader.setSummary(selectedProvider.summaryResource)
            preferenceHeader.setIcon(selectedProvider.iconResource)
            preferenceHeader.onPreferenceClickListener = null
        } else {
            preferenceHeader!!.setTitle(R.string.synchronization_choose_title)
            preferenceHeader.setSummary(R.string.synchronization_summary_unchoosen)
            val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cloud)
            drawable!!.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                accentColor(requireContext()), BlendModeCompat.SRC_IN
            )
            preferenceHeader.icon = drawable
            preferenceHeader.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    chooseProviderAndLogin()
                    true
                }
        }
        val gpodnetSetLoginPreference = findPreference<Preference>(
            PREFERENCE_GPODNET_SETLOGIN_INFORMATION
        )
        gpodnetSetLoginPreference!!.isVisible =
            isProviderSelected(SynchronizationProviderViewData.GPODDER_NET)
        gpodnetSetLoginPreference.isEnabled = loggedIn
        findPreference<Preference>(PREFERENCE_SYNC)!!.isEnabled = loggedIn
        findPreference<Preference>(PREFERENCE_FORCE_FULL_SYNC)!!.isEnabled = loggedIn
        findPreference<Preference>(PREFERENCE_LOGOUT)!!.isEnabled = loggedIn
        if (loggedIn) {
            val summary = getString(
                R.string.synchronization_login_status,
                SynchronizationCredentials.getUsername(), SynchronizationCredentials.getHosturl()
            )
            val formattedSummary = HtmlCompat.fromHtml(summary, HtmlCompat.FROM_HTML_MODE_LEGACY)
            findPreference<Preference>(PREFERENCE_LOGOUT)!!.summary = formattedSummary
            updateLastSyncReport(
                SynchronizationSettings.isLastSyncSuccessful,
                SynchronizationSettings.lastSyncAttempt
            )
        } else {
            findPreference<Preference>(PREFERENCE_LOGOUT)?.setSummary(null)
            (activity as? SettingsActivity)?.setSubtitle("")
        }
    }

    private fun chooseProviderAndLogin() {
        val builder: AlertDialog.Builder = AccentMaterialDialog(
            requireContext(),
            R.style.MaterialAlertDialogTheme
        )
        builder.setTitle(R.string.dialog_choose_sync_service_title)
        val providers = SynchronizationProviderViewData.values()
        val adapter: ListAdapter = object : ArrayAdapter<SynchronizationProviderViewData?>(
            requireContext(), R.layout.alertdialog_sync_provider_chooser, providers
        ) {
            var holder: ViewHolder? = null

            inner class ViewHolder {
                var icon: ImageView? = null
                var title: TextView? = null
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var convertView = convertView
                val inflater = LayoutInflater.from(context)
                if (convertView == null) {
                    convertView = inflater.inflate(
                        R.layout.alertdialog_sync_provider_chooser, null
                    )
                    holder = ViewHolder()
                    holder!!.icon = convertView.findViewById<View>(R.id.icon) as ImageView
                    holder!!.title = convertView.findViewById<View>(R.id.title) as TextView
                    convertView.tag = holder
                } else {
                    holder = convertView.tag as ViewHolder
                }
                val synchronizationProviderViewData = getItem(position)
                holder!!.title!!.setText(synchronizationProviderViewData!!.summaryResource)
                holder!!.icon!!.setImageResource(synchronizationProviderViewData.iconResource)
                return convertView!!
            }
        }
        builder.setAdapter(adapter) { dialog: DialogInterface?, which: Int ->
            when (providers[which]) {
                SynchronizationProviderViewData.GPODDER_NET -> GpodderAuthFragment()
                    .show(childFragmentManager, GpodderAuthFragment.TAG)
                SynchronizationProviderViewData.NEXTCLOUD_GPODDER -> NextcloudAuthFragment()
                    .show(childFragmentManager, NextcloudAuthFragment.TAG)
                else -> {}
            }
            updateScreen()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun isProviderSelected(provider: SynchronizationProviderViewData): Boolean {
        val selectedSyncProviderKey = selectedSyncProviderKey
        return provider.identifier == selectedSyncProviderKey
    }

    private val selectedSyncProviderKey: String?
        private get() = SynchronizationSettings.selectedSyncProviderKey

    private fun updateLastSyncReport(successful: Boolean, lastTime: Long) {
        val status = String.format(
            "%1\$s - %2\$s",
            getString(if (successful) R.string.gpodnetsync_pref_report_successful else R.string.gpodnetsync_pref_report_failed),
            DateUtils.getRelativeDateTimeString(
                context,
                lastTime,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_SHOW_TIME
            )
        )
        (activity as? SettingsActivity)?.setSubtitle(status)
    }

    override fun invalidateSettings() {}

    companion object {
        private const val PREFERENCE_SYNCHRONIZATION_DESCRIPTION =
            "preference_synchronization_description"
        private const val PREFERENCE_GPODNET_SETLOGIN_INFORMATION =
            "pref_gpodnet_setlogin_information"
        private const val PREFERENCE_SYNC = "pref_synchronization_sync"
        private const val PREFERENCE_FORCE_FULL_SYNC = "pref_synchronization_force_full_sync"
        private const val PREFERENCE_SYNC_ALL_DATA = "pref_synchronization_all_data_sync"
        private const val PREFERENCE_LOGOUT = "pref_synchronization_logout"
    }
}