package allen.town.podcast.fragment.pref;

import static allen.town.podcast.core.pref.Prefs.PREF_DISABLE_FIREBASE;

import android.os.Bundle;

import java.util.Locale;

import allen.town.focus_common.ui.customtabs.BrowserLauncher;
import allen.town.focus_common.util.Timber;
import allen.town.podcast.MyApp;
import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;

public class OthersFragment extends AbsSettingsFragment {
    private static final String PREF_DOCUMENTATION = "prefDocumentation";
    private static final String PREF_VIEW_FORUM = "prefViewForum";
    private static final String PREF_CHANGE_LOG = "pref_changelog";


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_others);
        String lan = Locale.getDefault().getLanguage();
        boolean isChina = "zh".equals(lan);
        boolean isTChinese = isChina && !Locale.getDefault().getCountry().equalsIgnoreCase("cn");
        String documentUrlBase = "https://y3ep4q3r4d.k.topthink.com/@podcast-";
        if (isChina) {
            if (isTChinese) {
                documentUrlBase += "zh-tw";
            } else {
                documentUrlBase += "zh-cn";
            }
        } else {
            documentUrlBase += "en";
        }

        final String finalDocumentUrl = documentUrlBase;
        findPreference(PREF_DISABLE_FIREBASE).setVisible(!MyApp.getInstance().isDroid());
        findPreference(PREF_DOCUMENTATION).setOnPreferenceClickListener(preference -> {
            BrowserLauncher.openUrl(getContext(), finalDocumentUrl);
            //https://portals.docsie.io/allentown/focuspodcast-doc-help/focuspodcast-doc-help-docs/deployment_gJNsFb3AxfYNQuSF7/?doc=/getting-started/;这个可以直接翻译，缺点是只能有一个
            return true;
        });
        findPreference(PREF_VIEW_FORUM).setOnPreferenceClickListener(preference -> {
            BrowserLauncher.openUrl(getActivity(), isChina ?
                    "https://support.qq.com/product/414482" : "https://focuspodcast.canny.io/");
            return true;
        });
        findPreference(PREF_CHANGE_LOG).setOnPreferenceClickListener(preference -> {
            BrowserLauncher.openUrl(getContext(), "https://focuspodcast.canny.io/changelog");
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.others);
    }

    @Override
    public void invalidateSettings() {

    }
}
