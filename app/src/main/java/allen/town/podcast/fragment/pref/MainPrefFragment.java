package allen.town.podcast.fragment.pref;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

import com.bytehamster.lib.preferencesearch.SearchConfiguration;
import com.bytehamster.lib.preferencesearch.SearchPreference;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import allen.town.focus_common.util.Constants;
import allen.town.focus_common.util.LogUtils;
import allen.town.podcast.BuildConfig;
import allen.town.podcast.MyApp;
import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.util.IntentUtils;
import allen.town.podcast.event.PurchaseEvent;

/**
 * 一级设置界面
 */
public class MainPrefFragment extends AbsSettingsFragment {

    private static final String PREF_SCREEN_USER_INTERFACE = "pref_interface";
    private static final String PREF_SCREEN_PLAYBACK = "pref_playback";
    private static final String PREF_SCREEN_NETWORK = "pref_netword";
    private static final String PREF_SCREEN_SYNCHRONIZATION = "pref_sync";
    private static final String PREF_SCREEN_STORAGE = "pref_storage";
    private static final String PREF_SEND_BUG_REPORT = "pref_report_bug";
    private static final String PREF_DONATE = "pref_donate";

    private static final String PREF_ABOUT = "pref_about";
    private static final String PREF_NOTIFICATION = "notifications";
    private static final String PREF_OTHER = "others";
    private static final String PREF_BUY = "buyPreference";
    private static final String PREF_GENERAL = "pref_general";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPurchaseChange(PurchaseEvent purchaseEvent){
        findPreference(PREF_BUY).setVisible(false);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_main);
        setupMainScreen();
        setupSearch();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.settings_label);
    }

    private void setupMainScreen() {
        findPreference(PREF_BUY).setVisible(!MyApp.getInstance().checkSupporter(null,false));

        findPreference(PREF_SCREEN_USER_INTERFACE).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_user_interface);
            return true;
        });

        findPreference(PREF_GENERAL).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_general);
            return true;
        });


        findPreference(PREF_SCREEN_PLAYBACK).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_playback);
            return true;
        });
        findPreference(PREF_SCREEN_NETWORK).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_network);
            return true;
        });
        findPreference(PREF_SCREEN_SYNCHRONIZATION).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_sync);
            return true;
        });
        findPreference(PREF_SCREEN_STORAGE).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_storage);
            return true;
        });
        findPreference(PREF_NOTIFICATION).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_notifications_under26);
            return true;
        });
        findPreference(PREF_ABOUT).setOnPreferenceClickListener(
                preference -> {
                    new AboutFragment().show(getActivity().getSupportFragmentManager(), null);
                    return true;
                }
        );
        findPreference(PREF_OTHER).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_others);
            return true;
        });

        findPreference(PREF_SEND_BUG_REPORT).setOnPreferenceClickListener(preference -> {
            LogUtils.delegateFeedback("FocusPodcast", BuildConfig.VERSION_NAME, Constants.PRODUCT_EMAIL,getActivity(),getContext().getString(R.string.provider_authority));
            return true;
        });

        if(MyApp.getInstance().isDroid()){
            Preference donatePref = findPreference(PREF_DONATE);
            donatePref.setVisible(true);
            donatePref.setOnPreferenceClickListener(preference -> {
                IntentUtils.openInBrowser(getActivity(), "https://ko-fi.com/focusapps");
                return true;
            });
        }

    }

    private void setupSearch() {
        SearchPreference searchPreference = findPreference("searchPreference");
        SearchConfiguration config = searchPreference.getSearchConfiguration();
        config.setActivity((AppCompatActivity) getActivity());
        config.setFragmentContainerViewId(R.id.settingsContainer);
        config.setBreadcrumbsEnabled(true);

        config.index(R.xml.pref_user_interface)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_user_interface));
        config.index(R.xml.pref_general)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_general));
        config.index(R.xml.pref_theme)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_user_interface))
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_theme));
        config.index(R.xml.pref_others)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_others));
        config.index(R.xml.pref_playback)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_playback));
        config.index(R.xml.pref_network)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_network));
        config.index(R.xml.pref_storage)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_storage));
        config.index(R.xml.pref_import_export)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_storage))
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_import_export));
        config.index(R.xml.pref_auto_download)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_network))
                .addBreadcrumb(R.string.automation)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_auto_download));
        config.index(R.xml.pref_sync)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_sync));
        config.index(R.xml.pref_notifications_under26)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_notifications_under26));
        config.index(R.xml.feed_settings)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.feed_settings));
        config.index(R.xml.pref_audio_effect)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.feed_settings))
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_audio_effect));
        config.index(R.xml.pref_swipe)
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_general))
                .addBreadcrumb(SettingsActivity.getTitleOfPage(R.xml.pref_swipe));
    }

    @Override
    public void invalidateSettings() {

    }
}
