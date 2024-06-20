package allen.town.podcast.core.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import allen.town.podcast.core.service.download.DownloadRequest;
import allen.town.podcast.core.service.download.DownloadService;
import allen.town.podcast.core.service.download.DownloadRequestCreator;
import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import allen.town.podcast.core.export.opml.OpmlElement;
import allen.town.podcast.core.export.opml.OpmlReader;
import allen.town.podcast.core.export.opml.OpmlWriter;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.core.storage.DBReader;

public class OPMLBackupAgent extends BackupAgentHelper {
    private static final String OPML_BACKUP_KEY = "opml";

    @Override
    public void onCreate() {
        addHelper(OPML_BACKUP_KEY, new OpmlBackupHelper(this));
    }

    /**
     * Class for backing up and restoring the OPML file.
     */
    private static class OpmlBackupHelper implements BackupHelper {
        private static final String TAG = "OpmlBackupHelper";

        private static final String OPML_ENTITY_KEY = "FocusPodcastFeeds.opml";

        private final Context mContext;

        /**
         * Checksum of restored OPML file
         */
        private byte[] mChecksum;

        public OpmlBackupHelper(Context context) {
            mContext = context;
        }

        @Override
        public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
            Log.d(TAG, "start backup");
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            MessageDigest digester = null;
            Writer writer;

            try {
                digester = MessageDigest.getInstance("MD5");
                writer = new OutputStreamWriter(new DigestOutputStream(byteStream, digester),
                        Charset.forName("UTF-8"));
            } catch (NoSuchAlgorithmException e) {
                writer = new OutputStreamWriter(byteStream, Charset.forName("UTF-8"));
            }

            try {
                // Write OPML
                new OpmlWriter().writeDocument(DBReader.getFeedList(), writer, mContext);

                // Compare checksum of new and old file to see if we need to perform a backup at all
                if (digester != null) {
                    byte[] newChecksum = digester.digest();

                    // Get the old checksum
                    if (oldState != null) {
                        FileInputStream inState = new FileInputStream(oldState.getFileDescriptor());
                        int len = inState.read();

                        if (len != -1) {
                            byte[] oldChecksum = new byte[len];
                            IOUtils.read(inState, oldChecksum, 0, len);

                            if (Arrays.equals(oldChecksum, newChecksum)) {
                                return;
                            }
                        }
                    }

                    writeNewStateDescription(newState, newChecksum);
                }

                Log.d(TAG, "backing up");
                byte[] bytes = byteStream.toByteArray();
                data.writeEntityHeader(OPML_ENTITY_KEY, bytes.length);
                data.writeEntityData(bytes, bytes.length);
            } catch (IOException e) {
                Log.e(TAG, "Error during backup", e);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }

        @Override
        public void restoreEntity(BackupDataInputStream data) {
            Log.d(TAG, "restore");

            if (!OPML_ENTITY_KEY.equals(data.getKey())) {
                Log.d(TAG, "unknown entity key -> " + data.getKey());
                return;
            }

            MessageDigest digester = null;
            Reader reader;

            try {
                digester = MessageDigest.getInstance("MD5");
                reader = new InputStreamReader(new DigestInputStream(data, digester),
                        Charset.forName("UTF-8"));
            } catch (NoSuchAlgorithmException e) {
                reader = new InputStreamReader(data, Charset.forName("UTF-8"));
            }

            try {
                ArrayList<OpmlElement> opmlElements = new OpmlReader().readDocument(reader);
                mChecksum = digester == null ? null : digester.digest();
                for (OpmlElement opmlElem : opmlElements) {
                    Feed feed = new Feed(opmlElem.getXmlUrl(), null, opmlElem.getText());
                    feed.setNeedAutoSubscribe(true);
                    DownloadRequest request = DownloadRequestCreator.create(feed).build();
                    DownloadService.download(mContext, false, request);
                }
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error while parsing the OPML file", e);
            } catch (IOException e) {
                Log.e(TAG, "Failed to restore OPML backup", e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        @Override
        public void writeNewStateDescription(ParcelFileDescriptor newState) {
            writeNewStateDescription(newState, mChecksum);
        }

        /**
         * Writes the new state description, which is the checksum of the OPML file.
         *
         * @param newState
         * @param checksum
         */
        private void writeNewStateDescription(ParcelFileDescriptor newState, byte[] checksum) {
            if (checksum == null) {
                return;
            }

            try {
                FileOutputStream outState = new FileOutputStream(newState.getFileDescriptor());
                outState.write(checksum.length);
                outState.write(checksum);
                outState.flush();
                outState.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to write new state description", e);
            }
        }
    }
}
