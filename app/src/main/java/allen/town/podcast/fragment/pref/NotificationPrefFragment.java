package allen.town.podcast.fragment.pref;

import static allen.town.podcast.core.pref.Prefs.PREF_GPODNET_NOTIFICATIONS;

import android.os.Bundle;

import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.sync.SynchronizationSettings;

public class NotificationPrefFragment extends AbsSettingsFragment {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_notifications_under26);
        setUpScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.notification_pref_fragment);
    }

    private void setUpScreen() {
        findPreference(PREF_GPODNET_NOTIFICATIONS).setEnabled(SynchronizationSettings.isProviderConnected());
    }

    @Override
    public void invalidateSettings() {

    }
}
