package allen.town.podcast.fragment.onlinesearch;

import android.util.Log;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import allen.town.podcast.adapter.ItunesAdapter;
import allen.town.podcast.discovery.ItunesEpisodesSearcher;
import allen.town.podcast.event.FeedItemEvent;
import allen.town.podcast.event.FeedListUpdateEvent;
import allen.town.podcast.event.PlayerStatusEvent;
import allen.town.podcast.event.UnreadItemsUpdateEvent;
import allen.town.podcast.event.playback.PlaybackPositionEvent;

public class OnlineSearchEpisodesFragment extends OnlineSearchFragmentBase {
    private static String TAG="OnlineSearchEpisodesFragment";
    public static OnlineSearchFragmentBase newInstance() {
        OnlineSearchFragmentBase fragment = new OnlineSearchEpisodesFragment();
        fragment.searchProvider = new ItunesEpisodesSearcher();
        return fragment;
    }

    @Override
    boolean isTypeEpisodes() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
//        for (FeedItem item : event.items) {
//            int pos = FeedItemUtil.indexOfItemWithId(episodes, item.getId());
//            if (pos >= 0) {
//                episodes.remove(pos);
//                if (shouldUpdatedItemRemainInList(item)) {
//                    episodes.add(pos, item);
//                    adapter.notifyItemChangedCompat(pos);
//                } else {
//                    adapter.notifyItemRemoved(pos);
//                }
//            }
//        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                ItunesAdapter.PodcastViewHolder holder = (ItunesAdapter.PodcastViewHolder) gridView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        adapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
    }

}
