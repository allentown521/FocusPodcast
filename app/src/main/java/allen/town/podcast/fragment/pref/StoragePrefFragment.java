package allen.town.podcast.fragment.pref;

import android.os.Bundle;

import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.dialog.ChooseStorageFolderDialog;

import java.io.File;

public class StoragePrefFragment extends AbsSettingsFragment {
    private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";
    private static final String PREF_IMPORT_EXPORT = "prefImportExport";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_storage);
        setupStorageScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.storage_pref);
    }

    @Override
    public void onResume() {
        super.onResume();
        setDataFolderText();
    }

    private void setupStorageScreen() {
        findPreference(PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
                preference -> {
                    ChooseStorageFolderDialog.showDialog(getContext(), path -> {
                        Prefs.setDataFolder(path);
                        setDataFolderText();
                    });
                    return true;
                }
        );
        findPreference(PREF_IMPORT_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    ((SettingsActivity) getActivity()).openScreen(R.xml.pref_import_export);
                    return true;
                }
        );
    }

    private void setDataFolderText() {
        File f = Prefs.getDataFolder(null);
        if (f != null) {
            findPreference(PREF_CHOOSE_DATA_DIR).setSummary(f.getAbsolutePath());
        }
    }

    @Override
    public void invalidateSettings() {

    }
}
