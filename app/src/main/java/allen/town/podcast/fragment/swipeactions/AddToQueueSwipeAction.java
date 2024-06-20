package allen.town.podcast.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import allen.town.podcast.R;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedItemFilter;

public class AddToQueueSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return ADD_TO_QUEUE;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_playlist;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_blue;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.add_to_queue_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isTagged(FeedItem.TAG_QUEUE)) {
            DBWriter.addQueueItem(fragment.requireContext(), item);
        } else {
            new RemoveFromQueueSwipeAction().performAction(item, fragment, filter);
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showQueued || filter.showNew;
    }
}
