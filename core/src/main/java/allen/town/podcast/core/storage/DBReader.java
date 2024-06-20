package allen.town.podcast.core.storage;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import allen.town.podcast.core.feed.SubscriptionsFilter;
import allen.town.podcast.core.pref.Prefs;
import allen.town.podcast.core.util.LongList;
import allen.town.podcast.core.util.comparator.DownloadStatusComparator;
import allen.town.podcast.core.util.comparator.FeedItemPubdateComparator;
import allen.town.podcast.core.util.comparator.PlaybackCompletionDateComparator;
import allen.town.podcast.model.download.DownloadStatus;
import allen.town.podcast.model.feed.Chapter;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.model.feed.FeedItemFilter;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.model.feed.FeedPreferences;
import allen.town.podcast.storage.db.Db;
import allen.town.podcast.storage.db.LongIntMap;
import allen.town.podcast.storage.db.mapper.ChapterCursorMapper;
import allen.town.podcast.storage.db.mapper.DownloadStatusCursorMapper;
import allen.town.podcast.storage.db.mapper.FeedCursorMapper;
import allen.town.podcast.storage.db.mapper.FeedItemCursorMapper;
import allen.town.podcast.storage.db.mapper.FeedMediaCursorMapper;
import allen.town.podcast.storage.db.mapper.FeedPreferencesCursorMapper;

/**
 * Provides methods for reading data from the database.
 * In general, all database calls in DBReader-methods are executed on the caller's thread.
 * This means that the caller should make sure that DBReader-methods are not executed on the GUI-thread.
 */
public final class DBReader {

    private static final String TAG = "DBReader";


    /**
     * Maximum size of the list returned by {@link #getDownloadLog()}.
     */
    private static final int DOWNLOAD_LOG_SIZE = 200;


    private DBReader() {
    }


    /**
     * 查询数据库中所有的feed，包括未订阅的
     * @return
     */
    @NonNull
    public static List<Feed> getAllFeedList() {
        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getAllFeedsCursor()) {
            List<Feed> feeds = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                Feed feed = extractFeedFromCursorRow(cursor);
                feeds.add(feed);
            }
            return feeds;
        }
    }

    /**
     * Returns a list of Feeds（已订阅的）, sorted alphabetically by their title.
     *
     * @return A list of Feeds, sorted alphabetically by their title. A Feed-object
     * of the returned list does NOT have its list of FeedItems yet. The FeedItem-list
     * can be loaded separately with {@link #getFeedItemList(Feed)}.
     */
    @NonNull
    public static List<Feed> getFeedList() {
        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return getFeedList(adapter);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    private static List<Feed> getFeedList(Db adapter) {
        try (Cursor cursor = adapter.getSubscribedFeedsCursor()) {
            List<Feed> feeds = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                Feed feed = extractFeedFromCursorRow(cursor);
                feeds.add(feed);
            }
            return feeds;
        }
    }

    /**
     * 查询没有在收藏和播放列表中的未订阅的feed
     * @param adapter
     * @return
     */
    @NonNull
    public static List<Long> getNotSubAndNotInPlaylistAndFavFeedList(Db adapter) {
        try (Cursor cursor = adapter.getFeedsNotSubAndNotInPlaylistAndFavCursor()) {
            List<Long> feeds = new ArrayList<>(cursor.getCount());
            long feedId = 0;
            int index = cursor.getColumnIndexOrThrow(Db.KEY_ID);
            while (cursor.moveToNext()) {
                feedId = cursor.getLong(index);
                feeds.add(feedId);
            }
            cursor.close();
            return feeds;
        }
    }




    /**
     * Returns a list with the download URLs of all feeds.（返回真正订阅的，这里是为了同步到云端使用的）
     *
     * @return A list of Strings with the download URLs of all feeds.
     */
    public static List<String> getFeedListDownloadUrls() {
        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getSubFeedCursorDownloadUrls()) {
            List<String> result = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String url = cursor.getString(1);
                if (url != null && !url.startsWith(Feed.PREFIX_LOCAL_FOLDER)) {
                    result.add(url);
                }
            }
            return result;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads additional data in to the feed items from other database queries
     *
     * @param items the FeedItems who should have other data loaded
     */
    public static void loadAdditionalFeedItemListData(List<FeedItem> items) {
        loadTagsOfFeedItemList(items);
        loadFeedDataOfFeedItemList(items);
    }

    private static void loadTagsOfFeedItemList(List<FeedItem> items) {
        LongList favoriteIds = getFavoriteIDList();
        LongList queueIds = getQueueIDList();

        for (FeedItem item : items) {
            if (favoriteIds.contains(item.getId())) {
                item.addTag(FeedItem.TAG_FAVORITE);
            }
            if (queueIds.contains(item.getId())) {
                item.addTag(FeedItem.TAG_QUEUE);
            }
        }
    }

    /**
     * Takes a list of FeedItems and loads their corresponding Feed-objects from the database.
     * The feedID-attribute of a FeedItem must be set to the ID of its feed or the method will
     * not find the correct feed of an item.
     *
     * @param items The FeedItems whose Feed-objects should be loaded.
     */
    private static void loadFeedDataOfFeedItemList(List<FeedItem> items) {
        List<Feed> feeds = getAllFeedList();//这里查询所有的订阅源而不是已订阅的，因为只是查询信息而已

        Map<Long, Feed> feedIndex = new ArrayMap<>(feeds.size());
        for (Feed feed : feeds) {
            feedIndex.put(feed.getId(), feed);
        }
        for (FeedItem item : items) {
            Feed feed = feedIndex.get(item.getFeedId());
            if (feed == null) {
                Log.w(TAG, "No match found for item with ID " + item.getId() + ". Feed ID was " + item.getFeedId());
                feed = new Feed("", "", "Error: Item without feed");
            }
            item.setFeed(feed);
        }
    }

    /**
     * Loads the list of FeedItems for a certain Feed-object.
     * This method should NOT be used if the FeedItems are not used.
     *
     * @param feed The Feed whose items should be loaded
     * @return A list with the FeedItems of the Feed. The Feed-attribute of the FeedItems will already be set correctly.
     * The method does NOT change the items-attribute of the feed.
     */
    public static List<FeedItem> getFeedItemList(final Feed feed) {
        return getFeedItemList(feed, FeedItemFilter.unfiltered());
    }

    public static List<FeedItem> getFeedItemList(final Feed feed, final FeedItemFilter filter) {
        Log.d(TAG, "getFeedItemList() called with: " + "feed = [" + feed + "]");

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getItemsOfFeedCursor(feed, filter)) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            Collections.sort(items, new FeedItemPubdateComparator());
            for (FeedItem item : items) {
                item.setFeed(feed);
            }
            return items;
        } finally {
            adapter.close();
        }
    }

    public static List<FeedItem> extractItemlistFromCursor(Cursor itemlistCursor) {
        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return extractItemlistFromCursor(adapter, itemlistCursor);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    private static List<FeedItem> extractItemlistFromCursor(Db adapter, Cursor cursor) {
        List<FeedItem> result = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            int indexMediaId = cursor.getColumnIndexOrThrow(Db.SELECT_KEY_MEDIA_ID);
            do {
                FeedItem item = FeedItemCursorMapper.convert(cursor);
                result.add(item);
                if (!cursor.isNull(indexMediaId)) {
                    item.setMedia(FeedMediaCursorMapper.convert(cursor));
                }
            } while (cursor.moveToNext());
        }
        return result;
    }

    private static Feed extractFeedFromCursorRow(Cursor cursor) {
        Feed feed = FeedCursorMapper.convert(cursor);
        FeedPreferences preferences = FeedPreferencesCursorMapper.convert(cursor);
        feed.setPreferences(preferences);
        return feed;
    }

    @NonNull
    static List<FeedItem> getQueue(Db adapter) {
        try (Cursor cursor = adapter.getQueueCursor()) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        }
    }

    /**
     * Loads the IDs of the FeedItems in the queue. This method should be preferred over
     * {@link #getQueue()} if the FeedItems of the queue are not needed.
     *
     * @return A list of IDs sorted by the same order as the queue.
     */
    public static LongList getQueueIDList() {
        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return getQueueIDList(adapter);
        } finally {
            adapter.close();
        }
    }

    private static LongList getQueueIDList(Db adapter) {
        try (Cursor cursor = adapter.getQueueIDCursor()) {
            LongList queueIds = new LongList(cursor.getCount());
            while (cursor.moveToNext()) {
                queueIds.add(cursor.getLong(0));
            }
            return queueIds;
        }
    }

    /**
     * Loads a list of the FeedItems in the queue. If the FeedItems of the queue are not used directly, consider using
     * {@link #getQueueIDList()} instead.
     *
     * @return A list of FeedItems sorted by the same order as the queue.
     */
    @NonNull
    public static List<FeedItem> getQueue() {

        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return getQueue(adapter);
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a list of FeedItems whose episode has been downloaded.
     *
     * @return A list of FeedItems whose episdoe has been downloaded.
     */
    @NonNull
    public static List<FeedItem> getDownloadedItems() {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getDownloadedItemsCursor()) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            loadAdditionalFeedItemListData(items);
            Collections.sort(items, new FeedItemPubdateComparator());
            return items;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a list of FeedItems whose episode has been played.
     *
     * @return A list of FeedItems whose episdoe has been played.
     */
    @NonNull
    public static List<FeedItem> getPlayedItems() {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getPlayedItemsCursor()) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a list of FeedItems that are considered new.
     * Excludes items from feeds that do not have keep updated enabled.
     *
     * @param offset The first episode that should be loaded.
     * @param limit  The maximum number of episodes that should be loaded.
     * @return A list of FeedItems that are considered new.
     */
    public static List<FeedItem> getNewItemsList(int offset, int limit) {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getNewItemsCursor(offset, limit)) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a list of favorite items.
     *
     * @param offset The first episode that should be loaded.
     * @param limit  The maximum number of episodes that should be loaded.
     * @return A list of FeedItems that are marked as favorite.
     */
    public static List<FeedItem> getFavoriteItemsList(int offset, int limit) {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getFavoritesCursor(offset, limit)) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    private static LongList getFavoriteIDList() {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getFavoritesCursor(0, Integer.MAX_VALUE)) {
            LongList favoriteIDs = new LongList(cursor.getCount());
            while (cursor.moveToNext()) {
                favoriteIDs.add(cursor.getLong(0));
            }
            return favoriteIDs;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a filtered list of FeedItems sorted by pubDate in descending order.
     *
     * @param offset The first episode that should be loaded.
     * @param limit  The maximum number of episodes that should be loaded.
     * @param filter The filter describing which episodes to filter out.
     */
    @NonNull
    public static List<FeedItem> getRecentlyPublishedEpisodes(int offset, int limit, FeedItemFilter filter) {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getRecentlyPublishedItemsCursor(offset, limit, filter)) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, cursor);
            loadAdditionalFeedItemListData(items);
            return items;
        } finally {
            adapter.close();
        }
    }

    public static int getTotalEpisodeCount(FeedItemFilter filter) {
        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getTotalEpisodeCountCursor(filter)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return -1;
        } finally {
            adapter.close();
        }
    }

    /**
     * 获取已订阅的feed数量
     * @return
     */
    public static int getSubscribedFeedsCount() {
        Db adapter = Db.getInstance();
        adapter.open();
        int subscribedFeedCount = 0;
        try (Cursor cursor = adapter.getSubscribedFeedsCountCursor()) {
            if (cursor.moveToFirst()) {
                subscribedFeedCount = cursor.getInt(0);
            }
            return subscribedFeedCount;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads the playback history from the database. A FeedItem is in the playback history if playback of the correpsonding episode
     * has been completed at least once.
     ** @param limit The maximum number of items to return.
     *      *
     * @return The playback history. The FeedItems are sorted by their media's playbackCompletionDate in descending order.
     */
    @NonNull
    public static List<FeedItem> getPlaybackHistory(int offset, int limit) {

        Db adapter = Db.getInstance();
        adapter.open();

        Cursor mediaCursor = null;
        Cursor itemCursor = null;
        try {
            mediaCursor = adapter.getCompletedMediaCursor(offset, limit);
            String[] itemIds = new String[mediaCursor.getCount()];
            for (int i = 0; i < itemIds.length && mediaCursor.moveToPosition(i); i++) {
                int index = mediaCursor.getColumnIndex(Db.KEY_FEEDITEM);
                itemIds[i] = Long.toString(mediaCursor.getLong(index));
            }
            itemCursor = adapter.getFeedItemCursor(itemIds);
            List<FeedItem> items = extractItemlistFromCursor(adapter, itemCursor);
            loadAdditionalFeedItemListData(items);
            Collections.sort(items, new PlaybackCompletionDateComparator());
            return items;
        } finally {
            if (mediaCursor != null) {
                mediaCursor.close();
            }
            if (itemCursor != null) {
                itemCursor.close();
            }
            adapter.close();
        }
    }

    /**
     * 返回没有订阅的同时不在播放列表和收藏中的items
     * @return
     */
    public static List<FeedItem> getUnsubFeedItems() {
        Db adapter = Db.getInstance();
        adapter.open();

        try (Cursor mediaCursor = adapter.getUnsubFeedItemsCursor()) {
            List<FeedItem> items = extractItemlistFromCursor(adapter, mediaCursor);
            if (items.size() > 0) {
                loadAdditionalFeedItemListData(items);
            }
            return items;
        } finally {
            adapter.close();
        }
    }


    public static long getPlaybackHistoryLength() {
        Db adapter = Db.getInstance();
        adapter.open();

        try {
            return adapter.getCompletedMediaLength();
        } finally {
            adapter.close();
        }
    }
    /**
     * Loads the download log from the database.
     *
     * @return A list with DownloadStatus objects that represent the download log.
     * The size of the returned list is limited by {@link #DOWNLOAD_LOG_SIZE}.
     */
    public static List<DownloadStatus> getDownloadLog() {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getDownloadLogCursor(DOWNLOAD_LOG_SIZE)) {
            List<DownloadStatus> downloadLog = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                downloadLog.add(DownloadStatusCursorMapper.convert(cursor));
            }
            Collections.sort(downloadLog, new DownloadStatusComparator());
            return downloadLog;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads the download log for a particular feed from the database.
     *
     * @param feedId Feed id for which the download log is loaded
     * @return A list with DownloadStatus objects that represent the feed's download log,
     * newest events first.
     */
    public static List<DownloadStatus> getFeedDownloadLog(long feedId) {

        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getDownloadLog(Feed.FEEDFILETYPE_FEED, feedId)) {
            List<DownloadStatus> downloadLog = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                downloadLog.add(DownloadStatusCursorMapper.convert(cursor));
            }
            Collections.sort(downloadLog, new DownloadStatusComparator());
            return downloadLog;
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a specific Feed from the database.
     *
     * @param feedId The ID of the Feed
     * @return The Feed or null if the Feed could not be found. The Feeds FeedItems will also be loaded from the
     * database and the items-attribute will be set correctly.
     */
    @Nullable
    public static Feed getFeed(final long feedId) {
        return getFeed(feedId, false);
    }

    /**
     * Loads a specific Feed from the database.
     *
     * @param feedId   The ID of the Feed
     * @param filtered <code>true</code> if only the visible items should be loaded according to the feed filter.
     * @return The Feed or null if the Feed could not be found. The Feeds FeedItems will also be loaded from the
     * database and the items-attribute will be set correctly.
     */
    @Nullable
    public static Feed getFeed(final long feedId, boolean filtered) {
        Db adapter = Db.getInstance();
        adapter.open();
        Feed feed = null;
        try (Cursor cursor = adapter.getFeedCursor(feedId)) {
            if (cursor.moveToNext()) {
                feed = extractFeedFromCursorRow(cursor);
                if (filtered) {
                    feed.setItems(getFeedItemList(feed, feed.getItemFilter()));
                } else {
                    feed.setItems(getFeedItemList(feed));
                }
            } else {
                Log.e(TAG, "getFeed could not find feed with id " + feedId);
            }
            return feed;
        } finally {
            adapter.close();
        }
    }

    @Nullable
    public static Feed getFeed(final String feedUrl, boolean filtered) {
        Db adapter = Db.getInstance();
        adapter.open();
        Feed feed = null;
        try (Cursor cursor = adapter.getFeedCursorByFeedUrl(feedUrl)) {
            if (cursor.moveToNext()) {
                feed = extractFeedFromCursorRow(cursor);
                if (filtered) {
                    feed.setItems(getFeedItemList(feed, feed.getItemFilter()));
                } else {
                    feed.setItems(getFeedItemList(feed));
                }
            } else {
                Log.e(TAG, "getFeed could not find feed with feedUrl " + feedUrl);
            }
            return feed;
        } finally {
            adapter.close();
        }
    }

    @Nullable
    public static Feed getFeedByItunesFeedId(final String itunesFeedId, boolean filtered) {
        if(TextUtils.isEmpty(itunesFeedId)){
            //如果itunesFeedId为空，那么肯定是没有匹配的feed的，只有不为空的时候才去查询
            return null;
        }
        Db adapter = Db.getInstance();
        adapter.open();
        Feed feed = null;
        try (Cursor cursor = adapter.getFeedCursorByItunesFeedId(itunesFeedId)) {
            if (cursor.moveToNext()) {
                feed = extractFeedFromCursorRow(cursor);
                if (filtered) {
                    feed.setItems(getFeedItemList(feed, feed.getItemFilter()));
                } else {
                    feed.setItems(getFeedItemList(feed));
                }
            } else {
                Log.e(TAG, "getFeed could not find feed with itunesFeedId " + itunesFeedId);
            }
            return feed;
        } finally {
            adapter.close();
        }
    }

    @Nullable
    private static FeedItem getFeedItem(final long itemId, Db adapter) {

        FeedItem item = null;
        try (Cursor cursor = adapter.getFeedItemCursor(Long.toString(itemId))) {
            if (cursor.moveToNext()) {
                List<FeedItem> list = extractItemlistFromCursor(adapter, cursor);
                if (!list.isEmpty()) {
                    item = list.get(0);
                    loadAdditionalFeedItemListData(list);
                }
            }
            return item;
        }
    }

    /**
     * Loads a specific FeedItem from the database. This method should not be used for loading more
     * than one FeedItem because this method might query the database several times for each item.
     *
     * @param itemId The ID of the FeedItem
     * @return The FeedItem or null if the FeedItem could not be found.
     */
    @Nullable
    public static FeedItem getFeedItem(final long itemId) {

        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return getFeedItem(itemId, adapter);
        } finally {
            adapter.close();
        }
    }

    /**
     * Get next feed item in queue following a particular feeditem
     *
     * @param item The FeedItem
     * @return The FeedItem next in queue or null if the FeedItem could not be found.
     */
    @Nullable
    public static FeedItem getNextInQueue(FeedItem item) {
        Db adapter = Db.getInstance();
        adapter.open();
        try {
            FeedItem nextItem = null;
            try (Cursor cursor = adapter.getNextInQueue(item)) {
                List<FeedItem> list = extractItemlistFromCursor(adapter, cursor);
                if (!list.isEmpty()) {
                    nextItem = list.get(0);
                    loadAdditionalFeedItemListData(list);
                }
                return nextItem;
            } catch (Exception e) {
                return null;
            }
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads a specific FeedItem from the database.
     *
     * @param guid       feed item guid
     * @param episodeUrl the feed item's url
     * @return The FeedItem or null if the FeedItem could not be found.
     * Does NOT load additional attributes like feed or queue state.
     */
    @Nullable
    private static FeedItem getFeedItemByGuidOrEpisodeUrl(final String guid, final String episodeUrl,
                                                          Db adapter) {
        try (Cursor cursor = adapter.getFeedItemCursor(guid, episodeUrl)) {
            if (!cursor.moveToNext()) {
                return null;
            }
            List<FeedItem> list = extractItemlistFromCursor(adapter, cursor);
            if (!list.isEmpty()) {
                return list.get(0);
            }
            return null;
        }
    }

    /**
     * Returns credentials based on image URL
     *
     * @param imageUrl The URL of the image
     * @return Credentials in format "Username:Password", empty String if no authorization given
     */
    public static String getImageAuthentication(final String imageUrl) {

        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return getImageAuthentication(imageUrl, adapter);
        } finally {
            adapter.close();
        }
    }

    private static String getImageAuthentication(final String imageUrl, Db adapter) {
        String credentials;
        try (Cursor cursor = adapter.getImageAuthenticationCursor(imageUrl)) {
            if (cursor.moveToFirst()) {
                String username = cursor.getString(0);
                String password = cursor.getString(1);
                if (!TextUtils.isEmpty(username) && password != null) {
                    credentials = username + ":" + password;
                } else {
                    credentials = "";
                }
            } else {
                credentials = "";
            }
        }
        return credentials;
    }

    /**
     * Loads a specific FeedItem from the database.
     *
     * @param guid       feed item guid
     * @param episodeUrl the feed item's url
     * @return The FeedItem or null if the FeedItem could not be found.
     * Does NOT load additional attributes like feed or queue state.
     */
    public static FeedItem getFeedItemByGuidOrEpisodeUrl(final String guid, final String episodeUrl) {
        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return getFeedItemByGuidOrEpisodeUrl(guid, episodeUrl, adapter);
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads shownotes information about a FeedItem.
     *
     * @param item The FeedItem
     */
    public static void loadDescriptionOfFeedItem(final FeedItem item) {
        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getDescriptionOfItem(item)) {
            if (cursor.moveToFirst()) {
                int indexDescription = cursor.getColumnIndex(Db.KEY_DESCRIPTION);
                String description = cursor.getString(indexDescription);
                item.setDescriptionIfLonger(description);
            }
        } finally {
            adapter.close();
        }
    }

    /**
     * Loads the list of chapters that belongs to this FeedItem if available. This method overwrites
     * any chapters that this FeedItem has. If no chapters were found in the database, the chapters
     * reference of the FeedItem will be set to null.
     *
     * @param item The FeedItem
     */
    public static List<Chapter> loadChaptersOfFeedItem(final FeedItem item) {

        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return loadChaptersOfFeedItem(adapter, item);
        } finally {
            adapter.close();
        }
    }

    private static List<Chapter> loadChaptersOfFeedItem(Db adapter, FeedItem item) {
        try (Cursor cursor = adapter.getSimpleChaptersOfFeedItemCursor(item)) {
            int chaptersCount = cursor.getCount();
            if (chaptersCount == 0) {
                item.setChapters(null);
                return null;
            }
            ArrayList<Chapter> chapters = new ArrayList<>();
            while (cursor.moveToNext()) {
                chapters.add(ChapterCursorMapper.convert(cursor));
            }
            return chapters;
        }
    }

    /**
     * Returns the number of downloaded episodes.
     *
     * @return The number of downloaded episodes.
     */

    public static int getNumberOfDownloadedEpisodes() {

        Db adapter = Db.getInstance();
        adapter.open();
        try {
            return adapter.getNumberOfDownloadedEpisodes();
        } finally {
            adapter.close();
        }
    }

    /**
     * Searches the DB for a FeedMedia of the given id.
     *
     * @param mediaId The id of the object
     * @return The found object, or null if it does not exist
     */
    @Nullable
    public static FeedMedia getFeedMedia(final long mediaId) {
        Db adapter = Db.getInstance();
        adapter.open();

        try (Cursor mediaCursor = adapter.getSingleFeedMediaCursor(mediaId)) {
            if (!mediaCursor.moveToFirst()) {
                return null;
            }

            int indexFeedItem = mediaCursor.getColumnIndex(Db.KEY_FEEDITEM);
            long itemId = mediaCursor.getLong(indexFeedItem);
            FeedMedia media = FeedMediaCursorMapper.convert(mediaCursor);
            FeedItem item = getFeedItem(itemId);
            if (item != null) {
                media.setItem(item);
                item.setMedia(media);
            }
            return media;
        } finally {
            adapter.close();
        }
    }

    public static class MonthlyStatisticsItem {
        public int year = 0;
        public int month = 0;
        public long timePlayed = 0;
    }

    @NonNull
    public static List<MonthlyStatisticsItem> getMonthlyTimeStatistics() {
        List<MonthlyStatisticsItem> months = new ArrayList<>();
        Db adapter = Db.getInstance();
        adapter.open();
        try (Cursor cursor = adapter.getMonthlyStatisticsCursor()) {
            int indexMonth = cursor.getColumnIndexOrThrow("month");
            int indexYear = cursor.getColumnIndexOrThrow("year");
            int indexTotalDuration = cursor.getColumnIndexOrThrow("total_duration");
            while (cursor.moveToNext()) {
                MonthlyStatisticsItem item = new MonthlyStatisticsItem();
                item.month = Integer.parseInt(cursor.getString(indexMonth));
                item.year = Integer.parseInt(cursor.getString(indexYear));
                item.timePlayed = cursor.getLong(indexTotalDuration);
                months.add(item);
            }
        }
        adapter.close();
        return months;
    }

    public static class StatisticsResult {
        public List<StatisticsItem> feedTime = new ArrayList<>();
        public long oldestDate = System.currentTimeMillis();
    }

    /**
     * Searches the DB for statistics（统计所有订阅源）.
     *
     * @return The list of statistics objects
     */
    @NonNull
    public static StatisticsResult getStatistics(boolean includeMarkedAsPlayed,
                                                 long timeFilterFrom, long timeFilterTo) {
        Db adapter = Db.getInstance();
        adapter.open();

        StatisticsResult result = new StatisticsResult();
        List<Feed> feeds = getAllFeedList();
        for (Feed feed : feeds) {
            long feedPlayedTime = 0;
            long feedTotalTime = 0;
            long episodes = 0;
            long episodesStarted = 0;
            long totalDownloadSize = 0;
            long episodesDownloadCount = 0;
            List<FeedItem> items = getFeed(feed.getId()).getItems();
            for (FeedItem item : items) {
                FeedMedia media = item.getMedia();
                if (media == null) {
                    continue;
                }

                if (media.getLastPlayedTime() > 0 && media.getPlayedDuration() != 0) {
                    result.oldestDate = Math.min(result.oldestDate, media.getLastPlayedTime());
                }
                if (media.getLastPlayedTime() >= timeFilterFrom
                        && media.getLastPlayedTime() <= timeFilterTo) {
                    if (media.getPlayedDuration() != 0) {
                        feedPlayedTime += media.getPlayedDuration() / 1000;
                    } else if (includeMarkedAsPlayed && item.isPlayed()) {
                        feedPlayedTime += media.getDuration() / 1000;
                    }
                }

                boolean markedAsStarted = item.isPlayed() || media.getPosition() != 0;
                boolean hasStatistics = media.getPlaybackCompletionDate() != null || media.getPlayedDuration() > 0;
                if (hasStatistics || (includeMarkedAsPlayed && markedAsStarted)) {
                    episodesStarted++;
                }

                feedTotalTime += media.getDuration() / 1000;

                if (media.isDownloaded()) {
                    totalDownloadSize += new File(media.getFile_url()).length();
                    episodesDownloadCount++;
                }

                episodes++;
            }
            if(feedPlayedTime > 0){
                //只统计播放过的feed
                result.feedTime.add(new StatisticsItem(feed, feedTotalTime, feedPlayedTime, episodes,
                        episodesStarted, totalDownloadSize, episodesDownloadCount));
            }

        }

        adapter.close();
        return result;
    }

    /**
     * Returns data necessary for displaying the navigation drawer. This includes
     * the list of subscriptions, the number of items in the queue and the number of unread
     * items.
     *
     * @param includeNoTags true返回所有订阅源列表 false仅返回有tag的
     */
    @NonNull
    public static NavDrawerData getNavDrawerData(boolean includeNoTags) {
        Db adapter = Db.getInstance();
        adapter.open();

        final LongIntMap feedCounters = adapter.getFeedCounters(Prefs.getFeedCounterSetting());
        SubscriptionsFilter subscriptionsFilter = Prefs.getSubscriptionsFilter();
        List<Feed> feeds = subscriptionsFilter.filter(getFeedList(adapter), feedCounters);

        Comparator<Feed> comparator;
        int feedOrder = Prefs.getFeedOrder();
        String orderMethod = Prefs.getFeedOrderMethod();
        if (feedOrder == Prefs.FEED_ORDER_COUNTER) {
            //数量
            comparator = (lhs, rhs) -> {
                long counterLhs = feedCounters.get(lhs.getId());
                long counterRhs = feedCounters.get(rhs.getId());
                if (Prefs.ORDER_DESC.equals(orderMethod)) {
                    if (counterLhs > counterRhs /*&& UserPreferences.ORDER_DESC.equals(orderMethod)*/) {
                        // reverse natural order: podcast with most unplayed episodes first
                        return -1;
                    } else if (counterLhs == counterRhs) {
                        return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                    } else {
                        return 1;
                    }
                } else {
                    if (counterLhs > counterRhs /*&& UserPreferences.ORDER_DESC.equals(orderMethod)*/) {
                        // reverse natural order: podcast with most unplayed episodes first
                        return 1;
                    } else if (counterLhs == counterRhs) {
                        return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                    } else {
                        return -1;
                    }
                }

            };
        } else if (feedOrder == Prefs.FEED_ORDER_ALPHABETICAL) {
            //名称
            comparator = (lhs, rhs) -> {
                String t1 = lhs.getTitle();
                String t2 = rhs.getTitle();
                if (t1 == null) {
                    return 1;
                } else if (t2 == null) {
                    return -1;
                } else {
                    if (Prefs.ORDER_ASC.equals(orderMethod)) {
                        return t1.compareToIgnoreCase(t2);
                    } else {
                        return t2.compareToIgnoreCase(t1);
                    }

                }
            };
        } else {
            //发布时间
            final Map<Long, Long> recentPubDates = adapter.getMostRecentItemDates();
            comparator = (lhs, rhs) -> {
                long dateLhs = recentPubDates.containsKey(lhs.getId()) ? recentPubDates.get(lhs.getId()) : 0;
                long dateRhs = recentPubDates.containsKey(rhs.getId()) ? recentPubDates.get(rhs.getId()) : 0;
                if (Prefs.ORDER_ASC.equals(orderMethod)) {
                    return Long.compare(dateLhs, dateRhs);
                } else {
                    return Long.compare(dateRhs, dateLhs);
                }

            };
        }

        Collections.sort(feeds, comparator);
        int queueSize = adapter.getQueueSize();
        int numNewItems = adapter.getNumberOfNewItems();
        int numDownloadedItems = adapter.getNumberOfDownloadedEpisodes();

        List<NavDrawerData.DrawerItem> items = new ArrayList<>();
        Map<String, NavDrawerData.TagDrawerItem> folders = new HashMap<>();
        for (Feed feed : feeds) {
            for (String tag : feed.getPreferences().getTags()) {
                NavDrawerData.FeedDrawerItem drawerItem = new NavDrawerData.FeedDrawerItem(feed, feed.getId(),
                        feedCounters.get(feed.getId()));
                if (FeedPreferences.TAG_ROOT.equals(tag)) {
                    if (feed.getPreferences().getTags().size() == 1 /*没有自定义tag才显示*/ || includeNoTags) {
                        items.add(drawerItem);
                    }
                    continue;
                }
                NavDrawerData.TagDrawerItem folder;
                if (folders.containsKey(tag)) {
                    folder = folders.get(tag);
                } else {
                    folder = new NavDrawerData.TagDrawerItem(tag);
                    folders.put(tag, folder);
                }
                drawerItem.id |= folder.id;
                folder.children.add(drawerItem);
            }
        }
        List<NavDrawerData.TagDrawerItem> foldersSorted = new ArrayList<>(folders.values());
        Collections.sort(foldersSorted, (o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
        items.addAll(foldersSorted);

        NavDrawerData result = new NavDrawerData(items, queueSize, numNewItems, numDownloadedItems,
                feedCounters, Prefs.getEpisodeCleanupAlgorithm().getReclaimableItems());
        adapter.close();
        return result;
    }

    /**
     * 通过下载的id查询feed id
     * @param downloadId
     * @return
     */
    public static int getFeedIdFromDownloadLog(int downloadId) {

        Db adapter = Db.getInstance();
        adapter.open();
        int feedId = -1;
        try (Cursor cursor = adapter.getFeedIdFromDownloadLogCursor(downloadId)) {
            while (cursor.moveToNext()) {
                int indexId = cursor.getColumnIndex(Db.KEY_FEEDFILE);
                feedId = cursor.getInt(indexId);
            }
            return feedId;
        } finally {
            adapter.close();
        }
    }
}
