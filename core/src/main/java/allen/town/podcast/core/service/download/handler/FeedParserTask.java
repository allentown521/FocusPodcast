package allen.town.podcast.core.service.download.handler;

import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;

import allen.town.focus_common.util.Timber;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedPreferences;
import allen.town.podcast.model.feed.VolumeAdaptionSetting;
import allen.town.podcast.core.service.download.DownloadRequest;
import allen.town.podcast.model.download.DownloadStatus;
import allen.town.podcast.parser.feed.FeedHandler;
import allen.town.podcast.parser.feed.FeedHandlerResult;
import allen.town.podcast.parser.feed.UnsupportedFeedtypeException;
import allen.town.podcast.model.download.DownloadError;
import allen.town.podcast.core.util.InvalidFeedException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

public class FeedParserTask implements Callable<FeedHandlerResult> {
    private static final String TAG = "FeedParserTask";
    private final DownloadRequest request;
    private DownloadStatus downloadStatus;
    private boolean successful = true;

    public FeedParserTask(DownloadRequest request) {
        this.request = request;
        downloadStatus = new DownloadStatus(
        0, request.getTitle(), 0, request.getFeedfileType(), false,
                false, true, DownloadError.ERROR_REQUEST_ERROR, new Date(),
                "Unknown error: Status not set", request.isInitiatedByUser());
    }

    @Override
    public FeedHandlerResult call() {
        Feed feed = new Feed(request.getSource(), request.getLastModified());
        if(request.isNeedAutoSubscribe()){
            //自动订阅需要把feed设置为已订阅
            Timber.i("set feed subscribed auto");
            feed.setSubscribed(true);
        }
        feed.setItunesId(request.getItunesFeedId());
        feed.setFile_url(request.getDestination());
        feed.setId(request.getFeedfileId());
        feed.setDownloaded(true);
        feed.setPreferences(new FeedPreferences(0, true, FeedPreferences.AutoDeleteAction.GLOBAL,
                VolumeAdaptionSetting.OFF, request.getUsername(), request.getPassword()));
        feed.setPageNr(request.getArguments().getInt(DownloadRequest.REQUEST_ARG_PAGE_NR, 0));

        DownloadError reason = null;
        String reasonDetailed = null;
        FeedHandler feedHandler = new FeedHandler();

        FeedHandlerResult result = null;
        try {
            result = feedHandler.parseFeed(feed);
            checkFeedData(feed);
            if (TextUtils.isEmpty(feed.getImageUrl())) {
                feed.setImageUrl(Feed.PREFIX_GENERATIVE_COVER + feed.getDownload_url());
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            successful = false;
            e.printStackTrace();
            reason = DownloadError.ERROR_PARSER_EXCEPTION;
            reasonDetailed = e.getMessage();
        } catch (UnsupportedFeedtypeException e) {
            e.printStackTrace();
            successful = false;
            reason = DownloadError.ERROR_UNSUPPORTED_TYPE;
            if ("html".equalsIgnoreCase(e.getRootElement())) {
                reason = DownloadError.ERROR_UNSUPPORTED_TYPE_HTML;
            }
            reasonDetailed = e.getMessage();
        } catch (InvalidFeedException e) {
            e.printStackTrace();
            successful = false;
            reason = DownloadError.ERROR_PARSER_EXCEPTION;
            reasonDetailed = e.getMessage();
        } finally {
            File feedFile = new File(request.getDestination());
            if (feedFile.exists()) {
                boolean deleted = feedFile.delete();
                Log.d(TAG, "delete file '" + feedFile.getAbsolutePath() + "' "
                        + (deleted ? "successful" : "failed"));
            }
        }

        if (successful) {
            downloadStatus = new DownloadStatus(feed, feed.getHumanReadableIdentifier(), DownloadError.SUCCESS,
                                                successful, reasonDetailed, request.isInitiatedByUser());
            return result;
        } else {
            downloadStatus = new DownloadStatus(feed, feed.getHumanReadableIdentifier(), reason,
                                                successful, reasonDetailed, request.isInitiatedByUser());
            return null;
        }
    }

    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Checks if the feed was parsed correctly.
     */
    private void checkFeedData(Feed feed) throws InvalidFeedException {
        if (feed.getTitle() == null) {
            throw new InvalidFeedException("Feed has no title");
        }
        checkFeedItems(feed);
    }

    private void checkFeedItems(Feed feed) throws InvalidFeedException  {
        for (FeedItem item : feed.getItems()) {
            if (item.getTitle() == null) {
                throw new InvalidFeedException("Item has no title: " + item);
            }
        }
    }

    @NonNull
    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }
}
