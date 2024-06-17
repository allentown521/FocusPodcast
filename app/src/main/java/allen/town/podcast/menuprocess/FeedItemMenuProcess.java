package allen.town.podcast.menuprocess;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.podcast.R;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.core.pref.PlaybackPreferences;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.service.download.DownloadRequest;
import allen.town.podcast.core.service.download.DownloadRequestCreator;
import allen.town.podcast.core.service.download.DownloadService;
import allen.town.podcast.core.service.playback.PlaybackService;
import allen.town.podcast.core.storage.DBWriter;
import allen.town.podcast.core.sync.SynchronizationSettings;
import allen.town.podcast.core.sync.queue.SynchronizationQueueSink;
import allen.town.podcast.core.util.FeedItemUtil;
import allen.town.podcast.core.util.IntentUtils;
import allen.town.podcast.core.util.ShareUtils;
import allen.town.podcast.dialog.ShareDialog;
import allen.town.podcast.fragment.FeedItemExtraInfoFragment;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.sync.model.EpisodeAction;

/**
 * Handles interactions with the FeedItemMenu.
 */
public class FeedItemMenuProcess {

    private static final String TAG = "FeedItemMenuProcess";

    private FeedItemMenuProcess() {
    }

    /**
     * This method should be called in the prepare-methods of menus. It changes
     * the visibility of the menu items depending on a FeedItem's attributes.
     *
     * @param menu               An instance of Menu
     * @param selectedItem     The FeedItem for which the menu is supposed to be prepared
     * @return Returns true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(Menu menu, FeedItem selectedItem) {
        if (menu == null || selectedItem == null) {
            return false;
        }
        final boolean hasMedia = selectedItem.getMedia() != null;
        final boolean isPlaying = hasMedia && FeedItemUtil.isPlaying(selectedItem.getMedia());
        final boolean isInQueue = selectedItem.isTagged(FeedItem.TAG_QUEUE);
        final boolean fileDownloaded = hasMedia && selectedItem.getMedia().fileExists();
        final boolean isFavorite = selectedItem.isTagged(FeedItem.TAG_FAVORITE);

        setItemVisibility(menu, R.id.skip_episode_item, isPlaying);
        setItemVisibility(menu, R.id.remove_from_queue_item, isInQueue);
        setItemVisibility(menu, R.id.add_to_queue_item, !isInQueue && selectedItem.getMedia() != null);
        setItemVisibility(menu, R.id.visit_website_item, !selectedItem.getFeed().isLocalFeed()
                && ShareUtils.hasLinkToShare(selectedItem));
        setItemVisibility(menu, R.id.share_item, !selectedItem.getFeed().isLocalFeed());
        setItemVisibility(menu, R.id.mark_read_item, !selectedItem.isPlayed());
        setItemVisibility(menu, R.id.mark_unread_item, selectedItem.isPlayed());
        setItemVisibility(menu, R.id.reset_position, hasMedia && selectedItem.getMedia().getPosition() != 0);

        // Display proper strings when item has no media
        if (hasMedia) {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label);
        } else {
            setItemTitle(menu, R.id.mark_read_item, R.string.mark_read_no_media_label);
            setItemTitle(menu, R.id.mark_unread_item, R.string.mark_unread_label_no_media);
        }

        setItemVisibility(menu, R.id.add_to_favorites_item, !isFavorite);
        setItemVisibility(menu, R.id.remove_from_favorites_item, isFavorite);
        setItemVisibility(menu, R.id.remove_item, fileDownloaded);
        setItemVisibility(menu, R.id.download, !fileDownloaded && hasMedia && !selectedItem.getFeed().isLocalFeed());
        return true;
    }

    /**
     * Used to set the viability of a menu item.
     * This method also does some null-checking so that neither menu nor the menu item are null
     * in order to prevent nullpointer exceptions.
     * @param menu The menu that should be used
     * @param menuId The id of the menu item that will be used
     * @param visibility The new visibility status of given menu item
     * */
    private static void setItemVisibility(Menu menu, int menuId, boolean visibility) {
        if (menu == null) {
            return;
        }
        MenuItem item = menu.findItem(menuId);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    /**
     * This method allows to replace to String of a menu item with a different one.
     * @param menu Menu item that should be used
     * @param id The id of the string that is going to be replaced.
     * @param noMedia The id of the new String that is going to be used.
     * */
    public static void setItemTitle(Menu menu, int id, int noMedia) {
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setTitle(noMedia);
        }
    }

    /**
     * The same method as {@link #onPrepareMenu(Menu, FeedItem)}, but lets the
     * caller also specify a list of menu items that should not be shown.
     *
     * @param excludeIds Menu item that should be excluded
     * @return true if selectedItem is not null.
     */
    public static boolean onPrepareMenu(Menu menu, FeedItem selectedItem, int... excludeIds) {
        if (menu == null || selectedItem == null) {
            return false;
        }
        boolean rc = onPrepareMenu(menu, selectedItem);
        if (rc && excludeIds != null) {
            for (int id : excludeIds) {
                setItemVisibility(menu, id, false);
            }
        }
        return rc;
    }

    /**
     * Default menu handling for the given FeedItem.
     *
     * A Fragment instance, (rather than the more generic Context), is needed as a parameter
     * to support some UI operations, e.g., creating a Snackbar.
     */
    public static boolean onMenuItemClicked(@NonNull Fragment fragment, int menuItemId,
                                            @NonNull FeedItem selectedItem) {

        @NonNull Context context = fragment.requireContext();
        if (menuItemId == R.id.skip_episode_item) {
            IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_SKIP_CURRENT_EPISODE);
        } else if (menuItemId == R.id.remove_item) {
            DBWriter.deleteFeedMediaOfItem(context, selectedItem.getMedia().getId());
        } else if (menuItemId == R.id.mark_read_item) {
            selectedItem.setPlayed(true);
            DBWriter.markItemPlayed(selectedItem, FeedItem.PLAYED, true);
            if (SynchronizationSettings.isProviderConnected()) {
                FeedMedia media = selectedItem.getMedia();
                // not all items have media, Gpodder only cares about those that do
                if (media != null) {
                    EpisodeAction actionPlay = new EpisodeAction.Builder(selectedItem, EpisodeAction.PLAY)
                            .currentTimestamp()
                            .started(media.getDuration() / 1000)
                            .position(media.getDuration() / 1000)
                            .total(media.getDuration() / 1000)
                            .build();
                    SynchronizationQueueSink.enqueueEpisodeActionIfSynchronizationIsActive(context, actionPlay);
                }
            }
        } else if (menuItemId == R.id.mark_unread_item) {
            selectedItem.setPlayed(false);
            DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, false);
            if (selectedItem.getMedia() != null) {
                EpisodeAction actionNew = new EpisodeAction.Builder(selectedItem, EpisodeAction.NEW)
                        .currentTimestamp()
                        .build();
                SynchronizationQueueSink.enqueueEpisodeActionIfSynchronizationIsActive(context, actionNew);
            }
        } else if (menuItemId == R.id.add_to_queue_item) {
            DBWriter.addQueueItem(context, selectedItem);
        } else if (menuItemId == R.id.remove_from_queue_item) {
            DBWriter.removeQueueItem(context, true, selectedItem);
        } else if (menuItemId == R.id.add_to_favorites_item) {
            DBWriter.addFavoriteItem(selectedItem);
        } else if (menuItemId == R.id.remove_from_favorites_item) {
            DBWriter.removeFavoriteItem(selectedItem);
        } else if (menuItemId == R.id.reset_position) {
            selectedItem.getMedia().setPosition(0);
            if (PlaybackPreferences.getCurrentlyPlayingFeedMediaId() == selectedItem.getMedia().getId()) {
                PlaybackPreferences.writeNoMediaPlaying();
                IntentUtils.sendLocalBroadcast(context, PlaybackService.ACTION_SHUTDOWN_PLAYBACK_SERVICE);
            }
            DBWriter.markItemPlayed(selectedItem, FeedItem.UNPLAYED, true);
        } else if (menuItemId == R.id.visit_website_item) {
            IntentUtils.openInBrowser(context, FeedItemUtil.getLinkWithFallback(selectedItem));
        } else if (menuItemId == R.id.share_item) {
            ShareDialog shareDialog = ShareDialog.newInstance(selectedItem);
            shareDialog.show((fragment.getActivity().getSupportFragmentManager()), "ShareEpisodeDialog");
        }  else if (menuItemId == R.id.extra_info_item) {
            new FeedItemExtraInfoFragment().show((fragment.getActivity().getSupportFragmentManager()), "FeedItemExtraInfoFragment");
        } else if (menuItemId == R.id.download) {
            List<DownloadRequest> requests = new ArrayList<>();
            if (selectedItem.hasMedia() && !selectedItem.getFeed().isLocalFeed()) {
                requests.add(DownloadRequestCreator.create(selectedItem.getMedia()).build());
            }
            DownloadService.download(context, true, requests.toArray(new DownloadRequest[0]));
            TopSnackbarUtil.showSnack(fragment.getActivity(),context.getResources()
                    .getQuantityString(R.plurals.downloading_batch_label, requests.size(), requests.size()), Toast.LENGTH_LONG);
        } else {
            Log.d(TAG, "unknown menu item  ->  " + menuItemId);
            return false;
        }
        // Refresh menu state

        return true;
    }

    /**
     * Remove new flag with additional UI logic to allow undo with Snackbar.
     *
     * Undo is useful for Remove new flag, given there is no UI to undo it otherwise
     * ,i.e., there is (context) menu item for add new flag
     */
    public static void markReadWithUndo(@NonNull Fragment fragment, FeedItem item,
                                        int playState, boolean showSnackbar) {
        if (item == null) {
            return;
        }

        Log.d(TAG, "undo mark read  -> " + item.getId());
        // we're marking it as unplayed since the user didn't actually play it
        // but they don't want it considered 'NEW' anymore
        DBWriter.markItemPlayed(playState, item.getId());

        final Handler h = new Handler(fragment.requireContext().getMainLooper());
        final Runnable r = () -> {
            FeedMedia media = item.getMedia();
            if (media != null && FeedItemUtil.hasAlmostEnded(media) && Prefs.isAutoDelete()) {
                DBWriter.deleteFeedMediaOfItem(fragment.requireContext(), media.getId());
            }
        };

        int playStateStringRes;
        switch (playState) {
            default:
            case FeedItem.UNPLAYED:
                playStateStringRes = R.string.marked_as_unplayed_label;
                break;
            case FeedItem.PLAYED:
                playStateStringRes = R.string.marked_as_played_label;
                break;
        }

        int duration = Snackbar.LENGTH_LONG;

        if (showSnackbar) {
            ((MainActivity) fragment.getActivity()).showSnackbarAbovePlayer(
                    playStateStringRes, duration)
                    .setAction(fragment.getString(R.string.undo), v -> {
                        DBWriter.markItemPlayed(item.getPlayState(), item.getId());
                        // don't forget to cancel the thing that's going to remove the media
                        h.removeCallbacks(r);
                    });
        }

        h.postDelayed(r, (int) Math.ceil(duration * 1.05f));
    }

    public static void removeNewFlagWithUndo(@NonNull Fragment fragment, FeedItem item) {
        markReadWithUndo(fragment, item, FeedItem.UNPLAYED, false);
    }

}
