package allen.town.podcast.storage.db.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import allen.town.podcast.model.feed.Chapter;
import allen.town.podcast.storage.db.Db;

/**
 * Converts a {@link Cursor} to a {@link Chapter} object.
 */
public abstract class ChapterCursorMapper {
    /**
     * Create a {@link Chapter} instance from a database row (cursor).
     */
    @NonNull
    public static Chapter convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndex(Db.KEY_ID);
        int indexTitle = cursor.getColumnIndex(Db.KEY_TITLE);
        int indexStart = cursor.getColumnIndex(Db.KEY_START);
        int indexLink = cursor.getColumnIndex(Db.KEY_LINK);
        int indexImage = cursor.getColumnIndex(Db.KEY_IMAGE_URL);

        long id = cursor.getLong(indexId);
        String title = cursor.getString(indexTitle);
        long start = cursor.getLong(indexStart);
        String link = cursor.getString(indexLink);
        String imageUrl = cursor.getString(indexImage);
        Chapter chapter = new Chapter(start, title, link, imageUrl);
        chapter.setId(id);
        return chapter;
    }
}
