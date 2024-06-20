package allen.town.podcast.fragment.pref;

import android.os.Bundle;

import allen.town.podcast.R;
import allen.town.podcast.activity.SettingsActivity;
import allen.town.podcast.dialog.SwipeActionsDialog;
import allen.town.podcast.fragment.EpisodesFragment;
import allen.town.podcast.fragment.FavoriteEpisodesFragment;
import allen.town.podcast.fragment.FeedItemlistFragment;
import allen.town.podcast.fragment.PlaylistFragment;

public class SwipePrefFragment extends AbsSettingsFragment {
    private static final String PREF_SWIPE_FEED = "prefSwipeFeed";
    private static final String PREF_SWIPE_QUEUE = "prefSwipeQueue";
    private static final String PREF_SWIPE_EPISODES = "pref_swipe_episodes";
    private static final String PREF_SWIPE_FAVORITE = "pref_swipe_favorite";



    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_swipe);

        findPreference(PREF_SWIPE_FEED).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), FeedItemlistFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_QUEUE).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), PlaylistFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_EPISODES).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), EpisodesFragment.TAG).show(() -> { });
            return true;
        });
        findPreference(PREF_SWIPE_FAVORITE).setOnPreferenceClickListener(preference -> {
            new SwipeActionsDialog(requireContext(), FavoriteEpisodesFragment.TAG).show(() -> { });
            return true;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        ((SettingsActivity) getActivity()).setTitle(R.string.swipeactions_label);
    }

    @Override
    public void invalidateSettings() {

    }
}
