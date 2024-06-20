package allen.town.podcast.fragment.pref;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.focus_common.views.AccentMaterialDialog;
import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.core.pref.Prefs;

public class GeneralPrefFragment extends AbsSettingsFragment {
    private static final String PREF_SWIPE = "pref_swipe";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_general);
        setupInterfaceScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.pref_general_title);
    }

    private void setupInterfaceScreen() {

        findPreference(Prefs.PREF_COMPACT_NOTIFICATION_BUTTONS)
                .setOnPreferenceClickListener(preference -> {
                    showNotificationButtonsDialog();
                    return true;
                });

        findPreference(PREF_SWIPE)
                .setOnPreferenceClickListener(preference -> {
                    ((SettingsActivity) getActivity()).openScreen(R.xml.pref_swipe);
                    return true;
                });


        if (Build.VERSION.SDK_INT >= 26) {
            findPreference(Prefs.PREF_EXPANDED_NOTIFICATION).setVisible(false);
        }
    }


    private void showNotificationButtonsDialog() {
        final Context context = getActivity();
        final List<Integer> preferredButtons = Prefs.getCompactNotificationButtons();
        final String[] allButtonNames = context.getResources().getStringArray(
                R.array.compact_notification_buttons_options);
        boolean[] checked = new boolean[allButtonNames.length]; // booleans default to false in java

        for (int i = 0; i < checked.length; i++) {
            if (preferredButtons.contains(i)) {
                checked[i] = true;
            }
        }

        AlertDialog.Builder builder = new AccentMaterialDialog(
                context,
                R.style.MaterialAlertDialogTheme
        );
        builder.setTitle(String.format(context.getResources().getString(
                R.string.pref_compact_notification_buttons_dialog_title), 2));
        builder.setMultiChoiceItems(allButtonNames, checked, (dialog, which, isChecked) -> {
            checked[which] = isChecked;

            if (isChecked) {
                if (preferredButtons.size() < 2) {
                    preferredButtons.add(which);
                } else {
                    // Only allow a maximum of two selections. This is because the notification
                    // on the lock screen can only display 3 buttons, and the play/pause button
                    // is always included.
                    checked[which] = false;
                    ListView selectionView = ((AlertDialog) dialog).getListView();
                    selectionView.setItemChecked(which, false);
                    TopSnackbarUtil.showSnack(
                            getActivity(),
                            String.format(context.getResources().getString(
                                    R.string.pref_compact_notification_buttons_dialog_error), 2),
                            Toast.LENGTH_SHORT);
                }
            } else {
                preferredButtons.remove((Integer) which);
            }
        });
        builder.setPositiveButton(R.string.confirm_label, (dialog, which) ->
                Prefs.setCompactNotificationButtons(preferredButtons));
        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    @Override
    public void invalidateSettings() {

    }
}
