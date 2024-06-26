package allen.town.podcast.storage.db.mapper;

import allen.town.focus_common.util.BaseDateUtils;
import allen.town.podcast.model.feed.FeedItemFilter;
import allen.town.podcast.storage.db.Db;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class FeedItemFilterQuery {
    private FeedItemFilterQuery() {
        // Must not be instantiated
    }

    /**
     * Express the filter using an SQL boolean statement that can be inserted into an SQL WHERE clause
     * to yield output filtered according to the rules of this filter.
     *
     * @return An SQL boolean statement that matches the desired items,
     *         empty string if there is nothing to filter
     */
    public static String generateFrom(FeedItemFilter filter) {
        // The keys used within this method, but explicitly combined with their table
        String keyRead = Db.TABLE_NAME_FEED_ITEMS + "." + Db.KEY_READ;
        String keyPosition = Db.TABLE_NAME_FEED_MEDIA + "." + Db.KEY_POSITION;
        String keyDownloaded = Db.TABLE_NAME_FEED_MEDIA + "." + Db.KEY_DOWNLOADED;
        String keyMediaId = Db.TABLE_NAME_FEED_MEDIA + "." + Db.KEY_ID;
        String keyItemId = Db.TABLE_NAME_FEED_ITEMS + "." + Db.KEY_ID;
        String keyFeedItem = Db.KEY_FEEDITEM;
        String tableQueue = Db.TABLE_NAME_QUEUE;
        String tableFavorites = Db.TABLE_NAME_FAVORITES;
        String keyPubDate = Db.TABLE_NAME_FEED_ITEMS + "." + Db.KEY_PUBDATE;

        List<String> statements = new ArrayList<>();
        if (filter.showPlayed) {
            statements.add(keyRead + " = 1 ");
        } else if (filter.showUnplayed) {
            statements.add(" NOT " + keyRead + " = 1 "); // Match "New" items (read = -1) as well
        }
        if (filter.showPaused) {
            statements.add(" (" + keyPosition + " NOT NULL AND " + keyPosition + " > 0 " + ") ");
        } else if (filter.showNotPaused) {
            statements.add(" (" + keyPosition + " IS NULL OR " + keyPosition + " = 0 " + ") ");
        }
        if (filter.showQueued) {
            statements.add(keyItemId + " IN (SELECT " + keyFeedItem + " FROM " + tableQueue + ") ");
        } else if (filter.showNotQueued) {
            statements.add(keyItemId + " NOT IN (SELECT " + keyFeedItem + " FROM " + tableQueue + ") ");
        }
        if (filter.showDownloaded) {
            statements.add(keyDownloaded + " = 1 ");
        } else if (filter.showNotDownloaded) {
            statements.add(keyDownloaded + " = 0 ");
        }
        if (filter.showHasMedia) {
            statements.add(keyMediaId + " NOT NULL ");
        } else if (filter.showNoMedia) {
            statements.add(keyMediaId + " IS NULL ");
        }
        if (filter.showIsFavorite) {
            statements.add(keyItemId + " IN (SELECT " + keyFeedItem + " FROM " + tableFavorites + ") ");
        } else if (filter.showNotFavorite) {
            statements.add(keyItemId + " NOT IN (SELECT " + keyFeedItem + " FROM " + tableFavorites + ") ");
        }

        if (filter.showToday) {
            statements.add(keyPubDate + " >= " + BaseDateUtils.getToadyTime());
        } else if (filter.showNotToday) {
            statements.add(keyPubDate + " < " + BaseDateUtils.getToadyTime());
        }
        if (statements.isEmpty()) {
            return "";
        }

        StringBuilder query = new StringBuilder(" (" + statements.get(0));
        for (String r : statements.subList(1, statements.size())) {
            query.append(" AND ");
            query.append(r);
        }
        query.append(") ");
        return query.toString();
    }
}
