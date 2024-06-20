package allen.town.podcast.fragment.swipeactions;

import android.content.Context;
import androidx.fragment.app.Fragment;
import allen.town.podcast.R;
import allen.town.podcast.actionbuttons.DownloadActionButton;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedItemFilter;

public class StartDownloadSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return START_DOWNLOAD;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_download;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_green;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.download_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        if (!item.isDownloaded() && !item.getFeed().isLocalFeed()) {
            new DownloadActionButton(item)
                    .onClick(fragment.getActivity());
        }
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return false;
    }
}
