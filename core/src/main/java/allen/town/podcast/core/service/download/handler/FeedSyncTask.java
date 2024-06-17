package allen.town.podcast.core.service.download.handler;

import android.content.Context;

import androidx.annotation.NonNull;

import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.core.service.download.DownloadRequest;
import allen.town.podcast.model.download.DownloadStatus;
import allen.town.podcast.core.storage.DBTasks;
import allen.town.podcast.parser.feed.FeedHandlerResult;

public class FeedSyncTask {
    private final DownloadRequest request;
    private final Context context;
    private Feed savedFeed;
    private final FeedParserTask task;
    private FeedHandlerResult feedHandlerResult;

    public FeedSyncTask(Context context, DownloadRequest request) {
        this.request = request;
        this.context = context;
        this.task = new FeedParserTask(request);
    }

    public boolean run() {
        feedHandlerResult = task.call();
        if (!task.isSuccessful()) {
            return false;
        }

        savedFeed = DBTasks.updateFeed(context, feedHandlerResult.feed, false);
        // If loadAllPages=true, check if another page is available and queue it for download
        final boolean loadAllPages = request.getArguments().getBoolean(DownloadRequest.REQUEST_ARG_LOAD_ALL_PAGES);
        final Feed feed = feedHandlerResult.feed;
        if (loadAllPages && feed.getNextPageLink() != null) {
            feed.setId(savedFeed.getId());
            DBTasks.loadNextPageOfFeed(context, feed, true);
        }
        return true;
    }

    @NonNull
    public DownloadStatus getDownloadStatus() {
        return task.getDownloadStatus();
    }

    public Feed getSavedFeed() {
        return savedFeed;
    }

    public String getRedirectUrl() {
        return feedHandlerResult.redirectUrl;
    }
}
