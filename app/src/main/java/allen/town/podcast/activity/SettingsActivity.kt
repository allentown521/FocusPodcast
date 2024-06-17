package allen.town.podcast.activity

import allen.town.focus_common.ad.BannerAdManager
import allen.town.focus_common.views.AccentMaterialDialog
import allen.town.podcast.MyApp
import allen.town.podcast.R
import allen.town.podcast.databinding.SettingsActivityBinding
import allen.town.podcast.fragment.pref.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentContainerView
import androidx.preference.PreferenceFragmentCompat
import allen.town.focus_common.extensions.applyToolbar
import code.name.monkey.retromusic.fragments.settings.ThemeSettingsFragment
import com.bytehamster.lib.preferencesearch.SearchPreferenceResult
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener

/**
 * PreferenceActivity for API 11+. In order to change the behavior of the preference UI, see
 * PreferenceController.
 */
open class SettingsActivity : SimpleToolbarActivity(), SearchPreferenceResultListener {
    var binding: SettingsActivityBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        applyToolbar(binding!!.toolbar)
        binding!!.toolbar.setNavigationOnClickListener { v -> onOptionsItemSelected() }
        if (supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer,
                    MainPrefFragment(), FRAGMENT_TAG)
                .commit()
        }
        val intent = intent
        if (intent.getBooleanExtra(OPEN_AUTO_DOWNLOAD_SETTINGS, false)) {
            openScreen(R.xml.pref_auto_download)
        }
        setPaddingBottom()
    }

    /**
     * 为底部横幅广告预留位置
     */
    private fun setPaddingBottom(){
        val mainView = findViewById<FragmentContainerView>(R.id.settingsContainer)
        val params = mainView.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            0,
            0,
            0,
            if (!MyApp.instance.isAdBlockUser()) resources.getDimension(R.dimen.external_player_height).toInt() else 0
        )
        mainView.layoutParams = params

        BannerAdManager.showBannerAd(this,findViewById(R.id.bottom_adView))
    }

    override fun setTitle(titleId: Int) {
        binding!!.collapsingToolbarLayout.title = getString(titleId)
    }

    open fun setSubtitle(titleId: Int) {
        binding!!.subTitle.text = getString(titleId)
    }

    open fun setSubtitle(title: String) {
        binding!!.subTitle.text = title
    }

    private fun getPreferenceScreen(screen: Int): PreferenceFragmentCompat? {
        var prefFragment: PreferenceFragmentCompat? = null
        if (screen == R.xml.pref_user_interface) {
            prefFragment =
                UserInterfacePrefFragment()
        } else if (screen == R.xml.pref_network) {
            prefFragment = NetworkPrefFragment()
        } else if (screen == R.xml.pref_storage) {
            prefFragment = StoragePrefFragment()
        } else if (screen == R.xml.pref_import_export) {
            prefFragment = ImportExportPreferencesFragment()
        } else if (screen == R.xml.pref_auto_download) {
            prefFragment =
                AutoDownloadPrefFragment()
        } else if (screen == R.xml.pref_sync) {
            prefFragment =
                SyncPrefFragment()
        } else if (screen == R.xml.pref_playback) {
            prefFragment =
                PlaybackPrefFragment()
        } else if (screen == R.xml.pref_notifications_under26) {
            prefFragment =
                NotificationPrefFragment()
        } else if (screen == R.xml.pref_swipe) {
            prefFragment = SwipePrefFragment()
        } else if (screen == R.xml.pref_theme) {
            prefFragment = ThemeSettingsFragment()
        } else if (screen == R.xml.pref_others) {
            prefFragment = OthersFragment()
        } else if (screen == R.xml.pref_general) {
            prefFragment = GeneralPrefFragment()
        }

        return prefFragment
    }

/*    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }*/

    fun openScreen(screen: Int): PreferenceFragmentCompat? {
        val fragment = getPreferenceScreen(screen)
        if (screen == R.xml.pref_notifications_under26 && Build.VERSION.SDK_INT >= 26) {
            val intent = Intent()
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } else {
            supportFragmentManager.beginTransaction().setCustomAnimations(
                    R.anim.retro_fragment_open_enter,
            R.anim.retro_fragment_open_exit,
            R.anim.retro_fragment_close_enter,
            R.anim.retro_fragment_close_exit).replace(R.id.settingsContainer, fragment!!)
                .addToBackStack(getString(getTitleOfPage(screen))).commit()
        }
        return fragment
    }

    fun onOptionsItemSelected(): Boolean {
        if (supportFragmentManager.backStackEntryCount == 0) {
            finish()
        } else {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            var view = currentFocus
            //If no view currently has focus, create a new one, just so we can grab a window token from it
            if (view == null) {
                view = View(this)
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            supportFragmentManager.popBackStack()
        }
        return true
    }

    override fun onSearchResultClicked(result: SearchPreferenceResult) {
        val screen = result.resourceFile
        if (screen == R.xml.feed_settings || screen == R.xml.pref_audio_effect) {
            val builder = AccentMaterialDialog(
                this,
                R.style.MaterialAlertDialogTheme
            )
            builder.setTitle(R.string.feed_settings_label)
            builder.setMessage(R.string.pref_feed_settings_dialog_msg)
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
        } else if (screen == R.xml.pref_notifications_under26) {
            openScreen(screen)
        } else {
            val fragment = openScreen(result.resourceFile)
            result.highlight(fragment)
        }
    }

    companion object {
        private const val FRAGMENT_TAG = "tag_preferences"
        const val OPEN_AUTO_DOWNLOAD_SETTINGS = "OpenAutoDownloadSettings"

        @JvmStatic
        fun getTitleOfPage(preferences: Int): Int {
            if (preferences == R.xml.pref_network) {
                return R.string.network_pref
            } else if (preferences == R.xml.pref_auto_download) {
                return R.string.pref_automatic_download_title
            } else if (preferences == R.xml.pref_playback) {
                return R.string.playback_pref
            } else if (preferences == R.xml.pref_storage) {
                return R.string.storage_pref
            } else if (preferences == R.xml.pref_import_export) {
                return R.string.import_export_pref
            } else if (preferences == R.xml.pref_user_interface) {
                return R.string.user_interface_label
            } else if (preferences == R.xml.pref_sync) {
                return R.string.synchronization_pref
            } else if (preferences == R.xml.pref_notifications_under26) {
                return R.string.notification_pref_fragment
            } else if (preferences == R.xml.feed_settings) {
                return R.string.feed_settings_label
            } else if (preferences == R.xml.pref_swipe) {
                return R.string.swipeactions_label
            } else if (preferences == R.xml.pref_others) {
                return R.string.others
            } else if (preferences == R.xml.pref_theme) {
                return R.string.pref_set_theme_title
            } else if (preferences == R.xml.pref_audio_effect) {
                return R.string.audio_effects
            } else if (preferences == R.xml.pref_general) {
                return R.string.pref_general_title
            }
            return R.string.settings_label
        }
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.anim_activity_stay, R.anim.retro_fragment_close_exit)
    }
}