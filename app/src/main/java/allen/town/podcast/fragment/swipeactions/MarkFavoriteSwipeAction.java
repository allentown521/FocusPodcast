package allen.town.podcast.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import allen.town.podcast.R;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedItemFilter;

public class MarkFavoriteSwipeAction implements SwipeAction {

    @Override
    public String getId() {
        return MARK_FAV;
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_star;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_yellow;
    }

    @Override
    public String getTitle(Context context) {
        return context.getString(R.string.add_to_favorite_label);
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        DBWriter.toggleFavoriteItem(item);
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return filter.showIsFavorite || filter.showNotFavorite;
    }
}
