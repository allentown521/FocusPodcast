package allen.town.podcast.core.util;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import allen.town.podcast.core.BuildConfig;

/**
 * Utility methods for dealing with URL encoding.
 */
public class URIUtil {
    private static final String TAG = "URIUtil";

    private URIUtil() {}

    public static URI getURIFromRequestUrl(String source) {
        // try without encoding the URI
        try {
            return new URI(source);
        } catch (URISyntaxException e) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Source is not encoded, encoding now");
        }
        try {
            URL url = new URL(source);
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
