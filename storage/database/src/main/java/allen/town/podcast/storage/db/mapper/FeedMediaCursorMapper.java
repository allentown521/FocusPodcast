package allen.town.podcast.storage.db.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.storage.db.Db;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link FeedMedia} object.
 */
public abstract class FeedMediaCursorMapper {
    /**
     * Create a {@link FeedMedia} instance from a database row (cursor).
     */
    @NonNull
    public static FeedMedia convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndexOrThrow(Db.SELECT_KEY_MEDIA_ID);
        int indexPlaybackCompletionDate = cursor.getColumnIndexOrThrow(Db.KEY_PLAYBACK_COMPLETION_DATE);
        int indexDuration = cursor.getColumnIndexOrThrow(Db.KEY_DURATION);
        int indexPosition = cursor.getColumnIndexOrThrow(Db.KEY_POSITION);
        int indexSize = cursor.getColumnIndexOrThrow(Db.KEY_SIZE);
        int indexMimeType = cursor.getColumnIndexOrThrow(Db.KEY_MIME_TYPE);
        int indexFileUrl = cursor.getColumnIndexOrThrow(Db.KEY_FILE_URL);
        int indexDownloadUrl = cursor.getColumnIndexOrThrow(Db.KEY_DOWNLOAD_URL);
        int indexDownloaded = cursor.getColumnIndexOrThrow(Db.KEY_DOWNLOADED);
        int indexPlayedDuration = cursor.getColumnIndexOrThrow(Db.KEY_PLAYED_DURATION);
        int indexLastPlayedTime = cursor.getColumnIndexOrThrow(Db.KEY_LAST_PLAYED_TIME);

        long mediaId = cursor.getLong(indexId);
        Date playbackCompletionDate = null;
        long playbackCompletionTime = cursor.getLong(indexPlaybackCompletionDate);
        if (playbackCompletionTime > 0) {
            playbackCompletionDate = new Date(playbackCompletionTime);
        }

        Boolean hasEmbeddedPicture;
        switch (cursor.getInt(cursor.getColumnIndexOrThrow(Db.KEY_HAS_EMBEDDED_PICTURE))) {
            case 1:
                hasEmbeddedPicture = Boolean.TRUE;
                break;
            case 0:
                hasEmbeddedPicture = Boolean.FALSE;
                break;
            default:
                hasEmbeddedPicture = null;
                break;
        }

        return new FeedMedia(
                mediaId,
                null,
                cursor.getInt(indexDuration),
                cursor.getInt(indexPosition),
                cursor.getLong(indexSize),
                cursor.getString(indexMimeType),
                cursor.getString(indexFileUrl),
                cursor.getString(indexDownloadUrl),
                cursor.getInt(indexDownloaded) > 0,
                playbackCompletionDate,
                cursor.getInt(indexPlayedDuration),
                hasEmbeddedPicture,
                cursor.getLong(indexLastPlayedTime)
        );
    }
}
