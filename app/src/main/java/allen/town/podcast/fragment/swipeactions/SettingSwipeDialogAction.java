package allen.town.podcast.fragment.swipeactions;

import android.content.Context;

import androidx.fragment.app.Fragment;

import allen.town.podcast.R;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedItemFilter;

public class SettingSwipeDialogAction implements SwipeAction {

    @Override
    public String getId() {
        return "SHOW_FIRST_SETTING_SWIPE_DIALOG";
    }

    @Override
    public int getActionIcon() {
        return R.drawable.ic_settings;
    }

    @Override
    public int getActionColor() {
        return R.attr.icon_gray;
    }

    @Override
    public String getTitle(Context context) {
        return "";
    }

    @Override
    public void performAction(FeedItem item, Fragment fragment, FeedItemFilter filter) {
        //handled in SwipeActions
    }

    @Override
    public boolean willRemove(FeedItemFilter filter) {
        return false;
    }
}
