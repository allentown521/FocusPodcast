package allen.town.podcast.fragment.onlinesearch;

import allen.town.podcast.discovery.CombinedSearcher;

public class OnlineSearchPodcastFragment extends OnlineSearchFragmentBase{
    public static OnlineSearchFragmentBase newInstance() {
        OnlineSearchFragmentBase fragment = new OnlineSearchPodcastFragment();
        fragment.searchProvider = new CombinedSearcher();
        return fragment;
    }

    @Override
    boolean isTypeEpisodes() {
        return false;
    }
}
