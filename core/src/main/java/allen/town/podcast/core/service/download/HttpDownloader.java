package allen.town.podcast.core.service.download;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import allen.town.podcast.core.util.NetworkUtils;
import allen.town.podcast.model.download.DownloadStatus;
import okhttp3.CacheControl;
import okhttp3.internal.http.StatusLine;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import allen.town.podcast.core.R;
import allen.town.podcast.model.feed.FeedMedia;
import allen.town.podcast.parser.feed.util.DateUtils;
import allen.town.podcast.model.download.DownloadError;
import allen.town.podcast.core.util.StorageUtils;
import allen.town.podcast.core.util.URIUtil;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";
    private static final int BUFFER_SIZE = 8 * 1024;

    public HttpDownloader(@NonNull DownloadRequest request) {
        super(request);
    }

    @Override
    protected void download() {
        File destination = new File(request.getDestination());
        final boolean fileExists = destination.exists();

        if (request.isDeleteOnFailure() && fileExists) {
            Log.w(TAG, "File already exists");
            onSuccess();
            return;
        }

        RandomAccessFile out = null;
        InputStream connection;
        ResponseBody responseBody = null;

        try {
            final URI uri = URIUtil.getURIFromRequestUrl(request.getSource());
            Request.Builder httpReq = new Request.Builder().url(uri.toURL());
            httpReq.tag(request);
            httpReq.cacheControl(new CacheControl.Builder().noStore().build());

            if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
                // set header explicitly so that okhttp doesn't do transparent gzip
                httpReq.addHeader("Accept-Encoding", "identity");
                httpReq.cacheControl(new CacheControl.Builder().noCache().build()); // noStore breaks CDNs
            }

            if (uri.getScheme().equals("http")) {
                httpReq.addHeader("Upgrade-Insecure-Requests", "1");
            }

            if (!TextUtils.isEmpty(request.getLastModified())) {
                String lastModified = request.getLastModified();
                Date lastModifiedDate = DateUtils.parse(lastModified);
                if (lastModifiedDate != null) {
                    long threeDaysAgo = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 3;
                    if (lastModifiedDate.getTime() > threeDaysAgo) {
                        httpReq.addHeader("If-Modified-Since", lastModified);
                    }
                } else {
                    httpReq.addHeader("If-None-Match", lastModified);
                }
            }

            // add range header if necessary
            if (fileExists && destination.length() > 0) {
                request.setSoFar(destination.length());
                httpReq.addHeader("Range", "bytes=" + request.getSoFar() + "-");
            }

            Response response = newCall(httpReq);
            responseBody = response.body();
            String contentEncodingHeader = response.header("Content-Encoding");
            boolean isGzip = false;
            if (!TextUtils.isEmpty(contentEncodingHeader)) {
                isGzip = TextUtils.equals(contentEncodingHeader.toLowerCase(), "gzip");
            }

            Log.d(TAG, "response code " + response.code());
            if (!response.isSuccessful() && response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                onCancelled();
                return;
            } else if (!response.isSuccessful() || response.body() == null) {
                callOnFailByResponseCode(response);
                return;
            } else if (!StorageUtils.storageAvailable()) {
                onFail(DownloadError.ERROR_DEVICE_NOT_FOUND, null);
                return;
            } else if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA
                    && isContentTypeTextAndSmallerThan100kb(response)) {
                onFail(DownloadError.ERROR_FILE_TYPE, null);
                return;
            }
            checkIfRedirect(response);

            connection = new BufferedInputStream(responseBody.byteStream());

            String contentRangeHeader = (fileExists) ? response.header("Content-Range") : null;
            if (fileExists && response.code() == HttpURLConnection.HTTP_PARTIAL
                    && !TextUtils.isEmpty(contentRangeHeader)) {
                String start = contentRangeHeader.substring("bytes ".length(),
                        contentRangeHeader.indexOf("-"));
                request.setSoFar(Long.parseLong(start));
                Log.d(TAG, "download from position " + request.getSoFar());

                out = new RandomAccessFile(destination, "rw");
                out.seek(request.getSoFar());
            } else {
                boolean success = destination.delete();
                success |= destination.createNewFile();
                if (!success) {
                    throw new IOException("Unable to recreate partially downloaded file");
                }
                out = new RandomAccessFile(destination, "rw");
            }

            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            request.setStatusMsg(R.string.download_running);
            request.setSize(responseBody.contentLength() + request.getSoFar());
            if (request.getSize() < 0) {
                request.setSize(DownloadStatus.SIZE_UNKNOWN);
            }

            long freeSpace = StorageUtils.getFreeSpaceAvailable();
            if (request.getSize() != DownloadStatus.SIZE_UNKNOWN && request.getSize() > freeSpace) {
                onFail(DownloadError.ERROR_NOT_ENOUGH_SPACE, null);
                return;
            }

            Log.d(TAG, "start download");
            try {
                while (!cancelled && (count = connection.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                    request.setSoFar(request.getSoFar() + count);
                    int progressPercent = (int) (100.0 * request.getSoFar() / request.getSize());
                    request.setProgressPercent(progressPercent);
                }
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            if (cancelled) {
                onCancelled();
            } else {
                // check if size specified in the response header is the same as the size of the
                // written file. This check cannot be made if compression was used
                if (!isGzip && request.getSize() != DownloadStatus.SIZE_UNKNOWN
                        && request.getSoFar() != request.getSize()) {
                    onFail(DownloadError.ERROR_IO_WRONG_SIZE, "Download completed but size: "
                            + request.getSoFar() + " does not equal expected size " + request.getSize());
                    return;
                } else if (request.getSize() > 0 && request.getSoFar() == 0) {
                    onFail(DownloadError.ERROR_IO_ERROR, "Download completed, but nothing was read");
                    return;
                }
                String lastModified = response.header("Last-Modified");
                if (lastModified != null) {
                    request.setLastModified(lastModified);
                } else {
                    request.setLastModified(response.header("ETag"));
                }
                onSuccess();
            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            onFail(DownloadError.ERROR_MALFORMED_URL, e.getMessage());
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            onFail(DownloadError.ERROR_CONNECTION_ERROR, e.getMessage());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            onFail(DownloadError.ERROR_UNKNOWN_HOST, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            if (NetworkUtils.wasDownloadBlocked(e)) {
                onFail(DownloadError.ERROR_IO_BLOCKED, e.getMessage());
                return;
            }
            String message = e.getMessage();
            if (message != null && message.contains("Trust anchor for certification path not found")) {
                onFail(DownloadError.ERROR_CERTIFICATE, e.getMessage());
                return;
            }
            onFail(DownloadError.ERROR_IO_ERROR, e.getMessage());
        } catch (NullPointerException e) {
            // might be thrown by connection.getInputStream()
            e.printStackTrace();
            onFail(DownloadError.ERROR_CONNECTION_ERROR, request.getSource());
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(responseBody);
        }
    }

    private Response newCall(Request.Builder httpReq) throws IOException {
        OkHttpClient httpClient = PodcastHttpClient.getHttpClient();
        try {
            return httpClient.newCall(httpReq.build()).execute();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            if (e.getMessage() != null && e.getMessage().contains("PROTOCOL_ERROR")) {
                // Apparently some servers announce they support SPDY but then actually don't.
                httpClient = httpClient.newBuilder()
                        .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                        .build();
                return httpClient.newCall(httpReq.build()).execute();
            } else {
                throw e;
            }
        }
    }

    private boolean isContentTypeTextAndSmallerThan100kb(Response response) {
        int contentLength = -1;
        String contentLen = response.header("Content-Length");
        if (contentLen != null) {
            try {
                contentLength = Integer.parseInt(contentLen);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        String contentType = response.header("Content-Type");
        return contentType != null && contentType.startsWith("text/") && contentLength < 100 * 1024;
    }

    private void callOnFailByResponseCode(Response response) {
        final DownloadError error;
        final String details;
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            error = DownloadError.ERROR_UNAUTHORIZED;
            details = String.valueOf(response.code());
        } else if (response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
            error = DownloadError.ERROR_FORBIDDEN;
            details = String.valueOf(response.code());
        } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            error = DownloadError.ERROR_NOT_FOUND;
            details = String.valueOf(response.code());
        } else {
            error = DownloadError.ERROR_HTTP_DATA_ERROR;
            details = String.valueOf(response.code());
        }
        onFail(error, details);
    }

    private void checkIfRedirect(Response response) {
        // detect 301 Moved permanently and 308 Permanent Redirect
        ArrayList<Response> responses = new ArrayList<>();
        while (response != null) {
            responses.add(response);
            response = response.priorResponse();
        }
        if (responses.size() < 2) {
            return;
        }
        Collections.reverse(responses);
        int firstCode = responses.get(0).code();
        String firstUrl = responses.get(0).request().url().toString();
        String secondUrl = responses.get(1).request().url().toString();
        if (firstCode == HttpURLConnection.HTTP_MOVED_PERM || firstCode == StatusLine.HTTP_PERM_REDIRECT) {
            permanentRedirectUrl = secondUrl;
        } else if (secondUrl.equals(firstUrl.replace("http://", "https://"))) {
            permanentRedirectUrl = secondUrl;
        }
    }

    private void onSuccess() {
        Log.d(TAG, "download successful");
        result.setSuccessful();
    }

    private void onFail(DownloadError reason, String reasonDetailed) {
        Log.d(TAG, "failed "  + reason +   reasonDetailed);
        result.setFailed(reason, reasonDetailed);
        if (request.isDeleteOnFailure()) {
            cleanup();
        }
    }

    private void onCancelled() {
        Log.d(TAG, "download cancelled");
        result.setCancelled();
        cleanup();
    }

    /**
     * Deletes unfinished downloads.
     */
    private void cleanup() {
        if (request.getDestination() != null) {
            File dest = new File(request.getDestination());
            if (dest.exists()) {
                boolean rc = dest.delete();
                Log.d(TAG, "delete file " + dest.getName());
            } else {
                Log.d(TAG, "file does not exist");
            }
        }
    }
}
