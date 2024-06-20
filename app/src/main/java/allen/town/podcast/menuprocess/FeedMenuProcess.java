package allen.town.podcast.menuprocess;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;

import allen.town.podcast.R;
import allen.town.podcast.core.dialog.ConfirmationDialog;
import allen.town.podcast.core.storage.DBTasks;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.core.util.IntentUtils;
import allen.town.podcast.core.util.ShareUtils;
import allen.town.podcast.dialog.FilterDialog;
import allen.town.podcast.dialog.BaseFeedSortDialog;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.SortOrder;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedMenuProcess {

    private FeedMenuProcess() {
    }

    private static final String TAG = "FeedMenuProcess";

    public static boolean onPrepareOptionsMenu(Menu menu, Feed selectedFeed) {
        if (selectedFeed == null) {
            return true;
        }


        menu.findItem(R.id.refresh_complete_item).setVisible(selectedFeed.isPaged());
        if (StringUtils.isBlank(selectedFeed.getLink())) {
            menu.findItem(R.id.visit_website_item).setVisible(false);
            menu.findItem(R.id.share_link_item).setVisible(false);
        }
        if (selectedFeed.isLocalFeed()) {
            // hide complete submenu "Share..." as both sub menu items are not visible
            menu.findItem(R.id.share_item).setVisible(false);
        }

        return true;
    }

    /**
     * NOTE: This method does not handle clicks on the 'remove feed' - item.
     */
    public static boolean onOptionsItemClicked(final Context context, final MenuItem item, final Feed selectedFeed) {
        final int itemId = item.getItemId();
        if (itemId == R.id.refresh_item) {
            DBTasks.forceRefreshFeed(context, selectedFeed, true);
        } else if (itemId == R.id.refresh_complete_item) {
            DBTasks.forceRefreshCompleteFeed(context, selectedFeed);
        } else if (itemId == R.id.sort_items) {
            showSortDialog(context, selectedFeed);
        } else if (itemId == R.id.filter_items) {
            showFilterDialog(context, selectedFeed);
        } else if (itemId == R.id.mark_all_read_item) {
            ConfirmationDialog conDialog = new ConfirmationDialog(context,
                    R.string.mark_all_read_label,
                    R.string.mark_all_read_feed_confirmation_msg) {

                @Override
                public void onConfirmButtonPressed(
                        DialogInterface dialog) {
                    dialog.dismiss();
                    DBWriter.markFeedRead(selectedFeed.getId());
                }
            };
            conDialog.createNewDialog().show();
        } else if (itemId == R.id.visit_website_item) {
            IntentUtils.openInBrowser(context, selectedFeed.getLink());
        } else if (itemId == R.id.share_link_item) {
            ShareUtils.shareFeedlink(context, selectedFeed);
        } else if (itemId == R.id.share_download_url_item) {
            ShareUtils.shareFeedDownloadLink(context, selectedFeed);
        } else {
            return false;
        }
        return true;
    }

    public static void showFilterDialog(Context context, Feed selectedFeed) {
        FilterDialog filterDialog = new FilterDialog(context, selectedFeed.getItemFilter()) {
            @Override
            protected void updateFilter(Set<String> filterValues) {
                selectedFeed.setItemFilter(filterValues.toArray(new String[0]));
                DBWriter.setFeedItemsFilter(selectedFeed.getId(), filterValues);
            }
        };

        filterDialog.openDialog();
    }


    public static void showSortDialog(Context context, Feed selectedFeed) {
        BaseFeedSortDialog sortDialog = new BaseFeedSortDialog(context,selectedFeed.getSortOrder()) {
            @Override
            protected void updateSort(@NonNull SortOrder sortOrder) {
                selectedFeed.setSortOrder(sortOrder);
                DBWriter.setFeedItemSortOrder(selectedFeed.getId(), sortOrder);
            }
        };
        sortDialog.openDialog();
    }

}
