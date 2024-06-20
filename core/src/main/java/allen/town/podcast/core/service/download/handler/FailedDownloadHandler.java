package allen.town.podcast.core.service.download.handler;

import android.util.Log;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.core.service.download.DownloadRequest;
import allen.town.podcast.core.storage.DBWriter;

/**
 * Handles failed downloads.
 * <p/>
 * If the file has been partially downloaded, this handler will set the file_url of the FeedFile to the location
 * of the downloaded file.
 * <p/>
 * Currently, this handler only handles FeedMedia objects, because Feeds and FeedImages are deleted if the download fails.
 */
public class FailedDownloadHandler implements Runnable {
    private static final String TAG = "FailedDownloadHandler";
    private final DownloadRequest request;

    public FailedDownloadHandler(DownloadRequest request) {
        this.request = request;
    }

    @Override
    public void run() {
        if (request.getFeedfileType() == Feed.FEEDFILETYPE_FEED) {
            DBWriter.setFeedLastUpdateFailed(request.getFeedfileId(), true);
        } else if (request.isDeleteOnFailure()) {
            Log.d(TAG, "delete on failure");
        }
    }
}
