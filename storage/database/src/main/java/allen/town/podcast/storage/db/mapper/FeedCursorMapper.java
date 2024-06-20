package allen.town.podcast.storage.db.mapper;

import android.database.Cursor;

import androidx.annotation.NonNull;

import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedPreferences;
import allen.town.podcast.model.feed.SortOrder;
import allen.town.podcast.storage.db.Db;

/**
 * Converts a {@link Cursor} to a {@link Feed} object.
 */
public abstract class FeedCursorMapper {

    /**
     * Create a {@link Feed} instance from a database row (cursor).
     */
    @NonNull
    public static Feed convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndex(Db.KEY_ID);
        int indexLastUpdate = cursor.getColumnIndex(Db.KEY_LASTUPDATE);
        int indexTitle = cursor.getColumnIndex(Db.KEY_TITLE);
        int indexCustomTitle = cursor.getColumnIndex(Db.KEY_CUSTOM_TITLE);
        int indexLink = cursor.getColumnIndex(Db.KEY_LINK);
        int indexDescription = cursor.getColumnIndex(Db.KEY_DESCRIPTION);
        int indexIsSubscribed = cursor.getColumnIndex(Db.KEY_IS_SUBSCRIBED);
        int indexItunesFeedId = cursor.getColumnIndex(Db.KEY_ITUNES_FEED_ID);
        int indexPaymentLink = cursor.getColumnIndex(Db.KEY_PAYMENT_LINK);
        int indexAuthor = cursor.getColumnIndex(Db.KEY_AUTHOR);
        int indexLanguage = cursor.getColumnIndex(Db.KEY_LANGUAGE);
        int indexType = cursor.getColumnIndex(Db.KEY_TYPE);
        int indexFeedIdentifier = cursor.getColumnIndex(Db.KEY_FEED_IDENTIFIER);
        int indexFileUrl = cursor.getColumnIndex(Db.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndex(Db.KEY_DOWNLOAD_URL);
        int indexIsPaged = cursor.getColumnIndex(Db.KEY_IS_PAGED);
        int indexNextPageLink = cursor.getColumnIndex(Db.KEY_NEXT_PAGE_LINK);
        int indexHide = cursor.getColumnIndex(Db.KEY_HIDE);
        int indexSortOrder = cursor.getColumnIndex(Db.KEY_SORT_ORDER);
        int indexLastUpdateFailed = cursor.getColumnIndex(Db.KEY_LAST_UPDATE_FAILED);
        int indexImageUrl = cursor.getColumnIndex(Db.KEY_IMAGE_URL);

        Feed feed = new Feed(
                cursor.getLong(indexId),
                cursor.getString(indexLastUpdate),
                cursor.getString(indexTitle),
                cursor.getString(indexCustomTitle),
                cursor.getString(indexLink),
                cursor.getString(indexDescription),
                cursor.getString(indexPaymentLink),
                cursor.getString(indexAuthor),
                cursor.getString(indexLanguage),
                cursor.getString(indexType),
                cursor.getString(indexFeedIdentifier),
                cursor.getString(indexImageUrl),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                true,
                cursor.getInt(indexIsPaged) > 0,
                cursor.getString(indexNextPageLink),
                cursor.getString(indexHide),
                SortOrder.fromCodeString(cursor.getString(indexSortOrder)),
                cursor.getInt(indexLastUpdateFailed) > 0,
                cursor.getInt(indexIsSubscribed) > 0,
                cursor.getString(indexItunesFeedId)
        );

        FeedPreferences preferences = FeedPreferencesCursorMapper.convert(cursor);
        feed.setPreferences(preferences);
        return feed;
    }
}
