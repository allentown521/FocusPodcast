package allen.town.podcast.fragment.pref;

import android.content.res.Resources;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.pref.Prefs;

public class AutoDownloadPrefFragment extends AbsSettingsFragment {
    private static final String TAG = "AutoDnldPrefFragment";

    private CheckBoxPreference[] selectedNetworks;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_auto_download);

        setupAutoDownloadScreen();
        buildEpisodeCleanupPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.pref_automatic_download_title);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAutodownloadItemVisibility(Prefs.isEnableAutodownload());
    }

    private void setupAutoDownloadScreen() {
        findPreference(Prefs.PREF_ENABLE_AUTODL).setOnPreferenceChangeListener(
                (preference, newValue) -> {
                    if (newValue instanceof Boolean) {
                        checkAutodownloadItemVisibility((Boolean) newValue);
                    }
                    return true;
                });

    }

    private void checkAutodownloadItemVisibility(boolean autoDownload) {
        findPreference(Prefs.PREF_EPISODE_CACHE_SIZE).setEnabled(autoDownload);
        findPreference(Prefs.PREF_ENABLE_AUTODL_ON_BATTERY).setEnabled(autoDownload);
        findPreference(Prefs.PREF_EPISODE_CLEANUP).setEnabled(autoDownload);
    }

    private static String blankIfNull(String val) {
        return val == null ? "" : val;
    }


    private void buildEpisodeCleanupPreference() {
        final Resources res = getActivity().getResources();

        ListPreference pref = findPreference(Prefs.PREF_EPISODE_CLEANUP);
        String[] values = res.getStringArray(
                R.array.episode_cleanup_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            int v = Integer.parseInt(values[x]);
            if (v == Prefs.EPISODE_CLEANUP_EXCEPT_FAVORITE) {
                entries[x] =  res.getString(R.string.episode_cleanup_except_favorite_removal);
            } else if (v == Prefs.EPISODE_CLEANUP_QUEUE) {
                entries[x] = res.getString(R.string.episode_cleanup_queue_removal);
            } else if (v == Prefs.EPISODE_CLEANUP_NULL){
                entries[x] = res.getString(R.string.episode_cleanup_never);
            } else if (v == 0) {
                entries[x] = res.getString(R.string.episode_cleanup_after_listening);
            } else if (v > 0 && v < 24) {
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_hours_after_listening, v, v);
            } else {
                int numDays = v / 24; // assume underlying value will be NOT fraction of days, e.g., 36 (hours)
                entries[x] = res.getQuantityString(R.plurals.episode_cleanup_days_after_listening, numDays, numDays);
            }
        }
        pref.setEntries(entries);
    }

    private void setSelectedNetworksEnabled(boolean b) {
        if (selectedNetworks != null) {
            for (Preference p : selectedNetworks) {
                p.setEnabled(b);
            }
        }
    }

    @Override
    public void invalidateSettings() {

    }
}
