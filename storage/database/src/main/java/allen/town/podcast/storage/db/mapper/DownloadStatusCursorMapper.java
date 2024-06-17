package allen.town.podcast.storage.db.mapper;

import android.database.Cursor;
import androidx.annotation.NonNull;
import allen.town.podcast.model.download.DownloadStatus;
import allen.town.podcast.model.download.DownloadError;
import allen.town.podcast.storage.db.Db;

import java.util.Date;

/**
 * Converts a {@link Cursor} to a {@link DownloadStatus} object.
 */
public abstract class DownloadStatusCursorMapper {
    /**
     * Create a {@link DownloadStatus} instance from a database row (cursor).
     */
    @NonNull
    public static DownloadStatus convert(@NonNull Cursor cursor) {
        int indexId = cursor.getColumnIndex(Db.KEY_ID);
        int indexTitle = cursor.getColumnIndex(Db.KEY_DOWNLOADSTATUS_TITLE);
        int indexFeedFile = cursor.getColumnIndex(Db.KEY_FEEDFILE);
        int indexFileFileType = cursor.getColumnIndex(Db.KEY_FEEDFILETYPE);
        int indexSuccessful = cursor.getColumnIndex(Db.KEY_SUCCESSFUL);
        int indexReason = cursor.getColumnIndex(Db.KEY_REASON);
        int indexCompletionDate = cursor.getColumnIndex(Db.KEY_COMPLETION_DATE);
        int indexReasonDetailed = cursor.getColumnIndex(Db.KEY_REASON_DETAILED);

        return new DownloadStatus(cursor.getLong(indexId), cursor.getString(indexTitle), cursor.getLong(indexFeedFile),
                cursor.getInt(indexFileFileType), cursor.getInt(indexSuccessful) > 0, false, true,
                DownloadError.fromCode(cursor.getInt(indexReason)),
                new Date(cursor.getLong(indexCompletionDate)),
                cursor.getString(indexReasonDetailed), false);
    }
}
