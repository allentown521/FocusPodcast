package allen.town.podcast.core.service.download;

import java.io.UnsupportedEncodingException;

import okio.ByteString;

public abstract class HttpCredentialEncoder {
    public static String encode(String username, String password, String charset) {
        try {
            String credentials = username + ":" + password;
            byte[] bytes = credentials.getBytes(charset);
            String encoded = ByteString.of(bytes).base64();
            return "Basic " + encoded;
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
