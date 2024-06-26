package allen.town.podcast.fragment.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.preference.PreferenceManager;
import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.dialog.FeedRefreshPrefDialog;
import allen.town.podcast.dialog.ProxyDialog;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;


public class NetworkPrefFragment extends AbsSettingsFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PREF_SCREEN_AUTODL = "prefAutoDownloadSettings";
    private static final String PREF_PROXY = "prefProxy";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_network);
        setupNetworkScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.network_pref);
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpdateIntervalText();
        setParallelDownloadsText(Prefs.getParallelDownloads());
    }

    private void setupNetworkScreen() {
        findPreference(PREF_SCREEN_AUTODL).setOnPreferenceClickListener(preference -> {
            ((SettingsActivity) getActivity()).openScreen(R.xml.pref_auto_download);
            return true;
        });
        findPreference(Prefs.PREF_UPDATE_INTERVAL)
                .setOnPreferenceClickListener(preference -> {
                    new FeedRefreshPrefDialog(getContext()).show();
                    return true;
                });

        findPreference(Prefs.PREF_PARALLEL_DOWNLOADS)
                .setOnPreferenceChangeListener(
                        (preference, o) -> {
                            if (o instanceof Integer) {
                                setParallelDownloadsText((Integer) o);
                            }
                            return true;
                        }
                );
        // validate and set correct value: number of downloads between 1 and 50 (inclusive)
        findPreference(PREF_PROXY).setOnPreferenceClickListener(preference -> {
            ProxyDialog dialog = new ProxyDialog(getActivity());
            dialog.show();
            return true;
        });
    }

    /**
     *  Used to init and handle changes to view
      */
    private void setUpdateIntervalText() {
        Context context = getActivity().getApplicationContext();
        String val;
        long interval = Prefs.getUpdateInterval();
        if (interval > 0) {
            int hours = (int) TimeUnit.MILLISECONDS.toHours(interval);
            val = context.getResources().getQuantityString(
                    R.plurals.feed_refresh_every_x_hours, hours, hours);
        } else {
            int[] timeOfDay = Prefs.getUpdateTimeOfDay();
            if (timeOfDay.length == 2) {
                Calendar cal = new GregorianCalendar();
                cal.set(Calendar.HOUR_OF_DAY, timeOfDay[0]);
                cal.set(Calendar.MINUTE, timeOfDay[1]);
                String timeOfDayStr = DateFormat.getTimeFormat(context).format(cal.getTime());
                val = String.format(context.getString(R.string.feed_refresh_interval_at),
                        timeOfDayStr);
            } else {
                val = context.getString(R.string.feed_refresh_never);
            }
        }
        String summary = context.getString(R.string.feed_refresh_sum) + "\n"
                + String.format(context.getString(R.string.pref_current_value), val);
        findPreference(Prefs.PREF_UPDATE_INTERVAL).setSummary(summary);
    }

    private void setParallelDownloadsText(int downloads) {
        final Resources res = getActivity().getResources();
        String s = res.getString(R.string.parallel_downloads, downloads);
        findPreference(Prefs.PREF_PARALLEL_DOWNLOADS).setSummary(s);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Prefs.PREF_UPDATE_INTERVAL.equals(key)) {
            setUpdateIntervalText();
        }
    }

    @Override
    public void invalidateSettings() {

    }
}


