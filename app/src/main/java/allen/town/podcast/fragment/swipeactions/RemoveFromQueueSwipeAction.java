package allen.town.podcast.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import allen.town.podcast.R;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.core.storage.DBReader;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedItemFilter;

public class RemoveFromQueueSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return REMOVE_FROM_QUEUE;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_playlist_remove;
    }

    @Override
    public int getActionColor() {
        return R.attr.colorAccent;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.remove_from_queue_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        int position = DBReader.getQueueIDList().indexOf(item.getId());

        DBWriter.removeQueueItem(fragment.requireActivity(), true, item);

        if (willRemove(filter)) {
            ((MainActivity) fragment.requireActivity()).showSnackbarAbovePlayer(
                    fragment.getResources().getQuantityString(R.plurals.removed_from_queue_batch_label, 1, 1),
                    Snackbar.LENGTH_LONG)
                    .setAction(fragment.getString(R.string.undo), v ->
                            DBWriter.addQueueItemAt(fragment.requireActivity(), item.getId(), position, false));
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showQueued || filter.showNotQueued;
    }
}
