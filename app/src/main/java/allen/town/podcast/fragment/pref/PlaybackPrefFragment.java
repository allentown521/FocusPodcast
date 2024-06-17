package allen.town.podcast.fragment.pref;

import static allen.town.podcast.core.pref.Prefs.PREF_FULL_LOCK_SCREEN;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import code.name.monkey.appthemehelper.util.VersionUtils;
import allen.town.podcast.BuildConfig;
import allen.town.podcast.MyApp;
import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.playback.NowPlayingScreen;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.dialog.SkipPrefDialog;
import allen.town.podcast.dialog.PlaySpeedDialog;

import java.util.ArrayList;
import java.util.Map;

public class PlaybackPrefFragment extends AbsSettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PREF_PLAYBACK_SPEED_LAUNCHER = "prefPlaybackSpeedLauncher";
    private static final String PREF_PLAYBACK_REWIND_DELTA_LAUNCHER = "prefPlaybackRewindDeltaLauncher";
    private static final String PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER = "prefPlaybackFastForwardDeltaLauncher";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_playback);

        setupPlaybackScreen();
        buildSmartMarkAsPlayedPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.playback_pref);
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ((SettingsActivity) getActivity()).setTitle(R.string.playback_pref);
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setupPlaybackScreen() {
        final Activity activity = getActivity();

        findPreference(PREF_PLAYBACK_SPEED_LAUNCHER).setOnPreferenceClickListener(preference -> {
            new PlaySpeedDialog().show(getChildFragmentManager(), null);
            return true;
        });
        findPreference(PREF_PLAYBACK_REWIND_DELTA_LAUNCHER).setOnPreferenceClickListener(preference -> {
            SkipPrefDialog.showSkipPreference(activity, SkipPrefDialog.SkipDirection.SKIP_REWIND, null);
            return true;
        });
        findPreference(PREF_PLAYBACK_FAST_FORWARD_DELTA_LAUNCHER).setOnPreferenceClickListener(preference -> {
            SkipPrefDialog.showSkipPreference(activity, SkipPrefDialog.SkipDirection.SKIP_FORWARD, null);
            return true;
        });


        //android 11以及以后不支持锁屏背景了
        findPreference(Prefs.PREF_LOCKSCREEN_BACKGROUND)
                .setVisible(BuildConfig.DEBUG || !VersionUtils.hasR());

        updateAdapterColor();
        buildEnqueueLocationPreference();
    }

    void updateAdapterColor(){
        ArrayList nowPlayingScreenList = new ArrayList<NowPlayingScreen>() {{
            add(NowPlayingScreen.Normal);
            add(NowPlayingScreen.Adaptive);
            add(NowPlayingScreen.Circle);
        }};
        findPreference(Prefs.PREF_ADAPTIVE_COLOR_APP).setEnabled(nowPlayingScreenList.contains(Prefs.getNowPlayingScreen()));
    }

    private void buildEnqueueLocationPreference() {
        final Resources res = requireActivity().getResources();
        final Map<String, String> options = new ArrayMap<>();
        {
            String[] keys = res.getStringArray(R.array.enqueue_location_values);
            String[] values = res.getStringArray(R.array.enqueue_location_options);
            for (int i = 0; i < keys.length; i++) {
                options.put(keys[i], values[i]);
            }
        }

        ListPreference pref = requirePreference(Prefs.PREF_ENQUEUE_LOCATION);
        pref.setSummary(res.getString(R.string.pref_enqueue_location_sum, options.get(pref.getValue())));

        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(newValue instanceof String)) {
                return false;
            }
            String newValStr = (String)newValue;
            pref.setSummary(res.getString(R.string.pref_enqueue_location_sum, options.get(newValStr)));
            return true;
        });
    }

    @NonNull
    private <T extends Preference> T requirePreference(@NonNull CharSequence key) {
        // Possibly put it to a common method in abstract base class
        T result = findPreference(key);
        if (result == null) {
            throw new IllegalArgumentException("Preference with key '" + key + "' is not found");

        }
        return result;
    }

    private void buildSmartMarkAsPlayedPreference() {
        final Resources res = getActivity().getResources();

        ListPreference pref = findPreference(Prefs.PREF_SMART_MARK_AS_PLAYED_SECS);
        String[] values = res.getStringArray(R.array.smart_mark_as_played_values);
        String[] entries = new String[values.length];
        for (int x = 0; x < values.length; x++) {
            if(x == 0) {
                entries[x] = res.getString(R.string.pref_smart_mark_as_played_disabled);
            } else {
                int v = Integer.parseInt(values[x]);
                if(v < 60) {
                    entries[x] = res.getQuantityString(R.plurals.time_seconds_quantified, v, v);
                } else {
                    v /= 60;
                    entries[x] = res.getQuantityString(R.plurals.time_minutes_quantified, v, v);
                }
            }
        }
        pref.setEntries(entries);
    }

    @Override
    public void invalidateSettings() {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(Prefs.NOW_PLAYING_SCREEN_ID.equals(key)){
            updateAdapterColor();
        }
    }
}
